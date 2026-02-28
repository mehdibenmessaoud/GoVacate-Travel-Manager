package tn.esprit.projet.gui;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.CacheHint;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.util.Duration;

import tn.esprit.projet.entities.Room;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Thin FXML controller for Client.fxml.
 *
 * Responsibilities:
 *  - FXML lifecycle (@FXML initialize)
 *  - Sidebar liquid-menu animation
 *  - Navigation between views (hotels grid / rooms grid / detail pages)
 *  - Filter setup (stars, hotel, room type/status)
 *  - Delegates all business logic to sub-controllers
 *
 * Sub-controllers:
 *  {@link ClientHotelViewController} - hotel cards, hotel detail, availability
 *  {@link ClientRoomViewController}  - room cards, room detail
 *  {@link ClientReviewViewController} - review list, add-review dialog
 *  {@link ClientApiViewController}   - Amadeus / OSM search
 *
 * Shared infrastructure:
 *  {@link ClientSharedState}         - services, destination cache
 *  {@link ClientDialogHelper}        - styled dialogs
 *  {@link ClientImageGalleryBuilder} - generic image gallery
 *  {@link ClientUtils}               - pure static utilities
 */
public class ClientController implements Initializable {

    // Included section roots
    @FXML private AnchorPane clientSidebar;
    @FXML private VBox clientContent;

    // â”€â”€ FXML: sidebar â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    @FXML private Pane   slidingPane;
    @FXML private VBox   menuContainer;

    @FXML private Button btnHotels;
    @FXML private Button btnChambres;
    @FXML private Button btnReservations;
    @FXML private Button btnProfil;
    @FXML private Button btnLogout;

    // â”€â”€ FXML: content area â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    @FXML private VBox     contentArea;
    @FXML private VBox     searchPanel;
    @FXML private HBox     headerBox;
    @FXML private FlowPane hotelsContainer;

    // â”€â”€ FXML: labels â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    @FXML private Label sectionTitle;
    @FXML private Label subtitleLabel;
    @FXML private Label welcomeLabel;
    @FXML private Label searchTitle;
    @FXML private Label apiStatusLabel;
    @FXML private Label filterLabel;
    @FXML private Label roomTypeFilterLabel;
    @FXML private Label roomStatusFilterLabel;

    // â”€â”€ FXML: search / filters â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    @FXML private TextField        searchField;
    @FXML private TextField        apiSearchField;
    @FXML private HBox             apiToolbar;
    @FXML private Button           searchButton;
    @FXML private Button           geoapifyApiButton;
    @FXML private ProgressIndicator apiLoadingIndicator;
    @FXML private ComboBox<String> starsFilterCombo;
    @FXML private ComboBox<String> hotelFilterCombo;
    @FXML private ComboBox<String> roomTypeFilterCombo;
    @FXML private ComboBox<String> roomStatusFilterCombo;

    @FXML private VBox filterContainer;
    @FXML private VBox roomTypeFilterContainer;
    @FXML private VBox roomStatusFilterContainer;
    @FXML private Pane roomFilterSeparator;

    // â”€â”€ Sub-controllers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    private ClientSharedState         sharedState;
    private ClientDialogHelper        dialogHelper;
    private ClientHotelViewController hotelVC;
    private ClientRoomViewController  roomVC;
    private ClientReviewViewController reviewVC;
    private ClientApiViewController   apiVC;
    private ClientReservationViewController reservationVC;

    // â”€â”€ Navigation state â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    private String currentView = "hotels";
    private Button currentActiveBtn = null;

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    //  Initialise
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        bindIncludedNodes();
        wireActionHandlers();

        // Sliding pane setup
        slidingPane.setMouseTransparent(true);
        slidingPane.setCache(true);
        slidingPane.setCacheHint(CacheHint.SPEED);

        setupResponsiveGrid();

        // Build shared infrastructure
        sharedState  = new ClientSharedState();
        sharedState.initServices();

