package tn.esprit.projet.gui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.control.DatePicker;

import tn.esprit.projet.entities.*;

import java.sql.SQLException;
import java.util.List;
import java.util.function.Function;

/**
 * Manages the room card grid and room detail view.
 */
public class ClientRoomViewController {

    private final ClientSharedState  state;
    private final DialogHelper.ClientDialogs dialogs;
    private final Function<String, Image> imageLoader;
    private final ClientController3 controller;

    public ClientRoomViewController(
            ClientSharedState  state,
            DialogHelper.ClientDialogs dialogs,
            Function<String, Image> imageLoader,
            ClientController3 controller
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
        typeLabel.getStyleClass().add("room-type-badge");
        StackPane.setAlignment(typeLabel, Pos.TOP_LEFT);
        StackPane.setMargin(typeLabel, new Insets(10));
        header.getChildren().add(typeLabel);

        Label priceBadge = new Label(room.getPricePerNight() + " DT");
        priceBadge.getStyleClass().add("room-price-badge");
        StackPane.setAlignment(priceBadge, Pos.TOP_RIGHT);
        StackPane.setMargin(priceBadge, new Insets(10));
        header.getChildren().add(priceBadge);

        VBox glass = new VBox(8);
        glass.getStyleClass().add("glass-card");
        glass.setPadding(new Insets(12));

        Label title = new Label("Chambre " + room.getRoomNumber());
        title.getStyleClass().add("room-title");

        Label hotelLabel = new Label(hotelName);
        hotelLabel.getStyleClass().add("room-hotel-label");

        HBox info = new HBox(15);
        info.setAlignment(Pos.CENTER_LEFT);
        Label capacity    = new Label(room.getCapacity() + " pers.");
        capacity.getStyleClass().add("room-capacity-label");
        Label priceLabel  = new Label(room.getPricePerNight() + " DT/nuit");
        priceLabel.getStyleClass().add("room-price-label");
        info.getChildren().addAll(capacity, priceLabel);

        HBox statusRow = new HBox(8);
        statusRow.setAlignment(Pos.CENTER);
        statusRow.setPadding(new Insets(5, 0, 0, 0));

        Label status = new Label(GuiUtils.getStatusLabel(room.getStatus()));
        status.getStyleClass().add(GuiUtils.getStatusBadgeClass(room.getStatus()));

        Button detailsBtn = new Button("Details");
        detailsBtn.getStyleClass().add("room-details-btn");
        detailsBtn.setOnAction(e -> controller.showRoomDetails(room, hotelName));

        Button bookBtn = new Button("Réserver");
        bookBtn.getStyleClass().addAll("btn-book", "room-book-btn-small");
        boolean available = "AVAILABLE".equals(room.getStatus());
        bookBtn.setDisable(!available);
        if (available) bookBtn.setOnAction(e -> showBookingForm(room, hotelName));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        statusRow.getChildren().addAll(status, spacer, detailsBtn, bookBtn);

        glass.getChildren().addAll(title, hotelLabel, info, statusRow);
        content.getChildren().addAll(header, glass);
        card.getChildren().add(content);

        card.setOnMouseEntered(e -> { if (!card.getStyleClass().contains("room-card-hover")) card.getStyleClass().add("room-card-hover"); });
        card.setOnMouseExited(e  -> card.getStyleClass().remove("room-card-hover"));
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
        backBtn.getStyleClass().addAll("btn-book", "hotel-detail-back-btn");
        backBtn.setOnAction(e -> controller.goToRooms());

        HBox mainSection = new HBox(25);
        mainSection.setAlignment(Pos.TOP_LEFT);

        // Gallery
        List<RoomImage> images = List.of();
        try { images = state.roomImageService.getByRoomId(room.getId()); } catch (SQLException ignored) {}
        StackPane mainImageContainer = new StackPane();
        mainImageContainer.setPrefSize(520, 330);
        mainImageContainer.getStyleClass().add("gallery-main-container");
        HBox thumbnailsContainer = new HBox(10);
        thumbnailsContainer.setAlignment(Pos.CENTER_LEFT);
        VBox imageGallery = new VBox(10, mainImageContainer, thumbnailsContainer);
        imageGallery.setPrefWidth(520);
        ImageGalleryBuilder.build(
                images,
                RoomImage::getImageUrl,
                mainImageContainer,
                thumbnailsContainer,
                "#679AC1",
                "R",
                getClass(),
                url -> dialogs.showImagePreview(url, "Image chambre")
        );

        // Info box
        VBox infoBox = new VBox(15);
        infoBox.setPadding(new Insets(10));
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        Label roomTitle = new Label("Chambre " + room.getRoomNumber());
        roomTitle.getStyleClass().add("room-detail-title");

        HBox hotelRow = new HBox(10);
        hotelRow.setAlignment(Pos.CENTER_LEFT);
        Label hotelLabel = new Label(hotelName);
        hotelLabel.getStyleClass().add("room-detail-hotel");
        hotelRow.getChildren().add(hotelLabel);

        HBox featuresRow = new HBox(15);
        featuresRow.setAlignment(Pos.CENTER_LEFT);

        VBox typeBox = styledInfoBox("Type", room.getRoomType(), "#679AC1");
        VBox capacityBox = styledInfoBox("Capacite", room.getCapacity() + " pers.", "white");
        featuresRow.getChildren().addAll(typeBox, capacityBox);

        VBox priceBox = new VBox(3);
        priceBox.getStyleClass().add("room-price-box");
        Label priceTitle = new Label("Prix par nuit");
        priceTitle.getStyleClass().add("room-price-title");
        Label priceValue = new Label(room.getPricePerNight() + " DT");
        priceValue.getStyleClass().add("room-price-value");
        priceBox.getChildren().addAll(priceTitle, priceValue);

        Label statusLabel = new Label(GuiUtils.getStatusLabel(room.getStatus()));
        statusLabel.getStyleClass().addAll("room-detail-status", GuiUtils.getStatusBadgeClass(room.getStatus()));

        Button bookBtn = new Button("Réserver maintenant");
        bookBtn.getStyleClass().addAll("btn-book", "room-book-btn-lg");
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
        backBtn.getStyleClass().add("booking-back-btn");
        backBtn.setOnAction(e -> controller.goToRooms());
        HBox backRow = new HBox(backBtn);
        backRow.setPadding(new Insets(0, 0, 24, 0));

        // ── Page header ──────────────────────────────────────────────────────
        VBox header = new VBox(6);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(0, 0, 28, 0));

