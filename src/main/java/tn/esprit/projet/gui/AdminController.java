package tn.esprit.projet.gui;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.CacheHint;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.Node;
import javafx.util.Duration;
import tn.esprit.projet.entities.*;
import tn.esprit.projet.services.*;
import tn.esprit.projet.utils.MyDBConnexion;

import java.io.File;
import java.net.URL;
import java.sql.*;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class AdminController {

    // Sliding pane for liquid menu animation
    @FXML private Pane slidingPane;
    @FXML private VBox menuContainer;
    private Button currentActiveBtn = null;

    @FXML private VBox mainContent;
    @FXML private VBox listViewSection;
    @FXML private TableView<Object> mainTable;
    @FXML private TableColumn<Object, String> col1;
    @FXML private TableColumn<Object, String> col2;
    @FXML private TableColumn<Object, String> col3;
    @FXML private TableColumn<Object, String> col4;
    @FXML private TableColumn<Object, String> col5;
    @FXML private TableColumn<Object, Object> col6;

    @FXML private Label pageTitle;
    @FXML private Label pageSubtitle;
    @FXML private TextField searchField;
    @FXML private Button searchButton;
    @FXML private Button addButton;
    @FXML private ComboBox<String> hotelFilterCombo;
    @FXML private ComboBox<String> starsFilterCombo;
    @FXML private ComboBox<String> roomTypeFilterCombo;
    @FXML private ComboBox<String> roomStatusFilterCombo;
    @FXML private VBox filterContainer;
    @FXML private VBox roomTypeFilterContainer;
    @FXML private VBox roomStatusFilterContainer;
    @FXML private Pane roomFilterSeparator;
    @FXML private Label filterLabel;
    @FXML private Label roomTypeFilterLabel;
    @FXML private Label roomStatusFilterLabel;
    @FXML private Button btnHotels;
    @FXML private Button btnChambres;
    @FXML private Button btnRefresh;
    @FXML private Button btnLogout;

    @FXML private VBox detailViewSection;
    @FXML private Button backButton;
    @FXML private StackPane mainImageContainer;
    @FXML private ImageView mainImageView;
    @FXML private HBox thumbnailsContainer;
    @FXML private Label detailName;
    @FXML private Label detailSubInfo;
    @FXML private FlowPane detailBadges;
    @FXML private Label detailDesc;
    @FXML private VBox detailServicesSection;
    @FXML private Label detailServicesTitle;
    @FXML private FlowPane detailServicesPane;
    @FXML private Button detailManageServicesBtn;
    @FXML private FlowPane detailActionButtons;
    @FXML private VBox reviewsSection;
    @FXML private VBox imagesSection;

    @FXML private TableView<HotelReview> reviewsTable;
    @FXML private TableColumn<HotelReview, String> colReviewRating;
    @FXML private TableColumn<HotelReview, String> colReviewComment;
    @FXML private TableColumn<HotelReview, String> colReviewUser;
    @FXML private TableColumn<HotelReview, Void> colReviewActions;

    @FXML private TableView<HotelImage> imagesTable;
    @FXML private TableColumn<HotelImage, String> colImagePath;
    @FXML private TableColumn<HotelImage, Void> colImagePreview;
    @FXML private TableColumn<HotelImage, Void> colImageActions;

    @FXML private VBox roomImagesSection;
    @FXML private TableView<RoomImage> roomImagesTable;
    @FXML private TableColumn<RoomImage, String> colRoomImagePath;
    @FXML private TableColumn<RoomImage, Void> colRoomImagePreview;
    @FXML private TableColumn<RoomImage, Void> colRoomImageActions;

    private HotelService hotelService;
    private RoomService roomService;
    private HotelReviewService reviewService;
    private HotelImageService hotelImageService;
    private RoomImageService roomImageService;
    private HotelServiceItemService hotelServiceItemService;

    private ObservableList<Hotel> hotelsList;
    private ObservableList<Room> roomsList;

    private String currentView = "hotels";
    private Hotel selectedHotel;
    private Room selectedRoom;
    private final Map<Integer, String> userDisplayCache = new HashMap<>();
    private final Map<Integer, String> destinationDisplayCache = new HashMap<>();
    private final Map<String, Integer> destinationIdByDisplay = new HashMap<>();
    private UserLookupConfig userLookupConfig;
    private boolean userLookupInitialized = false;

    private static final String MODERATION_PREFIX = "[Avis masque par moderation]";

    private static class UserLookupConfig {
        private final String table;
        private final String idColumn;
        private final String labelColumn;

        private UserLookupConfig(String table, String idColumn, String labelColumn) {
            this.table = table;
            this.idColumn = idColumn;
            this.labelColumn = labelColumn;
        }
    }

    @FXML
    public void initialize() {
        hotelService = new HotelService();
        roomService = new RoomService();
        reviewService = new HotelReviewService();
        hotelImageService = new HotelImageService();
        roomImageService = new RoomImageService();
        hotelServiceItemService = new HotelServiceItemService();

        hotelsList = FXCollections.observableArrayList();
        roomsList = FXCollections.observableArrayList();

        // Setup sliding pane for liquid menu animation
        setupSlidingPane();

        setupReviewsTable();
        setupImagesTable();
        setupRoomImagesTable();
        
        mainTable.setRowFactory(tv -> {
            TableRow<Object> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    Object item = row.getItem();
                    if (item instanceof Hotel) {
                        showHotelDetail((Hotel) item);
                    } else if (item instanceof Room) {
                        showRoomDetail((Room) item);
                    }
                }
            });
            return row;
        });

        mainTable.setPlaceholder(new Label("Aucune donnee disponible"));

        loadAllData();
        showHotelsView();
    }

    private void setupSlidingPane() {
        if (slidingPane != null) {
            slidingPane.setMouseTransparent(true);
            slidingPane.setCache(true);
            slidingPane.setCacheHint(CacheHint.SPEED);

            Button[] buttons = {btnHotels, btnChambres, btnRefresh, btnLogout};
            for (Button btn : buttons) {
                if (btn != null) {
                    btn.setOnMouseEntered(e -> handleHover(btn));
                }
            }

            Platform.runLater(() -> {
                if (btnHotels != null) {
                    slidingPane.setTranslateY(btnHotels.getLayoutY());
                    currentActiveBtn = btnHotels;
                    animateText(btnHotels, true);
                    if (!btnHotels.getStyleClass().contains("active")) {
                        btnHotels.getStyleClass().add("active");
                    }
                }
                slidingPane.toBack();
            });

            if (menuContainer != null) {
                menuContainer.setOnMouseMoved(event -> {
                    double mouseY = event.getY();
                    for (Button btn : buttons) {
                        if (btn != null) {
                            double startY = btn.getLayoutY();
                            double endY = startY + btn.getHeight();
                            if (mouseY >= startY && mouseY <= endY) {
                                handleHover(btn);
                                break;
                            }
                        }
                    }
                });
            }
        }
    }

    private void handleHover(Button targetBtn) {
        if (targetBtn == null || currentActiveBtn == targetBtn) return;

        if (currentActiveBtn != null) {
            animateText(currentActiveBtn, false);
            currentActiveBtn.getStyleClass().remove("active");
        }

        applyLiquidDirection(targetBtn);
        moveBubble(targetBtn);

        animateText(targetBtn, true);
        if (!targetBtn.getStyleClass().contains("active")) {
            targetBtn.getStyleClass().add("active");
        }

        currentActiveBtn = targetBtn;
    }

    private void moveBubble(Button target) {
        if (slidingPane == null || target == null) return;
        double targetY = target.getLayoutY();
        TranslateTransition tt = new TranslateTransition(Duration.millis(400), slidingPane);
        tt.setToY(targetY);
        tt.setInterpolator(Interpolator.SPLINE(0.25, 0.1, 0.25, 1));
        tt.play();
    }

    private void animateText(Button btn, boolean activate) {
        if (btn == null) return;
        ScaleTransition st = new ScaleTransition(Duration.millis(300), btn);
        st.setToX(activate ? 1.06 : 1.0);
        st.setToY(activate ? 1.12 : 1.0);
        st.setInterpolator(Interpolator.EASE_OUT);
        st.play();
    }

    private void applyLiquidDirection(Button target) {
        if (target == null) return;
        target.getStyleClass().removeAll("from-top", "from-bottom");
        if (slidingPane != null) {
            slidingPane.getStyleClass().removeAll("from-top", "from-bottom");
        }
        if (currentActiveBtn == null) return;

        double oldY = currentActiveBtn.getLayoutY();
        double newY = target.getLayoutY();

        String direction = newY > oldY ? "from-top" : "from-bottom";
        if (slidingPane != null) {
            slidingPane.getStyleClass().add(direction);
        }
    }

    private void loadAllData() {
        try {
            hotelsList.setAll(hotelService.getAll());
            roomsList.setAll(roomService.getAll());
            userDisplayCache.clear();
            refreshDestinationLookup();
        } catch (SQLException e) {
            destinationDisplayCache.clear();
            destinationIdByDisplay.clear();
            showNotification("Erreur de chargement: " + e.getMessage(), "error");
        }
    }

    @FXML
    public void showHotelsView() {
        currentView = "hotels";
        showListView();
        
        pageTitle.setText("Gestion des Hotels");
        pageSubtitle.setText("Double-cliquez sur un hotel pour voir les details");
        searchField.setPromptText("Rechercher un hotel...");
        searchField.clear();
        addButton.setText("+ Nouvel Hotel");
        addButton.setMinWidth(210);
        addButton.setPrefWidth(210);
        if (searchButton != null) {
            searchButton.setText("Rechercher");
            searchButton.setMinWidth(160);
            searchButton.setPrefWidth(160);
        }
        
        // Setup stars filter for hotels
        setupStarsFilter();
        starsFilterCombo.setVisible(true);
        starsFilterCombo.setManaged(true);
        hotelFilterCombo.setVisible(false);
        hotelFilterCombo.setManaged(false);
        filterLabel.setText("Etoiles");
        
        // Show hotel-specific advanced filters in the same area used by rooms
        showHotelFilters();

        updateSidebarButtons(btnHotels);
        setupHotelsTable();
        loadHotelsTable();
    }

    @FXML
    public void showRoomsView() {
        currentView = "rooms";
        showListView();
        
        pageTitle.setText("Gestion des Chambres");
        pageSubtitle.setText("Double-cliquez sur une chambre pour voir les details");
        searchField.setPromptText("Rechercher une chambre...");
        searchField.clear();
        addButton.setText("+ Nouvelle Chambre");
        addButton.setMinWidth(220);
        addButton.setPrefWidth(220);
        if (searchButton != null) {
            searchButton.setText("Rechercher");
            searchButton.setMinWidth(160);
            searchButton.setPrefWidth(160);
        }
        
        // Setup hotel filter for rooms
        setupHotelFilter();
        starsFilterCombo.setVisible(false);
        starsFilterCombo.setManaged(false);
        hotelFilterCombo.setVisible(true);
        hotelFilterCombo.setManaged(true);
        hotelFilterCombo.setMinWidth(180);
        hotelFilterCombo.setPrefWidth(180);
        filterLabel.setText("Hotel");
        
        // Show room-specific filters
        showRoomFilters();

        updateSidebarButtons(btnChambres);
        setupRoomsTable();
        loadRoomsTable();
    }

    private void showRoomFilters() {
        // Show room type filter
        roomFilterSeparator.setVisible(true);
        roomFilterSeparator.setManaged(true);
        roomTypeFilterContainer.setVisible(true);
        roomTypeFilterContainer.setManaged(true);
        if (roomTypeFilterLabel != null) roomTypeFilterLabel.setText("Type");
        if (roomStatusFilterLabel != null) roomStatusFilterLabel.setText("Statut");
        
        // Setup room type filter options
        roomTypeFilterCombo.getItems().clear();
        roomTypeFilterCombo.getItems().addAll("Tous", "SINGLE", "DOUBLE", "SUITE", "DELUXE", "FAMILY");
        roomTypeFilterCombo.setValue("Tous");
        roomTypeFilterCombo.setPromptText("Type");
        roomTypeFilterCombo.setMinWidth(120);
        roomTypeFilterCombo.setPrefWidth(120);
        
        // Show room status filter
        roomStatusFilterContainer.setVisible(true);
        roomStatusFilterContainer.setManaged(true);
        
        // Setup status filter options
        roomStatusFilterCombo.getItems().clear();
        roomStatusFilterCombo.getItems().addAll("Tous", "AVAILABLE", "OCCUPIED", "MAINTENANCE");
        roomStatusFilterCombo.setValue("Tous");
        roomStatusFilterCombo.setPromptText("Statut");
        roomStatusFilterCombo.setMinWidth(115);
        roomStatusFilterCombo.setPrefWidth(115);
    }

    private void showHotelFilters() {
        roomFilterSeparator.setVisible(true);
        roomFilterSeparator.setManaged(true);
        roomTypeFilterContainer.setVisible(true);
        roomTypeFilterContainer.setManaged(true);
        roomStatusFilterContainer.setVisible(true);
        roomStatusFilterContainer.setManaged(true);

        if (roomTypeFilterLabel != null) roomTypeFilterLabel.setText("Destination");
        if (roomStatusFilterLabel != null) roomStatusFilterLabel.setText("Statut");

        roomTypeFilterCombo.getItems().clear();
        roomTypeFilterCombo.getItems().add("Toutes destinations");
        roomTypeFilterCombo.getItems().addAll(getSortedDestinationLabels());
        roomTypeFilterCombo.setValue("Toutes destinations");
        roomTypeFilterCombo.setPromptText("Destination");
        roomTypeFilterCombo.setMinWidth(190);
        roomTypeFilterCombo.setPrefWidth(190);

        roomStatusFilterCombo.getItems().clear();
        roomStatusFilterCombo.getItems().addAll("Tous", "AVAILABLE", "OCCUPIED", "MAINTENANCE");
        roomStatusFilterCombo.setValue("Tous");
        roomStatusFilterCombo.setPromptText("Statut");
        roomStatusFilterCombo.setMinWidth(130);
        roomStatusFilterCombo.setPrefWidth(130);
    }

    private void hideRoomFilters() {
        roomFilterSeparator.setVisible(false);
        roomFilterSeparator.setManaged(false);
        roomTypeFilterContainer.setVisible(false);
        roomTypeFilterContainer.setManaged(false);
        roomStatusFilterContainer.setVisible(false);
        roomStatusFilterContainer.setManaged(false);
    }

    private void showListView() {
        listViewSection.setVisible(true);
        listViewSection.setManaged(true);
        detailViewSection.setVisible(false);
        detailViewSection.setManaged(false);
        selectedHotel = null;
        selectedRoom = null;
    }

    private void showDetailView() {
        listViewSection.setVisible(false);
        listViewSection.setManaged(false);
        detailViewSection.setVisible(true);
        detailViewSection.setManaged(true);
        
        FadeTransition ft = new FadeTransition(Duration.millis(300), detailViewSection);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }

    @FXML
    public void handleBackToList() {
        if (currentView.equals("hotels")) {
            showHotelsView();
        } else {
            showRoomsView();
        }
    }

    private void updateSidebarButtons(Button active) {
        // Remove active class from all menu buttons
        Button[] buttons = {btnHotels, btnChambres, btnRefresh, btnLogout};
        for (Button btn : buttons) {
            if (btn != null) {
                btn.getStyleClass().remove("active");
            }
        }
        
        // Add active class and animate to the active button
        if (active != null && !active.getStyleClass().contains("active")) {
            active.getStyleClass().add("active");
        }
        
        // Move the sliding pane to the active button
        if (slidingPane != null && active != null) {
            moveBubble(active);
            currentActiveBtn = active;
        }
    }

    private void configureActionButton(Button button, double width) {
        button.setWrapText(false);
        button.setTextOverrun(OverrunStyle.CLIP);
        button.setMinWidth(width);
        button.setPrefWidth(width);
        button.setMaxWidth(width);
    }

    private void configureDetailActionButtonsContainer() {
        if (detailActionButtons == null) {
            return;
        }
        detailActionButtons.setHgap(12);
        detailActionButtons.setVgap(12);
        detailActionButtons.setPrefWrapLength(620);
        detailActionButtons.setAlignment(Pos.CENTER_LEFT);
    }

    private void configureDetailActionButton(Button button, String backgroundColor) {
        button.setStyle("-fx-background-color: " + backgroundColor + "; -fx-text-fill: white; -fx-background-radius: 25; -fx-padding: 12 22; -fx-font-size: 14px; -fx-font-weight: 600; -fx-cursor: hand;");
        button.setWrapText(false);
        button.setTextOverrun(OverrunStyle.CLIP);
        button.setMinWidth(Region.USE_PREF_SIZE);
        button.setPrefWidth(Region.USE_COMPUTED_SIZE);
        button.setMaxWidth(Region.USE_PREF_SIZE);
        button.setMinHeight(44);
    }

    private void applyPlainTextCellFactory(TableColumn<Object, String> column) {
        column.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item);
                    setGraphic(null);
                }
                setAlignment(Pos.CENTER);
                setTextOverrun(OverrunStyle.CLIP);
                setWrapText(false);
            }
        });
    }

    private String renderStars(int count) {
        int safe = Math.max(0, Math.min(5, count));
        return "\u2605".repeat(safe) + "\u2606".repeat(5 - safe);
    }

    private String formatStarsWithScore(int count) {
        int safe = Math.max(1, Math.min(5, count));
        return renderStars(safe) + " (" + safe + "/5)";
    }


    private void setupHotelsTable() {
        col1.setVisible(true);
        col2.setVisible(true);
        col3.setVisible(true);
        col4.setVisible(true);
        col5.setVisible(true);
        col6.setVisible(true);

        col1.setText("Nom");
        col2.setText("Description");
        col3.setText("Etoiles");
        col3.setMinWidth(140);
        col3.setPrefWidth(165);
        col4.setText("Statut");
        col5.setText("Destination");
        col5.setMinWidth(170);
        col5.setPrefWidth(210);
        col6.setText("Actions");
        col6.setMinWidth(360);
        col6.setPrefWidth(390);

        col1.setCellValueFactory(data -> {
            if (data.getValue() instanceof Hotel h) {
                return new SimpleStringProperty(h.getName());
            }
            return new SimpleStringProperty("");
        });
        
        col2.setCellValueFactory(data -> {
            if (data.getValue() instanceof Hotel h) {
                String desc = h.getDescription();
                return new SimpleStringProperty(desc == null ? "" : desc);
            }
            return new SimpleStringProperty("");
        });
        applyPlainTextCellFactory(col2);
        
        col3.setCellValueFactory(data -> {
            if (data.getValue() instanceof Hotel h) {
                return new SimpleStringProperty(formatStarsWithScore(h.getStars()));
            }
            return new SimpleStringProperty("");
        });
        applyPlainTextCellFactory(col3);


        col4.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else if (getTableRow().getItem() instanceof Hotel hotel) {
                    Label badge = new Label(getStatusLabel(hotel.getStatus()));
                    badge.setStyle(getStatusStyle(hotel.getStatus()));
                    setGraphic(badge);
                } else {
                    setGraphic(null);
                }
            }
        });
        
        col4.setCellValueFactory(data -> {
            if (data.getValue() instanceof Hotel h) {
                return new SimpleStringProperty(h.getStatus());
            }
            return new SimpleStringProperty("");
        });

        col5.setCellValueFactory(data -> {
            if (data.getValue() instanceof Hotel h) {
                return new SimpleStringProperty(resolveDestinationLabel(h.getLocationId()));
            }
            return new SimpleStringProperty("");
        });
        applyPlainTextCellFactory(col5);

        col6.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue()));
        col6.setCellFactory(col -> new TableCell<>() {
            private final Button viewBtn = new Button("Details");
            private final Button editBtn = new Button("Modifier");
            private final Button deleteBtn = new Button("Supprimer");
            private final HBox box = new HBox(10, viewBtn, editBtn, deleteBtn);
            {
                // View button - Blue with icon
                viewBtn.setStyle("-fx-background-color: linear-gradient(to bottom, #679AC1, #5a8ab0); -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 8 16; -fx-cursor: hand; -fx-font-size: 12px; -fx-font-weight: bold; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 4, 0, 0, 1);");
                viewBtn.setOnMouseEntered(e -> viewBtn.setStyle("-fx-background-color: linear-gradient(to bottom, #7aadd4, #679AC1); -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 8 16; -fx-cursor: hand; -fx-font-size: 12px; -fx-font-weight: bold; -fx-effect: dropshadow(gaussian, rgba(103,154,193,0.5), 8, 0, 0, 2);"));
                viewBtn.setOnMouseExited(e -> viewBtn.setStyle("-fx-background-color: linear-gradient(to bottom, #679AC1, #5a8ab0); -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 8 16; -fx-cursor: hand; -fx-font-size: 12px; -fx-font-weight: bold; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 4, 0, 0, 1);"));

                // Edit button - Orange
                editBtn.setStyle("-fx-background-color: linear-gradient(to bottom, #FF8210, #e67400); -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 8 16; -fx-cursor: hand; -fx-font-size: 12px; -fx-font-weight: bold; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 4, 0, 0, 1);");
                editBtn.setOnMouseEntered(e -> editBtn.setStyle("-fx-background-color: linear-gradient(to bottom, #ff9933, #FF8210); -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 8 16; -fx-cursor: hand; -fx-font-size: 12px; -fx-font-weight: bold; -fx-effect: dropshadow(gaussian, rgba(255,130,16,0.5), 8, 0, 0, 2);"));
                editBtn.setOnMouseExited(e -> editBtn.setStyle("-fx-background-color: linear-gradient(to bottom, #FF8210, #e67400); -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 8 16; -fx-cursor: hand; -fx-font-size: 12px; -fx-font-weight: bold; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 4, 0, 0, 1);"));

                // Delete button - Red
                deleteBtn.setStyle("-fx-background-color: linear-gradient(to bottom, #dc3545, #c82333); -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 8 16; -fx-cursor: hand; -fx-font-size: 12px; -fx-font-weight: bold; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 4, 0, 0, 1);");
                deleteBtn.setOnMouseEntered(e -> deleteBtn.setStyle("-fx-background-color: linear-gradient(to bottom, #e74c5c, #dc3545); -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 8 16; -fx-cursor: hand; -fx-font-size: 12px; -fx-font-weight: bold; -fx-effect: dropshadow(gaussian, rgba(220,53,69,0.5), 8, 0, 0, 2);"));
                deleteBtn.setOnMouseExited(e -> deleteBtn.setStyle("-fx-background-color: linear-gradient(to bottom, #dc3545, #c82333); -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 8 16; -fx-cursor: hand; -fx-font-size: 12px; -fx-font-weight: bold; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 4, 0, 0, 1);"));

                configureActionButton(viewBtn, 88);
                configureActionButton(editBtn, 96);
                configureActionButton(deleteBtn, 106);

                viewBtn.setTooltip(new Tooltip("Voir les details de cet hotel"));
                editBtn.setTooltip(new Tooltip("Modifier les informations"));
                deleteBtn.setTooltip(new Tooltip("Supprimer cet hotel"));

                box.setAlignment(Pos.CENTER);
                box.setSpacing(8);
            }

            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }

                if (item instanceof Hotel hotel) {
                    viewBtn.setOnAction(e -> showHotelDetail(hotel));
                    editBtn.setOnAction(e -> showEditHotelDialog(hotel));
                    deleteBtn.setOnAction(e -> confirmDelete("hotel", hotel.getName(), () -> handleDeleteHotel(hotel)));
                    setGraphic(box);
                } else {
                    setGraphic(null);
                }
            }
        });

    }

    private void setupRoomsTable() {
        col1.setVisible(true);
        col2.setVisible(true);
        col3.setVisible(true);
        col4.setVisible(true);
        col5.setVisible(true);
        col6.setVisible(true);

        col1.setText("No Chambre");
        col2.setText("Hotel");
        col3.setText("Type");
        col4.setText("Prix");
        col5.setText("Statut");
        col6.setText("Actions");
        col6.setMinWidth(360);
        col6.setPrefWidth(390);

        col1.setCellValueFactory(data -> {
            if (data.getValue() instanceof Room r) {
                return new SimpleStringProperty(r.getRoomNumber());
            }
            return new SimpleStringProperty("");
        });

        col2.setCellValueFactory(data -> {
            if (data.getValue() instanceof Room r) {
                return new SimpleStringProperty(getHotelName(r.getHotelId()));
            }
            return new SimpleStringProperty("");
        });

        col3.setCellValueFactory(data -> {
            if (data.getValue() instanceof Room r) {
                return new SimpleStringProperty(r.getRoomType());
            }
            return new SimpleStringProperty("");
        });
        applyPlainTextCellFactory(col3);

        col4.setCellValueFactory(data -> {
            if (data.getValue() instanceof Room r) {
                return new SimpleStringProperty(formatPrice(r.getPricePerNight()) + " DT");
            }
            return new SimpleStringProperty("");
        });
        applyPlainTextCellFactory(col4);

        col5.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else if (getTableRow().getItem() instanceof Room room) {
                    Label badge = new Label(getStatusLabel(room.getStatus()));
                    badge.setStyle(getStatusStyle(room.getStatus()));
                    setGraphic(badge);
                } else {
                    setGraphic(null);
                }
            }
        });

        col5.setCellValueFactory(data -> {
            if (data.getValue() instanceof Room r) {
                return new SimpleStringProperty(r.getStatus());
            }
            return new SimpleStringProperty("");
        });

        col6.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue()));
        col6.setCellFactory(col -> new TableCell<>() {
            private final Button viewBtn = new Button("Details");
            private final Button editBtn = new Button("Modifier");
            private final Button deleteBtn = new Button("Supprimer");
            private final HBox box = new HBox(10, viewBtn, editBtn, deleteBtn);
            {
                // View button - Blue with hover
                viewBtn.setStyle("-fx-background-color: linear-gradient(to bottom, #679AC1, #5a8ab0); -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 8 16; -fx-cursor: hand; -fx-font-size: 12px; -fx-font-weight: bold; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 4, 0, 0, 1);");
                viewBtn.setOnMouseEntered(e -> viewBtn.setStyle("-fx-background-color: linear-gradient(to bottom, #7aadd4, #679AC1); -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 8 16; -fx-cursor: hand; -fx-font-size: 12px; -fx-font-weight: bold; -fx-effect: dropshadow(gaussian, rgba(103,154,193,0.5), 8, 0, 0, 2);"));
                viewBtn.setOnMouseExited(e -> viewBtn.setStyle("-fx-background-color: linear-gradient(to bottom, #679AC1, #5a8ab0); -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 8 16; -fx-cursor: hand; -fx-font-size: 12px; -fx-font-weight: bold; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 4, 0, 0, 1);"));

                // Edit button - Orange with hover
                editBtn.setStyle("-fx-background-color: linear-gradient(to bottom, #FF8210, #e67400); -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 8 16; -fx-cursor: hand; -fx-font-size: 12px; -fx-font-weight: bold; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 4, 0, 0, 1);");
                editBtn.setOnMouseEntered(e -> editBtn.setStyle("-fx-background-color: linear-gradient(to bottom, #ff9933, #FF8210); -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 8 16; -fx-cursor: hand; -fx-font-size: 12px; -fx-font-weight: bold; -fx-effect: dropshadow(gaussian, rgba(255,130,16,0.5), 8, 0, 0, 2);"));
                editBtn.setOnMouseExited(e -> editBtn.setStyle("-fx-background-color: linear-gradient(to bottom, #FF8210, #e67400); -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 8 16; -fx-cursor: hand; -fx-font-size: 12px; -fx-font-weight: bold; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 4, 0, 0, 1);"));

                // Delete button - Red with hover
                deleteBtn.setStyle("-fx-background-color: linear-gradient(to bottom, #dc3545, #c82333); -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 8 16; -fx-cursor: hand; -fx-font-size: 12px; -fx-font-weight: bold; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 4, 0, 0, 1);");
                deleteBtn.setOnMouseEntered(e -> deleteBtn.setStyle("-fx-background-color: linear-gradient(to bottom, #e74c5c, #dc3545); -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 8 16; -fx-cursor: hand; -fx-font-size: 12px; -fx-font-weight: bold; -fx-effect: dropshadow(gaussian, rgba(220,53,69,0.5), 8, 0, 0, 2);"));
                deleteBtn.setOnMouseExited(e -> deleteBtn.setStyle("-fx-background-color: linear-gradient(to bottom, #dc3545, #c82333); -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 8 16; -fx-cursor: hand; -fx-font-size: 12px; -fx-font-weight: bold; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 4, 0, 0, 1);"));

                configureActionButton(viewBtn, 88);
                configureActionButton(editBtn, 96);
                configureActionButton(deleteBtn, 106);

                viewBtn.setTooltip(new Tooltip("Voir les details de cette chambre"));
                editBtn.setTooltip(new Tooltip("Modifier les informations"));
                deleteBtn.setTooltip(new Tooltip("Supprimer cette chambre"));

                box.setAlignment(Pos.CENTER);
                box.setSpacing(8);
            }

            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }

                if (item instanceof Room room) {
                    viewBtn.setOnAction(e -> showRoomDetail(room));
                    editBtn.setOnAction(e -> showEditRoomDialog(room));
                    deleteBtn.setOnAction(e -> confirmDelete("chambre", room.getRoomNumber(), () -> handleDeleteRoom(room)));
                    setGraphic(box);
                } else {
                    setGraphic(null);
                }
            }
        });

    }

    private String getStatusLabel(String status) {
        return switch (status) {
            case "AVAILABLE" -> "Disponible";
            case "OCCUPIED" -> "Occupe";
            case "MAINTENANCE" -> "Maintenance";
            default -> status;
        };
    }

    private String getStatusStyle(String status) {
        return switch (status) {
            case "AVAILABLE" -> "-fx-background-color: #28a745; -fx-text-fill: white; -fx-padding: 5 12; -fx-background-radius: 15; -fx-font-size: 11px;";
            case "OCCUPIED" -> "-fx-background-color: #FF8210; -fx-text-fill: white; -fx-padding: 5 12; -fx-background-radius: 15; -fx-font-size: 11px;";
            default -> "-fx-background-color: #6C6D6F; -fx-text-fill: white; -fx-padding: 5 12; -fx-background-radius: 15; -fx-font-size: 11px;";
        };
    }

    private String formatPrice(double price) {
        if (price == Math.rint(price)) {
            return String.format(Locale.US, "%.0f", price);
        }
        return String.format(Locale.US, "%.2f", price);
    }

    private void loadHotelsTable() {
        String search = searchField.getText() != null ? searchField.getText().toLowerCase().trim() : "";
        String starsFilter = starsFilterCombo.getValue();
        String destinationFilter = roomTypeFilterCombo != null ? roomTypeFilterCombo.getValue() : null;
        String statusFilter = roomStatusFilterCombo != null ? roomStatusFilterCombo.getValue() : null;
        ObservableList<Object> filtered = FXCollections.observableArrayList();

        for (Hotel h : hotelsList) {
            boolean matchSearch = search.isEmpty() || h.getName().toLowerCase().contains(search)
                    || h.getDescription().toLowerCase().contains(search);
            
            boolean matchStars = starsFilter == null || starsFilter.equals("Toutes") || starsFilter.startsWith(String.valueOf(h.getStars()));
            boolean matchDestination = destinationFilter == null
                    || destinationFilter.equals("Toutes destinations")
                    || destinationFilter.equals(resolveDestinationLabel(h.getLocationId()));
            boolean matchStatus = statusFilter == null || statusFilter.equals("Tous") || h.getStatus().equals(statusFilter);
            
            if (matchSearch && matchStars && matchDestination && matchStatus) {
                filtered.add(h);
            }
        }
        mainTable.setItems(filtered);
        pageSubtitle.setText(filtered.size() + " hotel(s) trouve(s)  -  Double-cliquez pour details");
    }

    private void setupStarsFilter() {
        starsFilterCombo.getItems().clear();
        starsFilterCombo.getItems().addAll("Toutes", "5 etoiles", "4 etoiles", "3 etoiles", "2 etoiles", "1 etoile");
        starsFilterCombo.setValue("Toutes");
    }

    private void loadRoomsTable() {
        String search = searchField.getText() != null ? searchField.getText().toLowerCase().trim() : "";
        String hotelFilter = hotelFilterCombo.getValue();
        String typeFilter = roomTypeFilterCombo != null ? roomTypeFilterCombo.getValue() : null;
        String statusFilter = roomStatusFilterCombo != null ? roomStatusFilterCombo.getValue() : null;
        ObservableList<Object> filtered = FXCollections.observableArrayList();

        for (Room r : roomsList) {
            String hotelName = getHotelName(r.getHotelId());
            
            // Search filter
            boolean matchSearch = search.isEmpty() || r.getRoomNumber().toLowerCase().contains(search)
                    || r.getRoomType().toLowerCase().contains(search) || hotelName.toLowerCase().contains(search);
            
            // Hotel filter
            boolean matchHotel = hotelFilter == null || hotelFilter.equals("Tous hotels") || hotelFilter.equals(hotelName);
            
            // Type filter
            boolean matchType = typeFilter == null || typeFilter.equals("Tous") || r.getRoomType().equals(typeFilter);
            
            // Status filter
            boolean matchStatus = statusFilter == null || statusFilter.equals("Tous") || r.getStatus().equals(statusFilter);

            if (matchSearch && matchHotel && matchType && matchStatus) {
                filtered.add(r);
            }
        }
        mainTable.setItems(filtered);
        pageSubtitle.setText(filtered.size() + " chambre(s) trouvee(s)  -  Double-cliquez pour details");
    }

    private void setupHotelFilter() {
        hotelFilterCombo.getItems().clear();
        hotelFilterCombo.getItems().add("Tous hotels");
        for (Hotel h : hotelsList) {
            hotelFilterCombo.getItems().add(h.getName());
        }
        hotelFilterCombo.setValue("Tous hotels");
    }

    private int getRoomCountForHotel(int hotelId) {
        return (int) roomsList.stream().filter(r -> r.getHotelId() == hotelId).count();
    }

    private String getHotelName(int hotelId) {
        return hotelsList.stream().filter(h -> h.getId() == hotelId).map(Hotel::getName).findFirst().orElse("Hotel #" + hotelId);
    }

    private void refreshDestinationLookup() {
        destinationDisplayCache.clear();
        destinationIdByDisplay.clear();

        try {
            Connection connection = MyDBConnexion.getInstance().getConnection();
            if (connection != null) {
                String sql = "SELECT id, name_destination, pays, ville FROM destination ORDER BY name_destination, ville, id";
                try (PreparedStatement ps = connection.prepareStatement(sql);
                     ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int id = rs.getInt("id");
                        String label = buildDestinationLabel(
                                rs.getString("name_destination"),
                                rs.getString("ville"),
                                rs.getString("pays"),
                                id
                        );
                        addDestinationOption(id, label);
                    }
                }
            }
        } catch (SQLException ignored) {
            // keep fallback values only
        }

        for (Hotel hotel : hotelsList) {
            addDestinationOption(hotel.getLocationId(), "Destination #" + hotel.getLocationId());
        }
    }

    private String buildDestinationLabel(String name, String city, String country, int id) {
        String base = firstNonBlank(name, city);
        if (base == null) {
            base = "Destination #" + id;
        }

        StringBuilder label = new StringBuilder(base);
        if (city != null && !city.trim().isEmpty() && !city.trim().equalsIgnoreCase(base.trim())) {
            label.append(" - ").append(city.trim());
        }
        if (country != null && !country.trim().isEmpty()
                && !country.trim().equalsIgnoreCase(base.trim())
                && (city == null || !country.trim().equalsIgnoreCase(city.trim()))) {
            label.append(" (").append(country.trim()).append(")");
        }
        return label.toString();
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.trim().isEmpty()) return first.trim();
        if (second != null && !second.trim().isEmpty()) return second.trim();
        return null;
    }

    private void addDestinationOption(int destinationId, String destinationLabel) {
        if (destinationId <= 0 || destinationDisplayCache.containsKey(destinationId)) {
            return;
        }

        String label = (destinationLabel == null || destinationLabel.trim().isEmpty())
                ? "Destination #" + destinationId
                : destinationLabel.trim();

        Integer existing = destinationIdByDisplay.get(label);
        if (existing != null && existing != destinationId) {
            label = label + " (#" + destinationId + ")";
        }

        destinationDisplayCache.put(destinationId, label);
        destinationIdByDisplay.put(label, destinationId);
    }

    private List<String> getSortedDestinationLabels() {
        return destinationDisplayCache.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(String.CASE_INSENSITIVE_ORDER))
                .map(Map.Entry::getValue)
                .toList();
    }

    private int getDefaultDestinationId() {
        return destinationDisplayCache.keySet().stream().min(Comparator.naturalOrder()).orElse(1);
    }

    private String resolveDestinationLabel(int locationId) {
        if (locationId <= 0) return "Destination inconnue";
        String label = destinationDisplayCache.get(locationId);
        if (label != null) return label;
        label = "Destination #" + locationId;
        addDestinationOption(locationId, label);
        return destinationDisplayCache.getOrDefault(locationId, label);
    }

    private int resolveDestinationId(String destinationLabel, int fallbackId) {
        if (destinationLabel == null || destinationLabel.trim().isEmpty()) {
            return fallbackId;
        }

        Integer mapped = destinationIdByDisplay.get(destinationLabel);
        if (mapped != null) {
            return mapped;
        }

        int start = destinationLabel.lastIndexOf("(#");
        int end = destinationLabel.lastIndexOf(')');
        if (start >= 0 && end > start + 2) {
            try {
                return Integer.parseInt(destinationLabel.substring(start + 2, end));
            } catch (NumberFormatException ignored) {
                // keep fallback
            }
        }

        return fallbackId;
    }

    private void showHotelDetail(Hotel hotel) {
        selectedHotel = hotel;
        selectedRoom = null;
        showDetailView();

        detailName.setText(hotel.getName());
        detailSubInfo.setText(formatStarsWithScore(hotel.getStars()));
        detailSubInfo.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: #FFBD59;");
        detailDesc.setText(hotel.getDescription());

        detailBadges.getChildren().clear();

        Label statusBadge = new Label(getStatusLabel(hotel.getStatus()));
        statusBadge.setStyle(getStatusStyle(hotel.getStatus()) + " -fx-font-size: 13px;");

        Label locationBadge = new Label(resolveDestinationLabel(hotel.getLocationId()));
        locationBadge.setStyle("-fx-background-color: rgba(103,154,193,0.3); -fx-text-fill: #679AC1; -fx-padding: 6 14; -fx-background-radius: 15; -fx-font-size: 12px;");

        int roomCount = getRoomCountForHotel(hotel.getId());
        Label roomsBadge = new Label(roomCount + " chambre(s)");
        roomsBadge.setStyle("-fx-background-color: rgba(255,130,16,0.3); -fx-text-fill: #FF8210; -fx-padding: 6 14; -fx-background-radius: 15; -fx-font-size: 12px;");

        int servicesCount = getServiceCountForHotel(hotel.getId());
        Label servicesBadge = new Label(servicesCount + " service(s)");
        servicesBadge.setStyle("-fx-background-color: rgba(40,167,69,0.25); -fx-text-fill: #7ce19b; -fx-padding: 6 14; -fx-background-radius: 15; -fx-font-size: 12px;");

        detailBadges.getChildren().addAll(statusBadge, locationBadge, roomsBadge, servicesBadge);
        if (detailServicesSection != null) {
            detailServicesSection.setVisible(true);
            detailServicesSection.setManaged(true);
        }
        populateDetailServices(hotel.getId());

        detailActionButtons.getChildren().clear();
        configureDetailActionButtonsContainer();

        if (detailManageServicesBtn != null) {
            detailManageServicesBtn.setText("Gerer services");
            detailManageServicesBtn.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-background-radius: 20; -fx-padding: 8 16; -fx-font-size: 12px; -fx-font-weight: 700; -fx-cursor: hand;");
            detailManageServicesBtn.setOnAction(e -> showHotelServicesDialog(hotel));
        }

        Button editBtn = new Button("Modifier l'hotel");
        configureDetailActionButton(editBtn, "#FF8210");
        editBtn.setOnAction(e -> showEditHotelDialog(hotel));

        Button roomsBtn = new Button("Voir les chambres");
        configureDetailActionButton(roomsBtn, "#679AC1");
        roomsBtn.setOnAction(e -> {
            showRoomsView();
            String matchingHotelFilter = hotelFilterCombo.getItems().stream()
                    .filter(item -> item != null && item.equals(hotel.getName()))
                    .findFirst()
                    .orElse("Tous hotels");
            hotelFilterCombo.setValue(matchingHotelFilter);
            loadRoomsTable();
        });

        Button deleteBtn = new Button("Supprimer");
        configureDetailActionButton(deleteBtn, "#dc3545");
        deleteBtn.setOnAction(e -> confirmDelete("hotel", hotel.getName(), () -> {
            handleDeleteHotel(hotel);
            showHotelsView();
        }));

        detailActionButtons.getChildren().addAll(editBtn, roomsBtn, deleteBtn);

        reviewsSection.setVisible(true);
        reviewsSection.setManaged(true);
        
        // Show hotel images section, hide room images section
        imagesSection.setVisible(true);
        imagesSection.setManaged(true);
        roomImagesSection.setVisible(false);
        roomImagesSection.setManaged(false);

        loadHotelImages(hotel.getId());
        loadHotelReviews(hotel.getId());
    }

    private int getServiceCountForHotel(int hotelId) {
        if (hotelServiceItemService == null) return 0;
        try {
            return hotelServiceItemService.getActiveByHotelId(hotelId).size();
        } catch (SQLException e) {
            return 0;
        }
    }

    private void populateDetailServices(int hotelId) {
        if (detailServicesSection == null || detailServicesTitle == null || detailServicesPane == null) {
            return;
        }

        detailServicesSection.setVisible(true);
        detailServicesSection.setManaged(true);
        detailServicesTitle.setVisible(true);
        detailServicesTitle.setManaged(true);
        detailServicesPane.setVisible(true);
        detailServicesPane.setManaged(true);
        detailServicesPane.getChildren().clear();

        try {
            List<HotelServiceItem> services = hotelServiceItemService.getActiveByHotelId(hotelId);
            for (HotelServiceItem item : services) {
                String name = item.getName() == null ? "" : item.getName().trim();
                if (name.isEmpty()) {
                    continue;
                }

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

    private void showHotelServicesDialog(Hotel hotel) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Services - " + hotel.getName());

        ButtonType closeBtn = new ButtonType("Fermer", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().add(closeBtn);

        VBox content = new VBox(14);
        content.setPadding(new Insets(12));
        content.getStyleClass().add("gv-services-dialog-content");

        Label subtitle = new Label("Gerez les services visibles cote client pour cet hotel.");
        subtitle.getStyleClass().add("gv-services-dialog-subtitle");
        subtitle.setWrapText(true);

        Label countLabel = new Label();
        countLabel.getStyleClass().add("gv-services-count");

        Region subtitleSpacer = new Region();
        HBox.setHgrow(subtitleSpacer, Priority.ALWAYS);
        HBox header = new HBox(10, subtitle, subtitleSpacer, countLabel);
        header.setAlignment(Pos.CENTER_LEFT);

        ListView<HotelServiceItem> servicesList = new ListView<>();
        servicesList.setPrefHeight(280);
        servicesList.getStyleClass().add("gv-service-list");
        Label emptyServicesLabel = new Label("Aucun service configure pour cet hotel.");
        emptyServicesLabel.getStyleClass().add("gv-service-empty");
        servicesList.setPlaceholder(emptyServicesLabel);
        servicesList.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        servicesList.setFocusTraversable(true);
        servicesList.setOnMouseClicked(ev -> {
            if (servicesList.getSelectionModel().getSelectedItem() == null) {
                int focused = servicesList.getFocusModel().getFocusedIndex();
                if (focused >= 0 && focused < servicesList.getItems().size()) {
                    servicesList.getSelectionModel().select(focused);
                }
            }
        });
        servicesList.setCellFactory(list -> new ListCell<>() {
            private final Label nameLabel = new Label();
            private final Label stateBadge = new Label();
            private final Region spacer = new Region();
            private final HBox row = new HBox(10);

            {
                nameLabel.getStyleClass().add("gv-service-name");
                stateBadge.getStyleClass().add("gv-service-status");
                HBox.setHgrow(spacer, Priority.ALWAYS);
                row.getChildren().addAll(nameLabel, spacer, stateBadge);
                row.setAlignment(Pos.CENTER_LEFT);
                row.getStyleClass().add("gv-service-row");
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            }

            @Override
            protected void updateItem(HotelServiceItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    nameLabel.setText(item.getName());
                    stateBadge.setText(item.isActive() ? "Actif" : "Inactif");
                    stateBadge.getStyleClass().removeAll("gv-service-status-active", "gv-service-status-inactive");
                    stateBadge.getStyleClass().add(item.isActive() ? "gv-service-status-active" : "gv-service-status-inactive");
                    if (getListView() != null) {
                        row.setPrefWidth(Math.max(0, getListView().getWidth() - 34));
                    }
                    setText(null);
                    setGraphic(row);
                }
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
                    int indexToSelect = -1;
                    if (previousId > 0) {
                        for (int i = 0; i < items.size(); i++) {
                            if (items.get(i).getId() == previousId) {
                                indexToSelect = i;
                                break;
                            }
                        }
                    }
                    if (indexToSelect < 0) {
                        indexToSelect = 0;
                    }
                    servicesList.getSelectionModel().select(indexToSelect);
                    servicesList.getFocusModel().focus(indexToSelect);
                    servicesList.scrollTo(indexToSelect);
                }
            } catch (SQLException e) {
                showNotification("Erreur services: " + e.getMessage(), "error");
            }
        };
        reloadServices.run();

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.getStyleClass().add("gv-services-actions");

        Button addBtn = new Button("+ Ajouter");
        addBtn.getStyleClass().addAll("gv-service-action-btn", "gv-service-action-add");
        addBtn.setOnAction(e -> {
            HotelServiceItem created = showHotelServiceFormDialog(hotel, null);
            if (created == null) return;
            try {
                hotelServiceItemService.create(created);
                reloadServices.run();
                showNotification("Service ajoute!", "success");
            } catch (SQLException ex) {
                showNotification("Erreur: " + ex.getMessage(), "error");
            }
        });

        Button editBtn = new Button("Modifier");
        editBtn.getStyleClass().addAll("gv-service-action-btn", "gv-service-action-edit");
        editBtn.setOnAction(e -> {
            HotelServiceItem selected = resolveSelectedService(servicesList);
            if (selected == null) {
                showNotification("Selectionnez un service a modifier", "warning");
                return;
            }

            HotelServiceItem updated = showHotelServiceFormDialog(hotel, selected);
            if (updated == null) return;
            try {
                hotelServiceItemService.update(updated);
                reloadServices.run();
                showNotification("Service modifie!", "success");
            } catch (SQLException ex) {
                showNotification("Erreur: " + ex.getMessage(), "error");
            }
        });

        Button toggleBtn = new Button("Activer / Desactiver");
        toggleBtn.getStyleClass().addAll("gv-service-action-btn", "gv-service-action-toggle");
        toggleBtn.setOnAction(e -> {
            HotelServiceItem selected = resolveSelectedService(servicesList);
            if (selected == null) {
                showNotification("Selectionnez un service", "warning");
                return;
            }
            try {
                selected.setActive(!selected.isActive());
                hotelServiceItemService.update(selected);
                reloadServices.run();
                showNotification("Statut du service mis a jour!", "success");
            } catch (SQLException ex) {
                showNotification("Erreur: " + ex.getMessage(), "error");
            }
        });

        Button deleteBtn = new Button("Supprimer");
        deleteBtn.getStyleClass().addAll("gv-service-action-btn", "gv-service-action-delete");
        deleteBtn.setOnAction(e -> {
            HotelServiceItem selected = resolveSelectedService(servicesList);
            if (selected == null) {
                showNotification("Selectionnez un service a supprimer", "warning");
                return;
            }
            confirmDelete("service", selected.getName(), () -> {
                try {
                    hotelServiceItemService.delete(selected.getId());
                    reloadServices.run();
                    showNotification("Service supprime!", "success");
                } catch (SQLException ex) {
                    showNotification("Erreur: " + ex.getMessage(), "error");
                }
            });
        });

        actions.getChildren().addAll(addBtn, editBtn, toggleBtn, deleteBtn);

        content.getChildren().addAll(header, servicesList, actions);
        dialog.getDialogPane().setContent(content);
        applyDialogPaneSizing(dialog.getDialogPane(), 760, 460);
        styleDialog(dialog, false);
        dialog.showAndWait();

        loadAllData();
        if (selectedHotel != null && selectedHotel.getId() == hotel.getId()) {
            showHotelDetail(hotel);
        }
    }

    private HotelServiceItem showHotelServiceFormDialog(Hotel hotel, HotelServiceItem existing) {
        Dialog<HotelServiceItem> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Nouveau service" : "Modifier service");

        ButtonType saveBtn = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        GridPane grid = createDialogFormGrid();

        TextField nameField = new TextField(existing == null ? "" : existing.getName());
        nameField.setPromptText("Nom du service");
        applyDialogFieldSizing(nameField);

        CheckBox activeCheck = new CheckBox("Service actif");
        activeCheck.setSelected(existing == null || existing.isActive());
        activeCheck.setStyle("-fx-text-fill: #f8fbff; -fx-font-size: 14px; -fx-font-weight: bold;");

        grid.add(new Label("Hotel"), 0, 0); grid.add(new Label(hotel.getName()), 1, 0);
        grid.add(new Label("Nom *"), 0, 1); grid.add(nameField, 1, 1);
        grid.add(new Label("Etat"), 0, 2); grid.add(activeCheck, 1, 2);

        dialog.getDialogPane().setContent(grid);
        applyDialogPaneSizing(dialog.getDialogPane(), 700, 320);
        styleDialog(dialog, false);

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
        if (servicesList == null) {
            return null;
        }
        HotelServiceItem selected = servicesList.getSelectionModel().getSelectedItem();
        if (selected != null) {
            return selected;
        }
        int focusedIndex = servicesList.getFocusModel().getFocusedIndex();
        if (focusedIndex >= 0 && focusedIndex < servicesList.getItems().size()) {
            return servicesList.getItems().get(focusedIndex);
        }
        return null;
    }

    private void showRoomDetail(Room room) {
        selectedRoom = room;
        selectedHotel = null;
        showDetailView();

        String hotelName = getHotelName(room.getHotelId());

        detailName.setText("Chambre " + room.getRoomNumber());
        detailSubInfo.setText(hotelName);
        detailSubInfo.setStyle("-fx-font-size: 14px; -fx-text-fill: #679AC1;");
        detailDesc.setText("Type: " + room.getRoomType() + "\nCapacite: " + room.getCapacity() + " personnes\nPrix par nuit: " + room.getPricePerNight() + " DT");

        detailBadges.getChildren().clear();

        Label typeBadge = new Label(room.getRoomType());
        typeBadge.setStyle("-fx-background-color: #679AC1; -fx-text-fill: white; -fx-padding: 6 14; -fx-background-radius: 15; -fx-font-size: 12px;");

        Label priceBadge = new Label(room.getPricePerNight() + " DT/nuit");
        priceBadge.setStyle("-fx-background-color: #FF8210; -fx-text-fill: white; -fx-padding: 6 14; -fx-background-radius: 15; -fx-font-size: 12px;");

        Label statusBadge = new Label(getStatusLabel(room.getStatus()));
        statusBadge.setStyle(getStatusStyle(room.getStatus()) + " -fx-font-size: 13px;");

        Label capacityBadge = new Label(room.getCapacity() + " personnes");
        capacityBadge.setStyle("-fx-background-color: rgba(255,255,255,0.15); -fx-text-fill: white; -fx-padding: 6 14; -fx-background-radius: 15; -fx-font-size: 12px;");

        detailBadges.getChildren().addAll(typeBadge, priceBadge, statusBadge, capacityBadge);

        if (detailServicesSection != null) {
            detailServicesSection.setVisible(false);
            detailServicesSection.setManaged(false);
        }
        if (detailServicesPane != null) {
            detailServicesPane.getChildren().clear();
        }

        detailActionButtons.getChildren().clear();
        configureDetailActionButtonsContainer();

        Button editBtn = new Button("Modifier la chambre");
        configureDetailActionButton(editBtn, "#FF8210");
        editBtn.setOnAction(e -> showEditRoomDialog(room));

        Button statusBtn = new Button("Changer le statut");
        configureDetailActionButton(statusBtn, "#679AC1");
        statusBtn.setOnAction(e -> showChangeStatusDialog(room));

        Button deleteBtn = new Button("Supprimer");
        configureDetailActionButton(deleteBtn, "#dc3545");
        deleteBtn.setOnAction(e -> confirmDelete("chambre", room.getRoomNumber(), () -> {
            handleDeleteRoom(room);
            showRoomsView();
        }));

        detailActionButtons.getChildren().addAll(editBtn, statusBtn, deleteBtn);

        reviewsSection.setVisible(false);
        reviewsSection.setManaged(false);
        
        // Hide hotel images section, show room images section
        imagesSection.setVisible(false);
        imagesSection.setManaged(false);
        roomImagesSection.setVisible(true);
        roomImagesSection.setManaged(true);

        loadRoomImages(room.getId());
    }

    private void showChangeStatusDialog(Room room) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Changer le statut");

        ButtonType saveBtn = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        GridPane grid = createDialogFormGrid();

        Label roomValue = new Label("Chambre " + room.getRoomNumber());
        roomValue.setStyle("-fx-text-fill: #f8fbff; -fx-font-size: 16px; -fx-font-weight: bold;");

        ComboBox<String> statusCombo = new ComboBox<>(FXCollections.observableArrayList("AVAILABLE", "OCCUPIED", "MAINTENANCE"));
        statusCombo.setValue(room.getStatus());
        applyDialogFieldSizing(statusCombo);

        grid.add(new Label("Chambre"), 0, 0); grid.add(roomValue, 1, 0);
        grid.add(new Label("Nouveau statut"), 0, 1); grid.add(statusCombo, 1, 1);

        dialog.getDialogPane().setContent(grid);
        applyDialogPaneSizing(dialog.getDialogPane(), 680, 300);
        styleDialog(dialog, false);
        dialog.setResultConverter(btn -> btn == saveBtn ? statusCombo.getValue() : null);

        dialog.showAndWait().ifPresent(newStatus -> {
            try {
                room.setStatus(newStatus);
                roomService.update(room);
                loadAllData();
                showRoomDetail(room);
                showNotification("Statut mis a jour!", "success");
            } catch (SQLException e) {
                showNotification("Erreur: " + e.getMessage(), "error");
            }
        });
    }

    private void loadHotelImages(int hotelId) {
        mainImageContainer.getChildren().clear();
        thumbnailsContainer.getChildren().clear();

        try {
            List<HotelImage> images = hotelImageService.getByHotelId(hotelId);
            imagesTable.setItems(FXCollections.observableArrayList(images));

            if (!images.isEmpty()) {
                Image firstImg = loadImage(images.get(0).getImageUrl());
                if (firstImg != null) {
                    ImageView mainView = new ImageView();
                    applyImageCoverFit(mainView, firstImg, 520, 330);
                    mainImageContainer.getChildren().add(mainView);

                    final int[] currentIndex = {0};
                    if (images.size() > 1) {
                        Button prevBtn = new Button("<");
                        prevBtn.setStyle("-fx-background-color: rgba(0,0,0,0.55); -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold; -fx-background-radius: 20; -fx-min-width: 36; -fx-min-height: 36; -fx-cursor: hand;");
                        Button nextBtn = new Button(">");
                        nextBtn.setStyle("-fx-background-color: rgba(0,0,0,0.55); -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold; -fx-background-radius: 20; -fx-min-width: 36; -fx-min-height: 36; -fx-cursor: hand;");

                        prevBtn.setOnAction(ev -> {
                            currentIndex[0] = (currentIndex[0] - 1 + images.size()) % images.size();
                            Image newImg = loadImage(images.get(currentIndex[0]).getImageUrl());
                            if (newImg != null) applyImageCoverFit(mainView, newImg, 520, 330);
                        });
                        nextBtn.setOnAction(ev -> {
                            currentIndex[0] = (currentIndex[0] + 1) % images.size();
                            Image newImg = loadImage(images.get(currentIndex[0]).getImageUrl());
                            if (newImg != null) applyImageCoverFit(mainView, newImg, 520, 330);
                        });

                        StackPane.setAlignment(prevBtn, Pos.CENTER_LEFT);
                        StackPane.setAlignment(nextBtn, Pos.CENTER_RIGHT);
                        StackPane.setMargin(prevBtn, new Insets(0, 10, 0, 10));
                        StackPane.setMargin(nextBtn, new Insets(0, 10, 0, 10));
                        mainImageContainer.getChildren().addAll(prevBtn, nextBtn);
                    }

                    for (int i = 0; i < images.size(); i++) {
                        final int idx = i;
                        Image thumbImg = loadImage(images.get(i).getImageUrl());
                        if (thumbImg != null) {
                            ImageView thumb = new ImageView(thumbImg);
                            thumb.setFitWidth(104);
                            thumb.setFitHeight(70);
                            thumb.setPreserveRatio(false);
                            StackPane thumbBox = new StackPane(thumb);
                            thumbBox.setStyle("-fx-background-color: rgba(0,0,0,0.3); -fx-background-radius: 10; -fx-padding: 4; -fx-cursor: hand;");
                            thumbBox.setOnMouseEntered(ev -> thumbBox.setStyle("-fx-background-color: #FF8210; -fx-background-radius: 10; -fx-padding: 4; -fx-cursor: hand;"));
                            thumbBox.setOnMouseExited(ev -> thumbBox.setStyle("-fx-background-color: rgba(0,0,0,0.3); -fx-background-radius: 10; -fx-padding: 4; -fx-cursor: hand;"));
                            thumbBox.setOnMouseClicked(ev -> {
                                currentIndex[0] = idx;
                                Image newImg = loadImage(images.get(idx).getImageUrl());
                                if (newImg != null) applyImageCoverFit(mainView, newImg, 520, 330);
                            });
                            thumbnailsContainer.getChildren().add(thumbBox);
                        }
                    }
                } else {
                    showImagePlaceholder("H");
                }
            } else {
                showImagePlaceholder("H");
            }
        } catch (SQLException e) {
            showImagePlaceholder("!");
        }
    }

    private void loadRoomImages(int roomId) {
        mainImageContainer.getChildren().clear();
        thumbnailsContainer.getChildren().clear();

        try {
            List<RoomImage> images = roomImageService.getByRoomId(roomId);
            roomImagesTable.setItems(FXCollections.observableArrayList(images));

            if (!images.isEmpty()) {
                Image firstImg = loadImage(images.get(0).getImageUrl());
                if (firstImg != null) {
                    ImageView mainView = new ImageView();
                    applyImageCoverFit(mainView, firstImg, 520, 330);
                    mainImageContainer.getChildren().add(mainView);

                    final int[] currentIndex = {0};
                    if (images.size() > 1) {
                        Button prevBtn = new Button("<");
                        prevBtn.setStyle("-fx-background-color: rgba(0,0,0,0.55); -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold; -fx-background-radius: 20; -fx-min-width: 36; -fx-min-height: 36; -fx-cursor: hand;");
                        Button nextBtn = new Button(">");
                        nextBtn.setStyle("-fx-background-color: rgba(0,0,0,0.55); -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold; -fx-background-radius: 20; -fx-min-width: 36; -fx-min-height: 36; -fx-cursor: hand;");

                        prevBtn.setOnAction(ev -> {
                            currentIndex[0] = (currentIndex[0] - 1 + images.size()) % images.size();
                            Image newImg = loadImage(images.get(currentIndex[0]).getImageUrl());
                            if (newImg != null) applyImageCoverFit(mainView, newImg, 520, 330);
                        });
                        nextBtn.setOnAction(ev -> {
                            currentIndex[0] = (currentIndex[0] + 1) % images.size();
                            Image newImg = loadImage(images.get(currentIndex[0]).getImageUrl());
                            if (newImg != null) applyImageCoverFit(mainView, newImg, 520, 330);
                        });

                        StackPane.setAlignment(prevBtn, Pos.CENTER_LEFT);
                        StackPane.setAlignment(nextBtn, Pos.CENTER_RIGHT);
                        StackPane.setMargin(prevBtn, new Insets(0, 10, 0, 10));
                        StackPane.setMargin(nextBtn, new Insets(0, 10, 0, 10));
                        mainImageContainer.getChildren().addAll(prevBtn, nextBtn);
                    }

                    for (int i = 0; i < images.size(); i++) {
                        final int idx = i;
                        Image thumbImg = loadImage(images.get(i).getImageUrl());
                        if (thumbImg != null) {
                            ImageView thumb = new ImageView(thumbImg);
                            thumb.setFitWidth(104);
                            thumb.setFitHeight(70);
                            thumb.setPreserveRatio(false);
                            StackPane thumbBox = new StackPane(thumb);
                            thumbBox.setStyle("-fx-background-color: rgba(0,0,0,0.3); -fx-background-radius: 10; -fx-padding: 4; -fx-cursor: hand;");
                            thumbBox.setOnMouseEntered(ev -> thumbBox.setStyle("-fx-background-color: #679AC1; -fx-background-radius: 10; -fx-padding: 4; -fx-cursor: hand;"));
                            thumbBox.setOnMouseExited(ev -> thumbBox.setStyle("-fx-background-color: rgba(0,0,0,0.3); -fx-background-radius: 10; -fx-padding: 4; -fx-cursor: hand;"));
                            thumbBox.setOnMouseClicked(ev -> {
                                currentIndex[0] = idx;
                                Image newImg = loadImage(images.get(idx).getImageUrl());
                                if (newImg != null) applyImageCoverFit(mainView, newImg, 520, 330);
                            });
                            thumbnailsContainer.getChildren().add(thumbBox);
                        }
                    }
                } else {
                    showImagePlaceholder("R");
                }
            } else {
                showImagePlaceholder("R");
            }
        } catch (SQLException e) {
            showImagePlaceholder("!");
        }
    }

    private void showImagePlaceholder(String icon) {
        VBox placeholder = new VBox(15);
        placeholder.setAlignment(Pos.CENTER);
        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 60px; -fx-text-fill: rgba(255,255,255,0.3);");
        Label textLabel = new Label("Aucune image disponible");
        textLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.5); -fx-font-size: 14px;");
        placeholder.getChildren().addAll(iconLabel, textLabel);
        mainImageContainer.getChildren().add(placeholder);
    }

    private void applyImageCoverFit(ImageView view, Image image, double targetWidth, double targetHeight) {
        if (view == null || image == null) {
            return;
        }
        view.setFitWidth(targetWidth);
        view.setFitHeight(targetHeight);
        view.setPreserveRatio(false);
        view.setSmooth(true);
        view.setImage(image);
    }

    private void applyImageBestFit(ImageView view, Image image, double maxWidth, double maxHeight) {
        if (view == null || image == null) {
            return;
        }
        double imageWidth = image.getWidth();
        double imageHeight = image.getHeight();
        if (imageWidth > 0 && imageHeight > 0) {
            double scale = Math.min(Math.min(maxWidth / imageWidth, maxHeight / imageHeight), 1.0);
            view.setFitWidth(Math.max(1, imageWidth * scale));
            view.setFitHeight(Math.max(1, imageHeight * scale));
        } else {
            view.setFitWidth(maxWidth);
            view.setFitHeight(maxHeight);
        }
        view.setPreserveRatio(true);
        view.setSmooth(true);
        view.setImage(image);
    }

    private void loadHotelReviews(int hotelId) {
        try {
            reviewsTable.setItems(FXCollections.observableArrayList(reviewService.getReviewsByHotel(hotelId)));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void setupReviewsTable() {
        colReviewRating.setCellValueFactory(data -> new SimpleStringProperty(renderStars(data.getValue().getRating()) + " (" + data.getValue().getRating() + "/5)"));
        colReviewComment.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getComment()));
        colReviewUser.setCellValueFactory(data -> new SimpleStringProperty(resolveUserLabel(data.getValue().getUserId())));

        colReviewActions.setCellFactory(col -> new TableCell<>() {
            private final Button hideBtn = new Button("Masquer");
            private final Button deleteBtn = new Button("Supprimer");
            private final HBox box = new HBox(8, hideBtn, deleteBtn);
            {
                hideBtn.setStyle("-fx-background-color: linear-gradient(to bottom, #679AC1, #5a8ab0); -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 6 12; -fx-cursor: hand; -fx-font-size: 11px; -fx-font-weight: bold;");
                hideBtn.setOnMouseEntered(e -> hideBtn.setStyle("-fx-background-color: linear-gradient(to bottom, #7aadd4, #679AC1); -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 6 12; -fx-cursor: hand; -fx-font-size: 11px; -fx-font-weight: bold; -fx-effect: dropshadow(gaussian, rgba(103,154,193,0.4), 6, 0, 0, 1);"));
                hideBtn.setOnMouseExited(e -> hideBtn.setStyle("-fx-background-color: linear-gradient(to bottom, #679AC1, #5a8ab0); -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 6 12; -fx-cursor: hand; -fx-font-size: 11px; -fx-font-weight: bold;"));

                deleteBtn.setStyle("-fx-background-color: linear-gradient(to bottom, #dc3545, #c82333); -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 6 12; -fx-cursor: hand; -fx-font-size: 11px; -fx-font-weight: bold;");
                deleteBtn.setOnMouseEntered(e -> deleteBtn.setStyle("-fx-background-color: linear-gradient(to bottom, #e74c5c, #dc3545); -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 6 12; -fx-cursor: hand; -fx-font-size: 11px; -fx-font-weight: bold; -fx-effect: dropshadow(gaussian, rgba(220,53,69,0.4), 6, 0, 0, 1);"));
                deleteBtn.setOnMouseExited(e -> deleteBtn.setStyle("-fx-background-color: linear-gradient(to bottom, #dc3545, #c82333); -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 6 12; -fx-cursor: hand; -fx-font-size: 11px; -fx-font-weight: bold;"));

                hideBtn.setTooltip(new Tooltip("Masquer cet avis"));
                deleteBtn.setTooltip(new Tooltip("Supprimer cet avis"));

                box.setAlignment(Pos.CENTER);
                box.setSpacing(8);
                hideBtn.setOnAction(e -> moderateReview(getTableView().getItems().get(getIndex())));
                deleteBtn.setOnAction(e -> confirmDelete("avis", "", () -> handleDeleteReview(getTableView().getItems().get(getIndex()))));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                    return;
                }
                HotelReview review = getTableView().getItems().get(getIndex());
                boolean alreadyModerated = review.getComment() != null && review.getComment().startsWith(MODERATION_PREFIX);
                hideBtn.setDisable(alreadyModerated);
                setGraphic(box);
            }
        });
    }

    private void moderateReview(HotelReview review) {
        if (review == null) return;

        TextInputDialog dialog = new TextInputDialog("Contenu inapproprie");
        dialog.setTitle("Masquer l'avis");
        dialog.setHeaderText("Masquer cet avis client ?");
        dialog.setContentText("Motif (optionnel):");
        styleDialog(dialog, false);

        dialog.showAndWait().ifPresent(reason -> {
            try {
                String trimmedReason = reason == null ? "" : reason.trim();
                String moderatedComment = trimmedReason.isEmpty()
                        ? MODERATION_PREFIX
                        : MODERATION_PREFIX + " Motif: " + trimmedReason;

                HotelReview moderated = new HotelReview(review.getId(), review.getRating(), moderatedComment, review.getUserId(), review.getHotelId());
                moderated.setCreatedAt(review.getCreatedAt());

                reviewService.update(moderated);
                if (selectedHotel != null) {
                    loadHotelReviews(selectedHotel.getId());
                }
                showNotification("Avis masque avec succes!", "success");
            } catch (SQLException e) {
                showNotification("Erreur: " + e.getMessage(), "error");
            }
        });
    }

    private String resolveUserLabel(int userId) {
        if (userId <= 0) return "Client inconnu";
        return userDisplayCache.computeIfAbsent(userId, this::loadUserLabelFromDatabase);
    }

    private String loadUserLabelFromDatabase(int userId) {
        try {
            Connection connection = MyDBConnexion.getInstance().getConnection();
            if (connection == null) {
                return "Client inconnu";
            }

            if (!userLookupInitialized) {
                userLookupConfig = discoverUserLookupConfig(connection);
                userLookupInitialized = true;
            }

            if (userLookupConfig == null) {
                return "Client inconnu";
            }

            String sql = "SELECT `" + userLookupConfig.labelColumn + "` FROM `" + userLookupConfig.table + "` WHERE `" + userLookupConfig.idColumn + "` = ? LIMIT 1";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String value = rs.getString(1);
                        if (value != null && !value.trim().isEmpty()) {
                            return value.trim();
                        }
                    }
                }
            }
        } catch (SQLException ignored) {
            // keep fallback
        }
        return "Client inconnu";
    }

    private UserLookupConfig discoverUserLookupConfig(Connection connection) {
        List<String> tableCandidates = List.of("user", "users", "client", "clients", "utilisateur", "utilisateurs");
        List<String> idCandidates = List.of("id", "userId", "user_id", "clientId", "client_id");
        List<String> labelCandidates = List.of("name", "fullName", "full_name", "username", "displayName", "display_name", "nom", "email");

        for (String table : tableCandidates) {
            Set<String> columns = getTableColumns(connection, table);
            if (columns.isEmpty()) continue;

            String idColumn = findFirstExistingColumn(columns, idCandidates);
            String labelColumn = findFirstExistingColumn(columns, labelCandidates);

            if (idColumn != null && labelColumn != null) {
                return new UserLookupConfig(table, idColumn, labelColumn);
            }
        }
        return null;
    }

    private Set<String> getTableColumns(Connection connection, String tableName) {
        Set<String> columns = new HashSet<>();
        try {
            DatabaseMetaData metaData = connection.getMetaData();
            try (ResultSet rs = metaData.getColumns(connection.getCatalog(), null, tableName, null)) {
                while (rs.next()) {
                    columns.add(rs.getString("COLUMN_NAME").toLowerCase(Locale.ROOT));
                }
            }
        } catch (SQLException ignored) {
            // keep empty set
        }
        return columns;
    }

    private String findFirstExistingColumn(Set<String> availableColumns, List<String> candidates) {
        for (String candidate : candidates) {
            if (availableColumns.contains(candidate.toLowerCase(Locale.ROOT))) {
                return candidate;
            }
        }
        return null;
    }

    private void setupImagesTable() {
        colImagePath.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getImageUrl()));

        colImagePreview.setCellFactory(col -> new TableCell<>() {
            private final ImageView preview = new ImageView();
            {
                preview.setFitWidth(60);
                preview.setFitHeight(40);
                preview.setPreserveRatio(true);
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                } else {
                    HotelImage img = getTableView().getItems().get(getIndex());
                    Image image = loadImage(img.getImageUrl());
                    if (image != null) {
                        preview.setImage(image);
                        setGraphic(preview);
                    } else {
                        setGraphic(new Label("!"));
                    }
                }
            }
        });

        colImageActions.setCellFactory(col -> new TableCell<>() {
            private final Button deleteBtn = new Button("Supprimer");
            {
                deleteBtn.setStyle("-fx-background-color: linear-gradient(to bottom, #dc3545, #c82333); -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 6 14; -fx-cursor: hand; -fx-font-size: 11px; -fx-font-weight: bold;");
                deleteBtn.setOnMouseEntered(e -> deleteBtn.setStyle("-fx-background-color: linear-gradient(to bottom, #e74c5c, #dc3545); -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 6 14; -fx-cursor: hand; -fx-font-size: 11px; -fx-font-weight: bold; -fx-effect: dropshadow(gaussian, rgba(220,53,69,0.4), 6, 0, 0, 1);"));
                deleteBtn.setOnMouseExited(e -> deleteBtn.setStyle("-fx-background-color: linear-gradient(to bottom, #dc3545, #c82333); -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 6 14; -fx-cursor: hand; -fx-font-size: 11px; -fx-font-weight: bold;"));
                deleteBtn.setTooltip(new Tooltip("Supprimer cette image"));
                deleteBtn.setOnAction(e -> confirmDelete("image", "", () -> handleDeleteImage(getTableView().getItems().get(getIndex()))));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : deleteBtn);
            }
        });
    }

    private void setupRoomImagesTable() {
        colRoomImagePath.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getImageUrl()));

        colRoomImagePreview.setCellFactory(col -> new TableCell<>() {
            private final ImageView preview = new ImageView();
            {
                preview.setFitWidth(60);
                preview.setFitHeight(40);
                preview.setPreserveRatio(true);
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                } else {
                    RoomImage img = getTableView().getItems().get(getIndex());
                    Image image = loadImage(img.getImageUrl());
                    if (image != null) {
                        preview.setImage(image);
                        setGraphic(preview);
                    } else {
                        setGraphic(new Label("!"));
                    }
                }
            }
        });

        colRoomImageActions.setCellFactory(col -> new TableCell<>() {
            private final Button deleteBtn = new Button("Supprimer");
            {
                deleteBtn.setStyle("-fx-background-color: linear-gradient(to bottom, #dc3545, #c82333); -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 6 14; -fx-cursor: hand; -fx-font-size: 11px; -fx-font-weight: bold;");
                deleteBtn.setOnMouseEntered(e -> deleteBtn.setStyle("-fx-background-color: linear-gradient(to bottom, #e74c5c, #dc3545); -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 6 14; -fx-cursor: hand; -fx-font-size: 11px; -fx-font-weight: bold; -fx-effect: dropshadow(gaussian, rgba(220,53,69,0.4), 6, 0, 0, 1);"));
                deleteBtn.setOnMouseExited(e -> deleteBtn.setStyle("-fx-background-color: linear-gradient(to bottom, #dc3545, #c82333); -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 6 14; -fx-cursor: hand; -fx-font-size: 11px; -fx-font-weight: bold;"));
                deleteBtn.setTooltip(new Tooltip("Supprimer cette image"));
                deleteBtn.setOnAction(e -> confirmDelete("image", "", () -> handleDeleteRoomImage(getTableView().getItems().get(getIndex()))));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : deleteBtn);
            }
        });
    }

    private Image loadImage(String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) return null;
        try {
            if (imagePath.startsWith("http")) return new Image(imagePath, true);
            URL url = getClass().getResource("/images/" + imagePath);
            if (url != null) return new Image(url.toExternalForm());
            File file = new File("src/main/resources/images/" + imagePath);
            if (file.exists()) return new Image(file.toURI().toString());
        } catch (Exception e) { }
        return null;
    }

    @FXML public void handleSearch() { if (currentView.equals("hotels")) loadHotelsTable(); else loadRoomsTable(); }
    @FXML public void handleFilter() { if (currentView.equals("hotels")) loadHotelsTable(); else loadRoomsTable(); }

    @FXML
    public void handleAdd() {
        if (currentView.equals("hotels")) showAddHotelDialog();
        else showAddRoomDialog();
    }

    @FXML
    public void refreshData() {
        loadAllData();
        if (currentView.equals("hotels")) loadHotelsTable(); 
        else { setupHotelFilter(); loadRoomsTable(); }
        showNotification("Donnees actualisees!", "success");
    }

    @FXML
    public void handleLogout() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Deconnexion");
        confirm.setHeaderText("Voulez-vous vraiment quitter ?");
        styleConfirmationAlert(confirm, false, "Quitter", "Annuler");
        
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            System.exit(0);
        }
    }

    private void confirmDelete(String type, String name, Runnable onConfirm) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmer la suppression");
        confirm.setHeaderText("Supprimer " + (name.isEmpty() ? "cet " + type : type + " \"" + name + "\"") + " ?");
        confirm.setContentText("Cette action est irreversible.");
        styleConfirmationAlert(confirm, true, "Supprimer", "Annuler");
        
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            onConfirm.run();
        }
    }

    private void showAddHotelDialog() {
        Dialog<Hotel> dialog = createHotelDialog(null);
        dialog.showAndWait().ifPresent(h -> {
            try {
                hotelService.create(h);
                loadAllData();
                loadHotelsTable();
                showNotification("Hotel cree avec succes!", "success");
            } catch (SQLException e) { showNotification("Erreur: " + e.getMessage(), "error"); }
        });
    }

    private void showEditHotelDialog(Hotel hotel) {
        Dialog<Hotel> dialog = createHotelDialog(hotel);
        dialog.showAndWait().ifPresent(h -> {
            try {
                h.setId(hotel.getId());
                hotelService.update(h);
                loadAllData();
                if (selectedHotel != null && selectedHotel.getId() == hotel.getId()) showHotelDetail(h);
                else loadHotelsTable();
                showNotification("Hotel modifie!", "success");
            } catch (SQLException e) { showNotification("Erreur: " + e.getMessage(), "error"); }
        });
    }

    private Dialog<Hotel> createHotelDialog(Hotel hotel) {
        Dialog<Hotel> dialog = new Dialog<>();
        dialog.setTitle(hotel == null ? "Nouvel Hotel" : "Modifier l'Hotel");
        
        ButtonType saveBtn = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        GridPane grid = createDialogFormGrid();

        TextField nameField = new TextField(hotel != null ? hotel.getName() : "");
        nameField.setPromptText("Nom de l'hotel");
        applyDialogFieldSizing(nameField);
        
        TextArea descField = new TextArea(hotel != null ? hotel.getDescription() : "");
        descField.setPromptText("Description");
        descField.setPrefRowCount(4);
        descField.setWrapText(true);
        descField.setPrefHeight(108);
        applyDialogFieldSizing(descField);
        
        Spinner<Integer> starsSpinner = new Spinner<>(1, 5, hotel != null ? hotel.getStars() : 3);
        starsSpinner.setEditable(false);
        configureDialogSpinner(starsSpinner, 0);
        ComboBox<String> statusCombo = new ComboBox<>(FXCollections.observableArrayList("AVAILABLE", "OCCUPIED", "MAINTENANCE"));
        statusCombo.setValue(hotel != null ? hotel.getStatus() : "AVAILABLE");
        applyDialogFieldSizing(statusCombo);
        refreshDestinationLookup();
        int currentLocationId = hotel != null ? hotel.getLocationId() : getDefaultDestinationId();
        ComboBox<String> destinationCombo = new ComboBox<>();
        destinationCombo.getItems().addAll(getSortedDestinationLabels());
        if (destinationCombo.getItems().isEmpty()) {
            addDestinationOption(currentLocationId, "Destination #" + currentLocationId);
            destinationCombo.getItems().addAll(getSortedDestinationLabels());
        }
        destinationCombo.setValue(resolveDestinationLabel(currentLocationId));
        applyDialogFieldSizing(destinationCombo);

        grid.add(new Label("Nom *"), 0, 0); grid.add(nameField, 1, 0);
        grid.add(new Label("Description"), 0, 1); grid.add(descField, 1, 1);
        grid.add(new Label("Etoiles"), 0, 2); grid.add(starsSpinner, 1, 2);
        grid.add(new Label("Statut"), 0, 3); grid.add(statusCombo, 1, 3);
        grid.add(new Label("Destination"), 0, 4); grid.add(destinationCombo, 1, 4);

        Node saveButton = dialog.getDialogPane().lookupButton(saveBtn);
        Runnable syncSaveState = () -> saveButton.setDisable(
                nameField.getText() == null || nameField.getText().trim().isEmpty()
                        || destinationCombo.getValue() == null || destinationCombo.getValue().trim().isEmpty()
        );
        syncSaveState.run();
        nameField.textProperty().addListener((obs, old, val) -> syncSaveState.run());
        destinationCombo.valueProperty().addListener((obs, old, val) -> syncSaveState.run());

        dialog.getDialogPane().setContent(grid);
        applyDialogPaneSizing(dialog.getDialogPane(), 740, 430);
        styleDialog(dialog, false);
        Platform.runLater(nameField::requestFocus);

        dialog.setResultConverter(btn -> {
            if (btn != saveBtn || nameField.getText() == null || nameField.getText().trim().isEmpty()) {
                return null;
            }
            int locationId = resolveDestinationId(destinationCombo.getValue(), currentLocationId);
            return new Hotel(0, nameField.getText().trim(), descField.getText(), starsSpinner.getValue(), statusCombo.getValue(), locationId);
        });
        return dialog;
    }

    private void handleDeleteHotel(Hotel hotel) {
        try {
            hotelService.delete(hotel.getId());
            loadAllData();
            loadHotelsTable();
            showNotification("Hotel supprime!", "success");
        } catch (SQLException e) { showNotification("Erreur: " + e.getMessage(), "error"); }
    }

    private void showAddRoomDialog() {
        if (hotelsList.isEmpty()) { showNotification("Creez d'abord un hotel!", "warning"); return; }
        Dialog<Room> dialog = createRoomDialog(null);
        dialog.showAndWait().ifPresent(r -> {
            try {
                roomService.create(r);
                loadAllData();
                loadRoomsTable();
                showNotification("Chambre creee!", "success");
            } catch (SQLException e) { showNotification("Erreur: " + e.getMessage(), "error"); }
        });
    }

    private void showEditRoomDialog(Room room) {
        Dialog<Room> dialog = createRoomDialog(room);
        dialog.showAndWait().ifPresent(r -> {
            try {
                r.setId(room.getId());
                roomService.update(r);
                loadAllData();
                if (selectedRoom != null && selectedRoom.getId() == room.getId()) showRoomDetail(r);
                else loadRoomsTable();
                showNotification("Chambre modifiee!", "success");
            } catch (SQLException e) { showNotification("Erreur: " + e.getMessage(), "error"); }
        });
    }

    private Dialog<Room> createRoomDialog(Room room) {
        Dialog<Room> dialog = new Dialog<>();
        dialog.setTitle(room == null ? "Nouvelle Chambre" : "Modifier la Chambre");
        
        ButtonType saveBtn = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        GridPane grid = createDialogFormGrid();

        TextField numField = new TextField(room != null ? room.getRoomNumber() : "");
        numField.setPromptText("Ex: 101");
        applyDialogFieldSizing(numField);
        ComboBox<String> typeCombo = new ComboBox<>(FXCollections.observableArrayList("SINGLE", "DOUBLE", "SUITE", "DELUXE", "FAMILY"));
        typeCombo.setValue(room != null ? room.getRoomType() : "SINGLE");
        applyDialogFieldSizing(typeCombo);
        Spinner<Integer> capSpinner = new Spinner<>(1, 10, room != null ? room.getCapacity() : 2);
        capSpinner.setEditable(false);
        configureDialogSpinner(capSpinner, 0);
        TextField priceField = new TextField(room != null ? String.valueOf(room.getPricePerNight()) : "100");
        priceField.setPromptText("Prix par nuit");
        applyDialogFieldSizing(priceField);
        ComboBox<String> statusCombo = new ComboBox<>(FXCollections.observableArrayList("AVAILABLE", "OCCUPIED", "MAINTENANCE"));
        statusCombo.setValue(room != null ? room.getStatus() : "AVAILABLE");
        applyDialogFieldSizing(statusCombo);
        ComboBox<Hotel> hotelCombo = new ComboBox<>();
        hotelCombo.setItems(FXCollections.observableArrayList(hotelsList));
        hotelCombo.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(Hotel item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
            }
        });
        hotelCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Hotel item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
            }
        });
        if (room != null) {
            Hotel selectedHotelItem = hotelsList.stream()
                    .filter(h -> h.getId() == room.getHotelId())
                    .findFirst()
                    .orElse(null);
            hotelCombo.setValue(selectedHotelItem);
        } else if (!hotelsList.isEmpty()) {
            hotelCombo.setValue(hotelsList.get(0));
        }
        applyDialogFieldSizing(hotelCombo);

        grid.add(new Label("No Chambre *"), 0, 0); grid.add(numField, 1, 0);
        grid.add(new Label("Type"), 0, 1); grid.add(typeCombo, 1, 1);
        grid.add(new Label("Capacite"), 0, 2); grid.add(capSpinner, 1, 2);
        grid.add(new Label("Prix/Nuit *"), 0, 3); grid.add(priceField, 1, 3);
        grid.add(new Label("Statut"), 0, 4); grid.add(statusCombo, 1, 4);
        grid.add(new Label("Hotel *"), 0, 5); grid.add(hotelCombo, 1, 5);

        dialog.getDialogPane().setContent(grid);
        applyDialogPaneSizing(dialog.getDialogPane(), 740, 500);
        styleDialog(dialog, false);

        dialog.setResultConverter(btn -> {
            if (btn == saveBtn && hotelCombo.getValue() != null && capSpinner.getValue() != null && !numField.getText().isEmpty()) {
                try {
                    int hid = hotelCombo.getValue().getId();
                    double price = Double.parseDouble(priceField.getText());
                    return new Room(0, numField.getText(), typeCombo.getValue(), capSpinner.getValue(), price, statusCombo.getValue(), hid);
                } catch (Exception e) { return null; }
            }
            return null;
        });
        return dialog;
    }

    private void handleDeleteRoom(Room room) {
        try {
            roomService.delete(room.getId());
            loadAllData();
            loadRoomsTable();
            showNotification("Chambre supprimee!", "success");
        } catch (SQLException e) { showNotification("Erreur: " + e.getMessage(), "error"); }
    }

    @FXML
    public void showAddReviewDialog() {
        if (selectedHotel == null) return;
        Dialog<HotelReview> dialog = createReviewDialog(null);
        dialog.showAndWait().ifPresent(r -> {
            try {
                r.setHotelId(selectedHotel.getId());
                reviewService.create(r);
                loadHotelReviews(selectedHotel.getId());
                showNotification("Avis ajoute!", "success");
            } catch (SQLException e) { showNotification("Erreur: " + e.getMessage(), "error"); }
        });
    }

    private Dialog<HotelReview> createReviewDialog(HotelReview review) {
        Dialog<HotelReview> dialog = new Dialog<>();
        dialog.setTitle(review == null ? "Nouvel Avis" : "Modifier l'Avis");
        
        ButtonType saveBtn = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        GridPane grid = createDialogFormGrid();

        HBox starsBox = new HBox(8);
        starsBox.setAlignment(Pos.CENTER_LEFT);
        final int[] rating = {review != null ? review.getRating() : 4};
        Label[] stars = new Label[5];
        for (int i = 0; i < 5; i++) {
            final int r = i + 1;
            stars[i] = new Label(r <= rating[0] ? "\u2605" : "\u2606");
            stars[i].setStyle("-fx-font-size: 28px; -fx-cursor: hand; -fx-text-fill: #FFBD59;");
            stars[i].setOnMouseClicked(e -> {
                rating[0] = r;
                for (int j = 0; j < 5; j++) stars[j].setText(j < r ? "\u2605" : "\u2606");
            });
            starsBox.getChildren().add(stars[i]);
        }
        
        TextArea commentField = new TextArea(review != null ? review.getComment() : "");
        commentField.setPromptText("Commentaire de l'utilisateur");
        commentField.setPrefRowCount(4);
        commentField.setWrapText(true);
        commentField.setPrefHeight(108);
        applyDialogFieldSizing(commentField);
        Spinner<Integer> userSpinner = new Spinner<>(1, 1000, review != null ? review.getUserId() : 1);
        userSpinner.setEditable(false);
        configureDialogSpinner(userSpinner, 260);

        grid.add(new Label("Note"), 0, 0); grid.add(starsBox, 1, 0);
        grid.add(new Label("Commentaire"), 0, 1); grid.add(commentField, 1, 1);
        grid.add(new Label("User ID"), 0, 2); grid.add(userSpinner, 1, 2);

        dialog.getDialogPane().setContent(grid);
        applyDialogPaneSizing(dialog.getDialogPane(), 700, 400);
        styleDialog(dialog, false);
        dialog.setResultConverter(btn -> btn == saveBtn ? new HotelReview(0, rating[0], commentField.getText(), userSpinner.getValue(), 0) : null);
        return dialog;
    }

    private void handleDeleteReview(HotelReview review) {
        try {
            reviewService.delete(review.getId());
            loadHotelReviews(selectedHotel.getId());
            showNotification("Avis supprime!", "success");
        } catch (SQLException e) { showNotification("Erreur: " + e.getMessage(), "error"); }
    }

    @FXML
    public void showAddImageDialog() {
        if (selectedHotel == null && selectedRoom == null) return;
        
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Ajouter une image");
        dialog.setHeaderText("Entrez le chemin de l'image");
        dialog.setContentText("Chemin (ex: hotels/hotel_a.jpg):");
        styleDialog(dialog, false);
        
        dialog.showAndWait().ifPresent(path -> {
            try {
                if (selectedHotel != null) {
                    hotelImageService.create(new HotelImage(0, path, selectedHotel.getId()));
                    loadHotelImages(selectedHotel.getId());
                } else if (selectedRoom != null) {
                    roomImageService.create(new RoomImage(0, path, selectedRoom.getId()));
                    loadRoomImages(selectedRoom.getId());
                }
                showNotification("Image ajoutee!", "success");
            } catch (SQLException e) { showNotification("Erreur: " + e.getMessage(), "error"); }
        });
    }

    private void handleDeleteImage(HotelImage image) {
        try {
            hotelImageService.delete(image.getId());
            if (selectedHotel != null) loadHotelImages(selectedHotel.getId());
            showNotification("Image supprimee!", "success");
        } catch (SQLException e) { showNotification("Erreur: " + e.getMessage(), "error"); }
    }

    private void handleDeleteRoomImage(RoomImage image) {
        try {
            roomImageService.delete(image.getId());
            if (selectedRoom != null) loadRoomImages(selectedRoom.getId());
            showNotification("Image supprimee!", "success");
        } catch (SQLException e) { showNotification("Erreur: " + e.getMessage(), "error"); }
    }

    private void styleDialog(Dialog<?> dialog, boolean destructivePrimary) {
        if (dialog == null || dialog.getDialogPane() == null) return;

        DialogPane pane = dialog.getDialogPane();
        URL cssUrl = getClass().getResource("/css/admin-style.css");
        if (cssUrl != null) {
            String css = cssUrl.toExternalForm();
            if (!pane.getStylesheets().contains(css)) {
                pane.getStylesheets().add(css);
            }
        }

        if (!pane.getStyleClass().contains("gv-dialog")) {
            pane.getStyleClass().add("gv-dialog");
        }
        if (!pane.getStyleClass().contains("gv-admin-dialog")) {
            pane.getStyleClass().add("gv-admin-dialog");
        }

        Platform.runLater(() -> styleDialogButtons(pane, destructivePrimary));
    }

    private GridPane createDialogFormGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(14);
        grid.setPadding(new Insets(24));
        grid.getStyleClass().add("gv-dialog-form");

        ColumnConstraints labelColumn = new ColumnConstraints();
        labelColumn.setMinWidth(130);
        labelColumn.setPrefWidth(130);

        ColumnConstraints fieldColumn = new ColumnConstraints();
        fieldColumn.setHgrow(Priority.ALWAYS);
        fieldColumn.setFillWidth(true);
        fieldColumn.setMinWidth(360);
        fieldColumn.setPrefWidth(520);

        grid.getColumnConstraints().setAll(labelColumn, fieldColumn);
        return grid;
    }

    private void applyDialogFieldSizing(Region field) {
        field.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(field, Priority.ALWAYS);
    }

    private void configureDialogSpinner(Spinner<?> spinner, double prefWidth) {
        if (prefWidth > 0) {
            spinner.setMinWidth(prefWidth);
            spinner.setPrefWidth(prefWidth);
            spinner.setMaxWidth(prefWidth);
            GridPane.setFillWidth(spinner, false);
            GridPane.setHgrow(spinner, Priority.NEVER);
            return;
        }

        spinner.setMinWidth(Region.USE_COMPUTED_SIZE);
        spinner.setPrefWidth(Region.USE_COMPUTED_SIZE);
        spinner.setMaxWidth(Double.MAX_VALUE);
        GridPane.setFillWidth(spinner, true);
        GridPane.setHgrow(spinner, Priority.ALWAYS);
    }

    private void applyDialogPaneSizing(DialogPane pane, double minWidth, double minHeight) {
        pane.setMinWidth(minWidth);
        pane.setPrefWidth(minWidth);
        pane.setMinHeight(minHeight);
    }

    private void styleDialogButtons(DialogPane pane, boolean destructivePrimary) {
        for (ButtonType buttonType : pane.getButtonTypes()) {
            Node node = pane.lookupButton(buttonType);
            if (!(node instanceof Button button)) continue;

            button.getStyleClass().removeAll("gv-dialog-btn-primary", "gv-dialog-btn-neutral", "gv-dialog-btn-danger");
            if (!button.getStyleClass().contains("gv-dialog-btn")) {
                button.getStyleClass().add("gv-dialog-btn");
            }

            boolean isCancel = buttonType.getButtonData().isCancelButton() || buttonType == ButtonType.CANCEL;
            boolean isPrimary = buttonType.getButtonData().isDefaultButton() || buttonType == ButtonType.OK;

            if (isCancel) {
                button.getStyleClass().add("gv-dialog-btn-neutral");
            } else if (destructivePrimary && isPrimary) {
                button.getStyleClass().add("gv-dialog-btn-danger");
            } else {
                button.getStyleClass().add("gv-dialog-btn-primary");
            }

            ButtonBar.setButtonUniformSize(button, false);
            button.setWrapText(false);
            button.setMinWidth(132);
            button.setPrefWidth(148);
            button.setMinHeight(40);
        }
    }

    private void styleConfirmationAlert(Alert alert, boolean destructivePrimary, String primaryLabel, String cancelLabel) {
        if (alert == null || alert.getDialogPane() == null) return;
        alert.setGraphic(null);
        styleDialog(alert, destructivePrimary);

        DialogPane pane = alert.getDialogPane();
        pane.getStyleClass().removeAll("gv-notification", "gv-confirmation");
        if (!pane.getStyleClass().contains("gv-confirmation")) {
            pane.getStyleClass().add("gv-confirmation");
        }
        pane.setMinWidth(560);
        pane.setPrefWidth(560);
        pane.setMinHeight(240);

        Node okNode = pane.lookupButton(ButtonType.OK);
        if (okNode instanceof Button okButton && primaryLabel != null && !primaryLabel.isBlank()) {
            okButton.setText(primaryLabel);
        }
        Node cancelNode = pane.lookupButton(ButtonType.CANCEL);
        if (cancelNode instanceof Button cancelButton && cancelLabel != null && !cancelLabel.isBlank()) {
            cancelButton.setText(cancelLabel);
        }
    }

    private void showNotification(String message, String type) {
        Alert alert = new Alert(type.equals("error") ? Alert.AlertType.ERROR : type.equals("warning") ? Alert.AlertType.WARNING : Alert.AlertType.INFORMATION);
        String title = type.equals("error") ? "Erreur" : type.equals("warning") ? "Attention" : "Succes";
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        styleConfirmationAlert(alert, type.equals("error"), "OK", null);

        DialogPane pane = alert.getDialogPane();
        pane.getStyleClass().removeAll("gv-confirmation", "gv-notification");
        if (!pane.getStyleClass().contains("gv-notification")) {
            pane.getStyleClass().add("gv-notification");
        }
        pane.setMinWidth(560);
        pane.setPrefWidth(560);
        pane.setMinHeight(240);

        Node okNode = pane.lookupButton(ButtonType.OK);
        if (okNode instanceof Button okButton) {
            okButton.setText("OK");
        }

        alert.showAndWait();
    }
}
