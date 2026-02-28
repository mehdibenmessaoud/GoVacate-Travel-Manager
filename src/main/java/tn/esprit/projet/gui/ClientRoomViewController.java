package tn.esprit.projet.gui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.scene.control.DatePicker;

import tn.esprit.projet.entities.*;
import tn.esprit.projet.services.ReservationService;

import java.sql.SQLException;
import java.util.List;
import java.util.function.Function;

/**
 * Manages the room card grid and room detail view.
 */
public class ClientRoomViewController {

    private final ClientSharedState  state;
    private final ClientDialogHelper dialogs;
    private final Function<String, Image> imageLoader;
    private final ClientController   controller;

    public ClientRoomViewController(
            ClientSharedState  state,
            ClientDialogHelper dialogs,
            Function<String, Image> imageLoader,
            ClientController   controller
    ) {
        this.state       = state;
        this.dialogs     = dialogs;
        this.imageLoader = imageLoader;
        this.controller  = controller;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Room card grid
    // ─────────────────────────────────────────────────────────────────────────

    public int loadRooms(FlowPane container, Label sectionTitle,
                         TextField searchField,
                         ComboBox<String> hotelFilterCombo,
                         ComboBox<String> typeFilterCombo,
                         ComboBox<String> statusFilterCombo) {
        container.getChildren().clear();
        try {
            List<Room>  rooms  = state.roomService.getAll();
            List<Hotel> hotels = state.hotelService.getAll();
            String search       = searchField.getText() != null ? searchField.getText().toLowerCase().trim() : "";
            String hotelFilter  = hotelFilterCombo  != null ? hotelFilterCombo.getValue()  : null;
            String typeFilter   = typeFilterCombo   != null ? typeFilterCombo.getValue()   : null;
            String statusFilter = statusFilterCombo != null ? statusFilterCombo.getValue() : null;
            int count = 0;

            for (Room room : rooms) {
                String hotelName = getHotelName(room.getHotelId(), hotels);

                boolean matchSearch  = search.isEmpty()
                        || room.getRoomNumber().toLowerCase().contains(search)
                        || room.getRoomType().toLowerCase().contains(search)
                        || hotelName.toLowerCase().contains(search);
                boolean matchHotel   = hotelFilter  == null || hotelFilter.equals("Tous les hotels")
                        || hotelFilter.startsWith(hotelName + " (");
                boolean matchType    = typeFilter   == null || typeFilter.equals("Tous")
                        || room.getRoomType().equals(typeFilter);
                boolean matchStatus  = statusFilter == null || statusFilter.equals("Tous")
                        || room.getStatus().equals(statusFilter);

                if (matchSearch && matchHotel && matchType && matchStatus) {
                    container.getChildren().add(createRoomCard(room, hotelName));
                    count++;
                }
            }

            sectionTitle.setText(count + " Chambre(s) disponibles");
            if (count == 0) showEmptyState(container, "", "Aucune chambre trouvee", "Essayez de changer vos filtres");
            return count;
        } catch (SQLException e) {
            showEmptyState(container, "", "Erreur de chargement", e.getMessage());
            return 0;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Room card
    // ─────────────────────────────────────────────────────────────────────────

    public StackPane createRoomCard(Room room, String hotelName) {
        StackPane card = new StackPane();
        card.setPrefWidth(270);
        card.setPrefHeight(300);
        card.getStyleClass().add("water-card");
        card.setCursor(javafx.scene.Cursor.HAND);

        VBox content = new VBox(0);

        StackPane header = new StackPane();
        header.setPrefHeight(150);
        header.getStyleClass().addAll("card-header", "stars-4");

        try {
            List<RoomImage> images = state.roomImageService.getByRoomId(room.getId());
            if (!images.isEmpty()) {
                Image img = imageLoader.apply(images.get(0).getImageUrl());
                if (img != null) {
                    ImageView iv = new ImageView(img);
                    iv.setFitWidth(270);
                    iv.setFitHeight(150);
                    iv.setPreserveRatio(false);
                    header.getChildren().add(iv);
                }
            }
        } catch (SQLException ignored) {}

        Label typeLabel = new Label(room.getRoomType());
        typeLabel.setStyle("-fx-background-color: rgba(0,0,0,0.6); -fx-text-fill: white; -fx-padding: 5 10; -fx-background-radius: 15; -fx-font-size: 11px;");
        StackPane.setAlignment(typeLabel, Pos.TOP_LEFT);
        StackPane.setMargin(typeLabel, new Insets(10));
        header.getChildren().add(typeLabel);

        Label priceBadge = new Label(room.getPricePerNight() + " DT");
        priceBadge.setStyle("-fx-background-color: rgba(255,130,16,0.9); -fx-text-fill: white; -fx-padding: 5 10; -fx-background-radius: 15; -fx-font-size: 11px; -fx-font-weight: bold;");
        StackPane.setAlignment(priceBadge, Pos.TOP_RIGHT);
        StackPane.setMargin(priceBadge, new Insets(10));
        header.getChildren().add(priceBadge);

        VBox glass = new VBox(8);
        glass.getStyleClass().add("glass-card");
        glass.setPadding(new Insets(12));

        Label title = new Label("Chambre " + room.getRoomNumber());
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white;");

        Label hotelLabel = new Label(hotelName);
        hotelLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #679AC1;");

        HBox info = new HBox(15);
        info.setAlignment(Pos.CENTER_LEFT);
        Label capacity    = new Label(room.getCapacity() + " pers.");
        capacity.setStyle("-fx-font-size: 12px; -fx-text-fill: rgba(255,255,255,0.8);");
        Label priceLabel  = new Label(room.getPricePerNight() + " DT/nuit");
        priceLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #FF8210; -fx-font-weight: bold;");
        info.getChildren().addAll(capacity, priceLabel);

        HBox statusRow = new HBox(8);
        statusRow.setAlignment(Pos.CENTER);
        statusRow.setPadding(new Insets(5, 0, 0, 0));

        Label status = new Label(ClientUtils.getStatusLabel(room.getStatus()));
        status.getStyleClass().add(ClientUtils.getStatusBadgeClass(room.getStatus()));

        Button detailsBtn = new Button("Details");
        detailsBtn.setStyle("-fx-background-color: #679AC1; -fx-text-fill: white; -fx-background-radius: 15; -fx-padding: 6 12; -fx-cursor: hand; -fx-font-size: 11px;");
        detailsBtn.setOnAction(e -> controller.showRoomDetails(room, hotelName));

        Button bookBtn = new Button("Réserver");
        bookBtn.getStyleClass().add("btn-book");
        bookBtn.setStyle("-fx-font-size: 11px; -fx-padding: 6 12;");
        boolean available = "AVAILABLE".equals(room.getStatus());
        bookBtn.setDisable(!available);
        if (available) bookBtn.setOnAction(e -> showBookingForm(room, hotelName));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        statusRow.getChildren().addAll(status, spacer, detailsBtn, bookBtn);

        glass.getChildren().addAll(title, hotelLabel, info, statusRow);
        content.getChildren().addAll(header, glass);
        card.getChildren().add(content);

        card.setOnMouseEntered(e -> card.setStyle("-fx-effect: dropshadow(gaussian, rgba(103,154,193,0.5), 20, 0, 0, 5); -fx-scale-x: 1.02; -fx-scale-y: 1.02;"));
        card.setOnMouseExited(e  -> card.setStyle("-fx-effect: none; -fx-scale-x: 1; -fx-scale-y: 1;"));
        card.setOnMouseClicked(e -> { if (e.getClickCount() == 2) controller.showRoomDetails(room, hotelName); });

        return card;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Room detail view
    // ─────────────────────────────────────────────────────────────────────────

    public void showRoomDetails(Room room, String hotelName) {
        controller.showSearchPanel(false);
        FlowPane container = controller.getHotelsContainer();
        container.getChildren().clear();
        controller.getSectionTitle().setText("Details chambre");

        VBox detailsBox = new VBox(25);
        detailsBox.setPadding(new Insets(10));
        detailsBox.setMaxWidth(1160);

        Button backBtn = new Button("Retour aux chambres");
        backBtn.getStyleClass().add("btn-book");
        backBtn.setStyle("-fx-background-color: #FF8210; -fx-font-size: 14px; -fx-padding: 12 25;");
        backBtn.setOnAction(e -> controller.goToRooms());

        HBox mainSection = new HBox(25);
        mainSection.setAlignment(Pos.TOP_LEFT);

        // Gallery
        ClientImageGalleryBuilder<RoomImage> galleryBuilder = new ClientImageGalleryBuilder<>(
                "#679AC1", "R", RoomImage::getImageUrl, imageLoader,
                url -> dialogs.showImagePreview(url, "Image chambre")
        );
        List<RoomImage> images = List.of();
        try { images = state.roomImageService.getByRoomId(room.getId()); } catch (SQLException ignored) {}
        VBox imageGallery = galleryBuilder.build(images);

        // Info box
        VBox infoBox = new VBox(15);
        infoBox.setPadding(new Insets(10));
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        Label roomTitle = new Label("Chambre " + room.getRoomNumber());
        roomTitle.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: white;");

        HBox hotelRow = new HBox(10);
        hotelRow.setAlignment(Pos.CENTER_LEFT);
        Label hotelLabel = new Label(hotelName);
        hotelLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #679AC1; -fx-font-weight: bold;");
        hotelRow.getChildren().add(hotelLabel);

        HBox featuresRow = new HBox(15);
        featuresRow.setAlignment(Pos.CENTER_LEFT);

        VBox typeBox = styledInfoBox("Type", room.getRoomType(), "#679AC1");
        VBox capacityBox = styledInfoBox("Capacite", room.getCapacity() + " pers.", "white");
        featuresRow.getChildren().addAll(typeBox, capacityBox);

        VBox priceBox = new VBox(3);
        priceBox.setStyle("-fx-background-color: rgba(255,130,16,0.15); -fx-background-radius: 12; -fx-padding: 15 20;");
        Label priceTitle = new Label("Prix par nuit");
        priceTitle.setStyle("-fx-font-size: 12px; -fx-text-fill: rgba(255,255,255,0.7);");
        Label priceValue = new Label(room.getPricePerNight() + " DT");
        priceValue.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #FF8210;");
        priceBox.getChildren().addAll(priceTitle, priceValue);

        Label statusLabel = new Label(ClientUtils.getStatusLabel(room.getStatus()));
        statusLabel.setStyle("-fx-font-size: 14px; -fx-padding: 8 15;");
        statusLabel.getStyleClass().add(ClientUtils.getStatusBadgeClass(room.getStatus()));

        Button bookBtn = new Button("Réserver maintenant");
        bookBtn.getStyleClass().add("btn-book");
        bookBtn.setStyle("-fx-font-size: 16px; -fx-padding: 15 30; -fx-background-radius: 25;");
        boolean isAvailable = "AVAILABLE".equals(room.getStatus());
        bookBtn.setDisable(!isAvailable);
        if (isAvailable) bookBtn.setOnAction(e -> showBookingForm(room, hotelName));

        infoBox.getChildren().addAll(roomTitle, hotelRow, featuresRow, priceBox, statusLabel, bookBtn);
        mainSection.getChildren().addAll(imageGallery, infoBox);

        detailsBox.getChildren().addAll(backBtn, mainSection);
        container.getChildren().add(detailsBox);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Inline booking form (excursion-style)
    // ─────────────────────────────────────────────────────────────────────────

    public void showBookingForm(Room room, String hotelName) {
        controller.showSearchPanel(false);
        FlowPane container = controller.getHotelsContainer();
        container.getChildren().clear();
        controller.getSectionTitle().setText("Réservation");

        // ── Scroll-friendly outer wrapper ────────────────────────────────────
        VBox outer = new VBox(0);
        outer.setAlignment(Pos.TOP_CENTER);
        outer.setMaxWidth(Double.MAX_VALUE);
        outer.setPadding(new Insets(32, 24, 48, 24));

        // ── Back button (top-left) ───────────────────────────────────────────
        Button backBtn = new Button("← Retour");
        backBtn.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: rgba(255,255,255,0.45);" +
                        "-fx-font-size: 12px;" +
                        "-fx-cursor: hand;" +
                        "-fx-padding: 0 0 0 0;"
        );
        backBtn.setOnMouseEntered(e -> backBtn.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: #FF8210;" +
                        "-fx-font-size: 12px;" +
                        "-fx-cursor: hand;" +
                        "-fx-padding: 0 0 0 0;"
        ));
        backBtn.setOnMouseExited(e -> backBtn.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: rgba(255,255,255,0.45);" +
                        "-fx-font-size: 12px;" +
                        "-fx-cursor: hand;" +
                        "-fx-padding: 0 0 0 0;"
        ));
        backBtn.setOnAction(e -> controller.goToRooms());
        HBox backRow = new HBox(backBtn);
        backRow.setPadding(new Insets(0, 0, 24, 0));

        // ── Page header ──────────────────────────────────────────────────────
        VBox header = new VBox(6);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(0, 0, 28, 0));

        Label eyebrow = new Label("HÔTELS & SÉJOURS");
        eyebrow.setStyle(
                "-fx-font-size: 10px; -fx-font-weight: 800;" +
                        "-fx-text-fill: #FF8210; -fx-letter-spacing: 3px;"
        );

        Label pageTitle = new Label("Finaliser votre Réservation");
        pageTitle.setStyle(
                "-fx-font-size: 28px; -fx-font-weight: 800; -fx-text-fill: white;"
        );

        Label roomSub = new Label(
                "Chambre " + room.getRoomNumber() + "  ·  " + room.getRoomType() + "  ·  " + hotelName
        );
        roomSub.setStyle(
                "-fx-font-size: 13px; -fx-text-fill: rgba(255,255,255,0.40);"
        );

        Region accentLine = new Region();
        accentLine.setPrefWidth(48); accentLine.setPrefHeight(3);
        accentLine.setMaxWidth(48);
        accentLine.setStyle("-fx-background-color: #FF8210; -fx-background-radius: 2;");

        header.getChildren().addAll(eyebrow, pageTitle, accentLine, roomSub);

        // ── Two-column layout: form LEFT, summary RIGHT ──────────────────────
        HBox twoCol = new HBox(20);
        twoCol.setAlignment(Pos.TOP_CENTER);
        twoCol.setMaxWidth(900);

        // ════ LEFT: form card ════════════════════════════════════════════════
        VBox formCard = new VBox(20);
        formCard.setStyle(
                "-fx-background-color: rgba(255,255,255,0.04);" +
                        "-fx-background-radius: 18;" +
                        "-fx-border-color: rgba(255,255,255,0.07);" +
                        "-fx-border-radius: 18;" +
                        "-fx-border-width: 1;"
        );
        formCard.setPadding(new Insets(28, 28, 28, 28));
        HBox.setHgrow(formCard, Priority.ALWAYS);

        // Section label inside card
        Label formSection = new Label("DÉTAILS DU SÉJOUR");
        formSection.setStyle(
                "-fx-font-size: 9px; -fx-font-weight: 800; -fx-text-fill: rgba(255,255,255,0.30);" +
                        "-fx-letter-spacing: 2px;"
        );

        // Separator line
        Region formSep = new Region();
        formSep.setPrefHeight(1);
        formSep.setStyle("-fx-background-color: rgba(255,255,255,0.06);");

        // Row 1: Date + Nights
        HBox row1 = new HBox(16);
        row1.setAlignment(Pos.TOP_LEFT);

        VBox dateField = buildFormField("📅  Date d'arrivée");
        DatePicker datePicker = new DatePicker(java.time.LocalDate.now().plusDays(1));
        datePicker.setEditable(false);
        datePicker.setStyle(
                "-fx-background-color: rgba(255,255,255,0.06);" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-color: rgba(103,154,193,0.25);" +
                        "-fx-border-radius: 10;" +
                        "-fx-border-width: 1;" +
                        "-fx-font-size: 13px;" +
                        "-fx-text-fill: white;"
        );
        datePicker.setPrefWidth(220);
        dateField.getChildren().add(datePicker);
        HBox.setHgrow(dateField, Priority.ALWAYS);

        VBox nightsField = buildFormField("🌙  Nuits");
        Spinner<Integer> nightsSpinner = new Spinner<>(1, 30, 1);
        nightsSpinner.setEditable(true);
        nightsSpinner.setStyle(
                "-fx-background-color: rgba(255,255,255,0.06);" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-color: rgba(103,154,193,0.25);" +
                        "-fx-border-radius: 10;" +
                        "-fx-border-width: 1;" +
                        "-fx-font-size: 13px;"
        );
        nightsSpinner.setPrefWidth(130);
        nightsSpinner.getEditor().setStyle("-fx-background-color: transparent; -fx-text-fill: white;");
        nightsField.getChildren().add(nightsSpinner);

        row1.getChildren().addAll(dateField, nightsField);

        // Row 2: Price (read-only) + Persons spinner
        HBox row2 = new HBox(16);
        row2.setAlignment(Pos.TOP_LEFT);

        VBox priceUnitField = buildFormField("💰  Prix / nuit (DT)");
        TextField priceUnit = new TextField(String.valueOf(room.getPricePerNight()));
        priceUnit.setEditable(false);
        priceUnit.setStyle(
                "-fx-background-color: rgba(255,130,16,0.08);" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-color: rgba(255,130,16,0.20);" +
                        "-fx-border-radius: 10;" +
                        "-fx-border-width: 1;" +
                        "-fx-font-size: 15px;" +
                        "-fx-text-fill: #FF8210;" +
                        "-fx-font-weight: 800;" +
                        "-fx-padding: 10 16;"
        );
        priceUnit.setPrefWidth(220);
        priceUnitField.getChildren().add(priceUnit);
        HBox.setHgrow(priceUnitField, Priority.ALWAYS);

        VBox capacityField = buildFormField("👥  Personnes");
        Spinner<Integer> capacitySpinner = new Spinner<>(1, Math.max(1, room.getCapacity()), 1);
        capacitySpinner.setEditable(true);
        capacitySpinner.setStyle(
                "-fx-background-color: rgba(255,255,255,0.06);" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-color: rgba(103,154,193,0.25);" +
                        "-fx-border-radius: 10;" +
                        "-fx-border-width: 1;" +
                        "-fx-font-size: 13px;"
        );
        capacitySpinner.setPrefWidth(130);
        capacitySpinner.getEditor().setStyle("-fx-background-color: transparent; -fx-text-fill: white;");
        Label capacityHint = new Label("Max. " + room.getCapacity() + " pers.");
        capacityHint.setStyle("-fx-font-size: 10px; -fx-text-fill: rgba(255,255,255,0.28);");
        capacityField.getChildren().addAll(capacitySpinner, capacityHint);

        row2.getChildren().addAll(priceUnitField, capacityField);

        formCard.getChildren().addAll(formSection, formSep, row1, row2);

        // ════ RIGHT: summary card ════════════════════════════════════════════
        VBox summaryCard = new VBox(0);
        summaryCard.setStyle(
                "-fx-background-color: rgba(255,130,16,0.07);" +
                        "-fx-background-radius: 18;" +
                        "-fx-border-color: rgba(255,130,16,0.18);" +
                        "-fx-border-radius: 18;" +
                        "-fx-border-width: 1;"
        );
        summaryCard.setPrefWidth(240);
        summaryCard.setMinWidth(220);
        summaryCard.setMaxWidth(260);
        summaryCard.setPadding(new Insets(24, 22, 24, 22));
        summaryCard.setSpacing(0);

        Label summaryTitle = new Label("RÉCAPITULATIF");
        summaryTitle.setStyle(
                "-fx-font-size: 9px; -fx-font-weight: 800; -fx-text-fill: #FF8210;" +
                        "-fx-letter-spacing: 2px;"
        );

        Region sumSep1 = new Region();
        sumSep1.setPrefHeight(1);
        sumSep1.setStyle("-fx-background-color: rgba(255,130,16,0.15);");
        VBox.setMargin(sumSep1, new Insets(10, 0, 16, 0));

        // Room info line
        VBox roomInfoBox = new VBox(3);
        Label roomNumLbl = new Label("Chambre " + room.getRoomNumber());
        roomNumLbl.setStyle("-fx-font-size: 16px; -fx-font-weight: 800; -fx-text-fill: white;");
        Label roomTypeLbl = new Label(room.getRoomType());
        roomTypeLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: rgba(255,255,255,0.45);");
        Label hotelLbl = new Label(hotelName);
        hotelLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: rgba(255,255,255,0.35);");
        roomInfoBox.getChildren().addAll(roomNumLbl, roomTypeLbl, hotelLbl);
        VBox.setMargin(roomInfoBox, new Insets(0, 0, 18, 0));

        // Price breakdown
        final double[] totalArr = {room.getPricePerNight()};

        HBox priceRow = buildSummaryRow("Prix / nuit", String.format("%.0f DT", room.getPricePerNight()));
        VBox.setMargin(priceRow, new Insets(0, 0, 8, 0));

        Label nightsLabelSummary = new Label("1 nuit");
        nightsLabelSummary.setStyle("-fx-font-size: 12px; -fx-text-fill: rgba(255,255,255,0.45);");
        HBox nightsRowSum = new HBox();
        nightsRowSum.setAlignment(Pos.CENTER_LEFT);
        Label nightsKey = new Label("Durée");
        nightsKey.setStyle("-fx-font-size: 12px; -fx-text-fill: rgba(255,255,255,0.45);");
        Region nightsSpacer = new Region(); HBox.setHgrow(nightsSpacer, Priority.ALWAYS);
        nightsRowSum.getChildren().addAll(nightsKey, nightsSpacer, nightsLabelSummary);
        VBox.setMargin(nightsRowSum, new Insets(0, 0, 16, 0));

        Region sumSep2 = new Region();
        sumSep2.setPrefHeight(1);
        sumSep2.setStyle("-fx-background-color: rgba(255,130,16,0.15);");
        VBox.setMargin(sumSep2, new Insets(0, 0, 16, 0));

        // Total
        VBox totalBox = new VBox(2);
        totalBox.setAlignment(Pos.CENTER);
        Label totalLabelLbl = new Label("Total estimé");
        totalLabelLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: rgba(255,255,255,0.45); -fx-font-weight: 600;");
        Label totalValue = new Label(ClientUtils.formatPrice(totalArr[0]) + " DT");
        totalValue.setStyle("-fx-font-size: 30px; -fx-font-weight: 800; -fx-text-fill: #FF8210;");
        totalBox.getChildren().addAll(totalLabelLbl, totalValue);
        VBox.setMargin(totalBox, new Insets(0, 0, 20, 0));

        summaryCard.getChildren().addAll(
                summaryTitle, sumSep1, roomInfoBox,
                priceRow, nightsRowSum, sumSep2, totalBox
        );

        // Live updates from spinners
        nightsSpinner.valueProperty().addListener((obs, o, n) -> {
            totalArr[0] = room.getPricePerNight() * n;
            totalValue.setText(ClientUtils.formatPrice(totalArr[0]) + " DT");
            nightsLabelSummary.setText(n + " nuit" + (n > 1 ? "s" : ""));
        });

        twoCol.getChildren().addAll(formCard, summaryCard);

        // ── Confirm button ──────────────────────────────────────────────────
        Button confirmBtn = new Button("CONFIRMER LA RÉSERVATION");
        String confirmStyle =
                "-fx-background-color: linear-gradient(to right, #FF8210, #ffaa44);" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 14px;" +
                        "-fx-font-weight: 800;" +
                        "-fx-background-radius: 30;" +
                        "-fx-padding: 16 60;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, rgba(255,130,16,0.5), 24, 0.2, 0, 4);";
        String confirmHoverStyle =
                "-fx-background-color: linear-gradient(to right, #ff9830, #ffbb55);" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 14px;" +
                        "-fx-font-weight: 800;" +
                        "-fx-background-radius: 30;" +
                        "-fx-padding: 16 60;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, rgba(255,130,16,0.7), 28, 0.25, 0, 5);";
        confirmBtn.setStyle(confirmStyle);
        confirmBtn.setOnMouseEntered(e -> confirmBtn.setStyle(confirmHoverStyle));
        confirmBtn.setOnMouseExited(e -> confirmBtn.setStyle(confirmStyle));

        confirmBtn.setOnAction(e -> {
            int nights = nightsSpinner.getValue();
            int nbPersonnes = capacitySpinner.getValue();
            java.time.LocalDate checkIn = datePicker.getValue();
            // Persist to database
            Reservation dbReservation = new Reservation();
            dbReservation.setReservationId(0); // generated in service
            dbReservation.setHotelId(room.getHotelId());
            dbReservation.setChambreId(room.getId());
            dbReservation.setDateCheckin(checkIn);
            dbReservation.setDateCheckout(checkIn.plusDays(nights));
            dbReservation.setPrix(room.getPricePerNight() * nights);
            dbReservation.setNbPersonnes(nbPersonnes);
            dbReservation.setUserId(state.resolveClientReviewUserId());
            try {
                state.reservationService.create(dbReservation);
            } catch (java.sql.SQLException ex) {
                dialogs.showError("Erreur BD", "Impossible d'enregistrer la réservation : " + ex.getMessage());
                return;
            }

            // Also keep in-memory store for the current session
            ClientBookingStore.Booking booking = new ClientBookingStore.Booking(
                    room.getRoomNumber(), room.getRoomType(), hotelName,
                    room.getPricePerNight(), nights, checkIn);
            ClientBookingStore.getInstance().add(booking);

            // Show success then go to reservations
            dialogs.showInfo("Réservation confirmée",
                    "Votre réservation a été enregistrée !\n\n" +
                            "Chambre " + room.getRoomNumber() + " – " + room.getRoomType() + "\n" +
                            hotelName + "\n" +
                            nights + " nuit(s) · " + nbPersonnes + " personne(s) · Arrivée " + checkIn.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            controller.goToReservations();
        });

        Label tagLine = new Label("Annulation flexible · Paiement sécurisé · Confirmation immédiate");
        tagLine.setStyle(
                "-fx-font-size: 10px; -fx-text-fill: rgba(255,255,255,0.22);" +
                        "-fx-padding: 4 0 0 0;"
        );

        // Centered button wrapper
        HBox btnRow = new HBox(confirmBtn);
        btnRow.setAlignment(Pos.CENTER);
        btnRow.setPadding(new Insets(20, 0, 6, 0));

        HBox tagRow = new HBox(tagLine);
        tagRow.setAlignment(Pos.CENTER);

        outer.getChildren().addAll(backRow, header, twoCol, btnRow, tagRow);
        container.getChildren().add(outer);
    }

    private HBox buildSummaryRow(String key, String value) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        Label k = new Label(key);
        k.setStyle("-fx-font-size: 12px; -fx-text-fill: rgba(255,255,255,0.45);");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Label v = new Label(value);
        v.setStyle("-fx-font-size: 12px; -fx-text-fill: rgba(255,255,255,0.75); -fx-font-weight: 700;");
        row.getChildren().addAll(k, sp, v);
        return row;
    }

    private VBox buildFormField(String labelText) {
        VBox field = new VBox(8);
        Label lbl = new Label(labelText);
        lbl.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: rgba(255,255,255,0.65);");
        field.getChildren().add(lbl);
        return field;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private VBox styledInfoBox(String labelText, String valueText, String valueColor) {
        VBox box = new VBox(3);
        box.setStyle("-fx-background-color: rgba(103,154,193,0.2); -fx-background-radius: 10; -fx-padding: 10 15;");
        Label lbl = new Label(labelText);
        lbl.setStyle("-fx-font-size: 11px; -fx-text-fill: rgba(255,255,255,0.6);");
        Label val = new Label(valueText);
        val.setStyle("-fx-font-size: 14px; -fx-text-fill: " + valueColor + "; -fx-font-weight: bold;");
        box.getChildren().addAll(lbl, val);
        return box;
    }

    private String getHotelName(int hotelId, List<Hotel> hotels) {
        return hotels.stream()
                .filter(h -> h.getId() == hotelId)
                .map(Hotel::getName)
                .findFirst()
                .orElse("Hotel #" + hotelId);
    }

    public void setupHotelFilterCombo(ComboBox<String> combo) {
        combo.getItems().clear();
        combo.getItems().add("Tous les hotels");
        if (!state.databaseAvailable || state.hotelService == null || state.roomService == null) {
            combo.setValue("Tous les hotels");
            return;
        }
        try {
            List<Hotel> hotels = state.hotelService.getAll();
            List<Room>  rooms  = state.roomService.getAll();
            for (Hotel h : hotels) {
                int count = (int) rooms.stream().filter(r -> r.getHotelId() == h.getId()).count();
                combo.getItems().add(h.getName() + " (" + count + ")");
            }
        } catch (SQLException ignored) {}
        combo.setValue("Tous les hotels");
    }

    private void showEmptyState(FlowPane container, String icon, String title, String subtitle) {
        VBox box = new VBox(10);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(50));
        Label iconLabel  = new Label(icon);   iconLabel.setStyle("-fx-font-size: 48px;");
        Label titleLabel = new Label(title);  titleLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: rgba(255,255,255,0.6);");
        Label subLabel   = new Label(subtitle); subLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: rgba(255,255,255,0.4);");
        box.getChildren().addAll(iconLabel, titleLabel, subLabel);
        container.getChildren().add(box);
    }
}