        Label eyebrow = new Label("HÔTELS & SÉJOURS");
        eyebrow.getStyleClass().add("booking-eyebrow");

        Label pageTitle = new Label("Finaliser votre Réservation");
        pageTitle.getStyleClass().add("booking-page-title");

        Label roomSub = new Label(
                "Chambre " + room.getRoomNumber() + "  ·  " + room.getRoomType() + "  ·  " + hotelName
        );
        roomSub.getStyleClass().add("booking-room-sub");

        Region accentLine = new Region();
        accentLine.setPrefWidth(48); accentLine.setPrefHeight(3);
        accentLine.setMaxWidth(48);
        accentLine.getStyleClass().add("booking-accent-line");

        header.getChildren().addAll(eyebrow, pageTitle, accentLine, roomSub);

        // ── Two-column layout: form LEFT, summary RIGHT ──────────────────────
        HBox twoCol = new HBox(20);
        twoCol.setAlignment(Pos.TOP_CENTER);
        twoCol.setMaxWidth(900);

        // ════ LEFT: form card ════════════════════════════════════════════════
        VBox formCard = new VBox(20);
        formCard.getStyleClass().add("booking-form-card");
        formCard.setPadding(new Insets(28, 28, 28, 28));
        HBox.setHgrow(formCard, Priority.ALWAYS);

        // Section label inside card
        Label formSection = new Label("DÉTAILS DU SÉJOUR");
        formSection.getStyleClass().add("booking-form-section");

        Region formSep = new Region();
        formSep.setPrefHeight(1);
        formSep.getStyleClass().add("booking-form-sep");

        // Row 1: Date + Nights
        HBox row1 = new HBox(16);
        row1.setAlignment(Pos.TOP_LEFT);

        VBox dateField = buildFormField("📅  Date d'arrivée");
        DatePicker datePicker = new DatePicker(java.time.LocalDate.now().plusDays(1));
        datePicker.setEditable(false);
        datePicker.getStyleClass().add("booking-form-input");
        datePicker.setPrefWidth(220);
        dateField.getChildren().add(datePicker);
        HBox.setHgrow(dateField, Priority.ALWAYS);

        VBox nightsField = buildFormField("🌙  Nuits");
        Spinner<Integer> nightsSpinner = new Spinner<>(1, 30, 1);
        nightsSpinner.setEditable(true);
        nightsSpinner.getStyleClass().add("booking-form-input");
        nightsSpinner.setPrefWidth(130);
        nightsSpinner.getEditor().getStyleClass().add("booking-form-input");
        nightsField.getChildren().add(nightsSpinner);

        row1.getChildren().addAll(dateField, nightsField);

        // Row 2: Price (read-only) + Persons spinner
        HBox row2 = new HBox(16);
        row2.setAlignment(Pos.TOP_LEFT);

