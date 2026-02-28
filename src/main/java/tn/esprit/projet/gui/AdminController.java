package tn.esprit.projet.gui;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.util.Duration;
import tn.esprit.projet.API.hotels.NominatimHotelApiClient;
import tn.esprit.projet.entities.Hotel;
import tn.esprit.projet.entities.HotelImage;
import tn.esprit.projet.entities.HotelReview;
import tn.esprit.projet.entities.Room;
import tn.esprit.projet.entities.RoomImage;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Thin FXML controller for Admin.fxml.
 *
 * Responsibilities:
 *  – FXML lifecycle (bindIncludedNodes + initialize)
 *  – Sidebar liquid-menu animation
 *  – Navigation between list / detail views
 *  – Filter / search wiring
 *  – Delegates ALL business logic to sub-controllers
 *
 * Sub-controllers:
 *  {@link AdminHotelViewController}            – hotel table, hotel detail, images, services
 *  {@link AdminRoomViewController}             – room table, room detail, images
 *  {@link AdminReviewViewController}           – reviews table
 *  {@link AdminReservationViewController} – reservations table
 *  {@link AdminApiViewController}              – Geoapify / OSM APIs
 *
 * Shared infrastructure:
 *  {@link AdminSharedState}  – services, observable lists, localisation cache
 *  {@link AdminDialogHelper}      – styled dialogs / notifications
 *  {@link AdminUtils}        – pure static utilities
 *
 * Pattern mirrors ClientController exactly:
 *  – bindIncludedNodes() walks fx:include subtrees (nodes are NOT injected by @FXML)
 *  – initServices() is called on AdminSharedState (like ClientSharedState.initServices)
 *  – setupInProgress guard suppresses handleFilter() during programmatic setValue()
 *  – Sidebar hover/click animation identical to ClientController
 */
public class AdminController implements Initializable {

    // ── Injected roots from fx:include ────────────────────────────────────────
    @FXML private AnchorPane adminSidebar;      // AdminSidebar.fxml
    @FXML private VBox       adminListSection;  // HotelView.fxml  (root fx:id = listViewSection)
    @FXML private VBox       adminDetailSection;// RoomView.fxml   (root fx:id = detailViewSection)
    @FXML private StackPane  rootPane;
    @FXML private VBox       mainContent;

    // ── Sidebar nodes (resolved from adminSidebar) ────────────────────────────
    private Pane   slidingPane;
    private VBox   menuContainer;
    private Button btnHotels;
    private Button btnChambres;
    private Button btnReservations;
    private Button btnLogout;

    // ── List-section nodes (resolved from adminListSection / HotelView.fxml) ──
    private VBox      listViewSection;
    private Label     pageTitle;
    private Label     pageSubtitle;
    private Label     searchPanelTitle;
    private TextField searchField;
    private VBox      filterContainer;
    private Label     filterLabel;
    private ComboBox<String> starsFilterCombo;
    private ComboBox<String> hotelFilterCombo;
    private Pane      roomFilterSeparator;
    private VBox      roomTypeFilterContainer;
    private Label     roomTypeFilterLabel;
    private ComboBox<String> roomTypeFilterCombo;
    private VBox      roomStatusFilterContainer;
    private Label     roomStatusFilterLabel;
    private ComboBox<String> roomStatusFilterCombo;
    private Button    searchButton;
    private Button    addButton;
    private HBox      apiToolbar;
    private Button    geoapifyApiButton;
    private Button    nominatimApiButton;
    private TextField apiSearchField;
    private ProgressIndicator apiLoadingIndicator;
    private Label     apiStatusLabel;
    @SuppressWarnings("rawtypes")
    private TableView mainTableRaw;
    @SuppressWarnings("unchecked")
    private TableView<Object> mainTable;
    private TableColumn<Object, String> col1, col2, col3, col4, col5;
    private TableColumn<Object, Object> col6;

