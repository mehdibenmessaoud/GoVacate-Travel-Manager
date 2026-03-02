package tn.esprit.projet.gui;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;

import tn.esprit.projet.entities.*;

import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * Manages the hotel card grid, hotel detail view, hotel services section,
 * and the rooms-availability section inside a hotel detail view.
 */
public class ClientHotelViewController {

    private final ClientSharedState        state;
    private final DialogHelper.ClientDialogs       dialogs;
    private final ClientReviewViewController reviewVC;
    private final Function<String, Image>  imageLoader;
    /** Callback: tell ClientController to show the room detail view. */
    private final ClientController3 controller;

    public ClientHotelViewController(
            ClientSharedState        state,
            DialogHelper.ClientDialogs       dialogs,
            ClientReviewViewController reviewVC,
            Function<String, Image>  imageLoader,
            ClientController3 controller
    ) {
        this.state      = state;
        this.dialogs    = dialogs;
        this.reviewVC   = reviewVC;
        this.imageLoader = imageLoader;
        this.controller = controller;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Hotel card grid
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Loads and filters all hotels into the container.
     * Returns the count of displayed cards.
     */
    public int loadHotels(FlowPane container, Label sectionTitle,
                          TextField searchField, ComboBox<String> starsFilterCombo,
                          ComboBox<String> destinationFilterCombo, ComboBox<String> statusFilterCombo) {
        container.getChildren().clear();
        try {
            List<Hotel> hotels = state.hotelService.getAll();
            String search      = searchField.getText() != null ? searchField.getText().toLowerCase().trim() : "";
            String starsFilter = starsFilterCombo.getValue();
            String destFilter  = destinationFilterCombo != null ? destinationFilterCombo.getValue() : null;
            String statFilter  = statusFilterCombo      != null ? statusFilterCombo.getValue()      : null;
            int count = 0;

            for (Hotel hotel : hotels) {
                String destLabel = state.resolveDestinationLabel(hotel.getLocationId());

                boolean matchSearch = search.isEmpty()
                        || hotel.getName().toLowerCase().contains(search)
                        || hotel.getDescription().toLowerCase().contains(search)
                        || destLabel.toLowerCase(Locale.ROOT).contains(search);
                boolean matchStars  = starsFilter == null || starsFilter.equals("Toutes")
                        || starsFilter.startsWith(String.valueOf(hotel.getStars()));
                boolean matchDest   = destFilter  == null || destFilter.equals("Toutes localisations")
                        || destFilter.equals(destLabel);
                boolean matchStatus = statFilter  == null || statFilter.equals("Tous")
                        || hotel.getStatus().equals(statFilter);

                if (matchSearch && matchStars && matchDest && matchStatus) {
                    container.getChildren().add(createHotelCard(hotel));
                    count++;
                }
            }

            sectionTitle.setText(count + " Hotel(s) disponibles");
            if (count == 0) showEmptyState(container, "", "Aucun hotel trouve", "Essayez de changer vos filtres");
            return count;
        } catch (SQLException e) {
            showEmptyState(container, "", "Erreur de chargement", e.getMessage());
            return 0;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Hotel card
    // ─────────────────────────────────────────────────────────────────────────

    public StackPane createHotelCard(Hotel hotel) {
        StackPane card = new StackPane();
        card.setMinWidth(320); card.setPrefWidth(320); card.setMaxWidth(320);
        card.setMinHeight(410); card.setPrefHeight(410); card.setMaxHeight(410);
        card.getStyleClass().add("water-card");
        card.setCursor(javafx.scene.Cursor.HAND);
        applyRoundedClip(card, 20);

        VBox content = new VBox(0);
        content.setAlignment(Pos.TOP_CENTER);
        content.setMinWidth(320); content.setPrefWidth(320); content.setMaxWidth(320);

        StackPane header = new StackPane();
        header.setMinHeight(220); header.setPrefHeight(220); header.setMaxHeight(220);
        header.setMinWidth(320); header.setPrefWidth(320); header.setMaxWidth(320);
        header.setPadding(Insets.EMPTY);
        header.getStyleClass().addAll("card-header", "stars-" + hotel.getStars());
        applyRoundedClip(header, 24);

        // Image or placeholder
        List<HotelImage> images = List.of();
        int imageCount = 0;
        boolean hasImage = false;
        try {
            images = state.hotelImageService.getByHotelId(hotel.getId());
            imageCount = images.size();
            if (!images.isEmpty()) {
                Image img = imageLoader.apply(images.get(0).getImageUrl());
                if (img != null) {
                    ImageView iv = new ImageView();
                    iv.setSmooth(true);
                    iv.getStyleClass().add("card-header-image");
                    iv.setManaged(false);
                    applyCoverFit(iv, img, 320, 220);
                    header.getChildren().add(iv);
                    hasImage = true;
                }
            }
        } catch (SQLException ignored) { /* use placeholder */ }

        if (!hasImage) {
            VBox ph = createCardImagePlaceholder("Photo a venir", "Aucune image disponible");
            ph.setPrefSize(320, 220); ph.setMaxSize(320, 220);
            ph.getStyleClass().add("hotel-card-placeholder");
            header.getChildren().add(ph);
        }

        Region overlay = new Region();
        overlay.getStyleClass().add("card-header-overlay");
        overlay.setPrefSize(320, 220);
        overlay.setMouseTransparent(true);
        header.getChildren().add(overlay);

        Label starsBadge = new Label(GuiUtils.formatStarsWithScore(hotel.getStars()));
        starsBadge.getStyleClass().add("card-stars-badge");
        StackPane.setAlignment(starsBadge, Pos.TOP_LEFT);
        StackPane.setMargin(starsBadge, new Insets(10));
        header.getChildren().add(starsBadge);

        // Glass info panel
        VBox glass = new VBox(10);
        glass.getStyleClass().add("glass-card");
        glass.setPadding(new Insets(16, 18, 20, 18));
        glass.setAlignment(Pos.CENTER);
        glass.setMinHeight(190); glass.setPrefHeight(190);
        glass.setMinWidth(320); glass.setPrefWidth(320); glass.setMaxWidth(320);

        Label title = new Label(hotel.getName());
        title.getStyleClass().add("card-title");
        title.setMaxWidth(Double.MAX_VALUE);
        title.setAlignment(Pos.CENTER);

        Label desc = new Label(hotel.getDescription());
        desc.getStyleClass().add("card-sub");
        desc.setWrapText(true);
        desc.setMinHeight(44); desc.setPrefHeight(44); desc.setMaxHeight(44);
        desc.setMaxWidth(286);
        desc.setAlignment(Pos.CENTER);
        desc.getStyleClass().add("hotel-card-desc-center");

        HBox info = new HBox(12);
        info.setAlignment(Pos.CENTER);

        Label stars = new Label(GuiUtils.formatStarsWithScore(hotel.getStars()));
        stars.getStyleClass().add("card-rate");

        Label statusBadge = new Label(GuiUtils.getStatusLabel(hotel.getStatus()));
        statusBadge.getStyleClass().add(GuiUtils.getStatusBadgeClass(hotel.getStatus()));
        info.getChildren().addAll(stars, statusBadge);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        HBox actions = new HBox(12);
        actions.getStyleClass().add("card-actions");
        actions.setPadding(new Insets(4, 0, 0, 0));
        actions.setAlignment(Pos.CENTER);

        Button detailsBtn = new Button("Details");
        detailsBtn.getStyleClass().add("btn-card-secondary");
        detailsBtn.setMinWidth(128); detailsBtn.setPrefWidth(128); detailsBtn.setMinHeight(44);
        detailsBtn.setOnAction(e -> showHotelDetails(hotel));

        Button roomsBtn = new Button("Chambres");
        roomsBtn.getStyleClass().add("btn-card-primary");
        roomsBtn.setMinWidth(128); roomsBtn.setPrefWidth(128); roomsBtn.setMinHeight(44);
        roomsBtn.setOnAction(e -> controller.goToRoomsForHotel(hotel));

        actions.getChildren().addAll(detailsBtn, roomsBtn);
        glass.getChildren().addAll(title, desc, info, spacer, actions);

        content.getChildren().addAll(header, glass);
        card.getChildren().add(content);
        card.setOnMouseClicked(e -> { if (e.getClickCount() == 2) showHotelDetails(hotel); });

        return card;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Hotel detail view
    // ─────────────────────────────────────────────────────────────────────────

    public void showHotelDetails(Hotel hotel) {
        controller.showSearchPanel(false);
        FlowPane container = controller.getHotelsContainer();
        container.getChildren().clear();
        controller.getSectionTitle().setText("Details hotel");

        List<Room> hotelRooms = getRoomsByHotel(hotel.getId());

        VBox detailsBox = new VBox(25);
        detailsBox.setPadding(new Insets(10));
        detailsBox.setMaxWidth(1160);

        Button backBtn = new Button("Retour aux hotels");
        backBtn.getStyleClass().addAll("btn-book", "hotel-detail-back-btn");
        backBtn.setOnAction(e -> controller.goToHotels());

        // Image gallery + info
        HBox mainSection = new HBox(25);
        mainSection.setAlignment(Pos.TOP_LEFT);

        List<HotelImage> images = List.of();
        try { images = state.hotelImageService.getByHotelId(hotel.getId()); } catch (SQLException ignored) {}
        StackPane mainImageContainer = new StackPane();
        mainImageContainer.setPrefSize(520, 330);
        mainImageContainer.getStyleClass().add("gallery-main-container");
        HBox thumbnailsContainer = new HBox(10);
        thumbnailsContainer.setAlignment(Pos.CENTER_LEFT);
        VBox imageGallery = new VBox(10, mainImageContainer, thumbnailsContainer);
        imageGallery.setPrefWidth(520);
        ImageGalleryBuilder.build(
                images,
                HotelImage::getImageUrl,
                mainImageContainer,
                thumbnailsContainer,
                "#FF8210",
                "H",
                getClass(),
                url -> dialogs.showImagePreview(url, "Image hotel")
        );

        VBox infoBox = buildHotelInfoBox(hotel);
        mainSection.getChildren().addAll(imageGallery, infoBox);

        VBox servicesSection     = createHotelServicesSection(hotel);
        VBox availabilitySection = createRoomsAvailabilitySection(hotel, hotelRooms);
        VBox reviewsSection      = reviewVC.buildReviewsSection(hotel);

        detailsBox.getChildren().addAll(backBtn, mainSection, servicesSection, availabilitySection, reviewsSection);
        container.getChildren().add(detailsBox);
    }

    private VBox buildHotelInfoBox(Hotel hotel) {
        VBox infoBox = new VBox(15);
        infoBox.setPadding(new Insets(10));
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        Label nameLabel = new Label(hotel.getName());
        nameLabel.getStyleClass().add("hotel-detail-name");

        HBox starsRow = new HBox(12);
        starsRow.setAlignment(Pos.CENTER_LEFT);
        Label starsLabel = new Label(GuiUtils.renderStarsVisual(hotel.getStars()));
        starsLabel.getStyleClass().add("hotel-detail-stars");
        Label starsText = new Label("(" + GuiUtils.clampStars(hotel.getStars()) + "/5)");
        starsText.getStyleClass().add("hotel-detail-stars-text");
        starsRow.getChildren().addAll(starsLabel, starsText);

        Label descLabel = new Label(hotel.getDescription());
        descLabel.getStyleClass().add("hotel-detail-desc");
        descLabel.setWrapText(true);

        Label statusLabel = new Label(GuiUtils.getStatusLabel(hotel.getStatus()));
        statusLabel.getStyleClass().addAll("hotel-detail-status", GuiUtils.getStatusBadgeClass(hotel.getStatus()));

        infoBox.getChildren().addAll(nameLabel, starsRow, descLabel, statusLabel);

        // ── Map Button ────────────────────────────────────────────────────────
        Button mapBtn = new Button("\uD83D\uDCCD  Voir sur la Carte");
        mapBtn.getStyleClass().add("btn-map-location");
        mapBtn.setOnAction(e -> {
            mapBtn.setDisable(true);
            mapBtn.setText("\u23F3  Localisation...");
            // Call directly - showHotelOnMap already spawns a background thread
            controller.showHotelOnMap(hotel);
            // Restore button state after short delay
            javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(
                    javafx.util.Duration.millis(2000));
            pause.setOnFinished(ev -> {
                mapBtn.setDisable(false);
                mapBtn.setText("\uD83D\uDCCD  Voir sur la Carte");
            });
            pause.play();
        });

        infoBox.getChildren().add(mapBtn);

        return infoBox;
    }


    private VBox createHotelServicesSection(Hotel hotel) {
        VBox section = new VBox(10);
        section.getStyleClass().add("hotel-services-section");

        Label title = new Label("Services de l'hotel");
        title.getStyleClass().add("hotel-services-title");

        FlowPane servicesPane = new FlowPane();
        servicesPane.setHgap(10);
        servicesPane.setVgap(10);

        List<String> services = resolveHotelServices(hotel);
        if (services.isEmpty()) {
            Label empty = new Label("Aucun service configure pour cet hotel.");
            empty.getStyleClass().add("hotel-service-empty");
            servicesPane.getChildren().add(empty);
        } else {
            for (String service : services) {
                Label chip = new Label(service);
                chip.getStyleClass().add("hotel-service-chip");
                servicesPane.getChildren().add(chip);
            }
        }
        section.getChildren().addAll(title, servicesPane);
        return section;
    }

    private List<String> resolveHotelServices(Hotel hotel) {
        if (!state.databaseAvailable || state.hotelServiceItemService == null) return List.of();
        try {
            return state.hotelServiceItemService.getActiveByHotelId(hotel.getId()).stream()
                    .map(HotelServiceItem::getName)
                    .filter(n -> n != null && !n.isBlank())
                    .map(String::trim)
                    .distinct()
                    .toList();
        } catch (SQLException ignored) { return List.of(); }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Rooms availability section
    // ─────────────────────────────────────────────────────────────────────────

    private VBox createRoomsAvailabilitySection(Hotel hotel, List<Room> hotelRooms) {
        VBox section = new VBox(12);
        section.getStyleClass().add("availability-section");

        Label title = new Label("Disponibilite et tarifs");
        title.getStyleClass().add("availability-title");

        HBox controls = new HBox(10);
        controls.setAlignment(Pos.CENTER_LEFT);
        controls.getStyleClass().add("availability-controls");

        ComboBox<String> typeFilter = new ComboBox<>(
                FXCollections.observableArrayList("Tous", "SINGLE", "DOUBLE", "SUITE", "DELUXE", "FAMILY"));
        typeFilter.setValue("Tous");
        typeFilter.setPromptText("Type");
        typeFilter.setPrefWidth(150);
        typeFilter.getStyleClass().add("availability-filter");
        styleAvailabilityCombo(typeFilter);

        Spinner<Integer> guestsSpinner = new Spinner<>(1, 8, 1);
        guestsSpinner.setEditable(true);
        guestsSpinner.setPrefWidth(120);
        guestsSpinner.getStyleClass().add("availability-filter");
        guestsSpinner.getEditor().setOnAction(e ->
                guestsSpinner.getValueFactory().setValue(GuiUtils.parseGuestsInput(guestsSpinner.getEditor().getText())));
        guestsSpinner.focusedProperty().addListener((obs, was, is) -> {
            if (!is) guestsSpinner.getValueFactory().setValue(GuiUtils.parseGuestsInput(guestsSpinner.getEditor().getText()));
        });

        Spinner<Integer> nightsSpinner = new Spinner<>(1, 30, 1);
        nightsSpinner.setEditable(true);
        nightsSpinner.setPrefWidth(100);
        nightsSpinner.getStyleClass().add("availability-filter");
        nightsSpinner.getEditor().setOnAction(e ->
                nightsSpinner.getValueFactory().setValue(GuiUtils.parseNightsInput(nightsSpinner.getEditor().getText())));
        nightsSpinner.focusedProperty().addListener((obs, was, is) -> {
            if (!is) nightsSpinner.getValueFactory().setValue(GuiUtils.parseNightsInput(nightsSpinner.getEditor().getText()));
        });

        Button resetBtn = new Button("Reinitialiser");
        resetBtn.getStyleClass().add("availability-reset-btn");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        controls.getChildren().addAll(
                filterLabel("Type"), typeFilter,
                filterLabel("Voyageurs"), guestsSpinner,
                filterLabel("Nuits"), nightsSpinner,
                resetBtn, spacer
        );

        VBox roomsBox = new VBox(10);

        Runnable refresh = () -> {
            roomsBox.getChildren().clear();
            String selType = typeFilter.getValue();
            int    guests  = guestsSpinner.getValue();
            int    nights  = nightsSpinner.getValue();

            List<Room> filtered = hotelRooms.stream()
                    .filter(r -> "AVAILABLE".equals(r.getStatus()))
                    .filter(r -> selType == null || selType.equals("Tous") || selType.equals(r.getRoomType()))
                    .filter(r -> r.getCapacity() >= guests)
                    .sorted((a, b) -> Double.compare(a.getPricePerNight(), b.getPricePerNight()))
                    .toList();

            if (filtered.isEmpty()) {
                Label empty = new Label("Aucune chambre ne correspond a vos criteres.");
                empty.getStyleClass().add("hotel-service-empty");
                roomsBox.getChildren().add(empty);
                return;
            }
            for (Room room : filtered) roomsBox.getChildren().add(createRoomAvailabilityRow(room, hotel.getName(), nights));
        };

        typeFilter.setOnAction(e -> refresh.run());
        guestsSpinner.valueProperty().addListener((obs, o, n) -> refresh.run());
        nightsSpinner.valueProperty().addListener((obs, o, n) -> refresh.run());
        resetBtn.setOnAction(e -> {
            typeFilter.setValue("Tous");
            guestsSpinner.getValueFactory().setValue(1);
            nightsSpinner.getValueFactory().setValue(1);
            refresh.run();
        });
        refresh.run();

        section.getChildren().addAll(title, controls, roomsBox);
        return section;
    }

    private HBox createRoomAvailabilityRow(Room room, String hotelName, int nights) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("availability-room-row");
        row.setCursor(javafx.scene.Cursor.HAND);
        row.setOnMouseClicked(ev -> {
            if (ev.getClickCount() == 2 && !isClickInsideButton(ev.getTarget()))
                controller.showRoomDetails(room, hotelName);
        });

        VBox left = new VBox(2);
        Label roomTitle = new Label("Chambre " + room.getRoomNumber() + " - " + room.getRoomType());
        roomTitle.getStyleClass().add("availability-room-title");
        Label meta = new Label("Capacite: " + room.getCapacity() + " pers.");
        meta.getStyleClass().add("availability-room-meta");
        left.getChildren().addAll(roomTitle, meta);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label status = new Label("Disponible");
        status.getStyleClass().add("badge-available");

        double total = room.getPricePerNight() * Math.max(1, nights);
        Label price = new Label(GuiUtils.formatPrice(room.getPricePerNight()) + " DT / nuit | Total: " + GuiUtils.formatPrice(total) + " DT");
        price.getStyleClass().add("availability-room-price");

        Button detailsBtn = new Button("Details");
        detailsBtn.getStyleClass().add("btn-card-secondary");
        detailsBtn.setOnAction(e -> controller.showRoomDetails(room, hotelName));

        Button reserveBtn = new Button("Reserver");
        reserveBtn.getStyleClass().add("btn-book");
        boolean available = "AVAILABLE".equals(room.getStatus());
        reserveBtn.setDisable(!available);
        if (available) reserveBtn.setOnAction(e -> showBooking(room, hotelName, nights));

        row.getChildren().addAll(left, spacer, status, price, detailsBtn, reserveBtn);
        return row;
    }

    private void showBooking(Room room, String hotelName, int nights) {
        controller.showBookingForm(room, hotelName);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    private List<Room> getRoomsByHotel(int hotelId) {
        if (!state.databaseAvailable || state.roomService == null) return List.of();
        try {
            return state.roomService.getAll().stream().filter(r -> r.getHotelId() == hotelId).toList();
        } catch (SQLException e) { return List.of(); }
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

    private void applyRoundedClip(Region region, double arc) {
        Rectangle clip = new Rectangle();
        clip.setArcWidth(arc); clip.setArcHeight(arc);
        clip.widthProperty().bind(region.widthProperty());
        clip.heightProperty().bind(region.heightProperty());
        region.setClip(clip);
    }

    private void applyCoverFit(ImageView view, Image image, double w, double h) {
        view.setFitWidth(w); view.setFitHeight(h);
        view.setPreserveRatio(false); view.setSmooth(true);
        view.setImage(image);
    }

    private VBox createCardImagePlaceholder(String title, String subtitle) {
        VBox ph = new VBox(4);
        ph.setAlignment(Pos.CENTER);
        ph.setMouseTransparent(true);
        ph.getStyleClass().add("card-placeholder-box");
        Label icon  = new Label("PHOTO"); icon.getStyleClass().add("card-placeholder-icon");
        Label titleL = new Label(title);  titleL.getStyleClass().add("card-placeholder-title");
        Label subL   = new Label(subtitle); subL.getStyleClass().add("card-placeholder-sub");
        ph.getChildren().addAll(icon, titleL, subL);
        return ph;
    }

    private Label filterLabel(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("availability-filter-label");
        return l;
    }

    private void styleAvailabilityCombo(ComboBox<String> cb) {
        cb.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item);
                setStyle("-fx-background-color: transparent; -fx-text-fill: #f5fbff; -fx-font-weight: 700;");
            }
        });
        cb.setCellFactory(list -> new ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); getStyleClass().add("availability-combo-cell"); }
                else { setText(item); getStyleClass().add("availability-combo-cell-selected"); }
            }
        });
        cb.showingProperty().addListener((obs, was, is) -> {
            if (!is) return;
            Node lv = cb.lookup(".list-view");
            if (lv instanceof ListView<?> listView)
                listView.getStyleClass().add("availability-combo-popup");
        });
    }

    private boolean isClickInsideButton(Object target) {
        if (!(target instanceof Node node)) return false;
        Node current = node;
        while (current != null) {
            if (current instanceof ButtonBase) return true;
            current = current.getParent();
        }
        return false;
    }
}