        dialogHelper = new ClientDialogHelper(getClass(), this::loadImage);
        reviewVC     = new ClientReviewViewController(sharedState, dialogHelper, this::loadImage);
        hotelVC      = new ClientHotelViewController(sharedState, dialogHelper, reviewVC, this::loadImage, this);
        roomVC       = new ClientRoomViewController(sharedState, dialogHelper, this::loadImage, this);
        apiVC        = new ClientApiViewController(sharedState, dialogHelper);
        reservationVC = new ClientReservationViewController(this);

        // Wire API sub-controller FXML refs
        apiVC.setApiStatusLabel(apiStatusLabel);
        apiVC.setApiLoadingIndicator(apiLoadingIndicator);
        apiVC.setGeoapifyApiButton(geoapifyApiButton);
        apiVC.setSearchField(searchField);
        apiVC.setApiSearchField(apiSearchField);
        apiVC.setSectionTitle(sectionTitle);
        apiVC.setSubtitleLabel(subtitleLabel);
        apiVC.setHotelsContainer(hotelsContainer);

        // Menu buttons
        Button[] buttons = {btnHotels, btnChambres, btnReservations, btnProfil, btnLogout};
        for (Button btn : buttons) {
            if (btn == null) continue;
            btn.setOnAction(e -> handleMenuClick(btn));
        }

        // Prepare UI for current DB state
        if (sharedState.databaseAvailable) {
            setupHotelsFilter();
            apiVC.updateApiButtonState(true, true);
            apiVC.setApiStatus("Pret pour recherche API", "idle");
        } else {
            disableUiForOfflineMode();
            apiVC.setApiStatus("Base indisponible", "error");
        }

        // Post-layout: set initial sliding pane position and load content
        Platform.runLater(() -> {
            if (btnHotels != null) {
                slidingPane.setTranslateY(btnHotels.getLayoutY());
                currentActiveBtn = btnHotels;
                highlightButton(btnHotels, true);
            }
            slidingPane.toBack();
            if (sharedState.databaseAvailable) {
                loadHotels();
            } else {
                showDatabaseUnavailableState();
            }
        });