        VBox priceUnitField = buildFormField("💰  Prix / nuit (DT)");
        TextField priceUnit = new TextField(String.valueOf(room.getPricePerNight()));
        priceUnit.setEditable(false);
        priceUnit.getStyleClass().add("booking-price-input");
        priceUnit.setPrefWidth(220);
        priceUnitField.getChildren().add(priceUnit);
        HBox.setHgrow(priceUnitField, Priority.ALWAYS);

        VBox capacityField = buildFormField("👥  Personnes");
        Spinner<Integer> capacitySpinner = new Spinner<>(1, Math.max(1, room.getCapacity()), 1);
        capacitySpinner.setEditable(true);
        capacitySpinner.getStyleClass().add("booking-form-input");
        capacitySpinner.setPrefWidth(130);
        capacitySpinner.getEditor().getStyleClass().add("booking-form-input");
        Label capacityHint = new Label("Max. " + room.getCapacity() + " pers.");
        capacityHint.getStyleClass().add("booking-capacity-hint");
        capacityField.getChildren().addAll(capacitySpinner, capacityHint);

        row2.getChildren().addAll(priceUnitField, capacityField);

        formCard.getChildren().addAll(formSection, formSep, row1, row2);

        // ════ RIGHT: summary card ════════════════════════════════════════════
        VBox summaryCard = new VBox(0);
        summaryCard.getStyleClass().add("booking-summary-card");
        summaryCard.setPrefWidth(240);
        summaryCard.setMinWidth(220);
        summaryCard.setMaxWidth(260);
        summaryCard.setPadding(new Insets(24, 22, 24, 22));
        summaryCard.setSpacing(0);

        Label summaryTitle = new Label("RÉCAPITULATIF");
        summaryTitle.getStyleClass().add("booking-summary-title");

        Region sumSep1 = new Region();
        sumSep1.setPrefHeight(1);
        sumSep1.getStyleClass().add("booking-summary-sep");
        VBox.setMargin(sumSep1, new Insets(10, 0, 16, 0));

        // Room info line
        VBox roomInfoBox = new VBox(3);
        Label roomNumLbl = new Label("Chambre " + room.getRoomNumber());
        roomNumLbl.getStyleClass().add("booking-summary-room");
        Label roomTypeLbl = new Label(room.getRoomType());
        roomTypeLbl.getStyleClass().add("booking-summary-type");
        Label hotelLbl = new Label(hotelName);
        hotelLbl.getStyleClass().add("booking-summary-hotel");
        roomInfoBox.getChildren().addAll(roomNumLbl, roomTypeLbl, hotelLbl);
        VBox.setMargin(roomInfoBox, new Insets(0, 0, 18, 0));

        // Price breakdown
        final double[] totalArr = {room.getPricePerNight()};

        HBox priceRow = buildSummaryRow("Prix / nuit", String.format("%.0f DT", room.getPricePerNight()));
        VBox.setMargin(priceRow, new Insets(0, 0, 8, 0));

        Label nightsLabelSummary = new Label("1 nuit");
        nightsLabelSummary.getStyleClass().add("booking-summary-key");
        HBox nightsRowSum = new HBox();
        nightsRowSum.setAlignment(Pos.CENTER_LEFT);
        Label nightsKey = new Label("Durée");
        nightsKey.getStyleClass().add("booking-summary-key");
        Region nightsSpacer = new Region(); HBox.setHgrow(nightsSpacer, Priority.ALWAYS);
        nightsRowSum.getChildren().addAll(nightsKey, nightsSpacer, nightsLabelSummary);
        VBox.setMargin(nightsRowSum, new Insets(0, 0, 16, 0));

        Region sumSep2 = new Region();
        sumSep2.setPrefHeight(1);
        sumSep2.getStyleClass().add("booking-summary-sep");
        VBox.setMargin(sumSep2, new Insets(0, 0, 16, 0));

        // Total
        VBox totalBox = new VBox(2);
        totalBox.setAlignment(Pos.CENTER);
        Label totalLabelLbl = new Label("Total estimé");
        totalLabelLbl.getStyleClass().add("booking-total-label");
        Label totalValue = new Label(GuiUtils.formatPrice(totalArr[0]) + " DT");
        totalValue.getStyleClass().add("booking-total-value");
        totalBox.getChildren().addAll(totalLabelLbl, totalValue);
        VBox.setMargin(totalBox, new Insets(0, 0, 20, 0));

        summaryCard.getChildren().addAll(
                summaryTitle, sumSep1, roomInfoBox,
                priceRow, nightsRowSum, sumSep2, totalBox
        );