    // ── Detail-section nodes (resolved from adminDetailSection / RoomView.fxml)
    private VBox      detailViewSection;
    private Button    backButton;
    private StackPane mainImageContainer;
    private ImageView mainImageView;
    private HBox      thumbnailsContainer;
    private Label     detailName;
    private Label     detailSubInfo;
    private HBox      detailApiRow;
    private FlowPane  detailBadges;
    private Label     detailDesc;
    private FlowPane  detailActionButtons;
    private VBox      detailServicesSection;
    private Label     detailServicesTitle;
    private Button    detailManageServicesBtn;
    private FlowPane  detailServicesPane;
    private VBox      reviewsSection;
    private TableView<HotelReview> reviewsTable;
    private TableColumn<HotelReview, String> colReviewRating;
    private TableColumn<HotelReview, String> colReviewComment;
    private TableColumn<HotelReview, String> colReviewUser;
    private TableColumn<HotelReview, Void>   colReviewActions;
    private VBox      imagesSection;
    private Button    addImageBtn;
    private TableView<HotelImage> imagesTable;
    private TableColumn<HotelImage, String> colImagePath;
    private TableColumn<HotelImage, Void>   colImagePreview;
    private TableColumn<HotelImage, Void>   colImageActions;
    private VBox      roomImagesSection;
    private Button    addRoomImageBtn;
    private TableView<RoomImage> roomImagesTable;
    private TableColumn<RoomImage, String> colRoomImagePath;
    private TableColumn<RoomImage, Void>   colRoomImagePreview;
    private TableColumn<RoomImage, Void>   colRoomImageActions;

    // ── Sub-controllers ───────────────────────────────────────────────────────
    private AdminSharedState               state;
    private AdminHotelViewController            hotelVC;
    private AdminRoomViewController             roomVC;
    private AdminReviewViewController           reviewVC;
    private AdminReservationViewController reservationVC;
    private AdminApiViewController              apiVC;

    // ── Navigation state ──────────────────────────────────────────────────────
    private String  currentView      = "hotels";
    private Button  currentActiveBtn = null;
    /** Mirrors ClientController: suppresses handleFilter() during programmatic setValue(). */
    private boolean setupInProgress  = false;

    // ─────────────────────────────────────────────────────────────────────────
    //  Initialise  (same structure as ClientController.initialize)
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 1 – resolve nodes from included subtrees (nodes NOT injected by @FXML)
        bindIncludedNodes();

        // 2 – build shared state + init all services (wraps SQLException internally)
        state = new AdminSharedState();
        state.initServices();

        // 3 – build sub-controllers (same order as ClientController)
        hotelVC       = new AdminHotelViewController(state, this,
                state.hotelService, state.hotelImageService, state.hotelServiceItemService);
        roomVC        = new AdminRoomViewController(state, this,
                state.roomService, state.roomImageService);
        reviewVC      = new AdminReviewViewController(state, this, state.hotelReviewService);
        reservationVC = new AdminReservationViewController(this);
        apiVC         = new AdminApiViewController(this, state.hotelService);

        // 4 – load data into shared state (like ClientController.loadHotels calling state.hotelService)
        if (state.databaseAvailable) loadAllData();

        // 5 – wire handlers (same order as ClientController)
        wireActionHandlers();

        // 6 – sliding pane setup (same as ClientController)
        if (slidingPane != null) {
            slidingPane.setMouseTransparent(true);
        }

        // 7 – post-layout (same Platform.runLater block as ClientController)
        Platform.runLater(() -> {
            if (btnHotels != null && slidingPane != null) {
                slidingPane.setTranslateY(btnHotels.getLayoutY());
                currentActiveBtn = btnHotels;
                highlightButton(btnHotels, true);
            }
            if (state.databaseAvailable) {
                goToHotels();
            } else {
                showDatabaseUnavailableState();
            }
        });

