package tn.esprit.projet.gui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import tn.esprit.projet.services.ReservationService;
import tn.esprit.projet.services.ReservationService.ReservationDetail;
import tn.esprit.projet.utils.SessionManager;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Renders the "Réservation Chambres" page (hotel module, client side).
 * Shows only the reservations of the currently logged-in user.
 */
public class ClientReservationViewController {

    private final ClientController3 controller;
    private final ReservationService reservationService;

    private List<ReservationDetail> sessionReservations = new ArrayList<>();

    public ClientReservationViewController(ClientController3 controller) {
        this.controller         = controller;
        this.reservationService = new ReservationService();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Main view
    // ─────────────────────────────────────────────────────────────────────────

    public void showReservations() {
        controller.showSearchPanel(false);

        ComboBox<String> statusCombo = controller.getRoomStatusFilterCombo();
        TextField searchField = controller.getClientSearchField();

        controller.hideStarsFilter();
        controller.hideHotelFilter();
        controller.showReservationFilters();

        // Load only the current user's reservations
        try {
            int userId = SessionManager.getCurrentUserId();
            System.out.println("[ClientReservationView] loading for userId=" + userId);
            if (userId > 0) {
                sessionReservations = reservationService.getByUserIdWithDetails(userId);
            } else {
                sessionReservations = reservationService.getAllWithDetails();
            }
        } catch (Exception e) {
            e.printStackTrace();
            sessionReservations = new ArrayList<>();
        }

        FlowPane container = controller.getHotelsContainer();
        container.getChildren().clear();

        VBox root = new VBox(24);
        root.setPadding(new Insets(6, 10, 20, 10));
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

        // Table only — no payment button here
        VBox tableWrap = buildReservationTable(sessionReservations, searchField, statusCombo, countLabel);

        root.getChildren().addAll(header, tableWrap);
        container.getChildren().add(root);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Builders
    // ─────────────────────────────────────────────────────────────────────────

    private VBox buildReservationTable(List<ReservationDetail> all,
                                       TextField searchField,
                                       ComboBox<String> statusFilter,
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
                           TextField searchField,
                           ComboBox<String> statusFilter) {
        rows.getChildren().clear();
        String search = (searchField == null || searchField.getText() == null)
                ? "" : searchField.getText().toLowerCase().trim();
        String statusVal = (statusFilter == null) ? "Tous les statuts" : statusFilter.getValue();
        boolean first = true;

        List<ReservationDetail> filtered = all.stream()
                .filter(b -> {
                    boolean matchSearch = search.isEmpty()
                            || b.safeRoomNumber().toLowerCase().contains(search)
                            || b.safeHotelName().toLowerCase().contains(search)
                            || b.safeRoomType().toLowerCase().contains(search);

                    String normalizedDbStatus = normalizeStatus(b.status);
                    String filterStatus = switch (statusVal) {
                        case "En attente" -> "PENDING";
                        case "Confirmée"  -> "CONFIRMED";
                        case "Annulée"    -> "CANCELLED";
                        default           -> "";
                    };
                    
                    boolean matchStatus = filterStatus.isEmpty() || filterStatus.equals(normalizedDbStatus);
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
        HBox row = new HBox(15);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(14, 22, 14, 22));
        row.getStyleClass().add("res-col-header");
        
        Label roomCol = colLabel("CHAMBRE", 140);
        Label hotelCol = colLabel("HÔTEL", 150);
        Label dateCol = colLabel("SÉJOUR", 160);
        Label priceCol = colLabel("TOTAL (DT)", 100);
        Label statusCol = colLabel("STATUT", 110);
        Label actionCol = colLabel("ACTIONS", 140);

        // Use HGrow for flexibility
        HBox.setHgrow(hotelCol, Priority.ALWAYS);
        HBox.setHgrow(dateCol, Priority.ALWAYS);

        row.getChildren().addAll(roomCol, hotelCol, dateCol, priceCol, statusCol, actionCol);
        return row;
    }

    private HBox buildRow(ReservationDetail b, Runnable refresh) {
        HBox row = new HBox(15);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(16, 22, 16, 22));

        VBox roomCell = new VBox(2);
        Label roomNum = new Label("Ch. " + b.safeRoomNumber());
        roomNum.getStyleClass().add("res-room-num");
        Label roomType = new Label(b.safeRoomType());
        roomType.getStyleClass().add("res-room-type");
        roomCell.getChildren().addAll(roomNum, roomType);
        roomCell.setMinWidth(140); roomCell.setPrefWidth(140);

        Label hotelLbl = new Label(b.safeHotelName());
        hotelLbl.getStyleClass().add("res-hotel-name");
        hotelLbl.setWrapText(true);
        hotelLbl.setMinWidth(150); 
        HBox.setHgrow(hotelLbl, Priority.ALWAYS);

        VBox dateCell = new VBox(2);
        Label dateRange = new Label(b.dateRange());
        dateRange.getStyleClass().add("res-date-range");
        long nights = b.nights();
        Label nightsLbl = new Label(nights + " nuit" + (nights > 1 ? "s" : ""));
        nightsLbl.getStyleClass().add("res-nights");
        dateCell.getChildren().addAll(dateRange, nightsLbl);
        dateCell.setMinWidth(160);
        HBox.setHgrow(dateCell, Priority.ALWAYS);

        Label priceLbl = new Label(String.format("%.0f DT", b.prix));
        priceLbl.getStyleClass().add("res-price");
        priceLbl.setMinWidth(100); priceLbl.setPrefWidth(100);

        HBox statusCell = buildStatusCell(b.status);
        
        HBox actionCell = buildActionCell(b, refresh);

        row.getChildren().addAll(roomCell, hotelLbl, dateCell, priceLbl, statusCell, actionCell);

        row.getStyleClass().add("res-row");
        row.setOnMouseEntered(e -> { if (!row.getStyleClass().contains("res-row-hover")) row.getStyleClass().add("res-row-hover"); });
        row.setOnMouseExited(e  -> row.getStyleClass().remove("res-row-hover"));
        return row;
    }

    private HBox buildStatusCell(String status) {
        String normalized = normalizeStatus(status);
        String labelText = status;
        String cssVariant;

        switch (normalized) {
            case "CONFIRMED" -> {
                cssVariant = "res-badge-confirmed";
                labelText = "CONFIRMÉE";
            }
            case "CANCELLED" -> {
                cssVariant = "res-badge-cancelled";
                labelText = "ANNULÉE";
            }
            default -> {
                cssVariant = "res-badge-pending";
                labelText = "EN ATTENTE";
            }
        }
        
        Label badge = new Label(labelText);
        badge.getStyleClass().addAll("res-badge-base", cssVariant);
        HBox cell = new HBox(badge);
        cell.setAlignment(Pos.CENTER_LEFT);
        cell.setMinWidth(110); cell.setPrefWidth(110);
        return cell;
    }

    private HBox buildActionCell(ReservationDetail b, Runnable refresh) {
        HBox cell = new HBox(8);
        cell.setAlignment(Pos.CENTER_LEFT);
        cell.setMinWidth(140); cell.setPrefWidth(140);

        String normalized = normalizeStatus(b.status);

        switch (normalized) {
            case "PENDING" -> {
                Button cancel = actionBtn("Annuler ✕", "#dc3545", "#c82333");
                cancel.setOnAction(e -> {
                    try {
                        reservationService.updateStatus(b.id, "ANNULÉE");
                        b.status = "ANNULÉE";
                    } catch (SQLException ex) { ex.printStackTrace(); }
                    refresh.run();
                });
                cell.getChildren().add(cancel);
            }
            case "CONFIRMED" -> {
                Label confirmed = new Label("Confirmée ✓");
                confirmed.getStyleClass().add("res-confirmed-label");
                cell.getChildren().add(confirmed);
            }
            default -> {
                // For CANCELLED or REJECTED, show delete option
                Button del = actionBtn("Supprimer 🗑", "#6c757d", "#5a6268");
                del.setOnAction(e -> {
                    try { reservationService.delete(b.id); } catch (SQLException ex) { ex.printStackTrace(); }
                    sessionReservations.remove(b);
                    refresh.run();
                });
                cell.getChildren().add(del);
            }
        }
        return cell;
    }

    private String normalizeStatus(String status) {
        if (status == null) return "PENDING";
        String s = status.trim().toUpperCase();
        // Remove accents manually for common cases
        s = s.replace("É", "E").replace("È", "E");
        
        if (s.contains("CONFIRM")) return "CONFIRMED";
        if (s.contains("ANNUL") || s.contains("CANCEL") || s.contains("REFUS")) return "CANCELLED";
        return "PENDING";
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