        // Live updates from spinners
        nightsSpinner.valueProperty().addListener((obs, o, n) -> {
            totalArr[0] = room.getPricePerNight() * n;
            totalValue.setText(GuiUtils.formatPrice(totalArr[0]) + " DT");
            nightsLabelSummary.setText(n + " nuit" + (n > 1 ? "s" : ""));
        });

        twoCol.getChildren().addAll(formCard, summaryCard);

        // ── Confirm button ──────────────────────────────────────────────────
        Button confirmBtn = new Button("CONFIRMER LA RÉSERVATION");
        confirmBtn.getStyleClass().add("booking-confirm-btn");
        confirmBtn.getStyleClass().add("booking-confirm-btn");

        confirmBtn.setOnAction(e -> {
            int nights = nightsSpinner.getValue();
            int nbPersonnes = capacitySpinner.getValue();
            java.time.LocalDate checkIn = datePicker.getValue();

            Integer resolvedHotelId;
            try {
                resolvedHotelId = resolveExistingHotelId(room, hotelName);
            } catch (SQLException ex) {
                dialogs.showError("Erreur BD", "Impossible de verifier l'hotel de cette chambre : " + ex.getMessage());
                return;
            }
            if (resolvedHotelId == null) {
                dialogs.showError(
                        "Reservation impossible",
                        "Cette chambre est liee a un hotel introuvable dans la base.\n" +
                        "Veuillez recharger les donnees hotels/chambres puis reessayer."
                );
                return;
            }

            // Persist to database
            ReservationHotelChambre dbReservation = new ReservationHotelChambre();
            dbReservation.setReservationId(0); // generated in service
            dbReservation.setHotelId(resolvedHotelId);
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

            // Show success then go to reservations
            dialogs.showInfo("Réservation confirmée",
                    "Votre réservation a été enregistrée !\n\n" +
                            "Chambre " + room.getRoomNumber() + " – " + room.getRoomType() + "\n" +
                            hotelName + "\n" +
                            nights + " nuit(s) · " + nbPersonnes + " personne(s) · Arrivée " + checkIn.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            controller.goToReservations();
        });

        Label tagLine = new Label("Annulation flexible · Paiement sécurisé · Confirmation immédiate");
        tagLine.getStyleClass().add("booking-tagline");

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
        k.getStyleClass().add("booking-summary-key");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Label v = new Label(value);
        v.getStyleClass().add("booking-summary-value");
        row.getChildren().addAll(k, sp, v);
        return row;
    }

    private VBox buildFormField(String labelText) {
        VBox field = new VBox(8);
        Label lbl = new Label(labelText);
        lbl.getStyleClass().add("booking-form-label");
        field.getChildren().add(lbl);
        return field;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private VBox styledInfoBox(String labelText, String valueText, String valueColor) {
        VBox box = new VBox(3);
        box.getStyleClass().add("room-info-box");
        Label lbl = new Label(labelText);
        lbl.getStyleClass().add("room-info-box-label");
        Label val = new Label(valueText);
        val.getStyleClass().add("room-info-box-value");
        val.setStyle("-fx-text-fill: " + valueColor + ";");
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

    /**
     * Ensures the reservation uses a hotel id that actually exists in table `hotel`.
     * This protects booking inserts from FK failures when a room references stale data.
     */
    private Integer resolveExistingHotelId(Room room, String hotelName) throws SQLException {
        if (room == null || state.hotelService == null) return null;

        int roomHotelId = room.getHotelId();
        if (roomHotelId > 0 && state.hotelService.getById(roomHotelId) != null) {
            return roomHotelId;
        }

        // Refresh from DB in case the in-memory room object is stale.
        if (state.roomService != null) {
            Room fresh = state.roomService.getById(room.getId());
            if (fresh != null && fresh.getHotelId() > 0 && state.hotelService.getById(fresh.getHotelId()) != null) {
                return fresh.getHotelId();
            }
        }

        // Last fallback: match by hotel name displayed in the UI.
        if (hotelName != null && !hotelName.isBlank()) {
            String normalized = hotelName.trim();
            for (Hotel h : state.hotelService.getAll()) {
                if (h.getName() != null && h.getName().trim().equalsIgnoreCase(normalized)) {
                    return h.getId();
                }
            }
        }
        return null;
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
        Label iconLabel  = new Label(icon);   iconLabel.getStyleClass().add("empty-icon");
        Label titleLabel = new Label(title);  titleLabel.getStyleClass().add("empty-title");
        Label subLabel   = new Label(subtitle); subLabel.getStyleClass().add("empty-subtitle");
        box.getChildren().addAll(iconLabel, titleLabel, subLabel);
        container.getChildren().add(box);
    }
}