        // 8 – hover animation (same as ClientController menuContainer hover block)
        if (menuContainer != null) {
            Button[] buttons = { btnHotels, btnChambres, btnReservations, btnLogout };
            menuContainer.setOnMouseMoved(event -> {
                double mouseY = event.getY();
                for (Button btn : buttons) {
                    if (btn == null) continue;
                    double startY = btn.getLayoutY();
                    double endY   = startY + btn.getHeight();
                    if (mouseY >= startY && mouseY <= endY) {
                        handleHoverPreview(btn);
                        break;
                    }
                }
            });
            menuContainer.setOnMouseExited(event -> {
                if (currentActiveBtn != null) moveBubble(currentActiveBtn);
            });
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  bindIncludedNodes  (mirrors ClientController.bindIncludedNodes exactly)
    //  JavaFX only injects the ROOT of each fx:include, not its children.
    //  We walk the tree to resolve children by fx:id.
    // ─────────────────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private void bindIncludedNodes() {

        // ── AdminSidebar.fxml ─────────────────────────────────────────────
        if (adminSidebar != null) {
            slidingPane      = find(adminSidebar, "slidingPane",      Pane.class);
            menuContainer    = find(adminSidebar, "menuContainer",    VBox.class);
            btnHotels        = find(adminSidebar, "btnHotels",        Button.class);
            btnChambres      = find(adminSidebar, "btnChambres",      Button.class);
            btnReservations  = find(adminSidebar, "btnReservations",  Button.class);
            btnLogout        = find(adminSidebar, "btnLogout",        Button.class);
        }

        // ── HotelView.fxml (list section) ────────────────────────────────
        if (adminListSection != null) {
            listViewSection         = adminListSection; // root IS listViewSection
            pageTitle               = find(adminListSection, "pageTitle",               Label.class);
            pageSubtitle            = find(adminListSection, "pageSubtitle",            Label.class);
            searchPanelTitle        = find(adminListSection, "searchPanelTitle",        Label.class);
            searchField             = find(adminListSection, "searchField",             TextField.class);
            filterContainer         = find(adminListSection, "filterContainer",         VBox.class);
            filterLabel             = find(adminListSection, "filterLabel",             Label.class);
            starsFilterCombo        = find(adminListSection, "starsFilterCombo",        ComboBox.class);
            hotelFilterCombo        = find(adminListSection, "hotelFilterCombo",        ComboBox.class);
            roomFilterSeparator     = find(adminListSection, "roomFilterSeparator",     Pane.class);
            roomTypeFilterContainer = find(adminListSection, "roomTypeFilterContainer", VBox.class);
            roomTypeFilterLabel     = find(adminListSection, "roomTypeFilterLabel",     Label.class);
            roomTypeFilterCombo     = find(adminListSection, "roomTypeFilterCombo",     ComboBox.class);
            roomStatusFilterContainer = find(adminListSection, "roomStatusFilterContainer", VBox.class);
            roomStatusFilterLabel   = find(adminListSection, "roomStatusFilterLabel",   Label.class);
            roomStatusFilterCombo   = find(adminListSection, "roomStatusFilterCombo",   ComboBox.class);
            searchButton            = find(adminListSection, "searchButton",            Button.class);
            addButton               = find(adminListSection, "addButton",               Button.class);
            apiToolbar              = find(adminListSection, "apiToolbar",              HBox.class);
            geoapifyApiButton       = find(adminListSection, "geoapifyApiButton",       Button.class);
            nominatimApiButton      = find(adminListSection, "nominatimApiButton",      Button.class);
            apiSearchField          = find(adminListSection, "apiSearchField",          TextField.class);
            apiLoadingIndicator     = find(adminListSection, "apiLoadingIndicator",     ProgressIndicator.class);
            apiStatusLabel          = find(adminListSection, "apiStatusLabel",          Label.class);
            mainTableRaw            = find(adminListSection, "mainTable",               TableView.class);
            mainTable               = (TableView<Object>) mainTableRaw;
            // Columns by index (same pattern as ClientController gets hotelsContainer by id)
            if (mainTable != null && mainTable.getColumns().size() >= 6) {
                col1 = (TableColumn<Object, String>) mainTable.getColumns().get(0);
                col2 = (TableColumn<Object, String>) mainTable.getColumns().get(1);
                col3 = (TableColumn<Object, String>) mainTable.getColumns().get(2);
                col4 = (TableColumn<Object, String>) mainTable.getColumns().get(3);
                col5 = (TableColumn<Object, String>) mainTable.getColumns().get(4);
                col6 = (TableColumn<Object, Object>) mainTable.getColumns().get(5);
            }
        }

        // ── RoomView.fxml (detail section) ───────────────────────────────
        if (adminDetailSection != null) {
            detailViewSection       = adminDetailSection; // root IS detailViewSection
            backButton              = find(adminDetailSection, "backButton",              Button.class);
            mainImageContainer      = find(adminDetailSection, "mainImageContainer",      StackPane.class);
            mainImageView           = find(adminDetailSection, "mainImageView",           ImageView.class);
            thumbnailsContainer     = find(adminDetailSection, "thumbnailsContainer",     HBox.class);
            detailName              = find(adminDetailSection, "detailName",              Label.class);
            detailSubInfo           = find(adminDetailSection, "detailSubInfo",           Label.class);
            detailApiRow            = find(adminDetailSection, "detailApiRow",            HBox.class);
            detailBadges            = find(adminDetailSection, "detailBadges",            FlowPane.class);
            detailDesc              = find(adminDetailSection, "detailDesc",              Label.class);
            detailActionButtons     = find(adminDetailSection, "detailActionButtons",     FlowPane.class);
            detailServicesSection   = find(adminDetailSection, "detailServicesSection",   VBox.class);
            detailServicesTitle     = find(adminDetailSection, "detailServicesTitle",     Label.class);
            detailManageServicesBtn = find(adminDetailSection, "detailManageServicesBtn", Button.class);
            detailServicesPane      = find(adminDetailSection, "detailServicesPane",      FlowPane.class);
            reviewsSection          = find(adminDetailSection, "reviewsSection",          VBox.class);
            reviewsTable            = find(adminDetailSection, "reviewsTable",            TableView.class);
            imagesSection           = find(adminDetailSection, "imagesSection",           VBox.class);
            addImageBtn             = find(adminDetailSection, "addImageBtn",             Button.class);
            imagesTable             = find(adminDetailSection, "imagesTable",             TableView.class);
            roomImagesSection       = find(adminDetailSection, "roomImagesSection",       VBox.class);
            addRoomImageBtn         = find(adminDetailSection, "addRoomImageBtn",         Button.class);
            roomImagesTable         = find(adminDetailSection, "roomImagesTable",         TableView.class);

            // Review table columns by index
            if (reviewsTable != null && reviewsTable.getColumns().size() >= 4) {
                colReviewRating  = (TableColumn<HotelReview, String>) reviewsTable.getColumns().get(0);
                colReviewComment = (TableColumn<HotelReview, String>) reviewsTable.getColumns().get(1);
                colReviewUser    = (TableColumn<HotelReview, String>) reviewsTable.getColumns().get(2);
                colReviewActions = (TableColumn<HotelReview, Void>)   reviewsTable.getColumns().get(3);
            }
            // Hotel images columns
            if (imagesTable != null && imagesTable.getColumns().size() >= 3) {
                colImagePath    = (TableColumn<HotelImage, String>) imagesTable.getColumns().get(0);
                colImagePreview = (TableColumn<HotelImage, Void>)   imagesTable.getColumns().get(1);
                colImageActions = (TableColumn<HotelImage, Void>)   imagesTable.getColumns().get(2);
            }
            // Room images columns
            if (roomImagesTable != null && roomImagesTable.getColumns().size() >= 3) {
                colRoomImagePath    = (TableColumn<RoomImage, String>) roomImagesTable.getColumns().get(0);
                colRoomImagePreview = (TableColumn<RoomImage, Void>)   roomImagesTable.getColumns().get(1);
                colRoomImageActions = (TableColumn<RoomImage, Void>)   roomImagesTable.getColumns().get(2);
            }
        }
    }

