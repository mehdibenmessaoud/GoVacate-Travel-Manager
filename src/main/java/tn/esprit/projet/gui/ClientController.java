package tn.esprit.projet.gui;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.CacheHint;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.Node;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import tn.esprit.projet.entities.*;
import tn.esprit.projet.services.*;
import tn.esprit.projet.utils.MyDBConnexion;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.sql.*;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

public class ClientController implements Initializable {

    @FXML private Pane slidingPane;
    @FXML private VBox menuContainer;
    @FXML private VBox contentArea;
    @FXML private VBox searchPanel;
    @FXML private HBox headerBox;
    @FXML private FlowPane hotelsContainer;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> starsFilterCombo;
    @FXML private ComboBox<String> hotelFilterCombo;
    @FXML private ComboBox<String> roomTypeFilterCombo;
    @FXML private ComboBox<String> roomStatusFilterCombo;
    @FXML private VBox filterContainer;
    @FXML private VBox roomTypeFilterContainer;
    @FXML private VBox roomStatusFilterContainer;
    @FXML private Pane roomFilterSeparator;
    @FXML private Label filterLabel;
    @FXML private Label roomTypeFilterLabel;
    @FXML private Label roomStatusFilterLabel;
    @FXML private Label sectionTitle;
    @FXML private Label subtitleLabel;
    @FXML private Label welcomeLabel;
    @FXML private Label searchTitle;

    @FXML private Button btnHotels, btnChambres, btnReservations, btnFavoris, btnProfil, btnLogout;

    private Button currentActiveBtn = null;
    private HotelService hotelService;
    private RoomService roomService;
    private HotelImageService hotelImageService;
    private RoomImageService roomImageService;
    private HotelReviewService hotelReviewService;
    private HotelServiceItemService hotelServiceItemService;
    private String currentView = "hotels";
    private boolean databaseAvailable = true;
    private String databaseErrorMessage = "";
    private final Map<Integer, String> destinationDisplayCache = new HashMap<>();
    private final Map<String, Integer> destinationIdByDisplay = new HashMap<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        slidingPane.setMouseTransparent(true);
        slidingPane.setCache(true);
        slidingPane.setCacheHint(CacheHint.SPEED);
        setupResponsiveGrid();

        setupHotelsFilter();

        Button[] buttons = {btnHotels, btnChambres, btnReservations, btnFavoris, btnProfil, btnLogout};
        for (Button btn : buttons) {
            btn.setOnMouseEntered(e -> handleHover(btn));
            btn.setOnAction(e -> handleMenuClick(btn));
        }

        try {
            hotelService = new HotelService();
            roomService = new RoomService();
            hotelImageService = new HotelImageService();
            roomImageService = new RoomImageService();
            hotelReviewService = new HotelReviewService();
            hotelServiceItemService = new HotelServiceItemService();
            refreshDestinationLookup();
            showHotelFilters();
        } catch (RuntimeException e) {
            databaseAvailable = false;
            databaseErrorMessage = extractRootCauseMessage(e);
            disableUiForOfflineMode();
        }

        Platform.runLater(() -> {
            if (btnHotels != null) {
                slidingPane.setTranslateY(btnHotels.getLayoutY());
                currentActiveBtn = btnHotels;
                animateText(btnHotels, true);
                btnHotels.getStyleClass().add("active");
            }
            slidingPane.toBack();
            if (databaseAvailable) {
                loadHotels();
            } else {
                showDatabaseUnavailableState();
            }
        });

