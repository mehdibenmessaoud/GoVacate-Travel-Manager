package tn.esprit.projet.gui;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import tn.esprit.projet.services.ReservationService;
import tn.esprit.projet.services.ReservationService.ReservationDetail;
import tn.esprit.projet.utils.DialogHelper;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Admin sub-controller for the Réservations view.
 * – Search is reactive and driven by a single listener (no duplicate registrations).
 * – Status changes are persisted to the DB via ReservationService.updateStatus().
 * – Delete removes the row from DB then refreshes the table.
 */
public class AdminReservationViewController {

    private final AdminController3 admin;
    private final ReservationService reservationService;

    private List<ReservationDetail>            masterList     = Collections.emptyList();
    private final ObservableList<ReservationDetail> tableData = FXCollections.observableArrayList();

    private TableView<Object> boundTable;
    private TextField         boundSearch;
    private ComboBox<String>  boundHotelFilter;
    private ComboBox<String>  boundStatusFilter;
    private Label             boundSubtitle;
    private boolean           listenersWired = false;

    public AdminReservationViewController(AdminController3 admin) {
        this.admin              = admin;
        this.reservationService = new ReservationService();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // TABLE SETUP
    // ═══════════════════════════════════════════════════════════════════════

    @SuppressWarnings("unchecked")
    public void setupReservationsTable(TableView<Object> mainTable,
                                       TableColumn<Object, String> col1,
                                       TableColumn<Object, String> col2,
                                       TableColumn<Object, String> col3,
                                       TableColumn<Object, String> col4,
                                       TableColumn<Object, String> col5,
                                       TableColumn<Object, Object> col6) {

        col1.setVisible(true); col2.setVisible(true); col3.setVisible(true);
        col4.setVisible(true); col5.setVisible(true); col6.setVisible(true);

        // Unbind prefWidth — CONSTRAINED_RESIZE_POLICY binds it to the table width
        for (TableColumn<?, ?> c : new TableColumn<?, ?>[]{col1, col2, col3, col4, col5, col6})
            c.prefWidthProperty().unbind();

        col1.setText("ID Rés.");          col1.setMinWidth(80);  col1.setPrefWidth(80);
        col2.setText("Chambre");          col2.setMinWidth(150); col2.setPrefWidth(160);
        col3.setText("Hôtel");            col3.setMinWidth(200); col3.setPrefWidth(210);
        col4.setText("Séjour");           col4.setMinWidth(220); col4.setPrefWidth(240);
        col5.setText("Total (DT)");       col5.setMinWidth(110); col5.setPrefWidth(110);
        col6.setText("Statut / Actions"); col6.setMinWidth(300); col6.setPrefWidth(320);

        col1.setCellValueFactory(data ->
                data.getValue() instanceof ReservationDetail d
                        ? new SimpleStringProperty(String.valueOf(d.id))
                        : new SimpleStringProperty(""));
        col2.setCellValueFactory(data ->
                data.getValue() instanceof ReservationDetail d
                        ? new SimpleStringProperty("Ch. " + d.safeRoomNumber() + "  ·  " + d.safeRoomType())
                        : new SimpleStringProperty(""));
        col3.setCellValueFactory(data ->
                data.getValue() instanceof ReservationDetail d
                        ? new SimpleStringProperty(d.safeHotelName())
                        : new SimpleStringProperty(""));
        col4.setCellValueFactory(data ->
                data.getValue() instanceof ReservationDetail d
                        ? new SimpleStringProperty(d.dateRange() + "  (" + d.nights() + " nuit" + (d.nights() > 1 ? "s" : "") + ")")
                        : new SimpleStringProperty(""));
        col5.setCellValueFactory(data ->
                data.getValue() instanceof ReservationDetail d
                        ? new SimpleStringProperty(String.format("%.0f DT", d.prix))
                        : new SimpleStringProperty(""));

        col6.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue()));
        col6.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || !(item instanceof ReservationDetail d)) { setGraphic(null); return; }
                HBox cell = new HBox(8);
                cell.setAlignment(Pos.CENTER_LEFT);
                cell.getChildren().addAll(buildStatusBadge(d.status), buildActionButtons(d));
                setGraphic(cell);
            }
        });

        mainTable.setItems((ObservableList<Object>)(ObservableList<?>) tableData);
        mainTable.setPlaceholder(buildEmptyPlaceholder());
        this.boundTable = mainTable;
    }

    /** Called by AdminController when the user types in the shared search field. */
    public void refreshFilter() {
        applyFilter();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // DATA LOAD
    // ═══════════════════════════════════════════════════════════════════════

    public void loadReservationsTable(TableView<Object> mainTable,
                                      TextField searchField,
                                      ComboBox<String> hotelFilter,
                                      ComboBox<String> statusFilter,
                                      Label pageSubtitle) {
        this.boundSearch      = searchField;
        this.boundHotelFilter = hotelFilter;
        this.boundStatusFilter= statusFilter;
        this.boundSubtitle    = pageSubtitle;

        try {
            masterList = reservationService.getAllWithDetails();
        } catch (SQLException e) {
            masterList = Collections.emptyList();
            pageSubtitle.setText("Erreur de chargement : " + e.getMessage());
        }

        updateSubtitle();
        applyFilter();

        // Wire listeners only once — prevents stacking on repeated page visits
        if (!listenersWired) {
            searchField.textProperty().addListener((obs, o, n) -> applyFilter());
            if (hotelFilter != null) hotelFilter.valueProperty().addListener((obs, o, n) -> applyFilter());
            if (statusFilter != null) statusFilter.valueProperty().addListener((obs, o, n) -> applyFilter());
            listenersWired = true;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // FILTER
    // ═══════════════════════════════════════════════════════════════════════

    private void applyFilter() {
        String q = boundSearch == null || boundSearch.getText() == null
                ? "" : boundSearch.getText().trim().toLowerCase();

        String hotelFilter = boundHotelFilter == null ? null : boundHotelFilter.getValue();
        boolean allHotels  = hotelFilter == null || hotelFilter.isBlank() || hotelFilter.startsWith("Tous");

        String statusFilter = boundStatusFilter == null ? null : boundStatusFilter.getValue();
        boolean allStatuses = statusFilter == null || statusFilter.isBlank() || statusFilter.equals("Tous");

        List<ReservationDetail> filtered = masterList.stream()
                .filter(d -> q.isEmpty()
                        || d.safeRoomNumber().toLowerCase().contains(q)
                        || d.safeHotelName().toLowerCase().contains(q)
                        || d.safeRoomType().toLowerCase().contains(q)
                        || String.valueOf(d.id).contains(q)
                        || d.status.toLowerCase().contains(q))
                .filter(d -> allHotels || d.safeHotelName().equalsIgnoreCase(hotelFilter))
                .filter(d -> allStatuses || d.status.equalsIgnoreCase(statusFilter))
                .collect(Collectors.toList());

        tableData.setAll(filtered);
    }

    private void updateSubtitle() {
        if (boundSubtitle == null) return;
        long total     = masterList.size();
        long pending   = countByStatus(masterList, "EN_ATTENTE");
        long confirmed = countByStatus(masterList, "CONFIRMÉE");
        boundSubtitle.setText(total == 0
                ? "Aucune réservation enregistrée"
                : total + " réservation" + (total > 1 ? "s" : "") +
                "  ·  " + pending + " en attente" +
                "  ·  " + confirmed + " confirmée(s)");
    }

    // ═══════════════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════════════

    private Label buildStatusBadge(String status) {
        String text; String cssVariant;
        switch (status) {
            case "CONFIRMÉE" -> {
                text = "Confirmée";
                cssVariant = "badge-res-confirmed";
            }
            case "ANNULÉE" -> {
                text = "Annulée";
                cssVariant = "badge-res-cancelled";
            }
            default -> {
                text = "En attente";
                cssVariant = "badge-res-pending";
            }
        }
        Label b = new Label(text);
        b.getStyleClass().addAll("badge-res-base", cssVariant);
        b.setMinWidth(90);
        return b;
    }

    private HBox buildActionButtons(ReservationDetail d) {
        HBox box = new HBox(6);
        box.setAlignment(Pos.CENTER_LEFT);
        switch (d.status) {
            case "EN_ATTENTE" -> {
                Button confirm = actionBtn("Confirmer ✓", "#28a745", "#1e7e34");
                confirm.setOnAction(e -> changeStatus(d, "CONFIRMÉE"));
                Button cancel = actionBtn("Annuler ✕", "#dc3545", "#c82333");
                cancel.setOnAction(e -> changeStatus(d, "ANNULÉE"));
                box.getChildren().addAll(confirm, cancel);
            }
            case "CONFIRMÉE" -> {
                Button cancel = actionBtn("Annuler", "#dc3545", "#c82333");
                cancel.setOnAction(e -> changeStatus(d, "ANNULÉE"));
                Button del = actionBtn("Supprimer", "#6c757d", "#5a6268");
                del.setOnAction(e -> deleteRow(d));
                box.getChildren().addAll(cancel, del);
            }
            default -> {
                Button reopen = actionBtn("Réactiver", "#FF8210", "#e0730e");
                reopen.setOnAction(e -> changeStatus(d, "EN_ATTENTE"));
                Button del = actionBtn("Supprimer 🗑", "#6c757d", "#5a6268");
                del.setOnAction(e -> deleteRow(d));
                box.getChildren().addAll(reopen, del);
            }
        }
        return box;
    }

    private void changeStatus(ReservationDetail d, String newStatus) {
        try {
            reservationService.updateStatus(d.id, newStatus);
            d.status = newStatus;
            updateSubtitle();
            if (boundTable != null) boundTable.refresh();
        } catch (SQLException ex) {
            DialogHelper.showNotification("Erreur : " + ex.getMessage(), "error", admin.getClass());
        }
    }

    private void deleteRow(ReservationDetail d) {
        DialogHelper.confirmDelete("réservation", "ID " + d.id, () -> {
            try {
                reservationService.delete(d.id);
                masterList = masterList.stream().filter(r -> r.id != d.id).collect(Collectors.toList());
                applyFilter();
                updateSubtitle();
                DialogHelper.showNotification("Réservation supprimée.", "success", admin.getClass());
            } catch (SQLException ex) {
                DialogHelper.showNotification("Erreur : " + ex.getMessage(), "error", admin.getClass());
            }
        }, admin.getClass());
    }

    private Button actionBtn(String label, String bg, String hover) {
        Button btn = new Button(label);
        btn.getStyleClass().add("gv-res-action-btn");
        btn.setStyle("-fx-background-color:" + bg + ";");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color:" + hover + ";"));
        btn.setOnMouseExited(e  -> btn.setStyle("-fx-background-color:" + bg + ";"));
        return btn;
    }

    private Node buildEmptyPlaceholder() {
        Label lbl = new Label("Aucune réservation enregistrée\nLes réservations effectuées côté client apparaîtront ici.");
        lbl.getStyleClass().add("gv-res-empty-placeholder");
        lbl.setAlignment(Pos.CENTER);
        return lbl;
    }

    private long countByStatus(List<ReservationDetail> list, String status) {
        return list.stream().filter(d -> status.equals(d.status)).count();
    }
}