        // Hover: animate bubble to hovered button, but restore to active on mouse-exit
        if (menuContainer != null) {
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
                // Restore bubble to the actually active button
                if (currentActiveBtn != null) moveBubble(currentActiveBtn);
            });
        }
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    //  Filter setup helpers
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @SuppressWarnings("unchecked")
    private void bindIncludedNodes() {
        if (clientSidebar != null) {
            slidingPane = firstNonNull(slidingPane, findNode(clientSidebar, "slidingPane", Pane.class));
            menuContainer = firstNonNull(menuContainer, findNode(clientSidebar, "menuContainer", VBox.class));
            btnHotels = firstNonNull(btnHotels, findNode(clientSidebar, "btnHotels", Button.class));
            btnChambres = firstNonNull(btnChambres, findNode(clientSidebar, "btnChambres", Button.class));
            btnReservations = firstNonNull(btnReservations, findNode(clientSidebar, "btnReservations", Button.class));

            btnProfil = firstNonNull(btnProfil, findNode(clientSidebar, "btnProfil", Button.class));
            btnLogout = firstNonNull(btnLogout, findNode(clientSidebar, "btnLogout", Button.class));
        }

        if (clientContent != null) {
            contentArea = firstNonNull(contentArea, findNode(clientContent, "contentArea", VBox.class));
            searchPanel = firstNonNull(searchPanel, findNode(clientContent, "searchPanel", VBox.class));
            headerBox = firstNonNull(headerBox, findNode(clientContent, "headerBox", HBox.class));
            hotelsContainer = firstNonNull(hotelsContainer, findNode(clientContent, "hotelsContainer", FlowPane.class));

            sectionTitle = firstNonNull(sectionTitle, findNode(clientContent, "sectionTitle", Label.class));
            subtitleLabel = firstNonNull(subtitleLabel, findNode(clientContent, "subtitleLabel", Label.class));
            welcomeLabel = firstNonNull(welcomeLabel, findNode(clientContent, "welcomeLabel", Label.class));
            searchTitle = firstNonNull(searchTitle, findNode(clientContent, "searchTitle", Label.class));
            apiStatusLabel = firstNonNull(apiStatusLabel, findNode(clientContent, "apiStatusLabel", Label.class));
            filterLabel = firstNonNull(filterLabel, findNode(clientContent, "filterLabel", Label.class));
            roomTypeFilterLabel = firstNonNull(roomTypeFilterLabel, findNode(clientContent, "roomTypeFilterLabel", Label.class));
            roomStatusFilterLabel = firstNonNull(roomStatusFilterLabel, findNode(clientContent, "roomStatusFilterLabel", Label.class));

            searchField = firstNonNull(searchField, findNode(clientContent, "searchField", TextField.class));
            apiSearchField = firstNonNull(apiSearchField, findNode(clientContent, "apiSearchField", TextField.class));
            apiToolbar = firstNonNull(apiToolbar, findNode(clientContent, "apiToolbar", HBox.class));
            searchButton = firstNonNull(searchButton, findNode(clientContent, "searchButton", Button.class));
            geoapifyApiButton = firstNonNull(geoapifyApiButton, findNode(clientContent, "geoapifyApiButton", Button.class));
            apiLoadingIndicator = firstNonNull(apiLoadingIndicator, findNode(clientContent, "apiLoadingIndicator", ProgressIndicator.class));

            starsFilterCombo = firstNonNull(starsFilterCombo, findNode(clientContent, "starsFilterCombo", ComboBox.class));
            hotelFilterCombo = firstNonNull(hotelFilterCombo, findNode(clientContent, "hotelFilterCombo", ComboBox.class));
            roomTypeFilterCombo = firstNonNull(roomTypeFilterCombo, findNode(clientContent, "roomTypeFilterCombo", ComboBox.class));
            roomStatusFilterCombo = firstNonNull(roomStatusFilterCombo, findNode(clientContent, "roomStatusFilterCombo", ComboBox.class));

            filterContainer = firstNonNull(filterContainer, findNode(clientContent, "filterContainer", VBox.class));
            roomTypeFilterContainer = firstNonNull(roomTypeFilterContainer, findNode(clientContent, "roomTypeFilterContainer", VBox.class));
            roomStatusFilterContainer = firstNonNull(roomStatusFilterContainer, findNode(clientContent, "roomStatusFilterContainer", VBox.class));
            roomFilterSeparator = firstNonNull(roomFilterSeparator, findNode(clientContent, "roomFilterSeparator", Pane.class));
        }
    }

    private void wireActionHandlers() {
        if (searchButton != null) searchButton.setOnAction(e -> handleSearch());
        if (starsFilterCombo != null) starsFilterCombo.setOnAction(e -> handleFilter());
        if (hotelFilterCombo != null) hotelFilterCombo.setOnAction(e -> handleFilter());
        if (roomTypeFilterCombo != null) roomTypeFilterCombo.setOnAction(e -> handleFilter());
        if (roomStatusFilterCombo != null) roomStatusFilterCombo.setOnAction(e -> handleFilter());
        if (geoapifyApiButton != null) geoapifyApiButton.setOnAction(e -> handleGeoapifySearchAction());
    }

    private <T> T firstNonNull(T current, T fallback) {
        return current != null ? current : fallback;
    }

    private <T> T findNode(Node root, String fxId, Class<T> type) {
        Node found = findNodeById(root, fxId);
        return type.isInstance(found) ? type.cast(found) : null;
    }

    private Node findNodeById(Node root, String fxId) {
        if (root == null || fxId == null) return null;

        Object fxIdProperty = root.getProperties().get("fx:id");
        if (fxId.equals(root.getId()) || fxId.equals(fxIdProperty)) {
            return root;
        }

        if (root instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                Node found = findNodeById(child, fxId);
                if (found != null) return found;
            }
        }

        return null;
    }

    private void setupHotelsFilter() {
        showSearchPanel(true);
        filterLabel.setText("Etoiles");

        starsFilterCombo.getItems().clear();
        starsFilterCombo.getItems().addAll("Toutes", "5 etoiles", "4 etoiles", "3 etoiles", "2 etoiles", "1 etoile");
        starsFilterCombo.setValue("Toutes");
        starsFilterCombo.setVisible(true);
        starsFilterCombo.setManaged(true);
        hotelFilterCombo.setVisible(false);
        hotelFilterCombo.setManaged(false);

        subtitleLabel.setText("Decouvrez nos meilleurs hotels");
        searchTitle.setText("Ou voulez-vous aller ?");
        sectionTitle.setText("Hotels Disponibles");
        searchField.setPromptText("Nom d'hotel...");
        searchField.clear();

        showHotelAdvancedFilters();
        apiVC.updateApiButtonState(sharedState.databaseAvailable, true);
        if (true) apiVC.setApiStatus("Pret pour recherche API", "idle");
    }

    private void setupRoomsFilter() {
        showSearchPanel(true);
        filterLabel.setText("Hotel");

        roomVC.setupHotelFilterCombo(hotelFilterCombo);
        starsFilterCombo.setVisible(false);
        starsFilterCombo.setManaged(false);
        hotelFilterCombo.setVisible(true);
        hotelFilterCombo.setManaged(true);

        subtitleLabel.setText("Trouvez la chambre ideale");
        searchTitle.setText("Quelle chambre cherchez-vous ?");
        sectionTitle.setText("Chambres Disponibles");
        searchField.setPromptText("Numero chambre ou hotel...");
        searchField.clear();

        showRoomAdvancedFilters();
        apiVC.updateApiButtonState(sharedState.databaseAvailable, false);
        apiVC.setApiStatus("API disponible en vue Hotels uniquement", "idle");
    }

    private void showHotelAdvancedFilters() {
        roomFilterSeparator.setVisible(true);
        roomFilterSeparator.setManaged(true);
        roomTypeFilterContainer.setVisible(true);
        roomTypeFilterContainer.setManaged(true);
        roomStatusFilterContainer.setVisible(true);
        roomStatusFilterContainer.setManaged(true);

        if (roomTypeFilterLabel  != null) roomTypeFilterLabel.setText("Localisation");
        if (roomStatusFilterLabel != null) roomStatusFilterLabel.setText("Statut");

        sharedState.refreshLocalisationLookup();
        roomTypeFilterCombo.getItems().clear();
        roomTypeFilterCombo.getItems().add("Toutes localisations");
        roomTypeFilterCombo.getItems().addAll(sharedState.getSortedLocalisationLabels());
        roomTypeFilterCombo.setValue("Toutes localisations");
        roomTypeFilterCombo.setPromptText("Localisation");
        roomTypeFilterCombo.setMinWidth(190);
        roomTypeFilterCombo.setPrefWidth(190);

        roomStatusFilterCombo.getItems().clear();
        roomStatusFilterCombo.getItems().addAll("Tous", "AVAILABLE", "OCCUPIED", "MAINTENANCE");
        roomStatusFilterCombo.setValue("Tous");
        roomStatusFilterCombo.setPromptText("Statut");
        roomStatusFilterCombo.setMinWidth(130);
        roomStatusFilterCombo.setPrefWidth(130);
    }

    private void showRoomAdvancedFilters() {
        roomFilterSeparator.setVisible(true);
        roomFilterSeparator.setManaged(true);
        roomTypeFilterContainer.setVisible(true);
        roomTypeFilterContainer.setManaged(true);
        if (roomTypeFilterLabel != null) roomTypeFilterLabel.setText("Type");

        roomTypeFilterCombo.getItems().clear();
        roomTypeFilterCombo.getItems().addAll("Tous", "SINGLE", "DOUBLE", "SUITE", "DELUXE", "FAMILY");
        roomTypeFilterCombo.setValue("Tous");
        roomTypeFilterCombo.setPromptText("Type");
        roomTypeFilterCombo.setMinWidth(120);
        roomTypeFilterCombo.setPrefWidth(120);

        roomStatusFilterContainer.setVisible(true);
        roomStatusFilterContainer.setManaged(true);
        if (roomStatusFilterLabel != null) roomStatusFilterLabel.setText("Statut");

        roomStatusFilterCombo.getItems().clear();
        roomStatusFilterCombo.getItems().addAll("Tous", "AVAILABLE", "OCCUPIED", "MAINTENANCE");
        roomStatusFilterCombo.setValue("Tous");
        roomStatusFilterCombo.setPromptText("Statut");
        roomStatusFilterCombo.setMinWidth(120);
        roomStatusFilterCombo.setPrefWidth(120);
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    //  Navigation (called by sub-controllers and FXML actions)
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /** Navigate to hotel card grid. */
    public void goToHotels() {
        currentView = "hotels";
        setupHotelsFilter();
        if (apiToolbar != null) { apiToolbar.setVisible(true); apiToolbar.setManaged(true); }
        if (apiSearchField != null) apiSearchField.clear();
        loadHotels();
    }

    /** Navigate to rooms grid (plain). */
    public void goToRooms() {
        currentView = "rooms";
        setupRoomsFilter();
        if (apiToolbar != null) { apiToolbar.setVisible(false); apiToolbar.setManaged(false); }
        loadRooms();
    }

    /** Navigate to rooms grid filtered by a specific hotel (from hotel card). */
    public void goToRoomsForHotel(tn.esprit.projet.entities.Hotel hotel) {
        currentView = "rooms";
        setupRoomsFilter();
        searchField.setText(hotel.getName());
        loadRooms();
    }

    /** Navigate to the Mes Réservations view. */
    public void goToReservations() {
        currentView = "reservations";
        showSearchPanel(false);
        if (btnReservations != null) setActiveButton(btnReservations);
        reservationVC.showReservations();
    }

    /** Delegate: show room detail (called from hotel availability rows and room cards). */
    public void showRoomDetails(Room room, String hotelName) {
        roomVC.showRoomDetails(room, hotelName);
    }

    public void showBookingForm(Room room, String hotelName) {
        roomVC.showBookingForm(room, hotelName);
    }

    /**
     * Opens the OSM map dialog for a hotel by looking up its coordinates via Nominatim.
     * Called from the hotel detail view's "Voir sur la Carte" button.
     */
    public void showHotelOnMap(tn.esprit.projet.entities.Hotel hotel) {
        javafx.concurrent.Task<Void> task = new javafx.concurrent.Task<>() {
            @Override protected Void call() throws Exception {
                tn.esprit.projet.services.HotelService.HotelExternalInsight insight =
                        sharedState.hotelService.fetchExternalInsight(hotel);
                tn.esprit.projet.API.hotels.NominatimHotelApiClient.LocationSummary loc =
                        insight.locationSummary().orElse(null);
                String url = apiVC.buildOpenStreetMapUrl(loc);
                javafx.application.Platform.runLater(() -> apiVC.showOpenStreetMapPreviewDialog(loc, url));
                return null;
            }
            @Override protected void failed() {
                javafx.application.Platform.runLater(() ->
                        dialogHelper.showError("Carte indisponible",
                                "Impossible de charger la position: " + getException().getMessage()));
            }
        };
        // Show a brief loading indicator on the map button while fetching
        Thread t = new Thread(task, "map-lookup");
        t.setDaemon(true);
        t.start();
    }

    /** Expose apiVC so sub-controllers can open the map with an already-fetched location (no re-fetch). */
    public ClientApiViewController getApiVC() { return apiVC; }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    //  Load helpers
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private void loadHotels() {
        if (!sharedState.databaseAvailable) { showDatabaseUnavailableState(); return; }
        hotelVC.loadHotels(hotelsContainer, sectionTitle, searchField,
                starsFilterCombo, roomTypeFilterCombo, roomStatusFilterCombo);
    }

    private void loadRooms() {
        if (!sharedState.databaseAvailable) { showDatabaseUnavailableState(); return; }
        roomVC.loadRooms(hotelsContainer, sectionTitle, searchField,
                hotelFilterCombo, roomTypeFilterCombo, roomStatusFilterCombo);
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    //  Offline / error states
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private void showDatabaseUnavailableState() {
        hotelsContainer.getChildren().clear();
        sectionTitle.setText("Connexion base indisponible");
        subtitleLabel.setText("Le service MySQL est indisponible");
        String details = sharedState.databaseErrorMessage == null || sharedState.databaseErrorMessage.isBlank()
                ? "Impossible de se connecter a la base. Verifiez MySQL et vos identifiants."
                : sharedState.databaseErrorMessage;
        showEmptyState("", "Connexion MySQL impossible", details);
    }

    private void disableUiForOfflineMode() {
        searchPanel.setDisable(true);
        btnHotels.setDisable(true);
        btnChambres.setDisable(true);
        btnReservations.setDisable(true);

        btnProfil.setDisable(true);
    }

    private void showEmptyState(String icon, String title, String subtitle) {
        VBox box = new VBox(10);
        box.setAlignment(javafx.geometry.Pos.CENTER);
        box.setPadding(new javafx.geometry.Insets(50));
        Label iconL  = new Label(icon);  iconL.setStyle("-fx-font-size: 48px;");
        Label titleL = new Label(title); titleL.setStyle("-fx-font-size: 18px; -fx-text-fill: rgba(255,255,255,0.6);");
        Label subL   = new Label(subtitle); subL.setStyle("-fx-font-size: 12px; -fx-text-fill: rgba(255,255,255,0.4);");
        box.getChildren().addAll(iconL, titleL, subL);
        hotelsContainer.getChildren().add(box);
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    //  FXML action handlers
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @FXML
    private void handleSearch() {
        if (!sharedState.databaseAvailable) { showDatabaseUnavailableState(); return; }
        if (currentView.equals("rooms")) loadRooms();
        else {
            loadHotels();
            apiVC.setApiStatus("Retour aux donnees locales", "idle");
        }
    }

    @FXML
    private void handleFilter() {
        if (!sharedState.databaseAvailable) { showDatabaseUnavailableState(); return; }
        if (currentView.equals("rooms")) loadRooms();
        else {
            loadHotels();
            apiVC.setApiStatus("Retour aux donnees locales", "idle");
        }
    }

    @FXML
    private void handleGeoapifySearchAction() {
        apiVC.handleGeoapifySearch("hotels".equals(currentView));
    }

    @FXML

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    //  Menu click handler
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private void handleMenuClick(Button btn) {
        if (btn == btnLogout) { System.exit(0); return; }
        setActiveButton(btn);
        if (!sharedState.databaseAvailable) { showDatabaseUnavailableState(); return; }
        if (btn == btnHotels)           goToHotels();
        else if (btn == btnChambres)    goToRooms();
        else if (btn == btnReservations) goToReservations();
    }




































































    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

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

    private void handleHover(Button targetBtn) { setActiveButton(targetBtn); }

    private void glidePane(Button target, int ms) {
        if (slidingPane == null || target == null) return;
        TranslateTransition tt = new TranslateTransition(Duration.millis(ms), slidingPane);
        tt.setToY(target.getLayoutY());
        tt.setInterpolator(Interpolator.SPLINE(0.16, 0.84, 0.44, 1.0));
        tt.play();
    }

    private void moveBubble(Button target)     { glidePane(target, 320); }
    private void moveBubbleFast(Button target) { glidePane(target, 160); }

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

    private void animateText(Button btn, boolean activate) { highlightButton(btn, activate); }

    private void applyLiquidDirection(Button target) {
        if (slidingPane == null) return;
        slidingPane.getStyleClass().removeAll("from-top", "from-bottom");
        if (currentActiveBtn == null) return;
        slidingPane.getStyleClass().add(
                target.getLayoutY() > currentActiveBtn.getLayoutY() ? "from-top" : "from-bottom");
    }

    private void setupResponsiveGrid() {
        hotelsContainer.setHgap(24);
        hotelsContainer.setVgap(28);
        hotelsContainer.setPrefWrapLength(1080);
        hotelsContainer.setAlignment(javafx.geometry.Pos.TOP_CENTER);
        hotelsContainer.setMinWidth(0);
        hotelsContainer.setMaxWidth(Double.MAX_VALUE);

        Platform.runLater(() -> {
            double initial = Math.max(700, contentArea.getWidth() - 80);
            hotelsContainer.setPrefWidth(initial);
            hotelsContainer.setPrefWrapLength(initial);
        });

        contentArea.widthProperty().addListener((obs, oldVal, newVal) -> {
            double available = Math.max(700, newVal.doubleValue() - 80);
            hotelsContainer.setPrefWidth(available);
            hotelsContainer.setPrefWrapLength(available);
        });
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    //  Package-visible accessors (used by sub-controllers)
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    public FlowPane getHotelsContainer() { return hotelsContainer; }
    public Label    getSectionTitle()    { return sectionTitle;    }
    public Label    getSearchTitle()     { return searchTitle;     }
    public Label    getSubtitleLabel()   { return subtitleLabel;   }
    public javafx.scene.control.TextField        getClientSearchField()     { return searchField;          }
    public javafx.scene.control.ComboBox<String> getRoomStatusFilterCombo() { return roomStatusFilterCombo; }
    public javafx.scene.control.ComboBox<String> getRoomTypeFilterCombo()   { return roomTypeFilterCombo;  }

    public void hideStarsFilter() {
        if (starsFilterCombo != null) { starsFilterCombo.setVisible(false); starsFilterCombo.setManaged(false); }
    }
    public void hideHotelFilter() {
        if (hotelFilterCombo != null) { hotelFilterCombo.setVisible(false); hotelFilterCombo.setManaged(false); }
    }

    /**
     * Configure the extra-filter slots for the Reservations view:
     * hides room-type filter, shows status filter pre-filled with reservation statuses.
     */
    public void showReservationFilters() {
        if (roomFilterSeparator       != null) { roomFilterSeparator.setVisible(true);       roomFilterSeparator.setManaged(true);       }
        if (roomTypeFilterContainer   != null) { roomTypeFilterContainer.setVisible(false);  roomTypeFilterContainer.setManaged(false);  }
        if (roomStatusFilterContainer != null) { roomStatusFilterContainer.setVisible(true); roomStatusFilterContainer.setManaged(true); }
        if (roomStatusFilterLabel     != null) roomStatusFilterLabel.setText("Statut");
        if (roomStatusFilterCombo != null) {
            roomStatusFilterCombo.getItems().clear();
            roomStatusFilterCombo.getItems().addAll("Tous les statuts", "En attente", "Confirmée", "Annulée");
            roomStatusFilterCombo.setValue("Tous les statuts");
        }
    }

    public void showSearchPanel(boolean show) {
        searchPanel.setVisible(show);
        searchPanel.setManaged(show);
        headerBox.setVisible(show);
        headerBox.setManaged(show);
    }


    public Image loadImage(String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) return null;
        try {
            if (imagePath.startsWith("http://") || imagePath.startsWith("https://"))
                return new Image(imagePath, true);

            File absolute = new File(imagePath);
            if (absolute.exists()) return new Image(absolute.toURI().toString());

            URL resourceUrl = getClass().getResource("/images/" + imagePath);
            if (resourceUrl != null) return new Image(resourceUrl.toExternalForm());

            for (String path : new String[]{
                    "src/main/resources/images/" + imagePath,
                    "target/classes/images/" + imagePath
            }) {
                File f = new File(path);
                if (f.exists()) return new Image(f.toURI().toString());
            }
        } catch (Exception e) {
            System.err.println("Error loading image: " + imagePath + " - " + e.getMessage());
        }
        return null;
    }
}