    /** Walk node tree by fx:id — same as ClientController.findNodeById */
    private Node findNodeById(Node root, String fxId) {
        if (root == null || fxId == null) return null;
        if (fxId.equals(root.getId())) return root;
        if (root instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                Node found = findNodeById(child, fxId);
                if (found != null) return found;
            }
        }
        return null;
    }

    private <T> T find(Node root, String fxId, Class<T> type) {
        Node found = findNodeById(root, fxId);
        return type.isInstance(found) ? type.cast(found) : null;
    }

    private <T> T firstNonNull(T current, T fallback) {
        return current != null ? current : fallback;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Action handler wiring  (mirrors ClientController.wireActionHandlers)
    // ─────────────────────────────────────────────────────────────────────────

    private void wireActionHandlers() {
        // Sidebar buttons
        Button[] buttons = { btnHotels, btnChambres, btnReservations, btnLogout };
        for (Button btn : buttons) {
            if (btn == null) continue;
            btn.setOnAction(e -> handleMenuClick(btn));
        }

        // Search/filter — setOnAction so programmatic setValue() does NOT trigger
        if (searchButton     != null) searchButton.setOnAction(e -> handleSearch());
        if (starsFilterCombo != null) starsFilterCombo.setOnAction(e -> handleFilter());
        if (hotelFilterCombo != null) hotelFilterCombo.setOnAction(e -> handleFilter());
        if (roomTypeFilterCombo   != null) roomTypeFilterCombo.setOnAction(e -> handleFilter());
        if (roomStatusFilterCombo != null) roomStatusFilterCombo.setOnAction(e -> handleFilter());

        // API toolbar
        if (geoapifyApiButton != null) geoapifyApiButton.setOnAction(e -> handleGeoapifySearch());
        if (nominatimApiButton != null) nominatimApiButton.setOnAction(e -> handleNominatimSearch());

        // Back button
        if (backButton != null) backButton.setOnAction(e -> handleBack());

        // Add-image buttons in detail section
        if (addImageBtn != null) addImageBtn.setOnAction(e -> {
            Hotel sel = state.getSelectedHotel();
            if (sel != null) hotelVC.showAddImageDialog(sel, imagesTable, mainImageContainer, thumbnailsContainer);
        });
        if (addRoomImageBtn != null) addRoomImageBtn.setOnAction(e -> {
            Room sel = state.getSelectedRoom();
            if (sel != null) {
                try {
                    String path = promptImagePath();
                    if (path != null) roomVC.saveRoomImage(path, sel.getId(), roomImagesTable, mainImageContainer, thumbnailsContainer);
                } catch (java.sql.SQLException ex) {
                    AdminDialogHelper.showNotification("Erreur: " + ex.getMessage(), "error", getClass());
                }
            }
        });
    }

    private String promptImagePath() {
        TextInputDialog d = new TextInputDialog();
        d.setTitle("Ajouter image"); d.setHeaderText("Chemin ou URL de l'image");
        AdminDialogHelper.styleDialog(d, false, getClass());
        return d.showAndWait().orElse(null);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Navigation  (public – called by sub-controllers, same as ClientController)
    // ─────────────────────────────────────────────────────────────────────────

    /** Navigate to hotel list view. */
    public void goToHotels() {
        currentView = "hotels";
        setActiveButton(btnHotels);
        switchToListView();
        setupHotelsView();
    }

    /** Navigate to rooms list view (plain). */
    public void goToRooms() {
        currentView = "rooms";
        setActiveButton(btnChambres);
        switchToListView();
        setupRoomsView();
    }

    /** Navigate to rooms list filtered by a hotel. */
    public void navigateToRoomsFilteredByHotel(Hotel hotel) {
        goToRooms();
        setupInProgress = true;
        try { hotelFilterCombo.setValue(hotel.getName()); } finally { setupInProgress = false; }
        loadRoomsTable();
    }

    /** Alias used by AdminRoomViewController after a delete from the detail view. */
    public void showRoomsView() { goToRooms(); }

    /** Alias used by AdminHotelViewController after a delete from the detail view. */
    public void showHotelsView() { goToHotels(); }

    /** Navigate to reservations list view. */
    public void goToReservations() {
        currentView = "reservations";
        setActiveButton(btnReservations);
        switchToListView();
        setupReservationsView();
    }

    /** Show hotel detail view. */
    public void showHotelDetail(Hotel hotel) {
        state.setSelectedHotel(hotel);
        state.setSelectedRoom(null);
        switchToDetailView();
        hotelVC.populateHotelDetail(hotel,
                detailName, detailSubInfo, detailApiRow, detailDesc,
                detailBadges, detailActionButtons,
                detailServicesSection, detailServicesTitle, detailServicesPane,
                detailManageServicesBtn,
                reviewsSection, imagesSection, roomImagesSection,
                imagesTable, mainImageContainer, thumbnailsContainer, reviewsTable);
        reviewVC.setupReviewsTable(reviewsTable, colReviewRating, colReviewComment,
                colReviewUser, colReviewActions);
        reviewVC.loadHotelReviews(hotel.getId(), reviewsTable);
        hotelVC.setupImagesTable(colImagePath, colImagePreview, colImageActions);
        hotelVC.loadHotelImages(hotel.getId(), imagesTable, mainImageContainer, thumbnailsContainer);
    }

    /** Show room detail view. */
    public void showRoomDetail(Room room) {
        state.setSelectedRoom(room);
        switchToDetailView();
        roomVC.populateRoomDetail(room,
                detailName, detailSubInfo, detailDesc,
                detailBadges, detailActionButtons,
                detailServicesSection, detailServicesPane,
                reviewsSection, imagesSection, roomImagesSection,
                roomImagesTable, mainImageContainer, thumbnailsContainer);
        roomVC.setupRoomImagesTable(colRoomImagePath, colRoomImagePreview, colRoomImageActions);
        roomVC.loadRoomImages(room.getId(), roomImagesTable, mainImageContainer, thumbnailsContainer);
    }

    /** Reload data from DB. */
    public void loadAllData() {
        try { state.setHotelsList(FXCollections.observableArrayList(state.hotelService.getAll())); }
        catch (Exception e) { state.setHotelsList(FXCollections.observableArrayList()); }
        try { state.setRoomsList(FXCollections.observableArrayList(state.roomService.getAll())); }
        catch (Exception e) { state.setRoomsList(FXCollections.observableArrayList()); }
        state.refreshLocalisationLookup();
    }

    public void loadHotelsTable() {
        hotelVC.loadHotelsTable(mainTable, searchField, starsFilterCombo,
                roomTypeFilterCombo, roomStatusFilterCombo, pageSubtitle);
    }

    public void loadRoomsTable() {
        roomVC.loadRoomsTable(mainTable, searchField,
                hotelFilterCombo, roomTypeFilterCombo, roomStatusFilterCombo, pageSubtitle);
    }

    /** Open hotel on OSM map (async Nominatim lookup). Same as ClientController.showHotelOnMap. */
    public void openHotelMap(Hotel hotel) {
        Thread t = new Thread(() -> {
            try {
                NominatimHotelApiClient client = new NominatimHotelApiClient();
                NominatimHotelApiClient.LocationSummary loc =
                        client.findLocationSummary(hotel.getName()).orElse(null);
                Platform.runLater(() -> apiVC.openOpenStreetMapLocation(loc));
            } catch (Exception e) {
                Platform.runLater(() -> AdminDialogHelper.showNotification(
                        "Impossible de localiser l'hotel: " + AdminUtils.extractErrorMessage(e),
                        "error", getClass()));
            }
        }, "admin-nominatim-hotel-map");
        t.setDaemon(true);
        t.start();
    }

    public void reloadReviews(int hotelId) {
        reviewVC.loadHotelReviews(hotelId, reviewsTable);
    }

    public AdminApiViewController getApiVC() { return apiVC; }
    public AdminSharedState getState() { return state; }
    public AdminHotelViewController getHotelViewController() { return hotelVC; }

    public void configureGeoapifyColumns() {
        apiVC.setupGeoapifyColumns(col1, col2, col3, col4, col5, col6);
    }

    public void configureNominatimColumns() {
        apiVC.setupNominatimColumns(col1, col2, col3, col4, col5, col6);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  View setup helpers  (mirrors ClientController.setupHotelsFilter etc.)
    // ─────────────────────────────────────────────────────────────────────────

    private void setupHotelsView() {
        setupInProgress = true;
        try {
            if (pageTitle       != null) pageTitle.setText("Gestion des H\u00f4tels");
            if (searchPanelTitle != null) searchPanelTitle.setText("Recherche et Actions");
            if (filterLabel     != null) filterLabel.setText("Etoiles");

            // Stars combo visible, hotel combo hidden — mirrors ClientController.setupHotelsFilter
            if (starsFilterCombo != null) { starsFilterCombo.setVisible(true);  starsFilterCombo.setManaged(true);  }
            if (hotelFilterCombo != null) { hotelFilterCombo.setVisible(false); hotelFilterCombo.setManaged(false); }

            // Localisation + status filters — mirrors ClientController.showHotelAdvancedFilters
            if (roomTypeFilterLabel  != null) roomTypeFilterLabel.setText("Localisation");
            if (roomStatusFilterLabel != null) roomStatusFilterLabel.setText("Statut");
            setFilterSectionVisible(true);

            state.refreshLocalisationLookup();
            if (roomTypeFilterCombo != null) {
                roomTypeFilterCombo.getItems().clear();
                roomTypeFilterCombo.getItems().add("Toutes localisations");
                roomTypeFilterCombo.getItems().addAll(state.getSortedLocalisationLabels());
                roomTypeFilterCombo.setValue("Toutes localisations");
                roomTypeFilterCombo.setPromptText("Localisation");
            }
            if (roomStatusFilterCombo != null) {
                roomStatusFilterCombo.getItems().clear();
                roomStatusFilterCombo.getItems().addAll("Tous", "AVAILABLE", "OCCUPIED", "MAINTENANCE");
                roomStatusFilterCombo.setValue("Tous");
                roomStatusFilterCombo.setPromptText("Statut");
            }

            if (addButton != null) {
                addButton.setText("+ Ajouter");
                addButton.setDisable(false);
                addButton.setOnAction(e -> hotelVC.showAddHotelDialog());
            }
            if (searchButton != null) searchButton.setOnAction(e -> handleSearch());

            hotelVC.setupHotelsTable(mainTable, col1, col2, col3, col4, col5, col6);
            hotelVC.setupStarsFilter(starsFilterCombo);   // also does setValue("Toutes") — guarded

            if (apiToolbar != null) { apiToolbar.setVisible(true); apiToolbar.setManaged(true); }
            apiVC.updateApiControlsForView(true, geoapifyApiButton);
            if (nominatimApiButton != null) nominatimApiButton.setDisable(false);
        } finally {
            setupInProgress = false;
        }
        if (state.databaseAvailable) loadHotelsTable();
        else showDatabaseUnavailableState();
    }

    private void setupRoomsView() {
        setupInProgress = true;
        try {
            if (pageTitle       != null) pageTitle.setText("Gestion des Chambres");
            if (searchPanelTitle != null) searchPanelTitle.setText("Recherche et Actions");
            if (filterLabel     != null) filterLabel.setText("Hotel");

            if (starsFilterCombo != null) { starsFilterCombo.setVisible(false); starsFilterCombo.setManaged(false); }
            if (hotelFilterCombo != null) { hotelFilterCombo.setVisible(true);  hotelFilterCombo.setManaged(true);  }

            // Mirrors ClientController.showRoomAdvancedFilters
            if (roomTypeFilterLabel  != null) roomTypeFilterLabel.setText("Type");
            if (roomStatusFilterLabel != null) roomStatusFilterLabel.setText("Statut");
            setFilterSectionVisible(true);

            if (roomTypeFilterCombo != null) {
                roomTypeFilterCombo.getItems().clear();
                roomTypeFilterCombo.getItems().addAll("Tous", "SINGLE", "DOUBLE", "SUITE", "DELUXE", "FAMILY");
                roomTypeFilterCombo.setValue("Tous");
                roomTypeFilterCombo.setPromptText("Type");
            }
            if (roomStatusFilterCombo != null) {
                roomStatusFilterCombo.getItems().clear();
                roomStatusFilterCombo.getItems().addAll("Tous", "AVAILABLE", "OCCUPIED", "MAINTENANCE");
                roomStatusFilterCombo.setValue("Tous");
                roomStatusFilterCombo.setPromptText("Statut");
            }

            if (addButton != null) {
                addButton.setText("+ Ajouter Chambre");
                addButton.setDisable(false);
                addButton.setOnAction(e -> roomVC.showAddRoomDialog());
            }
            if (searchButton != null) searchButton.setOnAction(e -> handleSearch());

            if (mainTable != null) mainTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
            roomVC.setupRoomsTable(mainTable, col1, col2, col3, col4, col5, col6);
            roomVC.setupHotelFilter(hotelFilterCombo);

            if (apiToolbar != null) { apiToolbar.setVisible(false); apiToolbar.setManaged(false); }
            apiVC.updateApiControlsForView(false, geoapifyApiButton);
        } finally {
            setupInProgress = false;
        }
        if (state.databaseAvailable) loadRoomsTable();
        else showDatabaseUnavailableState();
    }

    private void setupReservationsView() {
        setupInProgress = true;
        try {
            if (pageTitle       != null) pageTitle.setText("Gestion des R\u00e9servations");
            if (searchPanelTitle != null) searchPanelTitle.setText("Recherche");

            if (starsFilterCombo  != null) { starsFilterCombo.setVisible(false);  starsFilterCombo.setManaged(false);  }
            if (hotelFilterCombo  != null) { hotelFilterCombo.setVisible(false);  hotelFilterCombo.setManaged(false);  }
            setFilterSectionVisible(false);

            if (addButton != null) addButton.setDisable(true);
            if (searchButton != null) searchButton.setOnAction(e -> reservationVC.refreshFilter());

            reservationVC.setupReservationsTable(mainTable, col1, col2, col3, col4, col5, col6);

            if (apiToolbar != null) { apiToolbar.setVisible(false); apiToolbar.setManaged(false); }
            apiVC.updateApiControlsForView(false, geoapifyApiButton);
        } finally {
            setupInProgress = false;
        }
        reservationVC.loadReservationsTable(mainTable, searchField,
                hotelFilterCombo, roomStatusFilterCombo, pageSubtitle);
    }

    /** Show / hide the advanced filter containers (localisation/type/status). */
    private void setFilterSectionVisible(boolean visible) {
        if (roomFilterSeparator      != null) { roomFilterSeparator.setVisible(visible);      roomFilterSeparator.setManaged(visible);      }
        if (roomTypeFilterContainer  != null) { roomTypeFilterContainer.setVisible(visible);   roomTypeFilterContainer.setManaged(visible);   }
        if (roomStatusFilterContainer != null){ roomStatusFilterContainer.setVisible(visible); roomStatusFilterContainer.setManaged(visible); }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  FXML action handlers  (mirrors ClientController @FXML handlers)
    // ─────────────────────────────────────────────────────────────────────────

    @FXML private void handleSearch() {
        if (!state.databaseAvailable) { showDatabaseUnavailableState(); return; }
        handleFilter();
    }

    @FXML private void handleFilter() {
        if (setupInProgress) return;
        if      ("hotels".equals(currentView))        loadHotelsTable();
        else if ("rooms".equals(currentView))         loadRoomsTable();
        else if ("reservations".equals(currentView))  reservationVC.refreshFilter();
    }

    private void handleGeoapifySearch() {
        if (!state.databaseAvailable) return;
        String query = apiSearchField != null ? apiSearchField.getText().trim() : "";
        if (query.isBlank()) {
            AdminDialogHelper.showNotification("Veuillez saisir une destination.", "warning", getClass());
            return;
        }
        apiVC.executeGeoapifySearchAsync(query, mainTable, pageTitle, pageSubtitle,
                addButton, apiLoadingIndicator, geoapifyApiButton);
    }

    private void handleNominatimSearch() {
        if (!state.databaseAvailable) return;
        String query = apiSearchField != null ? apiSearchField.getText().trim() : "";
        if (query.isBlank()) {
            AdminDialogHelper.showNotification("Veuillez saisir une destination.", "warning", getClass());
            return;
        }
        apiVC.executeNominatimSearchAsync(query, mainTable, pageTitle, pageSubtitle,
                addButton, apiLoadingIndicator, nominatimApiButton);
    }

    private void handleBack() {
        if ("rooms".equals(currentView) && state.getSelectedRoom() != null) {
            Hotel parent = state.getSelectedHotel();
            if (parent != null) { showHotelDetail(parent); return; }
        }
        goToHotels();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Menu click  (same pattern as ClientController.handleMenuClick)
    // ─────────────────────────────────────────────────────────────────────────

    private void handleMenuClick(Button btn) {
        if (btn == btnLogout) { System.exit(0); return; }
        setActiveButton(btn);
        if (!state.databaseAvailable) { showDatabaseUnavailableState(); return; }
        if      (btn == btnHotels)        goToHotels();
        else if (btn == btnChambres)      goToRooms();
        else if (btn == btnReservations)  goToReservations();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  View switching
    // ─────────────────────────────────────────────────────────────────────────

    private void switchToListView() {
        if (listViewSection   != null) { listViewSection.setVisible(true);    listViewSection.setManaged(true);   }
        if (detailViewSection != null) { detailViewSection.setVisible(false); detailViewSection.setManaged(false); }
    }

    private void switchToDetailView() {
        if (listViewSection   != null) { listViewSection.setVisible(false);  listViewSection.setManaged(false);  }
        if (detailViewSection != null) { detailViewSection.setVisible(true); detailViewSection.setManaged(true); }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Offline / error states  (mirrors ClientController.showDatabaseUnavailableState)
    // ─────────────────────────────────────────────────────────────────────────

    private void showDatabaseUnavailableState() {
        if (mainTable != null) mainTable.getItems().clear();
        if (pageTitle    != null) pageTitle.setText("Connexion base indisponible");
        if (pageSubtitle != null) {
            String msg = state.databaseErrorMessage == null || state.databaseErrorMessage.isBlank()
                    ? "Impossible de se connecter a la base. Verifiez MySQL."
                    : state.databaseErrorMessage;
            pageSubtitle.setText(msg);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Sidebar liquid-menu animation  (identical to ClientController)
    // ─────────────────────────────────────────────────────────────────────────

    private void setActiveButton(Button btn) {
        if (currentActiveBtn == btn) return;
        if (currentActiveBtn != null) highlightButton(currentActiveBtn, false);
        applyLiquidDirection(btn);
        glidePane(btn, 320);
        highlightButton(btn, true);
        currentActiveBtn = btn;
    }

    private void handleHoverPreview(Button targetBtn) {
        if (targetBtn != currentActiveBtn) glidePane(targetBtn, 160);
    }

    private void glidePane(Button target, int ms) {
        if (slidingPane == null || target == null) return;
        TranslateTransition tt = new TranslateTransition(Duration.millis(ms), slidingPane);
        tt.setToY(target.getLayoutY());
        tt.setInterpolator(Interpolator.SPLINE(0.16, 0.84, 0.44, 1.0));
        tt.play();
    }

    private void moveBubble(Button target) { glidePane(target, 320); }

    private void highlightButton(Button btn, boolean activate) {
        if (btn == null) return;
        ScaleTransition st = new ScaleTransition(Duration.millis(activate ? 240 : 200), btn);
        st.setToX(activate ? 1.04 : 1.0);
        st.setToY(activate ? 1.10 : 1.0);
        st.setInterpolator(Interpolator.EASE_OUT);
        FadeTransition ft = new FadeTransition(Duration.millis(activate ? 180 : 140), btn);
        ft.setToValue(activate ? 1.0 : 0.85);
        new ParallelTransition(st, ft).play();
        if (activate) { if (!btn.getStyleClass().contains("active")) btn.getStyleClass().add("active"); }
        else          { btn.getStyleClass().remove("active"); }
    }

    private void applyLiquidDirection(Button target) {
        if (slidingPane == null || currentActiveBtn == null) return;
        slidingPane.getStyleClass().removeAll("from-top", "from-bottom");
        slidingPane.getStyleClass().add(
                target.getLayoutY() > currentActiveBtn.getLayoutY() ? "from-top" : "from-bottom");
    }
}