package tn.esprit.projet.gui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import tn.esprit.projet.services.ReservationService;
import tn.esprit.projet.services.ReservationService.ReservationDetail;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Renders the "Mes Réservations" page.
 * Loads data from the reservation_hotel DB table (via ReservationService).
 */
public class ClientReservationViewController {

    private final ClientController3 controller;
    private final ReservationService reservationService;

    /** In-memory list loaded from DB – status changes live here during session. */
    private List<ReservationDetail> sessionReservations = new ArrayList<>();

    public ClientReservationViewController(ClientController3 controller) {
        this.controller         = controller;
        this.reservationService = new ReservationService();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Main view
    // ─────────────────────────────────────────────────────────────────────────

    public void showReservations() {
        // ── Show & configure the shared search panel (same as Hôtels / Chambres) ──
        controller.showSearchPanel(true);
        controller.hideApiToolbar(); // Hide the API toolbar (Destination/Ville + Geoapify) — not needed for Reservations

        // Configure search panel labels
        javafx.scene.control.Label searchTitle = controller.getSearchTitle();
        javafx.scene.control.Label subtitleLabel = controller.getSubtitleLabel();
        javafx.scene.control.TextField searchField = controller.getClientSearchField();
        javafx.scene.control.ComboBox<String> statusCombo = controller.getRoomStatusFilterCombo();
        javafx.scene.control.ComboBox<String> roomTypeCombo = controller.getRoomTypeFilterCombo();

        if (searchTitle   != null) searchTitle.setText("Rechercher une réservation");
        if (subtitleLabel != null) subtitleLabel.setText("Gérez vos réservations");
        if (searchField   != null) { searchField.setPromptText("🔍  Hôtel, chambre, type..."); searchField.clear(); }
        controller.getSectionTitle().setText("Mes Réservations");

        // Hide stars/hotel combos, show status filter in the extra-filter slots
        controller.hideStarsFilter();
        controller.hideHotelFilter();
        controller.showReservationFilters(); // configures statusCombo + hides roomType

        // Load from DB
        try {
            sessionReservations = reservationService.getAllWithDetails();
        } catch (Exception e) {
            sessionReservations = new java.util.ArrayList<>();
        }

        // ── Build the content area ─────────────────────────────────────────
        FlowPane container = controller.getHotelsContainer();
        container.getChildren().clear();

        VBox root = new VBox(24);
        root.setPadding(new javafx.geometry.Insets(6, 10, 20, 10));
        root.setMaxWidth(Double.MAX_VALUE);

        // Header
        VBox header = new VBox(4);
        Label agency = new Label("TRAVEL AGENCY");
        agency.getStyleClass().add("res-agency-label");
        Label title = new Label("Mes Réservations");
        title.getStyleClass().add("res-page-title");
        Label countLabel = new Label(sessionReservations.size() + " réservation(s) trouvée(s)");
        countLabel.getStyleClass().add("res-count-label");
        header.getChildren().addAll(agency, title, countLabel);

        // Table — wired to the shared search panel field and status combo
        VBox tableWrap = buildReservationTable(sessionReservations, searchField, statusCombo, countLabel);

        root.getChildren().addAll(header, tableWrap);
        container.getChildren().add(root);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Builders
    // ─────────────────────────────────────────────────────────────────────────

    private TextField buildSearchField() {
        TextField f = new TextField();
        f.setPromptText("🔍  Rechercher...");
        f.getStyleClass().add("res-search-field");
        f.setPrefWidth(280);
        return f;
    }

    private ComboBox<String> buildStatusFilter() {
        ComboBox<String> cb = new ComboBox<>();
        cb.getItems().addAll("Tous les statuts", "En attente", "Confirmée", "Annulée");
        cb.setValue("Tous les statuts");
        cb.getStyleClass().add("res-status-filter");
        cb.setPrefWidth(180);
        return cb;
    }

    private VBox buildReservationTable(List<ReservationDetail> all,
                                       javafx.scene.control.TextField searchField,
                                       javafx.scene.control.ComboBox<String> statusFilter,
                                       Label countLabel) {
        VBox wrap = new VBox(0);
        wrap.getStyleClass().add("res-table-wrap");
        wrap.setMaxWidth(Double.MAX_VALUE);

        wrap.getChildren().add(buildColHeader());

        Region headerDiv = new Region();
        headerDiv.setMinHeight(1); headerDiv.setPrefHeight(1); headerDiv.setMaxHeight(1);
        headerDiv.getStyleClass().add("res-header-divider");
        wrap.getChildren().add(headerDiv);

        VBox rows = new VBox(0);
        rows.setPadding(new Insets(6, 0, 6, 0));

        Runnable render = () -> {
            int count = renderRows(rows, all, searchField, statusFilter);
            if (countLabel != null) countLabel.setText(count + " réservation(s) trouvée(s)");
        };
        if (searchField  != null) searchField.textProperty().addListener((obs, o, n) -> render.run());
        if (statusFilter != null) statusFilter.valueProperty().addListener((obs, o, n) -> render.run());
        render.run();

        wrap.getChildren().add(rows);
        return wrap;
    }

    private int renderRows(VBox rows, List<ReservationDetail> all,
                           javafx.scene.control.TextField searchField,
                           javafx.scene.control.ComboBox<String> statusFilter) {
        rows.getChildren().clear();
        String search = (searchField == null || searchField.getText() == null)
                ? "" : searchField.getText().toLowerCase().trim();
        String status = (statusFilter == null) ? "Tous les statuts" : statusFilter.getValue();
        boolean first = true;

        List<ReservationDetail> filtered = all.stream()
                .filter(b -> {
                    boolean matchSearch = search.isEmpty()
                            || b.safeRoomNumber().toLowerCase().contains(search)
                            || b.safeHotelName().toLowerCase().contains(search)
                            || b.safeRoomType().toLowerCase().contains(search);
                    // Map friendly label → DB status value
                    String dbStatus = switch (status == null ? "" : status) {
                        case "En attente"  -> "EN_ATTENTE";
                        case "Confirmée"   -> "CONFIRMÉE";
                        case "Annulée"     -> "ANNULÉE";
                        default            -> "";  // "Tous les statuts"
                    };
                    boolean matchStatus = dbStatus.isEmpty() || dbStatus.equals(b.status);
                    return matchSearch && matchStatus;
                })
                .collect(Collectors.toList());

        for (ReservationDetail b : filtered) {
            if (!first) {
                Region div = new Region();
                div.setMinHeight(1); div.setPrefHeight(1); div.setMaxHeight(1);
                div.getStyleClass().add("res-row-divider");
                div.setMaxWidth(Double.MAX_VALUE);
                rows.getChildren().add(div);
            }
            Runnable refresh = () -> renderRows(rows, all, searchField, statusFilter);
            rows.getChildren().add(buildRow(b, refresh));
            first = false;
        }

        if (rows.getChildren().isEmpty()) {
            Label empty = new Label("Aucune réservation trouvée");
            empty.getStyleClass().add("res-empty");
            empty.setMaxWidth(Double.MAX_VALUE);
            empty.setAlignment(Pos.CENTER);
            rows.getChildren().add(empty);
        }
        return filtered.size();
    }

    private HBox buildColHeader() {
        HBox row = new HBox(0);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(14, 22, 14, 22));
        row.getStyleClass().add("res-col-header");
        row.getChildren().addAll(
                colLabel("CHAMBRE",   170),
                colLabel("HÔTEL",     200),
                colLabel("SÉJOUR",    200),
                colLabel("TOTAL (DT)",110),
                colLabel("STATUT",    120),
                colLabel("ACTIONS",   160)
        );
        return row;
    }

    private HBox buildRow(ReservationDetail b, Runnable refresh) {
        HBox row = new HBox(0);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(16, 22, 16, 22));

        // Room cell
        VBox roomCell = new VBox(2);
        Label roomNum = new Label("Ch. " + b.safeRoomNumber());
        roomNum.getStyleClass().add("res-room-num");
        Label roomType = new Label(b.safeRoomType());
        roomType.getStyleClass().add("res-room-type");
        roomCell.getChildren().addAll(roomNum, roomType);
        roomCell.setMinWidth(170); roomCell.setPrefWidth(170);

        // Hotel
        Label hotelLbl = new Label(b.safeHotelName());
        hotelLbl.getStyleClass().add("res-hotel-name");
        hotelLbl.setWrapText(false);
        hotelLbl.setMinWidth(200); hotelLbl.setPrefWidth(200); hotelLbl.setMaxWidth(200);

        // Date cell
        VBox dateCell = new VBox(2);
        Label dateRange = new Label(b.dateRange());
        dateRange.getStyleClass().add("res-date-range");
        long nights = b.nights();
        Label nightsLbl = new Label(nights + " nuit" + (nights > 1 ? "s" : ""));
        nightsLbl.getStyleClass().add("res-nights");
        dateCell.getChildren().addAll(dateRange, nightsLbl);
        dateCell.setMinWidth(200); dateCell.setPrefWidth(200);

        // Price
        Label priceLbl = new Label(String.format("%.0f DT", b.prix));
        priceLbl.getStyleClass().add("res-price");
        priceLbl.setMinWidth(110); priceLbl.setPrefWidth(110);

        row.getChildren().addAll(roomCell, hotelLbl, dateCell, priceLbl,
                buildStatusCell(b.status), buildActionCell(b, refresh));

        row.getStyleClass().add("res-row");
        row.setOnMouseEntered(e -> { if (!row.getStyleClass().contains("res-row-hover")) row.getStyleClass().add("res-row-hover"); });
        row.setOnMouseExited(e  -> row.getStyleClass().remove("res-row-hover"));
        return row;
    }