        menuContainer.setOnMouseMoved(event -> {
            double mouseY = event.getY();
            for (Button btn : buttons) {
                double startY = btn.getLayoutY();
                double endY = startY + btn.getHeight();
                if (mouseY >= startY && mouseY <= endY) {
                    handleHover(btn);
                    break;
                }
            }
        });
    }

    private void setupHotelsFilter() {
        showSearchPanel(true);
        filterLabel.setText("Etoiles");

        // Setup stars filter for hotels
        setupStarsFilter();
        starsFilterCombo.setVisible(true);
        starsFilterCombo.setManaged(true);
        hotelFilterCombo.setVisible(false);
        hotelFilterCombo.setManaged(false);

        subtitleLabel.setText("Decouvrez nos meilleurs hotels");
        searchTitle.setText("Ou voulez-vous aller ?");
        sectionTitle.setText("Hotels Disponibles");
        searchField.setPromptText("Nom d'hotel...");
        searchField.clear();

        // Show hotel-specific advanced filters like admin (Destination + Statut)
        showHotelFilters();
    }

    private void setupRoomsFilter() {
        showSearchPanel(true);
        filterLabel.setText("Hotel");

        // Setup hotel filter for rooms
        setupHotelFilter();
        starsFilterCombo.setVisible(false);
        starsFilterCombo.setManaged(false);
        hotelFilterCombo.setVisible(true);
        hotelFilterCombo.setManaged(true);

        subtitleLabel.setText("Trouvez la chambre ideale");
        searchTitle.setText("Quelle chambre cherchez-vous ?");
        sectionTitle.setText("Chambres Disponibles");
        searchField.setPromptText("Numero chambre ou hotel...");
        searchField.clear();

        // Show room-specific filters
        showRoomFilters();
    }

    private void setupStarsFilter() {
        starsFilterCombo.getItems().clear();
        starsFilterCombo.getItems().addAll("Toutes", "5 etoiles", "4 etoiles", "3 etoiles", "2 etoiles", "1 etoile");
        starsFilterCombo.setValue("Toutes");
    }

    private void setupHotelFilter() {
        hotelFilterCombo.getItems().clear();
        hotelFilterCombo.getItems().add("Tous les hotels");
        if (!databaseAvailable || hotelService == null || roomService == null) {
            hotelFilterCombo.setValue("Tous les hotels");
            return;
        }
        try {
            List<Hotel> hotels = hotelService.getAll();
            List<Room> rooms = roomService.getAll();
            for (Hotel h : hotels) {
                int count = getRoomCountForHotel(h.getId(), rooms);
                hotelFilterCombo.getItems().add(h.getName() + " (" + count + ")");
            }
        } catch (SQLException e) {
            // Keep default option
        }
        hotelFilterCombo.setValue("Tous les hotels");
    }

    private int getRoomCountForHotel(int hotelId, List<Room> rooms) {
        return (int) rooms.stream().filter(r -> r.getHotelId() == hotelId).count();
    }

    private String getHotelName(int hotelId, List<Hotel> hotels) {
        return hotels.stream()
                .filter(h -> h.getId() == hotelId)
                .map(Hotel::getName)
                .findFirst()
                .orElse("Hotel #" + hotelId);
    }

    private void showRoomFilters() {
        // Show room type filter
        roomFilterSeparator.setVisible(true);
        roomFilterSeparator.setManaged(true);
        roomTypeFilterContainer.setVisible(true);
        roomTypeFilterContainer.setManaged(true);
        if (roomTypeFilterLabel != null) roomTypeFilterLabel.setText("Type");
        
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
        if (roomStatusFilterLabel != null) roomStatusFilterLabel.setText("Statut");
        
        // Setup status filter options
        roomStatusFilterCombo.getItems().clear();
        roomStatusFilterCombo.getItems().addAll("Tous", "AVAILABLE", "OCCUPIED", "MAINTENANCE");
        roomStatusFilterCombo.setValue("Tous");
        roomStatusFilterCombo.setPromptText("Statut");
        roomStatusFilterCombo.setMinWidth(120);
        roomStatusFilterCombo.setPrefWidth(120);
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

        refreshDestinationLookup();
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

    private void handleMenuClick(Button btn) {
        if (btn == btnLogout) {
            System.exit(0);
            return;
        }
        if (!databaseAvailable) {
            showDatabaseUnavailableState();
            return;
        }
        if (btn == btnHotels) {
            currentView = "hotels";
            setupHotelsFilter();
            loadHotels();
        } else if (btn == btnChambres) {
            currentView = "rooms";
            setupRoomsFilter();
            loadRooms();
        }
    }

    private void loadHotels() {
        if (!databaseAvailable) {
            showDatabaseUnavailableState();
            return;
        }
        hotelsContainer.getChildren().clear();
        try {
            List<Hotel> hotels = hotelService.getAll();
            String search = searchField.getText() != null ? searchField.getText().toLowerCase().trim() : "";
            String starsFilter = starsFilterCombo.getValue();
            String destinationFilter = roomTypeFilterCombo != null ? roomTypeFilterCombo.getValue() : null;
            String statusFilter = roomStatusFilterCombo != null ? roomStatusFilterCombo.getValue() : null;
            int count = 0;

            for (Hotel hotel : hotels) {
                String destinationLabel = resolveDestinationLabel(hotel.getLocationId());
                boolean matchSearch = search.isEmpty() ||
                        hotel.getName().toLowerCase().contains(search) ||
                        hotel.getDescription().toLowerCase().contains(search) ||
                        destinationLabel.toLowerCase(Locale.ROOT).contains(search);
                
                boolean matchStars = starsFilter == null || starsFilter.equals("Toutes") || 
                        starsFilter.startsWith(String.valueOf(hotel.getStars()));

                boolean matchDestination = destinationFilter == null
                        || destinationFilter.equals("Toutes destinations")
                        || destinationFilter.equals(destinationLabel);

                boolean matchStatus = statusFilter == null
                        || statusFilter.equals("Tous")
                        || hotel.getStatus().equals(statusFilter);
                
                if (matchSearch && matchStars && matchDestination && matchStatus) {
                    hotelsContainer.getChildren().add(createHotelCard(hotel));
                    count++;
                }
            }

            sectionTitle.setText(count + " Hotel(s) disponibles");
            
            if (count == 0) {
                showEmptyState("", "Aucun hotel trouve", "Essayez de changer vos filtres");
            }
        } catch (SQLException e) {
            showEmptyState("", "Erreur de chargement", e.getMessage());
        }
    }

    private void refreshDestinationLookup() {
        destinationDisplayCache.clear();
        destinationIdByDisplay.clear();

        if (!databaseAvailable) {
            return;
        }

        try {
            Connection cnx = MyDBConnexion.getInstance().getConnection();
            String sql = "SELECT id, name_destination, pays, ville FROM destination ORDER BY name_destination, ville, id";
            PreparedStatement ps = cnx.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
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
        } catch (Exception ignored) {
            // Keep fallback below.
        }

        if (hotelService != null) {
            try {
                for (Hotel hotel : hotelService.getAll()) {
                    addDestinationOption(hotel.getLocationId(), "Destination #" + hotel.getLocationId());
                }
            } catch (SQLException ignored) {
                // Keep available labels.
            }
        }
    }

    private String buildDestinationLabel(String name, String city, String country, int id) {
        String cleanName = name == null ? "" : name.trim();
        String cleanCity = city == null ? "" : city.trim();
        String cleanCountry = country == null ? "" : country.trim();

        String base = !cleanName.isEmpty() ? cleanName : "Destination #" + id;
        if (!cleanCity.isEmpty() && !cleanCountry.isEmpty()) {
            return base + " - " + cleanCity + " (" + cleanCountry + ")";
        }
        if (!cleanCity.isEmpty()) {
            return base + " - " + cleanCity;
        }
        if (!cleanCountry.isEmpty()) {
            return base + " (" + cleanCountry + ")";
        }
        return base;
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
                .sorted(Map.Entry.comparingByValue(String::compareToIgnoreCase))
                .map(Map.Entry::getValue)
                .toList();
    }

    private String resolveDestinationLabel(int locationId) {
        if (locationId <= 0) return "Destination inconnue";
        String label = destinationDisplayCache.get(locationId);
        if (label == null) {
            label = "Destination #" + locationId;
            addDestinationOption(locationId, label);
        }
        return destinationDisplayCache.getOrDefault(locationId, label);
    }

    private void loadRooms() {
        if (!databaseAvailable) {
            showDatabaseUnavailableState();
            return;
        }
        hotelsContainer.getChildren().clear();
        try {
            List<Room> rooms = roomService.getAll();
            List<Hotel> hotels = hotelService.getAll();
            String search = searchField.getText() != null ? searchField.getText().toLowerCase().trim() : "";
            String hotelFilter = hotelFilterCombo != null ? hotelFilterCombo.getValue() : null;
            String typeFilter = roomTypeFilterCombo != null ? roomTypeFilterCombo.getValue() : null;
            String statusFilter = roomStatusFilterCombo != null ? roomStatusFilterCombo.getValue() : null;
            int count = 0;

            for (Room room : rooms) {
                String hotelName = getHotelName(room.getHotelId(), hotels);

                // Search filter
                boolean matchSearch = search.isEmpty() ||
                        room.getRoomNumber().toLowerCase().contains(search) ||
                        room.getRoomType().toLowerCase().contains(search) ||
                        hotelName.toLowerCase().contains(search);
                
                // Hotel filter
                boolean matchHotel = hotelFilter == null || hotelFilter.equals("Tous les hotels") || 
                        hotelFilter.startsWith(hotelName + " (");
                
                // Type filter
                boolean matchType = typeFilter == null || typeFilter.equals("Tous") || 
                        room.getRoomType().equals(typeFilter);
                
                // Status filter
                boolean matchStatus = statusFilter == null || statusFilter.equals("Tous") || 
                        room.getStatus().equals(statusFilter);

                if (matchSearch && matchHotel && matchType && matchStatus) {
                    hotelsContainer.getChildren().add(createRoomCard(room, hotelName));
                    count++;
                }
            }

            sectionTitle.setText(count + " Chambre(s) disponibles");
            
            if (count == 0) {
                showEmptyState("", "Aucune chambre trouvee", "Essayez de changer vos filtres");
            }
        } catch (SQLException e) {
            showEmptyState("", "Erreur de chargement", e.getMessage());
        }
    }

    private void showEmptyState(String icon, String title, String subtitle) {
        VBox emptyBox = new VBox(10);
        emptyBox.setAlignment(Pos.CENTER);
        emptyBox.setPadding(new Insets(50));

        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 48px;");

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: rgba(255,255,255,0.6);");

        Label subLabel = new Label(subtitle);
        subLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: rgba(255,255,255,0.4);");

        emptyBox.getChildren().addAll(iconLabel, titleLabel, subLabel);
        hotelsContainer.getChildren().add(emptyBox);
    }

    private void showDatabaseUnavailableState() {
        hotelsContainer.getChildren().clear();
        sectionTitle.setText("Connexion base indisponible");
        subtitleLabel.setText("Le service MySQL est indisponible");
        String details = databaseErrorMessage == null || databaseErrorMessage.isBlank()
                ? "Impossible de se connecter a la base. Verifiez MySQL et vos identifiants."
                : databaseErrorMessage;
        showEmptyState("", "Connexion MySQL impossible", details);
    }

    private void disableUiForOfflineMode() {
        searchPanel.setDisable(true);
        btnHotels.setDisable(true);
        btnChambres.setDisable(true);
        btnReservations.setDisable(true);
        btnFavoris.setDisable(true);
        btnProfil.setDisable(true);
    }

    private String extractRootCauseMessage(Throwable error) {
        Throwable root = error;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        String message = root.getMessage();
        if (message == null || message.isBlank()) {
            return root.getClass().getSimpleName();
        }
        return message;
    }

    private VBox createCardImagePlaceholder(String title, String subtitle) {
        VBox placeholder = new VBox(4);
        placeholder.setAlignment(Pos.CENTER);
        placeholder.setMouseTransparent(true);
        placeholder.getStyleClass().add("card-placeholder-box");

        Label icon = new Label("PHOTO");
        icon.getStyleClass().add("card-placeholder-icon");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("card-placeholder-title");

        Label subLabel = new Label(subtitle);
        subLabel.getStyleClass().add("card-placeholder-sub");

        placeholder.getChildren().addAll(icon, titleLabel, subLabel);
        return placeholder;
    }

    private Image loadImage(String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) {
            return null;
        }
        
        try {
            // Check if it's a URL
            if (imagePath.startsWith("http://") || imagePath.startsWith("https://")) {
                return new Image(imagePath, true);
            }
            
            // Try loading from resources - path is like "hotels/hotel_a_1.jpg" or "rooms/room_101_1.jpg"
            String resourcePath = "/images/" + imagePath;
            URL resourceUrl = getClass().getResource(resourcePath);
            if (resourceUrl != null) {
                return new Image(resourceUrl.toExternalForm());
            }
            
            // Try from file system (for development)
            String[] filePaths = {
                "src/main/resources/images/" + imagePath,
                "target/classes/images/" + imagePath
            };
            
            for (String filePath : filePaths) {
                File file = new File(filePath);
                if (file.exists()) {
                    return new Image(file.toURI().toString());
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error loading image: " + imagePath + " - " + e.getMessage());
        }
        return null;
    }

    private Image getDefaultImage(String type) {
        return null;
    }
    
    private void showSearchPanel(boolean show) {
        searchPanel.setVisible(show);
        searchPanel.setManaged(show);
        headerBox.setVisible(show);
        headerBox.setManaged(show);
    }

    private void setupResponsiveGrid() {
        hotelsContainer.setHgap(24);
        hotelsContainer.setVgap(28);
        hotelsContainer.setPrefWrapLength(1080);
        hotelsContainer.setAlignment(Pos.TOP_CENTER);
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

    private StackPane createHotelCard(Hotel hotel) {
    StackPane card = new StackPane();
    card.setMinWidth(320);
    card.setPrefWidth(320);
    card.setMaxWidth(320);
    card.setMinHeight(410);
    card.setPrefHeight(410);
    card.setMaxHeight(410);
    card.getStyleClass().add("water-card");
    card.setCursor(javafx.scene.Cursor.HAND);
    applyRoundedClip(card, 20);

    VBox content = new VBox(0);
    content.setAlignment(Pos.TOP_CENTER);
    content.setMinWidth(320);
    content.setPrefWidth(320);
    content.setMaxWidth(320);

    StackPane header = new StackPane();
    header.setMinHeight(220);
    header.setPrefHeight(220);
    header.setMaxHeight(220);
    header.setMinWidth(320);
    header.setPrefWidth(320);
    header.setMaxWidth(320);
    header.setPadding(Insets.EMPTY);
    header.getStyleClass().addAll("card-header", "stars-" + hotel.getStars());
    applyRoundedClip(header, 24);

    List<HotelImage> images = List.of();
    int imageCountValue = 0;
    boolean hasImage = false;
    try {
        images = hotelImageService.getByHotelId(hotel.getId());
        imageCountValue = images.size();
        if (!images.isEmpty()) {
            Image img = loadImage(images.get(0).getImageUrl());
            if (img != null) {
                ImageView imageView = new ImageView();
                imageView.setSmooth(true);
                imageView.getStyleClass().add("card-header-image");
                imageView.setManaged(false);
                applyImageCoverFit(imageView, img, 320, 220);
                header.getChildren().add(imageView);
                hasImage = true;
            }
        }
    } catch (SQLException ignored) {
        // keep fallback placeholder
    }

    if (!hasImage) {
        VBox placeholder = createCardImagePlaceholder("Photo a venir", "Aucune image disponible");
        placeholder.setPrefSize(320, 220);
        placeholder.setMaxSize(320, 220);
        placeholder.setStyle("-fx-background-color: linear-gradient(to bottom, rgba(255,130,16,0.78), rgba(20,83,130,0.78)); -fx-background-radius: 20 20 0 0; -fx-padding: 10;");
        header.getChildren().add(placeholder);
    }

    Region overlay = new Region();
    overlay.getStyleClass().add("card-header-overlay");
    overlay.setPrefSize(320, 220);
    overlay.setMouseTransparent(true);
    header.getChildren().add(overlay);

    Label imageCount = new Label(imageCountValue == 0
            ? "Sans image"
            : imageCountValue + (imageCountValue > 1 ? " images" : " image"));
    imageCount.getStyleClass().add("card-image-count");
    StackPane.setAlignment(imageCount, Pos.TOP_RIGHT);
    StackPane.setMargin(imageCount, new Insets(10));

    Label starsBadge = new Label(formatStarsWithScore(hotel.getStars()));
    starsBadge.getStyleClass().add("card-stars-badge");
    StackPane.setAlignment(starsBadge, Pos.TOP_LEFT);
    StackPane.setMargin(starsBadge, new Insets(10));
    header.getChildren().addAll(starsBadge, imageCount);

    VBox glassContent = new VBox(10);
    glassContent.getStyleClass().add("glass-card");
    glassContent.setPadding(new Insets(16, 18, 20, 18));
    glassContent.setAlignment(Pos.CENTER);
    glassContent.setMinHeight(190);
    glassContent.setPrefHeight(190);
    glassContent.setMinWidth(320);
    glassContent.setPrefWidth(320);
    glassContent.setMaxWidth(320);

    Label title = new Label(hotel.getName());
    title.getStyleClass().add("card-title");
    title.setMaxWidth(Double.MAX_VALUE);
    title.setAlignment(Pos.CENTER);

    Label desc = new Label(hotel.getDescription());
    desc.getStyleClass().add("card-sub");
    desc.setWrapText(true);
    desc.setMinHeight(44);
    desc.setPrefHeight(44);
    desc.setMaxHeight(44);
    desc.setMaxWidth(286);
    desc.setAlignment(Pos.CENTER);
    desc.setStyle("-fx-text-alignment: center;");

    HBox info = new HBox(12);
    info.setAlignment(Pos.CENTER);

    Label stars = new Label(formatStarsWithScore(hotel.getStars()));
    stars.getStyleClass().add("card-rate");

    Label status = new Label();
    switch (hotel.getStatus()) {
        case "AVAILABLE" -> { status.setText("Disponible"); status.getStyleClass().add("badge-available"); }
        case "OCCUPIED" -> { status.setText("Occupee"); status.getStyleClass().add("badge-occupied"); }
        case "MAINTENANCE" -> { status.setText("Maintenance"); status.getStyleClass().add("badge-maintenance"); }
    }

    info.getChildren().addAll(stars, status);
    Region verticalSpacer = new Region();
    VBox.setVgrow(verticalSpacer, Priority.ALWAYS);

    HBox actions = new HBox(12);
    actions.getStyleClass().add("card-actions");
    actions.setPadding(new Insets(4, 0, 0, 0));
    actions.setAlignment(Pos.CENTER);

    Button detailsBtn = new Button("Details");
    detailsBtn.getStyleClass().add("btn-card-secondary");
    detailsBtn.setMinWidth(128);
    detailsBtn.setPrefWidth(128);
    detailsBtn.setMinHeight(44);
    detailsBtn.setOnAction(e -> showHotelDetails(hotel));

    Button roomsBtn = new Button("Chambres");
    roomsBtn.getStyleClass().add("btn-card-primary");
    roomsBtn.setMinWidth(128);
    roomsBtn.setPrefWidth(128);
    roomsBtn.setMinHeight(44);
    roomsBtn.setOnAction(e -> {
        currentView = "rooms";
        setupRoomsFilter();
        searchField.setText(hotel.getName());
        loadRooms();
    });

    actions.getChildren().addAll(detailsBtn, roomsBtn);

    glassContent.getChildren().addAll(title, desc, info, verticalSpacer, actions);
    content.getChildren().addAll(header, glassContent);
    card.getChildren().add(content);

    card.setOnMouseClicked(e -> {
        if (e.getClickCount() == 2) {
            showHotelDetails(hotel);
        }
    });

    return card;
}

    private void showHotelDetails(Hotel hotel) {
        showSearchPanel(false);
        hotelsContainer.getChildren().clear();
        sectionTitle.setText("Details hotel");
        List<Room> hotelRooms = getRoomsByHotel(hotel.getId());

        VBox detailsBox = new VBox(25);
        detailsBox.setPadding(new Insets(10));
        detailsBox.setMaxWidth(1160);

        Button backBtn = new Button("Retour aux hotels");
        backBtn.getStyleClass().add("btn-book");
        backBtn.setStyle("-fx-background-color: #FF8210; -fx-font-size: 14px; -fx-padding: 12 25;");
        backBtn.setOnAction(e -> {
            currentView = "hotels";
            setupHotelsFilter();
            loadHotels();
        });

        // Main image gallery section
        HBox mainSection = new HBox(25);
        mainSection.setAlignment(Pos.TOP_LEFT);

        // Left side - Main image with thumbnails
        VBox imageGallery = new VBox(10);
        imageGallery.setPrefWidth(520);

        StackPane mainImageContainer = new StackPane();
        mainImageContainer.setPrefSize(520, 330);
        mainImageContainer.setStyle("-fx-background-color: rgba(0,0,0,0.4); -fx-background-radius: 15;");
        applyRoundedClip(mainImageContainer, 24);
        
        ImageView mainImageView = new ImageView();
        mainImageView.setFitWidth(520);
        mainImageView.setFitHeight(330);
        mainImageView.setPreserveRatio(true);
        mainImageView.setSmooth(true);
        mainImageView.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 15, 0, 0, 5);");

        Label imagePlaceholder = new Label("H");
        imagePlaceholder.setStyle("-fx-font-size: 80px; -fx-text-fill: rgba(255,255,255,0.5);");

        try {
            List<HotelImage> images = hotelImageService.getByHotelId(hotel.getId());
            if (!images.isEmpty()) {
                Image firstImg = loadImage(images.get(0).getImageUrl());
                if (firstImg != null) {
                    applyImageBestFit(mainImageView, firstImg, 520, 330);
                    mainImageContainer.getChildren().add(mainImageView);
                } else {
                    mainImageContainer.getChildren().add(imagePlaceholder);
                }

                // Thumbnails row
                if (images.size() > 1) {
                    final int[] currentIndex = {0};

                    Button prevBtn = new Button("<");
                    prevBtn.setStyle("-fx-background-color: rgba(0,0,0,0.55); -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold; -fx-background-radius: 20; -fx-min-width: 36; -fx-min-height: 36; -fx-cursor: hand;");
                    Button nextBtn = new Button(">");
                    nextBtn.setStyle("-fx-background-color: rgba(0,0,0,0.55); -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold; -fx-background-radius: 20; -fx-min-width: 36; -fx-min-height: 36; -fx-cursor: hand;");

                    prevBtn.setOnAction(ev -> {
                        currentIndex[0] = (currentIndex[0] - 1 + images.size()) % images.size();
                        Image newImg = loadImage(images.get(currentIndex[0]).getImageUrl());
                        if (newImg != null) applyImageBestFit(mainImageView, newImg, 520, 330);
                    });
                    nextBtn.setOnAction(ev -> {
                        currentIndex[0] = (currentIndex[0] + 1) % images.size();
                        Image newImg = loadImage(images.get(currentIndex[0]).getImageUrl());
                        if (newImg != null) applyImageBestFit(mainImageView, newImg, 520, 330);
                    });

                    StackPane.setAlignment(prevBtn, Pos.CENTER_LEFT);
                    StackPane.setAlignment(nextBtn, Pos.CENTER_RIGHT);
                    StackPane.setMargin(prevBtn, new Insets(0, 10, 0, 10));
                    StackPane.setMargin(nextBtn, new Insets(0, 10, 0, 10));
                    mainImageContainer.getChildren().addAll(prevBtn, nextBtn);

                    HBox thumbnails = new HBox(10);
                    thumbnails.setAlignment(Pos.CENTER_LEFT);
                    
                    for (int i = 0; i < images.size(); i++) {
                        final int index = i;
                        Image thumbImg = loadImage(images.get(i).getImageUrl());
                        if (thumbImg != null) {
                            ImageView thumb = new ImageView(thumbImg);
                            thumb.setFitWidth(112);
                            thumb.setFitHeight(76);
                            thumb.setPreserveRatio(true);
                            
                            StackPane thumbContainer = new StackPane(thumb);
                            thumbContainer.setStyle("-fx-background-color: rgba(0,0,0,0.3); -fx-background-radius: 8; -fx-padding: 3; -fx-cursor: hand;");
                            thumbContainer.setOnMouseEntered(ev -> thumbContainer.setStyle("-fx-background-color: #FF8210; -fx-background-radius: 8; -fx-padding: 3; -fx-cursor: hand;"));
                            thumbContainer.setOnMouseExited(ev -> thumbContainer.setStyle("-fx-background-color: rgba(0,0,0,0.3); -fx-background-radius: 8; -fx-padding: 3; -fx-cursor: hand;"));
                            thumbContainer.setOnMouseClicked(ev -> {
                                currentIndex[0] = index;
                                Image newImg = loadImage(images.get(index).getImageUrl());
                                if (newImg != null) applyImageBestFit(mainImageView, newImg, 520, 330);
                            });
                            
                            thumbnails.getChildren().add(thumbContainer);
                        }
                    }
                    imageGallery.getChildren().addAll(mainImageContainer, thumbnails);
                } else {
                    imageGallery.getChildren().add(mainImageContainer);
                }
            } else {
                mainImageContainer.getChildren().add(imagePlaceholder);
                imageGallery.getChildren().add(mainImageContainer);
            }
        } catch (SQLException ex) {
            mainImageContainer.getChildren().add(imagePlaceholder);
            imageGallery.getChildren().add(mainImageContainer);
        }

        // Right side - Hotel info
        VBox infoBox = new VBox(15);
        infoBox.setPadding(new Insets(10));
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        Label nameLabel = new Label(hotel.getName());
        nameLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: white;");

        HBox starsRow = new HBox(12);
        starsRow.setAlignment(Pos.CENTER_LEFT);
        Label starsLabel = new Label(renderStarsVisual(hotel.getStars()));
        starsLabel.setStyle("-fx-font-size: 24px; -fx-text-fill: #FFBD59; -fx-font-weight: bold;");
        Label starsText = new Label("(" + clampStars(hotel.getStars()) + "/5)");
        starsText.setStyle("-fx-font-size: 15px; -fx-text-fill: #FFBD59; -fx-font-weight: bold;");
        starsRow.getChildren().addAll(starsLabel, starsText);

        Label descLabel = new Label(hotel.getDescription());
        descLabel.setStyle("-fx-font-size: 15px; -fx-text-fill: rgba(255,255,255,0.8); -fx-wrap-text: true;");
        descLabel.setWrapText(true);

        Label statusLabel = new Label();
        statusLabel.setStyle("-fx-font-size: 14px; -fx-padding: 8 15;");
        switch (hotel.getStatus()) {
            case "AVAILABLE" -> { statusLabel.setText("Disponible"); statusLabel.getStyleClass().add("badge-available"); }
            case "OCCUPIED" -> { statusLabel.setText("Occupee"); statusLabel.getStyleClass().add("badge-occupied"); }
            case "MAINTENANCE" -> { statusLabel.setText("Maintenance"); statusLabel.getStyleClass().add("badge-maintenance"); }
        }

        infoBox.getChildren().addAll(nameLabel, starsRow, descLabel, statusLabel);
        mainSection.getChildren().addAll(imageGallery, infoBox);

        VBox hotelServicesSection = createHotelServicesSection(hotel);
        VBox roomsAvailabilitySection = createRoomsAvailabilitySection(hotel, hotelRooms);

        // Reviews section with client add action
        VBox reviewsSection = new VBox(15);
        reviewsSection.setStyle("-fx-background-color: rgba(255,255,255,0.03); -fx-background-radius: 15; -fx-padding: 20;");

        Label reviewsTitle = new Label("Avis des clients");
        reviewsTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");

        Button addReviewBtn = new Button("+ Ajouter un avis");
        addReviewBtn.getStyleClass().add("btn-orange-glow");
        addReviewBtn.setStyle("-fx-font-size: 12px; -fx-padding: 8 14; -fx-background-radius: 18;");

        Region reviewsSpacer = new Region();
        HBox.setHgrow(reviewsSpacer, Priority.ALWAYS);

        HBox reviewsHeader = new HBox(10, reviewsTitle, reviewsSpacer, addReviewBtn);
        reviewsHeader.setAlignment(Pos.CENTER_LEFT);

        VBox reviewsBox = new VBox(12);
        Runnable reloadReviews = () -> {
            reviewsBox.getChildren().clear();
            try {
                List<HotelReview> reviews = hotelReviewService.getReviewsByHotel(hotel.getId());
                if (reviews.isEmpty()) {
                    Label noReviews = new Label("Aucun avis pour le moment - Soyez le premier a donner votre avis!");
                    noReviews.setStyle("-fx-text-fill: rgba(255,255,255,0.5); -fx-font-style: italic;");
                    reviewsBox.getChildren().add(noReviews);
                } else {
                    for (HotelReview review : reviews) {
                        reviewsBox.getChildren().add(createReviewBox(review.getRating(), review.getComment()));
                    }
                }
            } catch (SQLException e) {
                Label error = new Label("Erreur de chargement des avis");
                error.setStyle("-fx-text-fill: #FF8210;");
                reviewsBox.getChildren().add(error);
            }
        };
        reloadReviews.run();

        addReviewBtn.setOnAction(e -> showAddReviewDialog(hotel, reloadReviews));

        reviewsSection.getChildren().addAll(reviewsHeader, reviewsBox);

        detailsBox.getChildren().addAll(backBtn, mainSection, hotelServicesSection, roomsAvailabilitySection, reviewsSection);
        hotelsContainer.getChildren().add(detailsBox);
    }

    private VBox createHotelServicesSection(Hotel hotel) {
        VBox section = new VBox(10);
        section.setStyle("-fx-background-color: rgba(255,255,255,0.03); -fx-background-radius: 15; -fx-padding: 20;");

        Label title = new Label("Services de l'hotel");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");

        FlowPane servicesPane = new FlowPane();
        servicesPane.setHgap(10);
        servicesPane.setVgap(10);

        List<String> services = resolveHotelServicesForDisplay(hotel);
        if (services.isEmpty()) {
            Label empty = new Label("Aucun service configure pour cet hotel.");
            empty.setStyle("-fx-text-fill: rgba(255,255,255,0.65); -fx-font-size: 12px; -fx-font-style: italic;");
            servicesPane.getChildren().add(empty);
        } else {
            for (String service : services) {
                Label chip = new Label(service);
                chip.setStyle("-fx-background-color: rgba(103,154,193,0.3); -fx-text-fill: #e8f3ff; -fx-padding: 6 12; -fx-background-radius: 14; -fx-font-size: 12px; -fx-font-weight: 600;");
                servicesPane.getChildren().add(chip);
            }
        }

        section.getChildren().addAll(title, servicesPane);
        return section;
    }

    private List<String> resolveHotelServicesForDisplay(Hotel hotel) {
        if (!databaseAvailable || hotelServiceItemService == null) {
            return List.of();
        }
        try {
            return hotelServiceItemService.getActiveByHotelId(hotel.getId()).stream()
                    .map(HotelServiceItem::getName)
                    .filter(name -> name != null && !name.isBlank())
                    .map(String::trim)
                    .distinct()
                    .toList();
        } catch (SQLException ignored) {
            return List.of();
        }
    }

    private VBox createRoomsAvailabilitySection(Hotel hotel, List<Room> hotelRooms) {

        VBox section = new VBox(12);
        section.getStyleClass().add("availability-section");

        Label title = new Label("Disponibilite et tarifs");
        title.getStyleClass().add("availability-title");

        HBox controls = new HBox(10);
        controls.setAlignment(Pos.CENTER_LEFT);
        controls.getStyleClass().add("availability-controls");

        ComboBox<String> typeFilter = new ComboBox<>(FXCollections.observableArrayList("Tous", "SINGLE", "DOUBLE", "SUITE", "DELUXE", "FAMILY"));
        typeFilter.setValue("Tous");
        typeFilter.setPromptText("Type");
        typeFilter.setPrefWidth(150);
        typeFilter.getStyleClass().add("availability-filter");
        styleAvailabilityComboBox(typeFilter);

        Spinner<Integer> guestsSpinner = new Spinner<>(1, 8, 1);
        guestsSpinner.setEditable(true);
        guestsSpinner.setPrefWidth(120);
        guestsSpinner.getStyleClass().add("availability-filter");
        guestsSpinner.getEditor().setOnAction(e -> {
            int parsedGuests = parseGuestsInput(guestsSpinner.getEditor().getText());
            guestsSpinner.getValueFactory().setValue(parsedGuests);
        });
        guestsSpinner.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) {
                int parsedGuests = parseGuestsInput(guestsSpinner.getEditor().getText());
                guestsSpinner.getValueFactory().setValue(parsedGuests);
            }
        });

        Spinner<Integer> nightsSpinner = new Spinner<>(1, 30, 1);
        nightsSpinner.setEditable(true);
        nightsSpinner.setPrefWidth(100);
        nightsSpinner.getStyleClass().add("availability-filter");
        nightsSpinner.getEditor().setOnAction(e -> {
            int parsedNights = parseNightsInput(nightsSpinner.getEditor().getText());
            nightsSpinner.getValueFactory().setValue(parsedNights);
        });
        nightsSpinner.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) {
                int parsedNights = parseNightsInput(nightsSpinner.getEditor().getText());
                nightsSpinner.getValueFactory().setValue(parsedNights);
            }
        });

        Button resetFiltersBtn = new Button("Reinitialiser");
        resetFiltersBtn.getStyleClass().add("availability-reset-btn");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        controls.getChildren().addAll(
                createFilterLabel("Type"), typeFilter,
                createFilterLabel("Voyageurs"), guestsSpinner,
                createFilterLabel("Nuits"), nightsSpinner,
                resetFiltersBtn, spacer
        );

        VBox roomsBox = new VBox(10);

        Runnable refreshRows = () -> {
            roomsBox.getChildren().clear();
            String selectedType = typeFilter.getValue();
            int guests = guestsSpinner.getValue();
            int nights = nightsSpinner.getValue();

            List<Room> filtered = hotelRooms.stream()
                    .filter(room -> "AVAILABLE".equals(room.getStatus()))
                    .filter(room -> selectedType == null || selectedType.equals("Tous") || selectedType.equals(room.getRoomType()))
                    .filter(room -> room.getCapacity() >= guests)
                    .sorted((a, b) -> Double.compare(a.getPricePerNight(), b.getPricePerNight()))
                    .toList();

            if (filtered.isEmpty()) {
                Label empty = new Label("Aucune chambre ne correspond a vos criteres.");
                empty.setStyle("-fx-text-fill: rgba(255,255,255,0.65); -fx-font-style: italic;");
                roomsBox.getChildren().add(empty);
                return;
            }

            for (Room room : filtered) {
                roomsBox.getChildren().add(createRoomAvailabilityRow(room, hotel.getName(), nights));
            }
        };

        typeFilter.setOnAction(e -> refreshRows.run());
        guestsSpinner.valueProperty().addListener((obs, oldVal, newVal) -> refreshRows.run());
        nightsSpinner.valueProperty().addListener((obs, oldVal, newVal) -> refreshRows.run());
        resetFiltersBtn.setOnAction(e -> {
            typeFilter.setValue("Tous");
            guestsSpinner.getValueFactory().setValue(1);
            nightsSpinner.getValueFactory().setValue(1);
            refreshRows.run();
        });

        refreshRows.run();

        section.getChildren().addAll(title, controls, roomsBox);
        return section;
    }

    private Label createFilterLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("availability-filter-label");
        return label;
    }

    private int parseNightsInput(String text) {
        if (text == null || text.isBlank()) {
            return 1;
        }
        try {
            int nights = Integer.parseInt(text.trim());
            if (nights < 1) return 1;
            return Math.min(nights, 30);
        } catch (NumberFormatException ignored) {
            return 1;
        }
    }

    private int parseGuestsInput(String text) {
        if (text == null || text.isBlank()) {
            return 1;
        }
        try {
            int guests = Integer.parseInt(text.trim());
            if (guests < 1) return 1;
            return Math.min(guests, 8);
        } catch (NumberFormatException ignored) {
            return 1;
        }
    }

    private void styleAvailabilityComboBox(ComboBox<String> comboBox) {
        comboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item);
                setStyle("-fx-background-color: transparent; -fx-text-fill: #f5fbff; -fx-font-weight: 700;");
            }
        });

        comboBox.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("-fx-background-color: #1b456f; -fx-text-fill: #f5fbff;");
                } else {
                    setText(item);
                    setStyle("-fx-background-color: #1b456f; -fx-text-fill: #f5fbff; -fx-font-weight: 700;");
                }
            }
        });

        comboBox.showingProperty().addListener((obs, wasShowing, isShowing) -> {
            if (!isShowing) {
                return;
            }
            Node listNode = comboBox.lookup(".list-view");
            if (listNode instanceof ListView<?> listView) {
                listView.setStyle("-fx-background-color: #1b456f; -fx-control-inner-background: #1b456f; -fx-border-color: rgba(173, 214, 247, 0.45); -fx-border-radius: 8;");
            }
        });
    }

    private HBox createRoomAvailabilityRow(Room room, String hotelName, int nights) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("availability-room-row");
        row.setCursor(javafx.scene.Cursor.HAND);
        row.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && !isClickInsideButton(event.getTarget())) {
                showRoomDetails(room, hotelName);
            }
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

        double totalPrice = room.getPricePerNight() * Math.max(1, nights);
        Label price = new Label(
                formatPrice(room.getPricePerNight()) + " DT / nuit | Total: " + formatPrice(totalPrice) + " DT"
        );
        price.getStyleClass().add("availability-room-price");

        Button detailsBtn = new Button("Details");
        detailsBtn.getStyleClass().add("btn-card-secondary");
        detailsBtn.setOnAction(e -> showRoomDetails(room, hotelName));

        Button reserveBtn = new Button("Reserver");
        reserveBtn.getStyleClass().add("btn-book");
        boolean available = "AVAILABLE".equals(room.getStatus());
        reserveBtn.setDisable(!available);
        if (available) {
            reserveBtn.setOnAction(e -> showBookingConfirmation(room, hotelName, nights));
        }

        row.getChildren().addAll(left, spacer, status, price, detailsBtn, reserveBtn);
        return row;
    }

    private boolean isClickInsideButton(Object target) {
        if (!(target instanceof Node node)) {
            return false;
        }
        Node current = node;
        while (current != null) {
            if (current instanceof ButtonBase) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    private List<Room> getRoomsByHotel(int hotelId) {
        if (!databaseAvailable || roomService == null) {
            return List.of();
        }
        try {
            return roomService.getAll().stream()
                    .filter(room -> room.getHotelId() == hotelId)
                    .toList();
        } catch (SQLException e) {
            return List.of();
        }
    }

    private String getStatusLabel(String status) {
        return switch (status) {
            case "AVAILABLE" -> "Disponible";
            case "OCCUPIED" -> "Occupee";
            case "MAINTENANCE" -> "Maintenance";
            default -> status;
        };
    }

    private String formatPrice(double price) {
        if (price == Math.rint(price)) {
            return String.format("%.0f", price);
        }
        return String.format("%.2f", price);
    }

    private void applyRoundedClip(Region region, double arc) {
        Rectangle clip = new Rectangle();
        clip.setArcWidth(arc);
        clip.setArcHeight(arc);
        clip.widthProperty().bind(region.widthProperty());
        clip.heightProperty().bind(region.heightProperty());
        region.setClip(clip);
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
            double scale = Math.min(maxWidth / imageWidth, maxHeight / imageHeight);
            if (scale > 2.0) {
                scale = 2.0;
            }
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

    private int clampStars(int stars) {
        return Math.max(1, Math.min(5, stars));
    }

    private String renderStarsVisual(int stars) {
        int safeStars = clampStars(stars);
        return "\u2605".repeat(safeStars) + "\u2606".repeat(5 - safeStars);
    }

    private String formatStarsWithScore(int stars) {
        int safeStars = clampStars(stars);
        return renderStarsVisual(safeStars) + " (" + safeStars + "/5)";
    }

    private VBox createImageBox(String imagePath) {
        VBox box = new VBox(5);
        box.setAlignment(Pos.CENTER);
        box.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-background-radius: 10; -fx-padding: 10;");

        StackPane imageContainer = new StackPane();
        imageContainer.setPrefSize(200, 130);
        imageContainer.setStyle("-fx-background-color: rgba(0,0,0,0.3); -fx-background-radius: 8;");

        Image img = loadImage(imagePath);
        if (img != null) {
            ImageView imageView = new ImageView(img);
            imageView.setFitWidth(200);
            imageView.setFitHeight(130);
            imageView.setPreserveRatio(true);
            imageContainer.getChildren().add(imageView);
        } else {
            Label placeholder = new Label("");
            placeholder.setStyle("-fx-font-size: 40px; -fx-text-fill: rgba(255,255,255,0.3);");
            imageContainer.getChildren().add(placeholder);
        }

        Label pathLabel = new Label(imagePath != null ? imagePath : "Image");
        pathLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: rgba(255,255,255,0.5);");
        pathLabel.setMaxWidth(200);

        box.getChildren().addAll(imageContainer, pathLabel);
        return box;
    }

    private HBox createReviewBox(int rating, String comment) {
        HBox box = new HBox(20);
        box.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-background-radius: 12; -fx-padding: 18;");
        box.setAlignment(Pos.CENTER_LEFT);

        // Avatar placeholder
        StackPane avatar = new StackPane();
        avatar.setPrefSize(45, 45);
        avatar.setStyle("-fx-background-color: linear-gradient(to bottom right, #FF8210, #FFBD59); -fx-background-radius: 25;");
        Label avatarIcon = new Label("U");
        avatarIcon.setStyle("-fx-font-size: 20px; -fx-text-fill: white; -fx-font-weight: bold;");
        avatar.getChildren().add(avatarIcon);

        VBox reviewContent = new VBox(5);
        HBox.setHgrow(reviewContent, Priority.ALWAYS);

        HBox ratingRow = new HBox(8);
        ratingRow.setAlignment(Pos.CENTER_LEFT);
        Label ratingLabel = new Label(rating + "/5");
        ratingLabel.setStyle("-fx-font-size: 14px;");
        Label ratingText = new Label("(" + rating + "/5)");
        ratingText.setStyle("-fx-font-size: 11px; -fx-text-fill: rgba(255,255,255,0.5);");
        ratingRow.getChildren().addAll(ratingLabel, ratingText);

        Label commentLabel = new Label(comment);
        commentLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: rgba(255,255,255,0.85);");
        commentLabel.setWrapText(true);

        reviewContent.getChildren().addAll(ratingRow, commentLabel);
        box.getChildren().addAll(avatar, reviewContent);
        return box;
    }

    private void showAddReviewDialog(Hotel hotel, Runnable onReviewSaved) {
        if (!databaseAvailable || hotelReviewService == null) {
            Alert unavailable = new Alert(Alert.AlertType.WARNING);
            unavailable.setTitle("Service indisponible");
            unavailable.setHeaderText(null);
            unavailable.setContentText("Impossible d'ajouter un avis pour le moment.");
            unavailable.setGraphic(null);
            styleClientDialog(unavailable, false);
            unavailable.showAndWait();
            return;
        }

        Dialog<HotelReview> dialog = new Dialog<>();
        dialog.setTitle("Ajouter un avis");

        ButtonType saveBtn = new ButtonType("Publier", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        GridPane form = new GridPane();
        form.setHgap(14);
        form.setVgap(12);
        form.setPadding(new Insets(12));
        form.getStyleClass().add("gv-dialog-form");

        Label hotelValue = new Label(hotel.getName());
        hotelValue.setStyle("-fx-text-fill: #FFBD59; -fx-font-size: 14px; -fx-font-weight: 700;");

        Label hotelLabel = new Label("Hotel");
        hotelLabel.setStyle("-fx-text-fill: #eaf3ff; -fx-font-size: 13px; -fx-font-weight: 600;");

        Label ratingLabel = new Label("Note");
        ratingLabel.setStyle("-fx-text-fill: #eaf3ff; -fx-font-size: 13px; -fx-font-weight: 600;");

        Label commentLabel = new Label("Commentaire *");
        commentLabel.setStyle("-fx-text-fill: #eaf3ff; -fx-font-size: 13px; -fx-font-weight: 600;");

        Spinner<Integer> ratingSpinner = new Spinner<>(1, 5, 5);
        ratingSpinner.setEditable(false);
        ratingSpinner.setPrefWidth(120);

        TextArea commentArea = new TextArea();
        commentArea.setPromptText("Partagez votre experience dans cet hotel...");
        commentArea.setWrapText(true);
        commentArea.setPrefRowCount(4);
        commentArea.setPrefWidth(430);


        form.add(hotelLabel, 0, 0);
        form.add(hotelValue, 1, 0);
        form.add(ratingLabel, 0, 1);
        form.add(ratingSpinner, 1, 1);
        form.add(commentLabel, 0, 2);
        form.add(commentArea, 1, 2);

        dialog.getDialogPane().setContent(form);
        styleClientDialog(dialog, false);

        Node saveButtonNode = dialog.getDialogPane().lookupButton(saveBtn);
        saveButtonNode.setDisable(true);
        commentArea.textProperty().addListener((obs, oldText, newText) ->
                saveButtonNode.setDisable(newText == null || newText.trim().isEmpty()));

        dialog.setResultConverter(btn -> {
            if (btn != saveBtn) return null;
            String comment = commentArea.getText() == null ? "" : commentArea.getText().trim();
            if (comment.isEmpty()) return null;

            int userId = resolveClientReviewUserId();
            return new HotelReview(0, ratingSpinner.getValue(), comment, userId, hotel.getId());
        });

        dialog.showAndWait().ifPresent(review -> {
            try {
                hotelReviewService.create(review);

                Alert success = new Alert(Alert.AlertType.INFORMATION);
                success.setTitle("Avis ajoute");
                success.setHeaderText(null);
                success.setContentText("Merci! Votre avis a ete publie.");
                success.setGraphic(null);
                styleClientDialog(success, false);
                success.showAndWait();

                if (onReviewSaved != null) {
                    onReviewSaved.run();
                }
            } catch (SQLException ex) {
                Alert error = new Alert(Alert.AlertType.ERROR);
                error.setTitle("Erreur");
                error.setHeaderText("Impossible d'ajouter l'avis");
                error.setContentText(ex.getMessage());
                error.setGraphic(null);
                styleClientDialog(error, false);
                error.showAndWait();
            }
        });
    }

    private int resolveClientReviewUserId() {
        try {
            Connection cnx = MyDBConnexion.getInstance().getConnection();
            if (cnx == null) {
                return 1;
            }

            DatabaseMetaData metaData = cnx.getMetaData();
            String catalog = cnx.getCatalog();
            List<String> tableCandidates = List.of("user", "users", "client", "clients", "utilisateur", "utilisateurs");
            List<String> idCandidates = List.of("id", "userId", "user_id", "clientId", "client_id");

            for (String table : tableCandidates) {
                String foundIdColumn = null;
                for (String idColumn : idCandidates) {
                    try (ResultSet columns = metaData.getColumns(catalog, null, table, idColumn)) {
                        if (columns.next()) {
                            foundIdColumn = idColumn;
                            break;
                        }
                    }
                }

                if (foundIdColumn != null) {
                    String sql = "SELECT `" + foundIdColumn + "` FROM `" + table + "` ORDER BY `" + foundIdColumn + "` ASC LIMIT 1";
                    try (PreparedStatement ps = cnx.prepareStatement(sql);
                         ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            int candidateId = rs.getInt(1);
                            if (candidateId > 0) {
                                return candidateId;
                            }
                        }
                    } catch (SQLException ignored) {
                        // Try next candidate table.
                    }
                }
            }

            String fallbackSql = "SELECT userId FROM hotel_review WHERE userId > 0 ORDER BY userId ASC LIMIT 1";
            try (PreparedStatement ps = cnx.prepareStatement(fallbackSql);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int candidateId = rs.getInt(1);
                    if (candidateId > 0) {
                        return candidateId;
                    }
                }
            }
        } catch (SQLException ignored) {
            // Keep default fallback.
        }
        return 1;
    }

    private StackPane createRoomCard(Room room, String hotelName) {
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
            List<RoomImage> images = roomImageService.getByRoomId(room.getId());
            if (!images.isEmpty()) {
                Image img = loadImage(images.get(0).getImageUrl());
                if (img != null) {
                    ImageView imageView = new ImageView(img);
                    imageView.setFitWidth(270);
                    imageView.setFitHeight(150);
                    imageView.setPreserveRatio(false);
                    header.getChildren().add(imageView);
                }
            }
        } catch (SQLException ignored) {}

        Label typeLabel = new Label(room.getRoomType());
        typeLabel.setStyle("-fx-background-color: rgba(0,0,0,0.6); -fx-text-fill: white; -fx-padding: 5 10; -fx-background-radius: 15; -fx-font-size: 11px;");
        StackPane.setAlignment(typeLabel, Pos.TOP_LEFT);
        StackPane.setMargin(typeLabel, new Insets(10));
        header.getChildren().add(typeLabel);
        
        // Price badge on image
        Label priceBadge = new Label(room.getPricePerNight() + " DT");
        priceBadge.setStyle("-fx-background-color: rgba(255,130,16,0.9); -fx-text-fill: white; -fx-padding: 5 10; -fx-background-radius: 15; -fx-font-size: 11px; -fx-font-weight: bold;");
        StackPane.setAlignment(priceBadge, Pos.TOP_RIGHT);
        StackPane.setMargin(priceBadge, new Insets(10));
        header.getChildren().add(priceBadge);

        VBox glassContent = new VBox(8);
        glassContent.getStyleClass().add("glass-card");
        glassContent.setPadding(new Insets(12));

        Label title = new Label("Chambre " + room.getRoomNumber());
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white;");

        Label hotelLabel = new Label(hotelName);
        hotelLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #679AC1;");

        HBox info = new HBox(15);
        info.setAlignment(Pos.CENTER_LEFT);

        Label capacity = new Label(room.getCapacity() + " pers.");
        capacity.setStyle("-fx-font-size: 12px; -fx-text-fill: rgba(255,255,255,0.8);");

        Label pricePerNight = new Label(room.getPricePerNight() + " DT/nuit");
        pricePerNight.setStyle("-fx-font-size: 12px; -fx-text-fill: #FF8210; -fx-font-weight: bold;");

        info.getChildren().addAll(capacity, pricePerNight);

        HBox statusRow = new HBox(8);
        statusRow.setAlignment(Pos.CENTER);
        statusRow.setPadding(new Insets(5, 0, 0, 0));

        Label status = new Label();
        switch (room.getStatus()) {
            case "AVAILABLE" -> { status.setText("Disponible"); status.getStyleClass().add("badge-available"); }
            case "OCCUPIED" -> { status.setText("Occupee"); status.getStyleClass().add("badge-occupied"); }
            case "MAINTENANCE" -> { status.setText("Maintenance"); status.getStyleClass().add("badge-maintenance"); }
        }

        Button detailsBtn = new Button("Details");
        detailsBtn.setStyle("-fx-background-color: #679AC1; -fx-text-fill: white; -fx-background-radius: 15; -fx-padding: 6 12; -fx-cursor: hand; -fx-font-size: 11px;");
        detailsBtn.setOnAction(e -> showRoomDetails(room, hotelName));

        Button bookBtn = new Button("Reserver");
        bookBtn.getStyleClass().add("btn-book");
        bookBtn.setStyle("-fx-font-size: 11px; -fx-padding: 6 12;");
        boolean isAvailable = room.getStatus().equals("AVAILABLE");
        bookBtn.setDisable(!isAvailable);
        if (isAvailable) {
            bookBtn.setOnAction(e -> showBookingConfirmation(room, hotelName));
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        statusRow.getChildren().addAll(status, spacer, detailsBtn, bookBtn);

        glassContent.getChildren().addAll(title, hotelLabel, info, statusRow);
        content.getChildren().addAll(header, glassContent);
        card.getChildren().add(content);

        // Hover effects
        card.setOnMouseEntered(e -> {
            card.setStyle("-fx-effect: dropshadow(gaussian, rgba(103,154,193,0.5), 20, 0, 0, 5); -fx-scale-x: 1.02; -fx-scale-y: 1.02;");
        });
        card.setOnMouseExited(e -> {
            card.setStyle("-fx-effect: none; -fx-scale-x: 1; -fx-scale-y: 1;");
        });

        card.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                showRoomDetails(room, hotelName);
            }
        });

        return card;
    }

    private void showRoomDetails(Room room, String hotelName) {
        showSearchPanel(false);
        hotelsContainer.getChildren().clear();
        sectionTitle.setText("Details chambre");

        VBox detailsBox = new VBox(25);
        detailsBox.setPadding(new Insets(10));
        detailsBox.setMaxWidth(1160);

        Button backBtn = new Button("Retour aux chambres");
        backBtn.getStyleClass().add("btn-book");
        backBtn.setStyle("-fx-background-color: #FF8210; -fx-font-size: 14px; -fx-padding: 12 25;");
        backBtn.setOnAction(e -> {
            currentView = "rooms";
            setupRoomsFilter();
            loadRooms();
        });

        // Main section with image gallery and info
        HBox mainSection = new HBox(25);
        mainSection.setAlignment(Pos.TOP_LEFT);

        // Left side - Image gallery
        VBox imageGallery = new VBox(10);
        imageGallery.setPrefWidth(520);

        StackPane mainImageContainer = new StackPane();
        mainImageContainer.setPrefSize(520, 330);
        mainImageContainer.setStyle("-fx-background-color: rgba(0,0,0,0.4); -fx-background-radius: 15;");
        applyRoundedClip(mainImageContainer, 24);

        ImageView mainImageView = new ImageView();
        mainImageView.setFitWidth(520);
        mainImageView.setFitHeight(330);
        mainImageView.setPreserveRatio(true);
        mainImageView.setSmooth(true);

        Label imagePlaceholder = new Label("R");
        imagePlaceholder.setStyle("-fx-font-size: 80px; -fx-text-fill: rgba(255,255,255,0.5);");

        try {
            List<RoomImage> images = roomImageService.getByRoomId(room.getId());
            if (!images.isEmpty()) {
                Image firstImg = loadImage(images.get(0).getImageUrl());
                if (firstImg != null) {
                    applyImageBestFit(mainImageView, firstImg, 520, 330);
                    mainImageContainer.getChildren().add(mainImageView);
                } else {
                    mainImageContainer.getChildren().add(imagePlaceholder);
                }

                // Thumbnails row
                if (images.size() > 1) {
                    final int[] currentIndex = {0};

                    Button prevBtn = new Button("<");
                    prevBtn.setStyle("-fx-background-color: rgba(0,0,0,0.55); -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold; -fx-background-radius: 20; -fx-min-width: 36; -fx-min-height: 36; -fx-cursor: hand;");
                    Button nextBtn = new Button(">");
                    nextBtn.setStyle("-fx-background-color: rgba(0,0,0,0.55); -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold; -fx-background-radius: 20; -fx-min-width: 36; -fx-min-height: 36; -fx-cursor: hand;");

                    prevBtn.setOnAction(ev -> {
                        currentIndex[0] = (currentIndex[0] - 1 + images.size()) % images.size();
                        Image newImg = loadImage(images.get(currentIndex[0]).getImageUrl());
                        if (newImg != null) applyImageBestFit(mainImageView, newImg, 520, 330);
                    });
                    nextBtn.setOnAction(ev -> {
                        currentIndex[0] = (currentIndex[0] + 1) % images.size();
                        Image newImg = loadImage(images.get(currentIndex[0]).getImageUrl());
                        if (newImg != null) applyImageBestFit(mainImageView, newImg, 520, 330);
                    });

                    StackPane.setAlignment(prevBtn, Pos.CENTER_LEFT);
                    StackPane.setAlignment(nextBtn, Pos.CENTER_RIGHT);
                    StackPane.setMargin(prevBtn, new Insets(0, 10, 0, 10));
                    StackPane.setMargin(nextBtn, new Insets(0, 10, 0, 10));
                    mainImageContainer.getChildren().addAll(prevBtn, nextBtn);

                    HBox thumbnails = new HBox(10);
                    thumbnails.setAlignment(Pos.CENTER_LEFT);

                    for (int i = 0; i < images.size(); i++) {
                        final int index = i;
                        Image thumbImg = loadImage(images.get(i).getImageUrl());
                        if (thumbImg != null) {
                            ImageView thumb = new ImageView(thumbImg);
                            thumb.setFitWidth(112);
                            thumb.setFitHeight(76);
                            thumb.setPreserveRatio(true);

                            StackPane thumbContainer = new StackPane(thumb);
                            thumbContainer.setStyle("-fx-background-color: rgba(0,0,0,0.3); -fx-background-radius: 8; -fx-padding: 3; -fx-cursor: hand;");
                            thumbContainer.setOnMouseEntered(ev -> thumbContainer.setStyle("-fx-background-color: #679AC1; -fx-background-radius: 8; -fx-padding: 3; -fx-cursor: hand;"));
                            thumbContainer.setOnMouseExited(ev -> thumbContainer.setStyle("-fx-background-color: rgba(0,0,0,0.3); -fx-background-radius: 8; -fx-padding: 3; -fx-cursor: hand;"));
                            thumbContainer.setOnMouseClicked(ev -> {
                                currentIndex[0] = index;
                                Image newImg = loadImage(images.get(index).getImageUrl());
                                if (newImg != null) applyImageBestFit(mainImageView, newImg, 520, 330);
                            });

                            thumbnails.getChildren().add(thumbContainer);
                        }
                    }
                    imageGallery.getChildren().addAll(mainImageContainer, thumbnails);
                } else {
                    imageGallery.getChildren().add(mainImageContainer);
                }
            } else {
                mainImageContainer.getChildren().add(imagePlaceholder);
                imageGallery.getChildren().add(mainImageContainer);
            }
        } catch (SQLException ex) {
            mainImageContainer.getChildren().add(imagePlaceholder);
            imageGallery.getChildren().add(mainImageContainer);
        }

        // Right side - Room info
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

        // Room features
        HBox featuresRow = new HBox(15);
        featuresRow.setAlignment(Pos.CENTER_LEFT);

        VBox typeBox = new VBox(3);
        typeBox.setStyle("-fx-background-color: rgba(103,154,193,0.2); -fx-background-radius: 10; -fx-padding: 10 15;");
        Label typeTitle = new Label("Type");
        typeTitle.setStyle("-fx-font-size: 11px; -fx-text-fill: rgba(255,255,255,0.6);");
        Label typeValue = new Label(room.getRoomType());
        typeValue.setStyle("-fx-font-size: 14px; -fx-text-fill: #679AC1; -fx-font-weight: bold;");
        typeBox.getChildren().addAll(typeTitle, typeValue);

        VBox capacityBox = new VBox(3);
        capacityBox.setStyle("-fx-background-color: rgba(103,154,193,0.2); -fx-background-radius: 10; -fx-padding: 10 15;");
        Label capacityTitle = new Label("Capacite");
        capacityTitle.setStyle("-fx-font-size: 11px; -fx-text-fill: rgba(255,255,255,0.6);");
        Label capacityValue = new Label(room.getCapacity() + " pers.");
        capacityValue.setStyle("-fx-font-size: 14px; -fx-text-fill: white; -fx-font-weight: bold;");
        capacityBox.getChildren().addAll(capacityTitle, capacityValue);

        featuresRow.getChildren().addAll(typeBox, capacityBox);

        // Price
        VBox priceBox = new VBox(3);
        priceBox.setStyle("-fx-background-color: rgba(255,130,16,0.15); -fx-background-radius: 12; -fx-padding: 15 20;");
        Label priceTitle = new Label("Prix par nuit");
        priceTitle.setStyle("-fx-font-size: 12px; -fx-text-fill: rgba(255,255,255,0.7);");
        Label priceValue = new Label(room.getPricePerNight() + " DT");
        priceValue.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #FF8210;");
        priceBox.getChildren().addAll(priceTitle, priceValue);

        Label statusLabel = new Label();
        statusLabel.setStyle("-fx-font-size: 14px; -fx-padding: 8 15;");
        switch (room.getStatus()) {
            case "AVAILABLE" -> { statusLabel.setText("Disponible"); statusLabel.getStyleClass().add("badge-available"); }
            case "OCCUPIED" -> { statusLabel.setText("Occupee"); statusLabel.getStyleClass().add("badge-occupied"); }
            case "MAINTENANCE" -> { statusLabel.setText("En maintenance"); statusLabel.getStyleClass().add("badge-maintenance"); }
        }

        Button bookBtn = new Button("Reserver maintenant");
        bookBtn.getStyleClass().add("btn-book");
        bookBtn.setStyle("-fx-font-size: 16px; -fx-padding: 15 30; -fx-background-radius: 25;");
        boolean isAvailable = room.getStatus().equals("AVAILABLE");
        bookBtn.setDisable(!isAvailable);
        if (isAvailable) {
            bookBtn.setOnAction(e -> showBookingConfirmation(room, hotelName));
        }

        infoBox.getChildren().addAll(roomTitle, hotelRow, featuresRow, priceBox, statusLabel, bookBtn);
        mainSection.getChildren().addAll(imageGallery, infoBox);

        detailsBox.getChildren().addAll(backBtn, mainSection);
        hotelsContainer.getChildren().add(detailsBox);
    }

    private void showBookingConfirmation(Room room, String hotelName) {
        showBookingConfirmation(room, hotelName, 1);
    }

    private void showBookingConfirmation(Room room, String hotelName, int nights) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation de reservation");
        alert.setHeaderText("Reserver la chambre " + room.getRoomNumber() + " ?");
        double total = room.getPricePerNight() * Math.max(1, nights);
        alert.setContentText(
                "Hotel: " + hotelName
                        + "\nType: " + room.getRoomType()
                        + "\nPrix: " + formatPrice(room.getPricePerNight()) + " DT/nuit"
                        + "\nNuits: " + Math.max(1, nights)
                        + "\nTotal estime: " + formatPrice(total) + " DT"
                        + "\nCapacite: " + room.getCapacity() + " personnes"
        );
        alert.setGraphic(null);
        styleClientDialog(alert, false);

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                Alert success = new Alert(Alert.AlertType.INFORMATION);
                success.setTitle("Reservation reussie");
                success.setHeaderText(null);
                success.setContentText(
                        "Votre reservation a ete effectuee avec succes!\n\n"
                                + "Chambre: " + room.getRoomNumber()
                                + "\nHotel: " + hotelName
                                + "\nNuits: " + Math.max(1, nights)
                                + "\nTotal: " + formatPrice(total) + " DT"
                );
                success.setGraphic(null);
                styleClientDialog(success, false);
                success.showAndWait();
            }
        });
    }

    private void styleClientDialog(Dialog<?> dialog, boolean destructivePrimary) {
        if (dialog == null || dialog.getDialogPane() == null) return;

        DialogPane pane = dialog.getDialogPane();
        URL cssUrl = getClass().getResource("/css/client-style.css");
        if (cssUrl != null) {
            String css = cssUrl.toExternalForm();
            if (!pane.getStylesheets().contains(css)) {
                pane.getStylesheets().add(css);
            }
        }

        if (!pane.getStyleClass().contains("gv-dialog")) {
            pane.getStyleClass().add("gv-dialog");
        }
        if (!pane.getStyleClass().contains("gv-client-dialog")) {
            pane.getStyleClass().add("gv-client-dialog");
        }

        Platform.runLater(() -> {
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
            }
        });
    }

    @FXML
    private void handleSearch() {
        if (!databaseAvailable) {
            showDatabaseUnavailableState();
            return;
        }
        if (currentView.equals("rooms")) {
            loadRooms();
        } else {
            loadHotels();
        }
    }

    @FXML
    private void handleFilter() {
        if (!databaseAvailable) {
            showDatabaseUnavailableState();
            return;
        }
        if (currentView.equals("rooms")) {
            loadRooms();
        } else {
            loadHotels();
        }
    }

    private void handleHover(Button targetBtn) {
        if (currentActiveBtn == targetBtn) return;

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
        double targetY = target.getLayoutY();
        TranslateTransition tt = new TranslateTransition(Duration.millis(400), slidingPane);
        tt.setToY(targetY);
        tt.setInterpolator(Interpolator.SPLINE(0.25, 0.1, 0.25, 1));
        tt.play();
    }

    private void animateText(Button btn, boolean activate) {
        ScaleTransition st = new ScaleTransition(Duration.millis(300), btn);
        st.setToX(activate ? 1.06 : 1.0);
        st.setToY(activate ? 1.12 : 1.0);
        st.setInterpolator(Interpolator.EASE_OUT);
        st.play();
    }

    private void applyLiquidDirection(Button target) {
        slidingPane.getStyleClass().removeAll("from-top", "from-bottom");
        if (currentActiveBtn == null) return;

        double oldY = currentActiveBtn.getLayoutY();
        double newY = target.getLayoutY();

        slidingPane.getStyleClass().add(newY > oldY ? "from-top" : "from-bottom");
    }
}

