package tn.esprit.projet.gui;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.json.JSONObject;
import tn.esprit.projet.entities.*;
import tn.esprit.projet.entities.Menu;
import tn.esprit.projet.services.*;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ClientRestaurantDetailController {

    @FXML
    private Label lblName, lblAddress, lblPhone, lblImageCounter, lblRatingValue;
    @FXML
    private ImageView mainCarouselImageView;
    @FXML
    private HBox thumbnailContainer, paginationContainer, starRatingContainer;
    @FXML
    private FlowPane menuFlowPane;
    @FXML
    private VBox reviewsContainer;
    @FXML
    private TextField txtSearchMenu;
    @FXML
    private TextArea txtComment;
    @FXML
    private Button btnSubmitReview, btnToggleAutoPlay;
    @FXML
    private StackPane carouselContainer;
    @FXML
    private ScrollPane rootScrollPane;


    @FXML
    private StackPane navigationOverlay;
    @FXML
    private Label distLabel;
    @FXML
    private Label timeLabel;
    @FXML
    private Label fullAddressLabel;
    @FXML
    private ImageView qrCodeView;

    // Translation Components
    @FXML
    private ComboBox<String> comboLanguage;
    private final Map<String, String> translationCache = new HashMap<>();

    private ClientController mainClientController;
    private final MenuService ms = new MenuService();
    private final MenuImageService mis = new MenuImageService();
    private final RestaurantImageService ris = new RestaurantImageService();
    private final RestaurantReviewService rrs = new RestaurantReviewService();

    private final List<String> imageList = new ArrayList<>();
    private List<Menu> allMenus = new ArrayList<>();
    private int currentIndex = 0;
    private int currentRestaurantId;
    private Restaurant currentRestaurant;
    private int currentSelectedRating = 5;
    private Timeline autoPlayTimeline;
    private boolean isAutoPlaying = true;
    private final int SESSION_USER_ID = 1;

    private final Map<Label, String> originalLabelText = new HashMap<>();
    private final Map<VBox, String> originalReviewText = new HashMap<>();

    private final Map<Node, String> originalTexts = new HashMap<>();

    private final String PLACEHOLDER_IMAGE = "/images/placeholder_dish.png";

    public void setMainClientController(ClientController controller) {
        this.mainClientController = controller;
    }

    private final NavigationService navService = new NavigationService();

    private UserService userService = new UserService();
    User currentUser = userService.getById(1);

    @FXML
    public void initialize() {
        mainCarouselImageView.fitWidthProperty().bind(carouselContainer.widthProperty());
        mainCarouselImageView.setFitHeight(450);

        if (txtSearchMenu != null) {
            txtSearchMenu.textProperty().addListener((obs, old, val) -> applyMenuFilters());
        }

        setupStarRating();
        setupLanguageSelector();
    }

    private void setupLanguageSelector() {
        if (comboLanguage != null) {
            comboLanguage.setItems(FXCollections.observableArrayList("Français", "English", "العربية", "Deutsch", "Español"));
            comboLanguage.setValue("Français");

            // Listeners are more robust than setOnAction for ComboBoxes
            comboLanguage.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null && !newVal.equals(oldVal)) {
                    System.out.println("Language selection detected: " + newVal);
                    translatePageContent();
                }
            });
        } else {
            System.err.println("CRITICAL: comboLanguage is NULL. Check fx:id in FXML.");
        }
    }

    public void setRestaurantData(Restaurant restaurant) {
        if (restaurant == null) return;
        this.currentRestaurant = restaurant;
        this.currentRestaurantId = restaurant.getId();
        lblName.setText(restaurant.getName());
        lblAddress.setText("📍 " + restaurant.getAddress());
        lblPhone.setText("📞 " + restaurant.getPhone());

        loadGallery(restaurant.getId());
        loadMenus(restaurant.getId());
        loadReviews(restaurant.getId());
        setupAutoPlay();
    }

    // --- TRANSLATION LOGIC ---
    private void translatePageContent() {

        String targetLang = getLangCode(comboLanguage.getValue());
        System.out.println("=== TRANSLATION STARTED ===");
        System.out.println("Target Language: " + targetLang);

        if (targetLang.equals("fr")) {
            System.out.println("Switching back to French. Reloading original data.");
            loadMenus(currentRestaurantId);
            loadReviews(currentRestaurantId);
            return;
        }

        new Thread(() -> {

            try {

                // Store original header text only once
                originalLabelText.putIfAbsent(lblName, lblName.getText());
                originalLabelText.putIfAbsent(lblAddress, lblAddress.getText());

                String originalName = originalLabelText.get(lblName);
                String originalAddress = originalLabelText.get(lblAddress);

                String translatedName = fetchTranslation(originalName, targetLang);
                String translatedAddress = fetchTranslation(originalAddress, targetLang);

                Platform.runLater(() -> {
                    lblName.setText(translatedName);
                    lblAddress.setText(translatedAddress);

                    menuFlowPane.getChildren().clear();
                    for (Menu m : allMenus) {
                        menuFlowPane.getChildren().add(createTranslatedMenuCard(m, targetLang));
                    }

                    translateReviews(targetLang);
                    translateAllNodes((Parent) rootScrollPane.getContent(), targetLang);
                });

            } catch (Exception e) {
                System.err.println("ERROR in translatePageContent: " + e.getMessage());
                e.printStackTrace();
            }

        }).start();
    }

    private String fetchTranslation(String text, String target) {

        if (text == null || text.isBlank()) {
            System.out.println("Skipped empty text.");
            return text;
        }

        String cacheKey = target + ":" + text;
        if (translationCache.containsKey(cacheKey)) {
            System.out.println("Cache hit for: " + text);
            return translationCache.get(cacheKey);
        }

        try {

            String encodedText = java.net.URLEncoder.encode(text, "UTF-8");

            String url = "https://api.mymemory.translated.net/get?q="
                    + encodedText
                    + "&langpair=" +
                    java.net.URLEncoder.encode("fr|" + target, "UTF-8");

            System.out.println("Calling API:");
            System.out.println(url);

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(java.time.Duration.ofSeconds(10))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(java.time.Duration.ofSeconds(15))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            System.out.println("Status Code: " + response.statusCode());
            System.out.println("Response Body: " + response.body());

            if (response.statusCode() == 200) {

                JSONObject json = new JSONObject(response.body());

                if (json.has("responseData")) {

                    String translated = json
                            .getJSONObject("responseData")
                            .getString("translatedText");

                    System.out.println("Translated: " + translated);

                    translationCache.put(cacheKey, translated);
                    return translated;

                } else {
                    System.out.println("responseData missing in JSON.");
                }

            } else {
                System.out.println("Non-200 status received.");
            }

        } catch (Exception e) {
            System.err.println("Translation ERROR:");
            e.printStackTrace();
        }

        System.out.println("Returning original text due to failure.");
        return text;
    }

    private String getLangCode(String language) {
        return switch (language) {
            case "English" -> "en";
            case "العربية" -> "ar";
            case "Deutsch" -> "de";
            case "Español" -> "es";
            default -> "fr";
        };
    }

    private HBox createTranslatedMenuCard(Menu m, String lang) {
        HBox card = createMenuCard(m); // Now returns HBox

        // info is the second child of HBox, name is the first child of info
        VBox info = (VBox) card.getChildren().get(1);
        Label nameLabel = (Label) info.getChildren().get(0);

        originalLabelText.putIfAbsent(nameLabel, m.getName());

        new Thread(() -> {
            String originalText = originalLabelText.get(nameLabel);
            String translated = fetchTranslation(originalText, lang);
            Platform.runLater(() -> nameLabel.setText(translated));
        }).start();

        return card;
    }

    private void translateReviews(String lang) {

        reviewsContainer.getChildren().forEach(node -> {

            if (node instanceof VBox box) {

                Label commentLabel = (Label) box.getChildren().get(1);

                originalReviewText.putIfAbsent(box, commentLabel.getText());

                String originalText = originalReviewText.get(box);

                new Thread(() -> {

                    String translated = fetchTranslation(originalText, lang);

                    Platform.runLater(() ->
                            commentLabel.setText(translated)
                    );

                }).start();
            }
        });
    }

    // --- NAVIGATION & UTILS (Existing Logic) ---
    @FXML
    private void handleBack() {
        if (autoPlayTimeline != null) autoPlayTimeline.stop();
        if (mainClientController != null) {
            mainClientController.loadSection("/ClientRestaurantView.fxml");
        }
    }

    private void displayImage(int index) {
        if (imageList.isEmpty()) return;
        currentIndex = index;
        Image img = new Image(imageList.get(index), 0, 450, true, true, true);

        FadeTransition ft = new FadeTransition(Duration.millis(250), mainCarouselImageView);
        ft.setFromValue(1.0);
        ft.setToValue(0.2);
        ft.setOnFinished(e -> {
            mainCarouselImageView.setImage(img);
            FadeTransition fi = new FadeTransition(Duration.millis(250), mainCarouselImageView);
            fi.setFromValue(0.2);
            fi.setToValue(1.0);
            fi.play();
        });
        ft.play();

        lblImageCounter.setText((index + 1) + " / " + imageList.size());
        updatePaginationDots(index);
    }

    private void updatePaginationDots(int activeIndex) {
        paginationContainer.getChildren().clear();
        for (int i = 0; i < imageList.size(); i++) {
            Circle dot = new Circle(i == activeIndex ? 6 : 4);
            dot.setFill(i == activeIndex ? Color.web("#FF8210") : Color.GRAY);
            paginationContainer.getChildren().add(dot);
        }
    }

    @FXML
    private void handleNextImage() {
        if (!imageList.isEmpty()) displayImage((currentIndex + 1) % imageList.size());
    }

    @FXML
    private void handlePrevImage() {
        if (!imageList.isEmpty()) displayImage((currentIndex - 1 + imageList.size()) % imageList.size());
    }

    @FXML
    private void toggleAutoPlay() {
        isAutoPlaying = !isAutoPlaying;
        if (isAutoPlaying) autoPlayTimeline.play();
        else autoPlayTimeline.stop();
        btnToggleAutoPlay.setText(isAutoPlaying ? "⏸" : "▶");
    }

    private void setupAutoPlay() {
        if (autoPlayTimeline != null) autoPlayTimeline.stop();
        autoPlayTimeline = new Timeline(new KeyFrame(Duration.seconds(5), e -> handleNextImage()));
        autoPlayTimeline.setCycleCount(Timeline.INDEFINITE);
        if (isAutoPlaying) autoPlayTimeline.play();
    }

    @FXML
    private void handleFullScreen() {
        if (mainCarouselImageView.getImage() != null) openFullScreenWindow(mainCarouselImageView.getImage());
    }

    private void openFullScreenWindow(Image img) {
        Stage stage = new Stage(StageStyle.UNDECORATED);
        ImageView fullView = new ImageView(img);
        fullView.setPreserveRatio(true);
        fullView.fitWidthProperty().bind(stage.widthProperty());
        fullView.fitHeightProperty().bind(stage.heightProperty());
        StackPane root = new StackPane(fullView);
        root.setStyle("-fx-background-color: black;");
        Scene scene = new Scene(root);
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) stage.close();
        });
        root.setOnMouseClicked(e -> stage.close());
        stage.setScene(scene);
        stage.setFullScreen(true);
        stage.show();
    }

    private void applyMenuFilters() {
        menuFlowPane.getChildren().clear();
        String search = txtSearchMenu.getText().toLowerCase();
        List<Menu> filtered = allMenus.stream()
                .filter(m -> m.getName().toLowerCase().contains(search))
                .collect(Collectors.toList());
        for (Menu m : filtered) menuFlowPane.getChildren().add(createMenuCard(m));
    }

    private HBox createMenuCard(Menu m) {
        // 1. Create the Main Card Container
        HBox card = new HBox(20);
        card.getStyleClass().add("menu-card");
        card.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        // RESPONSIVE: Card fills the width of the menuFlowPane automatically
        card.prefWidthProperty().bind(menuFlowPane.widthProperty().subtract(25));
        card.setMaxWidth(Double.MAX_VALUE);

        // 2. Dish Image with Placeholder logic
        ImageView iv = new ImageView();
        iv.setFitWidth(150);
        iv.setFitHeight(110);
        iv.setPreserveRatio(false);
        iv.getStyleClass().add("menu-image");

        // Clip for rounded corners on the image
        javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(150, 110);
        clip.setArcWidth(15);
        clip.setArcHeight(15);
        iv.setClip(clip);

        try {
            List<MenuImage> imgs = mis.getByMenuId(m.getId());
            if (imgs != null && !imgs.isEmpty()) {
                // Load actual image
                Image img = new Image(imgs.get(0).getImageUrl(), 150, 110, true, true, true);
                iv.setImage(img);
            } else {
                // Load Placeholder Image
                iv.setImage(new Image(getClass().getResourceAsStream("/images/placeholder_dish.png")));
            }
        } catch (Exception e) {
            // Fallback for missing resources
            System.out.println("Image error: " + e.getMessage());
        }

        // Full Screen functionality on image click
        iv.setOnMouseClicked(e -> {
            if (iv.getImage() != null) openFullScreenWindow(iv.getImage());
        });

        // 3. Details Container
        VBox info = new VBox(8);
        info.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        HBox.setHgrow(info, Priority.ALWAYS); // Stretch info to fill horizontal space

        Label name = new Label(m.getName());
        name.getStyleClass().add("menu-item-name");

        Label description = new Label(m.getDescription());
        description.getStyleClass().add("menu-item-description");
        description.setWrapText(true);

        Label price = new Label(String.format("%.2f TND", m.getPrice()));
        price.getStyleClass().add("menu-item-price");

        info.getChildren().addAll(name, description, price);

        // 4. Assemble
        card.getChildren().addAll(iv, info);

        return card;
    }

    @FXML
    private void handleSubmitReview() {
        String comment = txtComment.getText().trim();
        if (comment.isEmpty()) return;
        try {
            RestaurantReview rev = new RestaurantReview();
            rev.setRestaurantId(currentRestaurantId);
            rev.setUserId(SESSION_USER_ID);
            rev.setRating(currentSelectedRating);
            rev.setComment(comment);
            rev.setCreatedAt(LocalDateTime.now());
            rrs.create(rev);
            loadReviews(currentRestaurantId);
            txtComment.clear();
        } catch (SQLException e) {
        }
    }

    private void setupStarRating() {
        starRatingContainer.getChildren().clear();
        for (int i = 1; i <= 5; i++) {
            Label star = new Label("★");
            star.setStyle("-fx-font-size: 30; -fx-text-fill: #444; -fx-cursor: hand;");
            int val = i;
            star.setOnMouseClicked(e -> {
                currentSelectedRating = val;
                lblRatingValue.setText(val + " / 5");
                for (int j = 0; j < 5; j++) {
                    ((Label) starRatingContainer.getChildren().get(j)).setStyle(j < val ?
                            "-fx-text-fill: #FF8210; -fx-font-size: 30;" : "-fx-text-fill: #444; -fx-font-size: 30;");
                }
            });
            starRatingContainer.getChildren().add(star);
        }
    }

    @FXML
    private void handleReservation() {
        System.out.println("Reservation triggered.");
    }

    private void loadGallery(int id) {
        try {
            imageList.clear();
            thumbnailContainer.getChildren().clear();
            ris.getByRestaurantId(id).forEach(img -> {
                imageList.add(img.getImageUrl());
                ImageView thumb = new ImageView(new Image(img.getImageUrl(), 100, 70, true, true));
                thumb.setStyle("-fx-cursor: hand; -fx-opacity: 0.8; -fx-border-color: #333;");
                thumb.setOnMouseClicked(e -> displayImage(imageList.indexOf(img.getImageUrl())));
                thumbnailContainer.getChildren().add(thumb);
            });
            if (!imageList.isEmpty()) displayImage(0);
        } catch (SQLException e) {
        }
    }

    private void loadMenus(int id) {
        try {
            allMenus = ms.getAll().stream().filter(m -> m.getRestaurantId() == id).collect(Collectors.toList());
            applyMenuFilters();
        } catch (SQLException e) {
        }
    }

    private void loadReviews(int id) {
        try {
            reviewsContainer.getChildren().clear();
            rrs.getAll().stream().filter(r -> r.getRestaurantId() == id).forEach(r -> {
                VBox box = new VBox(5);
                box.setStyle("-fx-background-color: #161b22; -fx-padding: 12; -fx-background-radius: 10;");
                Label rStars = new Label("★".repeat(r.getRating()));
                rStars.setStyle("-fx-text-fill: #FF8210;");
                Label rComment = new Label(r.getComment());
                rComment.setStyle("-fx-text-fill: #ccc;");
                rComment.setWrapText(true);
                box.getChildren().addAll(rStars, rComment);
                reviewsContainer.getChildren().add(box);
            });
        } catch (SQLException e) {
        }
    }

    private void translateAllNodes(Parent root, String targetLang) {

        for (Node node : root.getChildrenUnmodifiable()) {

            if (node instanceof Label label) {
                translateLabel(label, targetLang);
            } else if (node instanceof Button button) {
                translateButton(button, targetLang);
            } else if (node instanceof TextField textField) {
                translateTextField(textField, targetLang);
            } else if (node instanceof TextArea textArea) {
                translateTextArea(textArea, targetLang);
            } else if (node instanceof Parent parent) {
                translateAllNodes(parent, targetLang);
            }
        }
    }

    private void translateLabel(Label label, String lang) {

        String text = label.getText();
        if (text == null || text.isBlank()) return;

        originalTexts.putIfAbsent(label, text);
        String original = originalTexts.get(label);

        new Thread(() -> {
            String translated = fetchTranslation(original, lang);
            Platform.runLater(() -> label.setText(translated));
        }).start();
    }

    private void translateButton(Button button, String lang) {

        String text = button.getText();
        if (text == null || text.isBlank() || text.equals("›")) return;

        originalTexts.putIfAbsent(button, text);
        String original = originalTexts.get(button);

        new Thread(() -> {
            String translated = fetchTranslation(original, lang);
            Platform.runLater(() -> button.setText(translated));
        }).start();
    }

    private void translateTextField(TextField field, String lang) {

        String prompt = field.getPromptText();
        if (prompt == null || prompt.isBlank()) return;

        originalTexts.putIfAbsent(field, prompt);
        String original = originalTexts.get(field);

        new Thread(() -> {
            String translated = fetchTranslation(original, lang);
            Platform.runLater(() -> field.setPromptText(translated));
        }).start();
    }

    private void translateTextArea(TextArea area, String lang) {

        String prompt = area.getPromptText();
        if (prompt == null || prompt.isBlank()) return;

        originalTexts.putIfAbsent(area, prompt);
        String original = originalTexts.get(area);

        new Thread(() -> {
            String translated = fetchTranslation(original, lang);
            Platform.runLater(() -> area.setPromptText(translated));
        }).start();
    }

    @FXML
    private void handleSetDestination() {
        // 1. Refresh user from DB to get the latest position (lat,lon|Address Name)
        currentUser = userService.getById(1);

        try {
            // 2. Clean and Parse Restaurant Coordinates (The Destination)
            String cleanAddress = currentRestaurant.getAddress().replace("📍", "").trim();
            String[] rParts = cleanAddress.split(",");
            double rLat = Double.parseDouble(rParts[0].trim());
            double rLon = Double.parseDouble(rParts[1].trim());

            String mapUrl;

            // 3. LOGIC CHECK: Is the user position set?
            if (currentUser == null || currentUser.getPosition() == null || currentUser.getPosition().isEmpty()) {

                // CASE A: Position NOT set -> Use "Current Location" mode
                mapUrl = "https://www.google.com/maps/dir/?api=1&destination=" + rLat + "," + rLon + "&travelmode=walking";

                distLabel.setText("-- km");
                timeLabel.setText("Calcul via Mobile");
                fullAddressLabel.setText("Position non définie (Utilise GPS mobile)");
                distLabel.setTextFill(javafx.scene.paint.Color.GRAY);

            } else {

                // CASE B: Position IS set -> Extract Coords and Name
                String dbPosition = currentUser.getPosition();
                String coordsPart;
                String namePart;

                // Split the string based on the pipe separator "|"
                if (dbPosition.contains("|")) {
                    String[] parts = dbPosition.split("\\|");
                    coordsPart = parts[0]; // e.g., "36.8,10.1"
                    namePart = parts[1];   // e.g., "Tunis, Tunisia"
                } else {
                    // Fallback for old data format
                    coordsPart = dbPosition;
                    namePart = dbPosition;
                }

                // Parse coordinates for calculation
                String[] uParts = coordsPart.split(",");
                double uLat = Double.parseDouble(uParts[0].trim());
                double uLon = Double.parseDouble(uParts[1].trim());

                // Calculate Distance/Time for the Desktop UI
                double dist = navService.calculateDistance(uLat, uLon, rLat, rLon);
                int walkingTime = (int) (dist / 5.0 * 60);

                // URL including the specific origin saved in your DB
                mapUrl = "https://www.google.com/maps/dir/?api=1&origin=" + uLat + "," + uLon
                        + "&destination=" + rLat + "," + rLon + "&travelmode=walking";

                // Update UI with calculated stats
                distLabel.setText(String.format("%.2f km", dist));
                timeLabel.setText(walkingTime + " min");

                // --- UPDATED LABEL: Use namePart instead of raw coordinates ---
                fullAddressLabel.setText("Itinéraire de " + namePart + " vers " + currentRestaurant.getName());

                distLabel.setTextFill(dist < 2.0 ? javafx.scene.paint.Color.web("#4CAF50") : javafx.scene.paint.Color.web("#FF8210"));
            }

            // 4. Generate the QR Code with the chosen URL
            qrCodeView.setImage(navService.generateQRCode(mapUrl));

            // 5. Show Overlay with Animation
            navigationOverlay.setVisible(true);
            javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(javafx.util.Duration.millis(300), navigationOverlay);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.play();

        } catch (Exception e) {
            System.err.println("Error calculating route: " + e.getMessage());
            showFallbackNavigation();
        }
    }

    private void showFallbackNavigation() {
        // Fallback: Just search for the restaurant name if coordinates fail
        try {
            String encodedName = java.net.URLEncoder.encode(currentRestaurant.getName(), "UTF-8");
            String mapUrl = "https://www.google.com/maps/search/?api=1&query=" + encodedName;

            qrCodeView.setImage(navService.generateQRCode(mapUrl));
            distLabel.setText("-- km");
            timeLabel.setText("Voir sur mobile");
            fullAddressLabel.setText("Localisation de " + currentRestaurant.getName());
            navigationOverlay.setVisible(true);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    private void handleCloseNavigation() {
        navigationOverlay.setVisible(false);
    }
}