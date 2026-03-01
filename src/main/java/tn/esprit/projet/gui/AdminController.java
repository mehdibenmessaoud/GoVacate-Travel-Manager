package tn.esprit.projet.gui;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;
import tn.esprit.projet.API.hotels.NominatimHotelApiClient;
import tn.esprit.projet.entities.Hotel;
import tn.esprit.projet.entities.Room;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Thin FXML controller for Admin.fxml.
 *
 * Uses fx:controller-injected sub-controllers from included FXML files
 * instead of manual tree walking. All child nodes are accessed via:
 *   sidebar.btnHotels, list.mainTable, detail.detailName, etc.
 */
public class AdminController implements Initializable {

    // ── Injected roots from fx:include ────────────────────────────────────────
    @FXML private AnchorPane adminSidebar;
    @FXML private VBox       adminListSection;
    @FXML private VBox       adminDetailSection;
    @FXML private StackPane  rootPane;
    @FXML private VBox       mainContent;

    // ── Injected sub-controllers (JavaFX convention: fx:id + "Controller") ────
    @FXML private AdminSidebarController       adminSidebarController;
    @FXML private AdminListSectionController   adminListSectionController;
    @FXML private AdminDetailSectionController adminDetailSectionController;

    // ── Convenience aliases (set once in initialize) ──────────────────────────
    private AdminSidebarController       sidebar;
    private AdminListSectionController   list;
    private AdminDetailSectionController detail;

    // ── Sub-controllers ───────────────────────────────────────────────────────
    private AdminSharedState               state;
    private AdminHotelViewController       hotelVC;
    private AdminRoomViewController        roomVC;
    private AdminReviewViewController      reviewVC;
    private AdminReservationViewController reservationVC;
    private AdminApiViewController         apiVC;

    // ── Navigation state ──────────────────────────────────────────────────────
    private String  currentView      = "hotels";
    private Button  currentActiveBtn = null;
    private boolean setupInProgress  = false;

    // ─────────────────────────────────────────────────────────────────────────
    //  Initialise
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 1 – alias sub-controllers for brevity
        sidebar = adminSidebarController;
        list    = adminListSectionController;
        detail  = adminDetailSectionController;

        // 2 – build shared state + init all services
        state = new AdminSharedState();
        state.initServices();

        // 3 – build sub-controllers
        hotelVC       = new AdminHotelViewController(state, this,
                state.hotelService, state.hotelImageService, state.hotelServiceItemService);
        roomVC        = new AdminRoomViewController(state, this,
                state.roomService, state.roomImageService);
        reviewVC      = new AdminReviewViewController(state, this, state.hotelReviewService);
        reservationVC = new AdminReservationViewController(this);
        apiVC         = new AdminApiViewController(this, state.hotelService);

        // 4 – load data
        if (state.databaseAvailable) loadAllData();

        // 5 – wire handlers
        wireActionHandlers();

        // 6 – sliding pane setup
        if (sidebar.slidingPane != null) {
            sidebar.slidingPane.setMouseTransparent(true);
        }

        // 7 – post-layout
        Platform.runLater(() -> {
            if (sidebar.btnHotels != null && sidebar.slidingPane != null) {
                sidebar.slidingPane.setTranslateY(sidebar.btnHotels.getLayoutY());
                currentActiveBtn = sidebar.btnHotels;
                highlightButton(sidebar.btnHotels, true);
            }
            if (state.databaseAvailable) {
                goToHotels();
            } else {
                showDatabaseUnavailableState();
            }
        });