    private HBox buildStatusCell(String status) {
        String cssVariant = switch (status) {
            case "CONFIRMÉE" -> "res-badge-confirmed";
            case "ANNULÉE"   -> "res-badge-cancelled";
            default          -> "res-badge-pending";
        };
        Label badge = new Label(status);
        badge.getStyleClass().addAll("res-badge-base", cssVariant);
        HBox cell = new HBox(badge);
        cell.setAlignment(Pos.CENTER_LEFT);
        cell.setMinWidth(120); cell.setPrefWidth(120);
        return cell;
    }

    private HBox buildActionCell(ReservationDetail b, Runnable refresh) {
        HBox cell = new HBox(8);
        cell.setAlignment(Pos.CENTER_LEFT);
        cell.setMinWidth(160); cell.setPrefWidth(160);

        switch (b.status) {
            case "EN_ATTENTE" -> {
                // Client can only cancel a pending reservation — confirmation is admin's role
                Button cancel = actionBtn("Annuler ✕", "#dc3545", "#c82333");
                cancel.setOnAction(e -> {
                    try {
                        reservationService.updateStatus(b.id, "ANNULÉE");
                        b.status = "ANNULÉE";
                    } catch (SQLException ex) { /* silent — status still updates in memory */ }
                    refresh.run();
                });
                cell.getChildren().add(cancel);
            }
            case "CONFIRMÉE" -> {
                // Confirmed — read-only badge, no actions for client
                Label confirmed = new Label("Confirmée ✓");
                confirmed.getStyleClass().add("res-confirmed-label");
                cell.getChildren().add(confirmed);
            }
            default -> { // ANNULÉE
                // Can remove a cancelled reservation from their list
                Button del = actionBtn("Supprimer 🗑", "#6c757d", "#5a6268");
                del.setOnAction(e -> {
                    try { reservationService.delete(b.id); } catch (SQLException ex) { /* log */ }
                    sessionReservations.remove(b);
                    refresh.run();
                });
                cell.getChildren().add(del);
            }
        }
        return cell;
    }

    private Button actionBtn(String label, String bg, String hoverBg) {
        Button btn = new Button(label);
        btn.getStyleClass().add("res-action-btn");
        btn.setStyle("-fx-background-color:" + bg + ";");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color:" + hoverBg + ";"));
        btn.setOnMouseExited(e  -> btn.setStyle("-fx-background-color:" + bg + ";"));
        return btn;
    }

    private Label colLabel(String text, double width) {
        Label lbl = new Label(text);
        lbl.getStyleClass().add("res-col-label");
        lbl.setMinWidth(width); lbl.setPrefWidth(width);
        return lbl;
    }
}