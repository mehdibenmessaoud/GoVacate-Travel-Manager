package tn.esprit.projet.gui;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import tn.esprit.projet.entities.*;
import tn.esprit.projet.services.*;

import java.io.File;
import java.net.URL;
import java.sql.SQLException;
import java.util.*;

/**
 * Handles all Hotel-related UI logic:
 * – Table setup / filtering
 * – Detail view population
 * – Add / Edit / Delete dialogs
 * – Hotel images (gallery + table)
 * – Hotel services management dialog
 *
 * Delegates pure shared state (hotelsList, roomsList, localisation maps)
 * to AdminSharedState and uses AdminDialogHelper for all dialog chrome.
 */
public class AdminHotelViewController {

    private final AdminSharedState state;
    private final AdminController admin; // for navigation callbacks

    // ── services ─────────────────────────────────────────────────────────────
    private final HotelService hotelService;
    private final HotelImageService hotelImageService;
    private final HotelServiceItemService hotelServiceItemService;

    public AdminHotelViewController(AdminSharedState state, AdminController admin,
                                    HotelService hotelService,
                                    HotelImageService hotelImageService,
                                    HotelServiceItemService hotelServiceItemService) {
        this.state = state;
        this.admin = admin;
        this.hotelService = hotelService;
        this.hotelImageService = hotelImageService;
        this.hotelServiceItemService = hotelServiceItemService;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // TABLE
    // ═══════════════════════════════════════════════════════════════════════

    public void setupHotelsTable(TableView<Object> mainTable,
                                 TableColumn<Object, String> col1,
                                 TableColumn<Object, String> col2,
                                 TableColumn<Object, String> col3,
                                 TableColumn<Object, String> col4,
                                 TableColumn<Object, String> col5,
                                 TableColumn<Object, Object> col6) {
        col1.setVisible(true); col2.setVisible(true); col3.setVisible(true);
        col4.setVisible(true); col5.setVisible(true); col6.setVisible(true);

        // ── Professional best practice: bind column widths as % of table width ──
        // Percentages: Nom 13% | Description 22% | Etoiles 13% | Statut 9% | Localisation 15% | Actions 28%
        if (mainTable != null) {
            mainTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
            // Unbind previous bindings before re-binding (guard for re-entry)
            col1.prefWidthProperty().unbind(); col2.prefWidthProperty().unbind();
            col3.prefWidthProperty().unbind(); col4.prefWidthProperty().unbind();
            col5.prefWidthProperty().unbind(); col6.prefWidthProperty().unbind();

            // Reset any maxWidth constraints left by Geoapify mode
            col1.setMaxWidth(Double.MAX_VALUE); col2.setMaxWidth(Double.MAX_VALUE);
            col3.setMaxWidth(Double.MAX_VALUE); col4.setMaxWidth(Double.MAX_VALUE);
            col5.setMaxWidth(Double.MAX_VALUE); col6.setMaxWidth(Double.MAX_VALUE);

            // Prevent user from manually dragging column widths (keeps proportions clean)
            col1.setResizable(false); col2.setResizable(false); col3.setResizable(false);
            col4.setResizable(false); col5.setResizable(false); col6.setResizable(false);

            // Defer binding to Platform.runLater — on first load the table width is 0
            // (not yet laid out). runLater fires after the scene has done its first layout
            // pass so the binding immediately resolves to the correct pixel value.
            final TableView<Object> tbl = mainTable;
            javafx.application.Platform.runLater(() -> {
                javafx.beans.binding.DoubleBinding usable = tbl.widthProperty().subtract(18);
                col1.prefWidthProperty().bind(usable.multiply(0.13));
                col2.prefWidthProperty().bind(usable.multiply(0.22));
                col3.prefWidthProperty().bind(usable.multiply(0.13));
                col4.prefWidthProperty().bind(usable.multiply(0.09));
                col5.prefWidthProperty().bind(usable.multiply(0.15));
                col6.prefWidthProperty().bind(usable.multiply(0.28));
                // 0.13+0.22+0.13+0.09+0.15+0.28 = 1.00 → always fills exactly 100%
            });
        }

        col1.setText("Nom"); col2.setText("Description"); col3.setText("Etoiles");
        col4.setText("Statut"); col5.setText("Localisation"); col6.setText("Actions");

        col1.setCellValueFactory(data ->
                data.getValue() instanceof Hotel h ? new SimpleStringProperty(h.getName()) : new SimpleStringProperty(""));
        // ── CRITICAL: reset cellFactory — Geoapify sets a custom one on col1
        // that checks instanceof HotelPlace; without this reset, Hotel rows show blank.
        AdminDialogHelper.applyPlainTextCellFactory(col1);

        col2.setCellValueFactory(data -> {
            if (data.getValue() instanceof Hotel h) {
                String desc = h.getDescription();
                return new SimpleStringProperty(desc == null ? "" : desc);
            }
            return new SimpleStringProperty("");
        });
        AdminDialogHelper.applyPlainTextCellFactory(col2);

        col3.setCellValueFactory(data ->
                data.getValue() instanceof Hotel h
                        ? new SimpleStringProperty(AdminUtils.formatStarsWithScore(h.getStars()))
                        : new SimpleStringProperty(""));
        AdminDialogHelper.applyPlainTextCellFactory(col3);

        col4.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) { setGraphic(null); return; }
                if (getTableRow().getItem() instanceof Hotel hotel) {
                    Label badge = new Label(AdminUtils.getStatusLabel(hotel.getStatus()));
                    badge.getStyleClass().add(AdminUtils.getStatusStyleClass(hotel.getStatus()));
                    setGraphic(badge);
                } else { setGraphic(null); }
            }
        });
        col4.setCellValueFactory(data ->
                data.getValue() instanceof Hotel h ? new SimpleStringProperty(h.getStatus()) : new SimpleStringProperty(""));

        col5.setCellValueFactory(data ->
                data.getValue() instanceof Hotel h
                        ? new SimpleStringProperty(state.resolveLocalisationLabel(h.getLocationId()))
                        : new SimpleStringProperty(""));
        AdminDialogHelper.applyPlainTextCellFactory(col5);

        col6.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue()));
        col6.setCellFactory(col -> new TableCell<>() {
            private final Button viewBtn = AdminDialogHelper.createTableActionButton("Details", AdminDialogHelper.BLUE_BTN, AdminDialogHelper.BLUE_BTN_HOVER, 88);
            private final Button editBtn = AdminDialogHelper.createTableActionButton("Modifier", AdminDialogHelper.ORANGE_BTN, AdminDialogHelper.ORANGE_BTN_HOVER, 96);
            private final Button deleteBtn = AdminDialogHelper.createTableActionButton("Supprimer", AdminDialogHelper.RED_BTN, AdminDialogHelper.RED_BTN_HOVER, 106);
            private final HBox box = new HBox(10, viewBtn, editBtn, deleteBtn);
            { viewBtn.setTooltip(new Tooltip("Voir les details de cet hotel")); editBtn.setTooltip(new Tooltip("Modifier les informations")); deleteBtn.setTooltip(new Tooltip("Supprimer cet hotel")); box.setAlignment(Pos.CENTER); }

            @Override protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || !(item instanceof Hotel hotel)) { setGraphic(null); return; }
                viewBtn.setOnAction(e -> admin.showHotelDetail(hotel));
                editBtn.setOnAction(e -> showEditHotelDialog(hotel));
                deleteBtn.setOnAction(e -> AdminDialogHelper.confirmDelete("hotel", hotel.getName(), () -> handleDeleteHotel(hotel), getClass()));
                setGraphic(box);
            }
        });
    }

    public void loadHotelsTable(TableView<Object> mainTable,
                                TextField searchField,
                                ComboBox<String> starsFilterCombo,
                                ComboBox<String> roomTypeFilterCombo,
                                ComboBox<String> roomStatusFilterCombo,
                                Label pageSubtitle) {
        String search = searchField.getText() != null ? searchField.getText().toLowerCase().trim() : "";
        String starsFilter = starsFilterCombo.getValue();
        String localisationFilter = roomTypeFilterCombo != null ? roomTypeFilterCombo.getValue() : null;
        String statusFilter = roomStatusFilterCombo != null ? roomStatusFilterCombo.getValue() : null;
        ObservableList<Object> filtered = FXCollections.observableArrayList();

        for (Hotel h : state.getHotelsList()) {
            boolean matchSearch = search.isEmpty() || h.getName().toLowerCase().contains(search)
                    || h.getDescription().toLowerCase().contains(search);
            boolean matchStars = starsFilter == null || starsFilter.equals("Toutes") || starsFilter.startsWith(String.valueOf(h.getStars()));
            boolean matchDestination = localisationFilter == null || localisationFilter.equals("Toutes localisations")
                    || localisationFilter.equals(state.resolveLocalisationLabel(h.getLocationId()));
            boolean matchStatus = statusFilter == null || statusFilter.equals("Tous") || h.getStatus().equals(statusFilter);
            if (matchSearch && matchStars && matchDestination && matchStatus) filtered.add(h);
        }
        mainTable.setItems(filtered);
        pageSubtitle.setText(filtered.size() + " hotel(s) trouve(s)  -  Double-cliquez pour details");
    }

    public void setupStarsFilter(ComboBox<String> starsFilterCombo) {
        starsFilterCombo.getItems().clear();
        starsFilterCombo.getItems().addAll("Toutes", "5 etoiles", "4 etoiles", "3 etoiles", "2 etoiles", "1 etoile");
        starsFilterCombo.setValue("Toutes");
    }

    // ═══════════════════════════════════════════════════════════════════════
    // DETAIL VIEW
    // ═══════════════════════════════════════════════════════════════════════

    public void populateHotelDetail(Hotel hotel,
                                    Label detailName, Label detailSubInfo, HBox detailApiRow,
                                    Label detailDesc,
                                    FlowPane detailBadges, FlowPane detailActionButtons,
                                    VBox detailServicesSection, Label detailServicesTitle,
                                    FlowPane detailServicesPane,
                                    Button detailManageServicesBtn,
                                    VBox reviewsSection, VBox imagesSection, VBox roomImagesSection,
                                    TableView<HotelImage> imagesTable,
                                    StackPane mainImageContainer, HBox thumbnailsContainer,
                                    TableView<HotelReview> reviewsTable) {

        detailName.setText(hotel.getName());
        detailSubInfo.setText(AdminUtils.formatStarsWithScore(hotel.getStars()));
        detailSubInfo.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: #FFBD59;");
        detailDesc.setText(hotel.getDescription());

        // ── API insight row — cleared, map is accessed via the 🗺 Carte button on demand ─
        if (detailApiRow != null) detailApiRow.getChildren().clear();

        detailBadges.getChildren().clear();
        Label statusBadge = new Label(AdminUtils.getStatusLabel(hotel.getStatus()));
        statusBadge.getStyleClass().add(AdminUtils.getStatusStyleClass(hotel.getStatus()));
        statusBadge.setStyle("-fx-font-size: 13px;");
        Label locationBadge = new Label(state.resolveLocalisationLabel(hotel.getLocationId()));
        locationBadge.setStyle("-fx-background-color: rgba(103,154,193,0.3); -fx-text-fill: #679AC1; -fx-padding: 6 14; -fx-background-radius: 15; -fx-font-size: 12px;");
        int roomCount = (int) state.getRoomsList().stream().filter(r -> r.getHotelId() == hotel.getId()).count();
        Label roomsBadge = new Label(roomCount + " chambre(s)");
        roomsBadge.setStyle("-fx-background-color: rgba(255,130,16,0.3); -fx-text-fill: #FF8210; -fx-padding: 6 14; -fx-background-radius: 15; -fx-font-size: 12px;");
        int servicesCount = getServiceCountForHotel(hotel.getId());
        Label servicesBadge = new Label(servicesCount + " service(s)");
        servicesBadge.setStyle("-fx-background-color: rgba(40,167,69,0.25); -fx-text-fill: #7ce19b; -fx-padding: 6 14; -fx-background-radius: 15; -fx-font-size: 12px;");
        detailBadges.getChildren().addAll(statusBadge, locationBadge, roomsBadge, servicesBadge);

        if (detailServicesSection != null) { detailServicesSection.setVisible(true); detailServicesSection.setManaged(true); }
        populateDetailServices(hotel.getId(), detailServicesSection, detailServicesTitle, detailServicesPane);

        detailActionButtons.getChildren().clear();
        detailActionButtons.setHgap(12); detailActionButtons.setVgap(12);
        detailActionButtons.setPrefWrapLength(620); detailActionButtons.setAlignment(Pos.CENTER_LEFT);

        if (detailManageServicesBtn != null) {
            detailManageServicesBtn.setText("Gerer services");
            detailManageServicesBtn.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-background-radius: 20; -fx-padding: 8 16; -fx-font-size: 12px; -fx-font-weight: 700; -fx-cursor: hand;");
            detailManageServicesBtn.setOnAction(e -> showHotelServicesDialog(hotel));
        }

        Button editBtn = AdminDialogHelper.createDetailActionButton("Modifier l'hotel", "#FF8210");
        editBtn.setOnAction(e -> showEditHotelDialog(hotel));
        Button roomsBtn = AdminDialogHelper.createDetailActionButton("Voir les chambres", "#679AC1");
        roomsBtn.setOnAction(e -> admin.navigateToRoomsFilteredByHotel(hotel));
        Button deleteBtn = AdminDialogHelper.createDetailActionButton("Supprimer", "#dc3545");
        deleteBtn.setOnAction(e -> AdminDialogHelper.confirmDelete("hotel", hotel.getName(), () -> {
            handleDeleteHotel(hotel);
            admin.showHotelsView();
        }, getClass()));

        // ── Voir sur la Carte (OSM) — same style as client view ──────────
        Button mapBtn = new Button("\uD83D\uDCCD  Voir sur la Carte");
        mapBtn.getStyleClass().add("btn-map-location");
        mapBtn.setOnAction(e -> {
            mapBtn.setDisable(true);
            mapBtn.setText("\u23F3  Localisation...");
            admin.openHotelMap(hotel);
            javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(
                    javafx.util.Duration.millis(2500));
            pause.setOnFinished(ev -> {
                mapBtn.setDisable(false);
                mapBtn.setText("\uD83D\uDCCD  Voir sur la Carte");
            });
            pause.play();
        });

        detailActionButtons.getChildren().addAll(editBtn, roomsBtn, deleteBtn, mapBtn);

        reviewsSection.setVisible(true); reviewsSection.setManaged(true);
        imagesSection.setVisible(true); imagesSection.setManaged(true);
        roomImagesSection.setVisible(false); roomImagesSection.setManaged(false);

        loadHotelImages(hotel.getId(), imagesTable, mainImageContainer, thumbnailsContainer);
    }

    private int getServiceCountForHotel(int hotelId) {
        try { return hotelServiceItemService.getActiveByHotelId(hotelId).size(); }
        catch (SQLException e) { return 0; }
    }

    private void populateDetailServices(int hotelId,
                                        VBox detailServicesSection, Label detailServicesTitle, FlowPane detailServicesPane) {
        if (detailServicesSection == null || detailServicesTitle == null || detailServicesPane == null) return;
        detailServicesSection.setVisible(true); detailServicesSection.setManaged(true);
        detailServicesTitle.setVisible(true); detailServicesTitle.setManaged(true);
        detailServicesPane.setVisible(true); detailServicesPane.setManaged(true);
        detailServicesPane.getChildren().clear();
        try {
            List<HotelServiceItem> services = hotelServiceItemService.getActiveByHotelId(hotelId);
            for (HotelServiceItem item : services) {
                String name = item.getName() == null ? "" : item.getName().trim();
                if (name.isEmpty()) continue;
                Label chip = new Label(name);
                chip.setStyle("-fx-background-color: rgba(103,154,193,0.22); -fx-text-fill: #d7ebfb; -fx-padding: 5 12; -fx-background-radius: 14; -fx-font-size: 12px;");
                detailServicesPane.getChildren().add(chip);
            }
            if (detailServicesPane.getChildren().isEmpty()) {
                Label empty = new Label("Aucun service actif");
                empty.setStyle("-fx-text-fill: rgba(255,255,255,0.55); -fx-font-size: 12px; -fx-font-style: italic;");
                detailServicesPane.getChildren().add(empty);
            }
        } catch (SQLException e) {
            Label error = new Label("Erreur de chargement des services");
            error.setStyle("-fx-text-fill: #FF8210; -fx-font-size: 12px;");
            detailServicesPane.getChildren().add(error);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // HOTEL CRUD DIALOGS
    // ═══════════════════════════════════════════════════════════════════════


    // HOTEL CRUD DIALOGS
    // ═══════════════════════════════════════════════════════════════════════

    public void showAddHotelDialog() {
        Dialog<Hotel> dialog = createHotelDialog(null);
        dialog.showAndWait().ifPresent(h -> runSqlAction(() -> {
            hotelService.create(h);
            admin.loadAllData();
            admin.loadHotelsTable();
            AdminDialogHelper.showNotification("Hotel cree avec succes!", "success", getClass());
        }));
    }

    public void showEditHotelDialog(Hotel hotel) {
        Dialog<Hotel> dialog = createHotelDialog(hotel);
        dialog.showAndWait().ifPresent(h -> runSqlAction(() -> {
            h.setId(hotel.getId());
            hotelService.update(h);
            admin.loadAllData();
            if (state.getSelectedHotel() != null && state.getSelectedHotel().getId() == hotel.getId()) {
                admin.showHotelDetail(h);
            } else {
                admin.loadHotelsTable();
            }
            AdminDialogHelper.showNotification("Hotel modifie!", "success", getClass());
        }));
    }

    private Dialog<Hotel> createHotelDialog(Hotel hotel) {
        final boolean isNew = (hotel == null);

        Dialog<Hotel> dialog = new Dialog<>();
        dialog.setTitle(isNew ? "Nouvel Hotel" : "Modifier l'Hotel");
        ButtonType saveBtn = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        GridPane grid = AdminDialogHelper.createDialogFormGrid();

        // ── Nom ────────────────────────────────────────────────────────────
        TextField nameField = new TextField(isNew ? "" : hotel.getName());
        nameField.setPromptText("Nom de l'hotel");
        AdminDialogHelper.applyDialogFieldSizing(nameField);

        // ── Description ────────────────────────────────────────────────────
        TextArea descField = new TextArea(isNew ? "" : hotel.getDescription());
        descField.setPromptText("Description");
        descField.setPrefRowCount(4);
        descField.setWrapText(true);
        descField.setPrefHeight(108);
        AdminDialogHelper.applyDialogFieldSizing(descField);

        // ── Etoiles ────────────────────────────────────────────────────────
        Spinner<Integer> starsSpinner = new Spinner<>(1, 5, isNew ? 3 : hotel.getStars());
        starsSpinner.setEditable(false);
        AdminDialogHelper.configureDialogSpinner(starsSpinner, 0);

        // ── Statut ─────────────────────────────────────────────────────────
        ComboBox<String> statusCombo = new ComboBox<>(
                FXCollections.observableArrayList("AVAILABLE", "OCCUPIED", "MAINTENANCE"));
        statusCombo.setValue(isNew ? "AVAILABLE" : hotel.getStatus());
        AdminDialogHelper.applyDialogFieldSizing(statusCombo);

        // ── Localisation — existing destinations + inline "create" button ──
        state.refreshLocalisationLookup();
        ComboBox<String> localisationCombo = new ComboBox<>();
        localisationCombo.getItems().addAll(state.getSortedLocalisationLabels());
        localisationCombo.setPromptText("Sélectionner une destination");
        AdminDialogHelper.applyDialogFieldSizing(localisationCombo);

        if (isNew) {
            localisationCombo.setValue(null);
        } else {
            // Modifier: restore the hotel's exact current destination
            localisationCombo.setValue(state.resolveLocalisationLabel(hotel.getLocationId()));
        }

        Button newDestBtn = buildNewDestinationButton(localisationCombo, null, null, dialog.getDialogPane());

        HBox locRow = new HBox(8, localisationCombo, newDestBtn);
        locRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(localisationCombo, Priority.ALWAYS);

        // ── Layout ─────────────────────────────────────────────────────────
        grid.add(new Label("Nom *"),          0, 0); grid.add(nameField,    1, 0);
        grid.add(new Label("Description"),    0, 1); grid.add(descField,    1, 1);
        grid.add(new Label("Etoiles"),        0, 2); grid.add(starsSpinner, 1, 2);
        grid.add(new Label("Statut"),         0, 3); grid.add(statusCombo,  1, 3);
        grid.add(new Label("Localisation *"), 0, 4); grid.add(locRow,       1, 4);

        // ── Save-button guard ──────────────────────────────────────────────
        Node saveButton = dialog.getDialogPane().lookupButton(saveBtn);
        Runnable syncSaveState = () -> saveButton.setDisable(
                AdminUtils.isBlank(nameField.getText())
                        || AdminUtils.isBlank(localisationCombo.getValue()));
        syncSaveState.run();
        nameField.textProperty().addListener((obs, old, val) -> syncSaveState.run());
        localisationCombo.valueProperty().addListener((obs, old, val) -> syncSaveState.run());

        dialog.getDialogPane().setContent(grid);
        AdminDialogHelper.applyDialogPaneSizing(dialog.getDialogPane(), 740, 430);
        AdminDialogHelper.styleDialog(dialog, false, getClass());
        Platform.runLater(nameField::requestFocus);

        // ── Result converter ───────────────────────────────────────────────
        dialog.setResultConverter(btn -> {
            if (btn != saveBtn) return null;
            // Modifier: fall back to the hotel's original locationId if label
            // resolution somehow fails (prevents accidental destination change)
            int fallback = isNew ? state.getDefaultDestinationId() : hotel.getLocationId();
            int locationId = state.resolveLocalisationId(localisationCombo.getValue(), fallback);
            return new Hotel(0,
                    AdminUtils.trimToEmpty(nameField.getText()),
                    descField.getText(),
                    starsSpinner.getValue(),
                    statusCombo.getValue(),
                    locationId);
        });
        return dialog;
    }

    public void handleDeleteHotel(Hotel hotel) {
        runSqlAction(() -> {
            hotelService.delete(hotel.getId());
            admin.loadAllData();
            admin.loadHotelsTable();
            AdminDialogHelper.showNotification("Hotel supprime!", "success", getClass());
        });
    }

    // ═══════════════════════════════════════════════════════════════════════
    // HOTEL SERVICES DIALOG
    // ═══════════════════════════════════════════════════════════════════════

    public void showHotelServicesDialog(Hotel hotel) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Services - " + hotel.getName());
        ButtonType closeBtn = new ButtonType("Fermer", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().add(closeBtn);

        VBox content = new VBox(14);
        content.setPadding(new Insets(12));
        content.getStyleClass().add("gv-services-dialog-content");

        Label subtitle = new Label("Gerez les services visibles cote client pour cet hotel.");
        subtitle.getStyleClass().add("gv-services-dialog-subtitle"); subtitle.setWrapText(true);
        Label countLabel = new Label();
        countLabel.getStyleClass().add("gv-services-count");
        Region subtitleSpacer = new Region(); HBox.setHgrow(subtitleSpacer, Priority.ALWAYS);
        HBox header = new HBox(10, subtitle, subtitleSpacer, countLabel);
        header.setAlignment(Pos.CENTER_LEFT);

        ListView<HotelServiceItem> servicesList = new ListView<>();
        servicesList.setPrefHeight(280); servicesList.getStyleClass().add("gv-service-list");
        servicesList.setPlaceholder(new Label("Aucun service configure pour cet hotel."));
        servicesList.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        servicesList.setFocusTraversable(true);
        servicesList.setOnMouseClicked(ev -> {
            if (servicesList.getSelectionModel().getSelectedItem() == null) {
                int focused = servicesList.getFocusModel().getFocusedIndex();
                if (focused >= 0 && focused < servicesList.getItems().size())
                    servicesList.getSelectionModel().select(focused);
            }
        });
        servicesList.setCellFactory(list -> new ListCell<>() {
            private final Label nameLabel = new Label(); private final Label stateBadge = new Label();
            private final Region spacer = new Region(); private final HBox row = new HBox(10);
            {
                nameLabel.getStyleClass().add("gv-service-name");
                stateBadge.getStyleClass().add("gv-service-status");
                HBox.setHgrow(spacer, Priority.ALWAYS);
                row.getChildren().addAll(nameLabel, spacer, stateBadge);
                row.setAlignment(Pos.CENTER_LEFT); row.getStyleClass().add("gv-service-row");
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            }
            @Override protected void updateItem(HotelServiceItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); return; }
                nameLabel.setText(item.getName());
                stateBadge.setText(item.isActive() ? "Actif" : "Inactif");
                stateBadge.getStyleClass().removeAll("gv-service-status-active", "gv-service-status-inactive");
                stateBadge.getStyleClass().add(item.isActive() ? "gv-service-status-active" : "gv-service-status-inactive");
                if (getListView() != null) row.setPrefWidth(Math.max(0, getListView().getWidth() - 34));
                setText(null); setGraphic(row);
            }
        });

        Runnable reloadServices = () -> {
            try {
                HotelServiceItem previous = resolveSelectedService(servicesList);
                int previousId = previous == null ? -1 : previous.getId();
                ObservableList<HotelServiceItem> items = FXCollections.observableArrayList(hotelServiceItemService.getByHotelId(hotel.getId()));
                servicesList.setItems(items);
                countLabel.setText(items.size() + " service(s)");
                if (!items.isEmpty()) {
                    int idx = -1;
                    if (previousId > 0) {
                        for (int i = 0; i < items.size(); i++) { if (items.get(i).getId() == previousId) { idx = i; break; } }
                    }
                    if (idx < 0) idx = 0;
                    servicesList.getSelectionModel().select(idx);
                    servicesList.getFocusModel().focus(idx);
                    servicesList.scrollTo(idx);
                }
            } catch (SQLException e) {
                AdminDialogHelper.showNotification("Erreur services: " + e.getMessage(), "error", getClass());
            }
        };
        reloadServices.run();

        HBox actions = new HBox(10); actions.setAlignment(Pos.CENTER_LEFT); actions.getStyleClass().add("gv-services-actions");
        Button addBtn = new Button("+ Ajouter"); addBtn.getStyleClass().addAll("gv-service-action-btn", "gv-service-action-add");
        addBtn.setOnAction(e -> {
            HotelServiceItem created = showHotelServiceFormDialog(hotel, null);
            if (created == null) return;
            try { hotelServiceItemService.create(created); reloadServices.run(); AdminDialogHelper.showNotification("Service ajoute!", "success", getClass()); }
            catch (SQLException ex) { AdminDialogHelper.showNotification("Erreur: " + ex.getMessage(), "error", getClass()); }
        });
        Button editBtn = new Button("Modifier"); editBtn.getStyleClass().addAll("gv-service-action-btn", "gv-service-action-edit");
        editBtn.setOnAction(e -> {
            HotelServiceItem selected = resolveSelectedService(servicesList);
            if (selected == null) { AdminDialogHelper.showNotification("Selectionnez un service a modifier", "warning", getClass()); return; }
            HotelServiceItem updated = showHotelServiceFormDialog(hotel, selected);
            if (updated == null) return;
            try { hotelServiceItemService.update(updated); reloadServices.run(); AdminDialogHelper.showNotification("Service modifie!", "success", getClass()); }
            catch (SQLException ex) { AdminDialogHelper.showNotification("Erreur: " + ex.getMessage(), "error", getClass()); }
        });
        Button toggleBtn = new Button("Activer / Desactiver"); toggleBtn.getStyleClass().addAll("gv-service-action-btn", "gv-service-action-toggle");
        toggleBtn.setOnAction(e -> {
            HotelServiceItem selected = resolveSelectedService(servicesList);
            if (selected == null) { AdminDialogHelper.showNotification("Selectionnez un service", "warning", getClass()); return; }
            try { selected.setActive(!selected.isActive()); hotelServiceItemService.update(selected); reloadServices.run(); AdminDialogHelper.showNotification("Statut du service mis a jour!", "success", getClass()); }
            catch (SQLException ex) { AdminDialogHelper.showNotification("Erreur: " + ex.getMessage(), "error", getClass()); }
        });
        Button deleteBtn = new Button("Supprimer"); deleteBtn.getStyleClass().addAll("gv-service-action-btn", "gv-service-action-delete");
        deleteBtn.setOnAction(e -> {
            HotelServiceItem selected = resolveSelectedService(servicesList);
            if (selected == null) { AdminDialogHelper.showNotification("Selectionnez un service a supprimer", "warning", getClass()); return; }
            AdminDialogHelper.confirmDelete("service", selected.getName(), () -> {
                try { hotelServiceItemService.delete(selected.getId()); reloadServices.run(); AdminDialogHelper.showNotification("Service supprime!", "success", getClass()); }
                catch (SQLException ex) { AdminDialogHelper.showNotification("Erreur: " + ex.getMessage(), "error", getClass()); }
            }, getClass());
        });
        actions.getChildren().addAll(addBtn, editBtn, toggleBtn, deleteBtn);
        content.getChildren().addAll(header, servicesList, actions);
        dialog.getDialogPane().setContent(content);
        AdminDialogHelper.applyDialogPaneSizing(dialog.getDialogPane(), 760, 460);
        AdminDialogHelper.styleDialog(dialog, false, getClass());
        dialog.showAndWait();
        admin.loadAllData();
        Hotel refreshedHotel = state.getSelectedHotel();
        if (refreshedHotel != null && refreshedHotel.getId() == hotel.getId()) admin.showHotelDetail(refreshedHotel);
    }

    private HotelServiceItem showHotelServiceFormDialog(Hotel hotel, HotelServiceItem existing) {
        Dialog<HotelServiceItem> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Nouveau service" : "Modifier service");
        ButtonType saveBtn = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);
        GridPane grid = AdminDialogHelper.createDialogFormGrid();
        TextField nameField = new TextField(existing == null ? "" : existing.getName());
        nameField.setPromptText("Nom du service"); AdminDialogHelper.applyDialogFieldSizing(nameField);
        CheckBox activeCheck = new CheckBox("Service actif");
        activeCheck.setSelected(existing == null || existing.isActive());
        activeCheck.setStyle("-fx-text-fill: #f8fbff; -fx-font-size: 14px; -fx-font-weight: bold;");
        grid.add(new Label("Hotel"), 0, 0); grid.add(new Label(hotel.getName()), 1, 0);
        grid.add(new Label("Nom *"), 0, 1); grid.add(nameField, 1, 1);
        grid.add(new Label("Etat"), 0, 2); grid.add(activeCheck, 1, 2);
        dialog.getDialogPane().setContent(grid);
        AdminDialogHelper.applyDialogPaneSizing(dialog.getDialogPane(), 700, 320);
        AdminDialogHelper.styleDialog(dialog, false, getClass());
        dialog.setResultConverter(btn -> {
            if (btn != saveBtn) return null;
            String serviceName = nameField.getText() == null ? "" : nameField.getText().trim();
            if (serviceName.isEmpty()) return null;
            int id = existing == null ? 0 : existing.getId();
            return new HotelServiceItem(id, hotel.getId(), serviceName, activeCheck.isSelected());
        });
        return dialog.showAndWait().orElse(null);
    }

    private HotelServiceItem resolveSelectedService(ListView<HotelServiceItem> servicesList) {
        if (servicesList == null) return null;
        HotelServiceItem selected = servicesList.getSelectionModel().getSelectedItem();
        if (selected != null) return selected;
        int focused = servicesList.getFocusModel().getFocusedIndex();
        if (focused >= 0 && focused < servicesList.getItems().size()) return servicesList.getItems().get(focused);
        return null;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // HOTEL IMAGES
    // ═══════════════════════════════════════════════════════════════════════

    public void setupImagesTable(TableColumn<HotelImage, String> colImagePath,
                                 TableColumn<HotelImage, Void> colImagePreview,
                                 TableColumn<HotelImage, Void> colImageActions) {
        colImagePath.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getImageUrl()));
        colImagePreview.setCellFactory(col -> new TableCell<>() {
            private final ImageView preview = new ImageView();
            { preview.setFitWidth(60); preview.setFitHeight(40); preview.setPreserveRatio(true); }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() >= getTableView().getItems().size()) { setGraphic(null); return; }
                Image image = ImageLoader.load(getTableView().getItems().get(getIndex()).getImageUrl(), getClass());
                if (image != null) { preview.setImage(image); setGraphic(preview); } else { setGraphic(new Label("!")); }
            }
        });
        colImageActions.setCellFactory(col -> new TableCell<>() {
            private final Button deleteBtn = AdminDialogHelper.createTableActionButton("Supprimer", AdminDialogHelper.RED_BTN, AdminDialogHelper.RED_BTN_HOVER, 96);
            { deleteBtn.setTooltip(new Tooltip("Supprimer cette image")); deleteBtn.setOnAction(e -> AdminDialogHelper.confirmDelete("image", "", () -> handleDeleteImage(getTableView().getItems().get(getIndex())), getClass())); }
            @Override protected void updateItem(Void item, boolean empty) { super.updateItem(item, empty); setGraphic(empty ? null : deleteBtn); }
        });
    }

    public void loadHotelImages(int hotelId, TableView<HotelImage> imagesTable,
                                StackPane mainImageContainer, HBox thumbnailsContainer) {
        mainImageContainer.getChildren().clear();
        thumbnailsContainer.getChildren().clear();
        try {
            List<HotelImage> images = hotelImageService.getByHotelId(hotelId);
            imagesTable.setItems(FXCollections.observableArrayList(images));
            ImageGalleryBuilder.build(images, img -> img.getImageUrl(),
                    mainImageContainer, thumbnailsContainer, "#FF8210", "H", getClass());
        } catch (SQLException e) {
            ImageGalleryBuilder.showPlaceholder("!", mainImageContainer);
        }
    }

    public void saveHotelImage(String imagePath, int hotelId, int hotelImgId,
                               TableView<HotelImage> imagesTable,
                               StackPane mainImageContainer, HBox thumbnailsContainer) throws SQLException {
        hotelImageService.createWithImagePipeline(imagePath, hotelId);
        loadHotelImages(hotelId, imagesTable, mainImageContainer, thumbnailsContainer);
    }

    private void handleDeleteImage(HotelImage image) {
        runSqlAction(() -> {
            hotelImageService.delete(image.getId());
            Hotel sel = state.getSelectedHotel();
            if (sel != null) AdminDialogHelper.showNotification("Image supprimee!", "success", getClass());
        });
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SHARED ADD IMAGE DIALOG (hotel side)
    // ═══════════════════════════════════════════════════════════════════════

    public void showAddImageDialog(Hotel selectedHotel,
                                   TableView<HotelImage> imagesTable,
                                   StackPane mainImageContainer, HBox thumbnailsContainer) {
        if (selectedHotel == null) return;
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Ajouter une image");
        dialog.setHeaderText("Selectionnez un fichier image ou collez une URL.");
        ButtonType addBtn = new ButtonType("Ajouter", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addBtn, ButtonType.CANCEL);

        GridPane grid = AdminDialogHelper.createDialogFormGrid();
        TextField pathField = new TextField();
        pathField.setPromptText("Chemin local ou URL");
        AdminDialogHelper.applyDialogFieldSizing(pathField);
        pathField.setPrefWidth(460);

        Button chooseBtn = new Button("Choisir image");
        chooseBtn.setStyle("-fx-background-color: #679AC1; -fx-text-fill: white; -fx-background-radius: 10; -fx-padding: 8 14; -fx-font-size: 12px; -fx-font-weight: bold;");
        chooseBtn.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Selectionner une image");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.webp", "*.gif", "*.bmp"));
            String existingPath = pathField.getText() == null ? "" : pathField.getText().trim();
            if (!existingPath.isBlank()) { try { File parent = new File(existingPath).getParentFile(); if (parent != null && parent.exists()) fileChooser.setInitialDirectory(parent); } catch (Exception ignored) {} }
            Window owner = dialog.getDialogPane().getScene() == null ? null : dialog.getDialogPane().getScene().getWindow();
            File selected = fileChooser.showOpenDialog(owner);
            if (selected != null) pathField.setText(selected.getAbsolutePath());
        });

        HBox inputRow = new HBox(10, pathField, chooseBtn);
        inputRow.setAlignment(Pos.CENTER_LEFT); HBox.setHgrow(pathField, Priority.ALWAYS);
        grid.add(new Label("Image *"), 0, 0); grid.add(inputRow, 1, 0);

        dialog.getDialogPane().setContent(grid);
        AdminDialogHelper.applyDialogPaneSizing(dialog.getDialogPane(), 760, 280);
        AdminDialogHelper.styleDialog(dialog, false, getClass());

        Node addButtonNode = dialog.getDialogPane().lookupButton(addBtn);
        addButtonNode.setDisable(true);
        pathField.textProperty().addListener((obs, oldText, newText) -> addButtonNode.setDisable(AdminUtils.isBlank(newText)));
        dialog.setResultConverter(btn -> btn == addBtn ? AdminUtils.trimToNull(pathField.getText()) : null);

        dialog.showAndWait().ifPresent(path -> {
            try {
                saveHotelImage(path, selectedHotel.getId(), 0, imagesTable, mainImageContainer, thumbnailsContainer);
                AdminDialogHelper.showNotification("Image ajoutee!", "success", getClass());
            } catch (IllegalArgumentException ex) { AdminDialogHelper.showNotification(ex.getMessage(), "warning", getClass()); }
            catch (SQLException ex) { AdminDialogHelper.showNotification("Erreur: " + ex.getMessage(), "error", getClass()); }
        });
    }


    private HBox offerRow(String icon, String key, String value, boolean alt) {
        HBox row = new HBox(0);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(13, 0, 13, 0));
        if (alt) row.setStyle("-fx-border-color: transparent transparent rgba(255,255,255,0.05) transparent; -fx-border-width: 0 0 1 0;");
        else     row.setStyle("-fx-border-color: transparent transparent rgba(255,255,255,0.05) transparent; -fx-border-width: 0 0 1 0;");

        Label iconLbl = new Label(icon);
        iconLbl.setStyle("-fx-font-size: 14px; -fx-min-width: 26;");

        Label keyLbl = new Label(key);
        keyLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: rgba(255,255,255,0.45); -fx-font-weight: 600; -fx-min-width: 110;");

        Label valLbl = new Label(value);
        valLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: rgba(255,255,255,0.88); -fx-font-weight: 600;");
        valLbl.setWrapText(true);
        HBox.setHgrow(valLbl, Priority.ALWAYS);

        row.getChildren().addAll(iconLbl, keyLbl, valLbl);
        return row;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // NEW DESTINATION HELPER
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Creates a small "➕" button that opens an inline dialog for creating a new
     * destination. After creation the destination is auto-selected in {@code combo}.
     *
     * @param combo       the localisation ComboBox to update after creation
     * @param prefCity    optional pre-filled city (may be null)
     * @param prefCountry optional pre-filled country (may be null)
     * @param owner       the parent DialogPane (used to resolve the CSS stylesheet)
     */
    Button buildNewDestinationButton(ComboBox<String> combo,
                                     String prefCity,
                                     String prefCountry,
                                     javafx.scene.control.DialogPane owner) {
        Button btn = new Button("➕");
        btn.setTooltip(new Tooltip("Créer une nouvelle destination"));
        btn.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-font-size: 13px;"
                + "-fx-background-radius: 8; -fx-padding: 6 10; -fx-cursor: hand; -fx-font-weight: bold;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #1e7e34; -fx-text-fill: white; -fx-font-size: 13px;"
                + "-fx-background-radius: 8; -fx-padding: 6 10; -fx-cursor: hand; -fx-font-weight: bold;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-font-size: 13px;"
                + "-fx-background-radius: 8; -fx-padding: 6 10; -fx-cursor: hand; -fx-font-weight: bold;"));

        btn.setOnAction(e -> {
            Dialog<String[]> destDialog = new Dialog<>();
            destDialog.setTitle("Nouvelle destination");
            ButtonType createBtn = new ButtonType("Créer", ButtonBar.ButtonData.OK_DONE);
            destDialog.getDialogPane().getButtonTypes().addAll(createBtn, ButtonType.CANCEL);

            GridPane dGrid = AdminDialogHelper.createDialogFormGrid();

            TextField nameF    = new TextField();
            nameF.setPromptText("Nom de la destination");
            AdminDialogHelper.applyDialogFieldSizing(nameF);

            TextField cityF    = new TextField(prefCity    == null ? "" : prefCity);
            cityF.setPromptText("Ville");
            AdminDialogHelper.applyDialogFieldSizing(cityF);

            TextField countryF = new TextField(prefCountry == null ? "" : prefCountry);
            countryF.setPromptText("Pays");
            AdminDialogHelper.applyDialogFieldSizing(countryF);

            // Auto-fill name from city if user leaves it blank
            cityF.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
                if (!isFocused && nameF.getText().isBlank() && !cityF.getText().isBlank())
                    nameF.setText(cityF.getText().trim());
            });

            dGrid.add(new Label("Nom *"),  0, 0); dGrid.add(nameF,    1, 0);
            dGrid.add(new Label("Ville"),  0, 1); dGrid.add(cityF,    1, 1);
            dGrid.add(new Label("Pays"),   0, 2); dGrid.add(countryF, 1, 2);

            Node createNode = destDialog.getDialogPane().lookupButton(createBtn);
            Runnable sync = () -> createNode.setDisable(AdminUtils.isBlank(nameF.getText())
                    && AdminUtils.isBlank(cityF.getText()));
            sync.run();
            nameF.textProperty().addListener((obs, o, v) -> sync.run());
            cityF.textProperty().addListener((obs, o, v) -> sync.run());

            destDialog.getDialogPane().setContent(dGrid);
            AdminDialogHelper.applyDialogPaneSizing(destDialog.getDialogPane(), 640, 320);
            AdminDialogHelper.styleDialog(destDialog, false, getClass());
            Platform.runLater(cityF::requestFocus);

            destDialog.setResultConverter(b -> b == createBtn
                    ? new String[]{nameF.getText().trim(), cityF.getText().trim(), countryF.getText().trim()}
                    : null);

            destDialog.showAndWait().ifPresent(parts -> {
                String dName    = parts[0].isBlank() ? parts[1] : parts[0]; // fallback name = city
                String dCity    = parts[1];
                String dCountry = parts[2];
                try {
                    int newId = state.createDestinationInDB(dName, dCity, dCountry);
                    String newLabel = state.resolveLocalisationLabel(newId);
                    combo.getItems().clear();
                    combo.getItems().addAll(state.getSortedLocalisationLabels());
                    combo.setValue(newLabel);
                    AdminDialogHelper.showNotification("Destination \"" + newLabel + "\" créée!", "success", getClass());
                } catch (Exception ex) {
                    AdminDialogHelper.showNotification("Erreur création destination: " + ex.getMessage(), "error", getClass());
                }
            });
        });
        return btn;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════════════

    @FunctionalInterface
    private interface SqlAction { void run() throws SQLException; }

    private void runSqlAction(SqlAction action) {
        try { action.run(); }
        catch (SQLException e) { AdminDialogHelper.showNotification("Erreur: " + e.getMessage(), "error", getClass()); }
    }
}