        // 8 – hover animation
        if (sidebar.menuContainer != null) {
            Button[] buttons = { sidebar.btnHotels, sidebar.btnChambres, sidebar.btnReservations, sidebar.btnLogout };
            sidebar.menuContainer.setOnMouseMoved(event -> {
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
            sidebar.menuContainer.setOnMouseExited(event -> {
                if (currentActiveBtn != null) moveBubble(currentActiveBtn);
            });
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Action handler wiring
    // ─────────────────────────────────────────────────────────────────────────

    private void wireActionHandlers() {
        Button[] buttons = { sidebar.btnHotels, sidebar.btnChambres, sidebar.btnReservations, sidebar.btnLogout };
        for (Button btn : buttons) {
            if (btn == null) continue;
            btn.setOnAction(e -> handleMenuClick(btn));
        }

        if (list.searchButton     != null) list.searchButton.setOnAction(e -> handleSearch());
        if (list.starsFilterCombo != null) list.starsFilterCombo.setOnAction(e -> handleFilter());
        if (list.hotelFilterCombo != null) list.hotelFilterCombo.setOnAction(e -> handleFilter());
        if (list.roomTypeFilterCombo   != null) list.roomTypeFilterCombo.setOnAction(e -> handleFilter());
        if (list.roomStatusFilterCombo != null) list.roomStatusFilterCombo.setOnAction(e -> handleFilter());

        if (list.geoapifyApiButton  != null) list.geoapifyApiButton.setOnAction(e -> handleGeoapifySearch());
        if (list.nominatimApiButton != null) list.nominatimApiButton.setOnAction(e -> handleNominatimSearch());

        if (detail.backButton != null) detail.backButton.setOnAction(e -> handleBack());

        if (detail.addImageBtn != null) detail.addImageBtn.setOnAction(e -> {
            Hotel sel = state.getSelectedHotel();
            if (sel != null) hotelVC.showAddImageDialog(sel, detail.imagesTable, detail.mainImageContainer, detail.thumbnailsContainer);
        });
        if (detail.addRoomImageBtn != null) detail.addRoomImageBtn.setOnAction(e -> {
            Room sel = state.getSelectedRoom();
            if (sel != null) {
                try {
                    String path = promptImagePath();
                    if (path != null) roomVC.saveRoomImage(path, sel.getId(), detail.roomImagesTable, detail.mainImageContainer, detail.thumbnailsContainer);
                } catch (java.sql.SQLException ex) {
                    DialogHelper.showNotification("Erreur: " + ex.getMessage(), "error", getClass());
                }
            }
        });
    }

    private String promptImagePath() {
        TextInputDialog d = new TextInputDialog();
        d.setTitle("Ajouter image"); d.setHeaderText("Chemin ou URL de l'image");
        DialogHelper.styleDialog(d, false, getClass());
        return d.showAndWait().orElse(null);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Navigation
    // ─────────────────────────────────────────────────────────────────────────

    public void goToHotels() {
        currentView = "hotels";
        setActiveButton(sidebar.btnHotels);
        switchToListView();
        setupHotelsView();
    }

    public void goToRooms() {
        currentView = "rooms";
        setActiveButton(sidebar.btnChambres);
        switchToListView();
        setupRoomsView();
    }

    public void navigateToRoomsFilteredByHotel(Hotel hotel) {
        goToRooms();
        setupInProgress = true;
        try { list.hotelFilterCombo.setValue(hotel.getName()); } finally { setupInProgress = false; }
        loadRoomsTable();
    }

    public void showRoomsView() { goToRooms(); }
    public void showHotelsView() { goToHotels(); }

    public void goToReservations() {
        currentView = "reservations";
        setActiveButton(sidebar.btnReservations);
        switchToListView();
        setupReservationsView();
    }

    @SuppressWarnings("unchecked")
    public void showHotelDetail(Hotel hotel) {
        state.setSelectedHotel(hotel);
        state.setSelectedRoom(null);
        switchToDetailView();
        hotelVC.populateHotelDetail(hotel,
                detail.detailName, detail.detailSubInfo, detail.detailApiRow, detail.detailDesc,
                detail.detailBadges, detail.detailActionButtons,
                detail.detailServicesSection, detail.detailServicesTitle, detail.detailServicesPane,
                detail.detailManageServicesBtn,
                detail.reviewsSection, detail.imagesSection, detail.roomImagesSection,
                detail.imagesTable, detail.mainImageContainer, detail.thumbnailsContainer, detail.reviewsTable);
        reviewVC.setupReviewsTable(detail.reviewsTable, detail.colReviewRating, detail.colReviewComment,
                detail.colReviewUser, detail.colReviewActions);
        reviewVC.loadHotelReviews(hotel.getId(), detail.reviewsTable);
        hotelVC.setupImagesTable(detail.colImagePath, detail.colImagePreview, detail.colImageActions);
        hotelVC.loadHotelImages(hotel.getId(), detail.imagesTable, detail.mainImageContainer, detail.thumbnailsContainer);
    }

    public void showRoomDetail(Room room) {
        state.setSelectedRoom(room);
        switchToDetailView();
        roomVC.populateRoomDetail(room,
                detail.detailName, detail.detailSubInfo, detail.detailDesc,
                detail.detailBadges, detail.detailActionButtons,
                detail.detailServicesSection, detail.detailServicesPane,
                detail.reviewsSection, detail.imagesSection, detail.roomImagesSection,
                detail.roomImagesTable, detail.mainImageContainer, detail.thumbnailsContainer);
        roomVC.setupRoomImagesTable(detail.colRoomImagePath, detail.colRoomImagePreview, detail.colRoomImageActions);
        roomVC.loadRoomImages(room.getId(), detail.roomImagesTable, detail.mainImageContainer, detail.thumbnailsContainer);
    }

    public void loadAllData() {
        try { state.setHotelsList(FXCollections.observableArrayList(state.hotelService.getAll())); }
        catch (Exception e) { state.setHotelsList(FXCollections.observableArrayList()); }
        try { state.setRoomsList(FXCollections.observableArrayList(state.roomService.getAll())); }
        catch (Exception e) { state.setRoomsList(FXCollections.observableArrayList()); }
        state.refreshLocalisationLookup();
    }

    @SuppressWarnings("unchecked")
    public void loadHotelsTable() {
        hotelVC.loadHotelsTable(list.mainTable, list.searchField, list.starsFilterCombo,
                list.roomTypeFilterCombo, list.roomStatusFilterCombo, list.pageSubtitle);
    }

    @SuppressWarnings("unchecked")
    public void loadRoomsTable() {
        roomVC.loadRoomsTable(list.mainTable, list.searchField,
                list.hotelFilterCombo, list.roomTypeFilterCombo, list.roomStatusFilterCombo, list.pageSubtitle);
    }

    public void openHotelMap(Hotel hotel) {
        Thread t = new Thread(() -> {
            try {
                NominatimHotelApiClient client = new NominatimHotelApiClient();
                NominatimHotelApiClient.LocationSummary loc =
                        client.findLocationSummary(hotel.getName()).orElse(null);
                Platform.runLater(() -> apiVC.openOpenStreetMapLocation(loc));
            } catch (Exception e) {
                Platform.runLater(() -> DialogHelper.showNotification(
                        "Impossible de localiser l'hotel: " + GuiUtils.extractErrorMessage(e),
                        "error", getClass()));
            }
        }, "admin-nominatim-hotel-map");
        t.setDaemon(true);
        t.start();
    }

    public void reloadReviews(int hotelId) {
        reviewVC.loadHotelReviews(hotelId, detail.reviewsTable);
    }

    public AdminApiViewController getApiVC() { return apiVC; }
    public AdminSharedState getState() { return state; }
    public AdminHotelViewController getHotelViewController() { return hotelVC; }

    @SuppressWarnings("unchecked")
    public void configureGeoapifyColumns() {
        apiVC.setupGeoapifyColumns(list.col1, list.col2, list.col3, list.col4, list.col5, list.col6);
    }

    @SuppressWarnings("unchecked")
    public void configureNominatimColumns() {
        apiVC.setupNominatimColumns(list.col1, list.col2, list.col3, list.col4, list.col5, list.col6);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  View setup helpers
    // ─────────────────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private void setupHotelsView() {
        setupInProgress = true;
        try {
            if (list.pageTitle       != null) list.pageTitle.setText("Gestion des H\u00f4tels");
            if (list.sectionTitle    != null) list.sectionTitle.setText("Liste d'hotels");
            if (list.searchPanelTitle != null) list.searchPanelTitle.setText("Recherche et Actions");
            if (list.filterLabel     != null) list.filterLabel.setText("Etoiles");

            if (list.starsFilterCombo != null) { list.starsFilterCombo.setVisible(true);  list.starsFilterCombo.setManaged(true);  }
            if (list.hotelFilterCombo != null) { list.hotelFilterCombo.setVisible(false); list.hotelFilterCombo.setManaged(false); }

            if (list.roomTypeFilterLabel  != null) list.roomTypeFilterLabel.setText("Localisation");
            if (list.roomStatusFilterLabel != null) list.roomStatusFilterLabel.setText("Statut");
            setFilterSectionVisible(true);

            state.refreshLocalisationLookup();
            if (list.roomTypeFilterCombo != null) {
                list.roomTypeFilterCombo.getItems().clear();
                list.roomTypeFilterCombo.getItems().add("Toutes localisations");
                list.roomTypeFilterCombo.getItems().addAll(state.getSortedLocalisationLabels());
                list.roomTypeFilterCombo.setValue("Toutes localisations");
                list.roomTypeFilterCombo.setPromptText("Localisation");
            }
            if (list.roomStatusFilterCombo != null) {
                list.roomStatusFilterCombo.getItems().clear();
                list.roomStatusFilterCombo.getItems().addAll("Tous", "AVAILABLE", "OCCUPIED", "MAINTENANCE");
                list.roomStatusFilterCombo.setValue("Tous");
                list.roomStatusFilterCombo.setPromptText("Statut");
            }

            if (list.addButton != null) {
                list.addButton.setText("+ Ajouter");
                list.addButton.setDisable(false);
                list.addButton.setOnAction(e -> hotelVC.showAddHotelDialog());
            }
            if (list.searchButton != null) list.searchButton.setOnAction(e -> handleSearch());

            hotelVC.setupHotelsTable(list.mainTable, list.col1, list.col2, list.col3, list.col4, list.col5, list.col6);
            hotelVC.setupStarsFilter(list.starsFilterCombo);

            if (list.apiToolbar != null) { ((VBox)list.apiToolbar).setVisible(true); ((VBox)list.apiToolbar).setManaged(true); }
            apiVC.updateApiControlsForView(true, list.geoapifyApiButton);
            if (list.nominatimApiButton != null) list.nominatimApiButton.setDisable(false);
        } finally {
            setupInProgress = false;
        }
        if (state.databaseAvailable) loadHotelsTable();
        else showDatabaseUnavailableState();
    }

    @SuppressWarnings("unchecked")
    private void setupRoomsView() {
        setupInProgress = true;
        try {
            if (list.pageTitle       != null) list.pageTitle.setText("Gestion des Chambres");
            if (list.sectionTitle    != null) list.sectionTitle.setText("Liste de chambres");
            if (list.searchPanelTitle != null) list.searchPanelTitle.setText("Recherche et Actions");
            if (list.filterLabel     != null) list.filterLabel.setText("Hotel");

            if (list.starsFilterCombo != null) { list.starsFilterCombo.setVisible(false); list.starsFilterCombo.setManaged(false); }
            if (list.hotelFilterCombo != null) { list.hotelFilterCombo.setVisible(true);  list.hotelFilterCombo.setManaged(true);  }

            if (list.roomTypeFilterLabel  != null) list.roomTypeFilterLabel.setText("Type");
            if (list.roomStatusFilterLabel != null) list.roomStatusFilterLabel.setText("Statut");
            setFilterSectionVisible(true);

            if (list.roomTypeFilterCombo != null) {
                list.roomTypeFilterCombo.getItems().clear();
                list.roomTypeFilterCombo.getItems().addAll("Tous", "SINGLE", "DOUBLE", "SUITE", "DELUXE", "FAMILY");
                list.roomTypeFilterCombo.setValue("Tous");
                list.roomTypeFilterCombo.setPromptText("Type");
            }
            if (list.roomStatusFilterCombo != null) {
                list.roomStatusFilterCombo.getItems().clear();
                list.roomStatusFilterCombo.getItems().addAll("Tous", "AVAILABLE", "OCCUPIED", "MAINTENANCE");
                list.roomStatusFilterCombo.setValue("Tous");
                list.roomStatusFilterCombo.setPromptText("Statut");
            }

            if (list.addButton != null) {
                list.addButton.setText("+ Ajouter Chambre");
                list.addButton.setDisable(false);
                list.addButton.setOnAction(e -> roomVC.showAddRoomDialog());
            }
            if (list.searchButton != null) list.searchButton.setOnAction(e -> handleSearch());

            if (list.mainTable != null) list.mainTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
            roomVC.setupRoomsTable(list.mainTable, list.col1, list.col2, list.col3, list.col4, list.col5, list.col6);
            roomVC.setupHotelFilter(list.hotelFilterCombo);

            if (list.apiToolbar != null) { ((VBox)list.apiToolbar).setVisible(false); ((VBox)list.apiToolbar).setManaged(false); }
            apiVC.updateApiControlsForView(false, list.geoapifyApiButton);
        } finally {
            setupInProgress = false;
        }
        if (state.databaseAvailable) loadRoomsTable();
        else showDatabaseUnavailableState();
    }

    @SuppressWarnings("unchecked")
    private void setupReservationsView() {
        setupInProgress = true;
        try {
            if (list.pageTitle       != null) list.pageTitle.setText("Gestion des R\u00e9servations");
            if (list.sectionTitle    != null) list.sectionTitle.setText("Liste de reservations");
            if (list.searchPanelTitle != null) list.searchPanelTitle.setText("Recherche");

            if (list.starsFilterCombo  != null) { list.starsFilterCombo.setVisible(false);  list.starsFilterCombo.setManaged(false);  }
            if (list.hotelFilterCombo  != null) { list.hotelFilterCombo.setVisible(false);  list.hotelFilterCombo.setManaged(false);  }
            setFilterSectionVisible(false);

            if (list.addButton != null) list.addButton.setDisable(true);
            if (list.searchButton != null) list.searchButton.setOnAction(e -> reservationVC.refreshFilter());

            reservationVC.setupReservationsTable(list.mainTable, list.col1, list.col2, list.col3, list.col4, list.col5, list.col6);

            if (list.apiToolbar != null) { ((VBox)list.apiToolbar).setVisible(false); ((VBox)list.apiToolbar).setManaged(false); }
            apiVC.updateApiControlsForView(false, list.geoapifyApiButton);
        } finally {
            setupInProgress = false;
        }
        reservationVC.loadReservationsTable(list.mainTable, list.searchField,
                list.hotelFilterCombo, list.roomStatusFilterCombo, list.pageSubtitle);
    }

    private void setFilterSectionVisible(boolean visible) {
        if (list.roomFilterSeparator      != null) { list.roomFilterSeparator.setVisible(visible);      list.roomFilterSeparator.setManaged(visible);      }
        if (list.roomTypeFilterContainer  != null) { list.roomTypeFilterContainer.setVisible(visible);   list.roomTypeFilterContainer.setManaged(visible);   }
        if (list.roomStatusFilterContainer != null){ list.roomStatusFilterContainer.setVisible(visible); list.roomStatusFilterContainer.setManaged(visible); }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  FXML action handlers
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
        String query = list.apiSearchField != null ? list.apiSearchField.getText().trim() : "";
        if (query.isBlank()) {
            DialogHelper.showNotification("Veuillez saisir une destination.", "warning", getClass());
            return;
        }
        apiVC.executeGeoapifySearchAsync(query, list.mainTable, list.pageTitle, list.pageSubtitle,
                list.addButton, list.apiLoadingIndicator, list.geoapifyApiButton);
    }

    private void handleNominatimSearch() {
        if (!state.databaseAvailable) return;
        String query = list.apiSearchField != null ? list.apiSearchField.getText().trim() : "";
        if (query.isBlank()) {
            DialogHelper.showNotification("Veuillez saisir une destination.", "warning", getClass());
            return;
        }
        apiVC.executeNominatimSearchAsync(query, list.mainTable, list.pageTitle, list.pageSubtitle,
                list.addButton, list.apiLoadingIndicator, list.nominatimApiButton);
    }

    private void handleBack() {
        if ("rooms".equals(currentView) && state.getSelectedRoom() != null) {
            Hotel parent = state.getSelectedHotel();
            if (parent != null) { showHotelDetail(parent); return; }
        }
        goToHotels();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Menu click
    // ─────────────────────────────────────────────────────────────────────────

    private void handleMenuClick(Button btn) {
        if (btn == sidebar.btnLogout) { System.exit(0); return; }
        setActiveButton(btn);
        if (!state.databaseAvailable) { showDatabaseUnavailableState(); return; }
        if      (btn == sidebar.btnHotels)        goToHotels();
        else if (btn == sidebar.btnChambres)      goToRooms();
        else if (btn == sidebar.btnReservations)  goToReservations();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  View switching
    // ─────────────────────────────────────────────────────────────────────────

    private void switchToListView() {
        if (adminListSection   != null) { adminListSection.setVisible(true);    adminListSection.setManaged(true);   }
        if (adminDetailSection != null) { adminDetailSection.setVisible(false); adminDetailSection.setManaged(false); }
    }

    private void switchToDetailView() {
        if (adminListSection   != null) { adminListSection.setVisible(false);  adminListSection.setManaged(false);  }
        if (adminDetailSection != null) { adminDetailSection.setVisible(true); adminDetailSection.setManaged(true); }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Offline / error states
    // ─────────────────────────────────────────────────────────────────────────

    private void showDatabaseUnavailableState() {
        if (list.mainTable != null) list.mainTable.getItems().clear();
        if (list.pageTitle    != null) list.pageTitle.setText("Connexion base indisponible");
        if (list.pageSubtitle != null) {
            String msg = state.databaseErrorMessage == null || state.databaseErrorMessage.isBlank()
                    ? "Impossible de se connecter a la base. Verifiez MySQL."
                    : state.databaseErrorMessage;
            list.pageSubtitle.setText(msg);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Sidebar liquid-menu animation
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
        if (sidebar.slidingPane == null || target == null) return;
        TranslateTransition tt = new TranslateTransition(Duration.millis(ms), sidebar.slidingPane);
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
        if (sidebar.slidingPane == null || currentActiveBtn == null) return;
        sidebar.slidingPane.getStyleClass().removeAll("from-top", "from-bottom");
        sidebar.slidingPane.getStyleClass().add(
                target.getLayoutY() > currentActiveBtn.getLayoutY() ? "from-top" : "from-bottom");
    }
}
