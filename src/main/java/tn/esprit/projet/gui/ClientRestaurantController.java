package tn.esprit.projet.gui;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import tn.esprit.projet.entities.*;
import tn.esprit.projet.entities.Menu;
import tn.esprit.projet.services.*;
import tn.esprit.projet.test.App;
import tn.esprit.projet.utils.CuisineWikiService;


import javafx.event.ActionEvent;
import tn.esprit.projet.utils.SessionManager;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;



public class ClientRestaurantController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> categoryFilter;
    @FXML private ComboBox<String> scoreFilter;
    @FXML private FlowPane restaurantGrid;

    @FXML private VBox aiPanel;
    @FXML private TextArea aiInputField;
    @FXML private Label aiResponseLabel;
    @FXML private Label aiStatusLabel;
    @FXML private ProgressBar aiProgress;
    @FXML private VBox loadingOverlay;

    @FXML private VBox locationSearchOverlay;
    @FXML private TextField txtLocationSearch;
    @FXML private ListView<String> listLocationResults;

    private final RestaurantService rs = new RestaurantService();
    private final RestaurantImageService ris = new RestaurantImageService();
    private final RestaurantReviewService rrs = new RestaurantReviewService();
    private final MenuService ms = new MenuService();

    private final CuisineWikiService wikiService = new CuisineWikiService();
    private final Map<String, CuisineWikiService.WikiData> wikiCache = new HashMap<>();

    private ClientController mainClientController;

    private List<Restaurant> allRestaurants = new ArrayList<>();
    private List<RestaurantReview> allReviews = new ArrayList<>();
    private List<Menu> allMenusCache = new ArrayList<>();

    private final tn.esprit.projet.services.LocalAIService aiService = new tn.esprit.projet.services.LocalAIService();
    private final javafx.animation.PauseTransition aiAutomationTimer = new javafx.animation.PauseTransition(Duration.seconds(1.5));
    private List<Integer> aiRecommendedRestaurantIds = new ArrayList<>();
    private Map<String, String> searchResults = new HashMap<>();

    private User currentUser;
    private UserService userService = new UserService();
    private Timeline searchThrottle;




    @FXML
    public void initialize() {

        currentUser = userService.getById(1);

        loadDataFromDatabase();

        searchField.textProperty().addListener((obs, old, val) -> applyDeepFilters());
        categoryFilter.valueProperty().addListener((obs, old, val) -> applyDeepFilters());
        scoreFilter.valueProperty().addListener((obs, old, val) -> applyDeepFilters());

        aiInputField.textProperty().addListener((obs, old, val) -> {
            aiProgress.setProgress(-1); // Indeterminate (spinning)
            aiStatusLabel.setText("L'IA analyse votre demande...");
            aiAutomationTimer.playFromStart();
        });
        aiInputField.setOnKeyPressed(event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.ENTER) {
                if (!event.isShiftDown()) { // Allow Shift+Enter for new lines
                    event.consume();
                    aiAutomationTimer.stop(); // Cancel the auto-timer
                    runAIWorkflow();          // Run immediately
                }
            }
        });

        txtLocationSearch.textProperty().addListener((obs, old, newVal) -> {
            if (searchThrottle != null) searchThrottle.stop();
            searchThrottle = new Timeline(new KeyFrame(Duration.millis(500), e -> {
                if (newVal.length() > 2) {
                    fetchAutocomplete(newVal);
                }
            }));
            searchThrottle.play();
        });

        listLocationResults.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                txtLocationSearch.setText(newVal); // Put the selected name in the box
                listLocationResults.setVisible(false); // Hide the list
                listLocationResults.setManaged(false);
            }
        });
    }

    public void setMainClientController(ClientController mainClientController) {
        this.mainClientController = mainClientController;
    }

    private void loadDataFromDatabase() {
        try {
            allRestaurants = rs.getAll().stream()
                    .filter(r -> "OPEN".equalsIgnoreCase(r.getStatus()))
                    .collect(Collectors.toList());

            allReviews = rrs.getAll();
            allMenusCache = ms.getAll();

            populateFilters();
            applyDeepFilters();
        } catch (SQLException e) {
            System.err.println("Error loading restaurant data: " + e.getMessage());
        }
    }

    private void populateFilters() {
        List<String> categories = allRestaurants.stream()
                .map(Restaurant::getCategory)
                .filter(c -> c != null && !c.isEmpty())
                .distinct()
                .collect(Collectors.toList());

        categoryFilter.getItems().clear();
        categoryFilter.getItems().add("Toutes les catégories");
        categoryFilter.getItems().addAll(categories);
        categoryFilter.setValue("Toutes les catégories");

        categoryFilter.setCellFactory(lv -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    HBox container = new HBox(12);
                    container.setAlignment(Pos.CENTER_LEFT);
                    container.setPadding(new Insets(5, 10, 5, 10));

                    Label nameLabel = new Label(item);
                    nameLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");

                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);
                    container.getChildren().addAll(nameLabel, spacer);

                    if (!item.equals("Toutes les catégories")) {
                        Label infoIcon = new Label("ⓘ");
                        infoIcon.setStyle("-fx-text-fill: #FF8210; -fx-cursor: hand; -fx-font-size: 15px; -fx-font-weight: bold;");

                        Tooltip wikiTooltip = new Tooltip("Recherche d'informations...");
                        wikiTooltip.setPrefWidth(350);
                        wikiTooltip.setWrapText(true);
                        wikiTooltip.setContentDisplay(ContentDisplay.TOP);

                        wikiTooltip.setShowDelay(Duration.ZERO);
                        wikiTooltip.setHideDelay(Duration.INDEFINITE);
                        wikiTooltip.setShowDuration(Duration.hours(1));
                        Tooltip.install(infoIcon, wikiTooltip);

                        infoIcon.setOnMouseEntered(e -> {
                            if (wikiCache.containsKey(item)) {
                                updateTooltipUI(wikiTooltip, wikiCache.get(item));
                            } else {
                                wikiService.getCuisineInfo(item)
                                        .thenAccept(data -> {
                                            wikiCache.put(item, data);
                                            Platform.runLater(() -> updateTooltipUI(wikiTooltip, data));
                                        })
                                        .exceptionally(ex -> {
                                            ex.printStackTrace();
                                            return null;
                                        });
                            }
                        });
                        container.getChildren().add(infoIcon);
                    }
                    setGraphic(container);
                }
            }
        });

        categoryFilter.setButtonCell(new ListCell<String>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(item);
                setStyle("-fx-text-fill: white;");
            }
        });

        scoreFilter.getItems().clear();
        scoreFilter.getItems().addAll("Toutes les notes", "4.5+ ⭐", "4.0+ ⭐", "3.0+ ⭐");
        scoreFilter.setValue("Toutes les notes");
    }

    public void makeTooltipStay(Node node, String text) {
        Tooltip tooltip = new Tooltip(text);

        // Set tooltip to appear immediately and stay for a long time
        tooltip.setShowDelay(Duration.ZERO);          // no delay to show
        tooltip.setHideDelay(Duration.INDEFINITE);    // don't hide automatically
        tooltip.setShowDuration(Duration.hours(1));   // stays for 1 hour

        // Attach tooltip to the node
        Tooltip.install(node, tooltip);
    }

    private void updateTooltipUI(Tooltip tooltip, CuisineWikiService.WikiData data) {

        VBox container = new VBox(10);
        container.setPadding(new Insets(10));
        container.setPrefWidth(320);
        container.setStyle("-fx-background-color: #2b2b2b; -fx-background-radius: 10;");

        // ===== IMAGE =====
        Image image = null;
        if (data.imageUrl != null && !data.imageUrl.isEmpty()) {
            try {
                image = new Image(data.imageUrl, 300, 180, true, true, true);

                ImageView imageView = new ImageView(image);
                imageView.setFitWidth(300);
                imageView.setFitHeight(180);
                imageView.setPreserveRatio(true);


                //Rectangle clip = new Rectangle(300, 180);
                //clip.setArcWidth(20);
                //clip.setArcHeight(20);
                //imageView.setClip(clip);

                container.getChildren().add(imageView);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // ===== TEXT =====
        Label descriptionLabel = new Label(data.description);
        descriptionLabel.setWrapText(true);
        descriptionLabel.setMaxWidth(300);
        descriptionLabel.setStyle("-fx-text-fill: white; -fx-font-size: 13px;");

        container.getChildren().add(descriptionLabel);

        tooltip.setGraphic(container);
        tooltip.setText(""); // IMPORTANT: remove default text
        System.out.println("Image URL: " + data.imageUrl);
        System.out.println("Image error: " + image.isError());

    }

    @FXML
    private void handleSwitchRole() {
        // You can add a confirmation alert here if you want
        App.showAdminView();
    }


    private void applyDeepFilters() {
        String query = (searchField.getText() == null) ? "" : searchField.getText().toLowerCase().trim();
        String selectedCat = (categoryFilter.getValue() == null) ? "Toutes les catégories" : categoryFilter.getValue();
        String selectedScore = (scoreFilter.getValue() == null) ? "Toutes les notes" : scoreFilter.getValue();

        List<Restaurant> filtered = allRestaurants.stream()
                .filter(r -> {
                    // --- STEP 1: AI OVERRIDE ---
                    // If the AI has generated a recommendation list, we filter STRICTLY by that list.
                    // This ensures the "Solid Response" you requested.
                    if (aiRecommendedRestaurantIds != null && !aiRecommendedRestaurantIds.isEmpty()) {
                        return aiRecommendedRestaurantIds.contains(r.getId());
                    }

                    // --- STEP 2: MANUAL FILTERS (Fallback) ---
                    // This code only runs if the AI list is empty or hasn't triggered yet.

                    // A. Category Filter
                    boolean matchCat = selectedCat.equals("Toutes les catégories") ||
                            (r.getCategory() != null && r.getCategory().equalsIgnoreCase(selectedCat));
                    if (!matchCat) return false;

                    // B. Rating Filter
                    double avg = calculateAverage(r.getId());
                    if (selectedScore.equals("4.5+ ⭐") && avg < 4.5) return false;
                    if (selectedScore.equals("4.0+ ⭐") && avg < 4.0) return false;
                    if (selectedScore.equals("3.0+ ⭐") && avg < 3.0) return false;

                    // C. Search Bar Logic
                    if (query.isEmpty()) return true;

                    // Basic Restaurant Info Match
                    boolean basicMatch = (r.getName() != null && r.getName().toLowerCase().contains(query)) ||
                            (r.getDestinationName() != null && r.getDestinationName().toLowerCase().contains(query)) ||
                            (r.getAddress() != null && r.getAddress().toLowerCase().contains(query));
                    if (basicMatch) return true;

                    // Deep Menu Search (Matches items even if restaurant name doesn't match)
                    return allMenusCache.stream()
                            .filter(m -> m.getRestaurantId() == r.getId())
                            .anyMatch(m -> (m.getName() != null && m.getName().toLowerCase().contains(query)) ||
                                    (m.getDescription() != null && m.getDescription().toLowerCase().contains(query)));
                })
                // --- STEP 3: SORTING ---
                .sorted((r1, r2) -> {
                    // Keep AI matches at the top even if other filters are active
                    int p1 = (aiRecommendedRestaurantIds != null && aiRecommendedRestaurantIds.contains(r1.getId())) ? 0 : 1;
                    int p2 = (aiRecommendedRestaurantIds != null && aiRecommendedRestaurantIds.contains(r2.getId())) ? 0 : 1;

                    int compare = Integer.compare(p1, p2);
                    if (compare == 0) {
                        // Alphabetical fallback
                        return r1.getName().compareToIgnoreCase(r2.getName());
                    }
                    return compare;
                })
                .collect(Collectors.toList());

        updateUI(filtered);
    }

    private double calculateAverage(int restaurantId) {
        return allReviews.stream()
                .filter(rev -> rev.getRestaurantId() == restaurantId)
                .mapToInt(RestaurantReview::getRating)
                .average().orElse(0.0);
    }

    private void updateUI(List<Restaurant> list) {
        restaurantGrid.getChildren().clear();
        if (list.isEmpty()) {
            Label placeholder = new Label("Aucun établissement ne correspond à vos critères.");
            placeholder.setStyle("-fx-text-fill: gray; -fx-font-style: italic; -fx-padding: 20;");
            restaurantGrid.getChildren().add(placeholder);
        } else {
            double delay = 0;
            for (Restaurant r : list) {
                VBox card = buildCard(r);

                // 2. Prepare for animation (start invisible and slightly lower)
                card.setOpacity(0);
                card.setTranslateY(15);

                // 3. Add to grid
                restaurantGrid.getChildren().add(card);

                // 4. Create the animation
                javafx.animation.FadeTransition fade = new javafx.animation.FadeTransition(Duration.millis(400), card);
                fade.setFromValue(0);
                fade.setToValue(1);

                javafx.animation.TranslateTransition move = new javafx.animation.TranslateTransition(Duration.millis(400), card);
                move.setFromY(15);
                move.setToY(0);

                // 5. Play together with a slight delay for each card (staggered effect)
                javafx.animation.ParallelTransition parallel = new javafx.animation.ParallelTransition(fade, move);
                parallel.setDelay(Duration.millis(delay));
                parallel.play();

                delay += 50; // Increase delay for the next card (50ms)
            }
        }
    }

    private VBox buildCard(Restaurant r) {
        VBox card = new VBox(0);
        card.getStyleClass().add("restaurant-card");
        card.setPrefWidth(300);

        StackPane topArea = new StackPane();
        ImageView iv = new ImageView();
        iv.setFitWidth(300); iv.setFitHeight(185);
        Rectangle clip = new Rectangle(300, 185); clip.setArcWidth(45); clip.setArcHeight(45);
        iv.setClip(clip);

        try {
            List<RestaurantImage> imgs = ris.getByRestaurantId(r.getId());
            String path = imgs.isEmpty() ? "/assets/placeholder.png" : imgs.get(0).getImageUrl();
            iv.setImage(new Image(path, true));
        } catch (Exception e) { iv.setImage(new Image("/assets/placeholder.png")); }

        Button like = new Button("❤"); like.getStyleClass().add("btn-like");
        StackPane.setAlignment(like, Pos.TOP_RIGHT); StackPane.setMargin(like, new Insets(12));

        double avg = calculateAverage(r.getId());
        int reviewCount = (int) allReviews.stream().filter(rev -> rev.getRestaurantId() == r.getId()).count();
        if (reviewCount >= 3 && avg >= 4.0) {
            Label pop = new Label("🔥 POPULAIRE"); pop.getStyleClass().add("badge-popular");
            StackPane.setAlignment(pop, Pos.TOP_LEFT); StackPane.setMargin(pop, new Insets(12));
            topArea.getChildren().add(pop);
        }
        topArea.getChildren().addAll(iv, like);

        VBox body = new VBox(10); body.setPadding(new Insets(15, 20, 20, 20));

        HBox titleRow = new HBox();
        Label name = new Label(r.getName()); name.getStyleClass().add("card-title");
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        Label rateLabel = new Label(avg == 0 ? "Nouveau" : String.format("⭐ %.1f", avg));
        rateLabel.getStyleClass().add("rating-note");
        titleRow.getChildren().addAll(name, spacer, rateLabel);

        Label details = new Label("📍 " + r.getDestinationName() + " • " + r.getCategory());
        details.getStyleClass().add("card-category-text");

        HBox footer = new HBox(new Label("👥 " + r.getCapacity() + " max"));
        footer.setAlignment(Pos.CENTER_LEFT);
        Region spacer2 = new Region(); HBox.setHgrow(spacer2, Priority.ALWAYS);
        Button btnRes = new Button("Réserver"); btnRes.getStyleClass().add("btn-reserve");
        btnRes.setOnAction(e -> openRestaurantDetail(r));
        footer.getChildren().addAll(spacer2, btnRes);

        body.getChildren().addAll(titleRow, details, new Separator(), footer);
        card.getChildren().addAll(topArea, body);


        if (aiRecommendedRestaurantIds.contains(r.getId())) {
            card.setStyle("-fx-border-color: #FF8210; -fx-border-width: 2; -fx-border-radius: 15; -fx-background-radius: 15;");

            // Optional: Add a small sparkle icon to the top area
            Label aiSparkle = new Label("✨");
            aiSparkle.setStyle("-fx-font-size: 20;");
            StackPane.setAlignment(aiSparkle, Pos.TOP_LEFT);
            StackPane.setMargin(aiSparkle, new Insets(40, 0, 0, 10)); // Below the "Popular" badge
            topArea.getChildren().add(aiSparkle);
        }
        return card;
    }

    private void openRestaurantDetail(Restaurant restaurant) {
        if (this.mainClientController == null) {
            System.err.println("NAVIGATION ERROR: mainClientController is null!");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ClientRestaurantDetailView.fxml"));
            Parent root = loader.load();
            ClientRestaurantDetailController controller = loader.getController();

            if (controller != null) {
                controller.setMainClientController(this.mainClientController);
                controller.setRestaurantData(restaurant);
            }

            mainClientController.getMainBorderPane().setCenter(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML private void handleSortName() {
        allRestaurants.sort(Comparator.comparing(Restaurant::getName));
        applyDeepFilters();
    }

    @FXML private void handleSortDate() {
        allRestaurants.sort((r1, r2) -> r2.getCreatedAt().compareTo(r1.getCreatedAt()));
        applyDeepFilters();
    }

    @FXML private void handleReset() {
        searchField.clear();
        categoryFilter.setValue("Toutes les catégories");
        scoreFilter.setValue("Toutes les notes");
        applyDeepFilters();
    }

    @FXML
    private void toggleAIPanel() {
        aiPanel.setVisible(!aiPanel.isVisible());
    }

    private void runAIWorkflow() {
        String input = aiInputField.getText();
        if (input == null || input.trim().isEmpty()) return;

        loadingOverlay.setVisible(true);
        aiProgress.setProgress(-1);
        aiStatusLabel.setText("L'IA analyse votre demande...");

        Task<AIRecommendation> task = new Task<>() {
            @Override
            protected AIRecommendation call() {
                // Send Restaurants, Menus, and Reviews
                return aiService.getAutomatedAdvice(input, allRestaurants, allMenusCache, allReviews);
            }
        };

        task.setOnSucceeded(e -> {

            loadingOverlay.setVisible(false);
            AIRecommendation result = task.getValue();
            aiResponseLabel.setText(result.explanation());

            // Directly set the IDs returned by the AI
            this.aiRecommendedRestaurantIds = result.ids();

            // Refresh the Grid - Now the "Solid Response" works
            // The table will only show restaurants the AI picked!
            applyDeepFilters();

            aiProgress.setProgress(1);
            aiStatusLabel.setText("Recherche terminée avec succès !");


        });

        task.setOnFailed(e -> {
            // 3. Hide loading screen even if it fails
            loadingOverlay.setVisible(false);
            aiProgress.setProgress(0);
            aiStatusLabel.setText("Désolé, une erreur est survenue.");

            System.err.println("AI Workflow Error: " + task.getException().getMessage());
            task.getException().printStackTrace();
        });

        Thread t = new Thread(task);
        t.setDaemon(true); // Prevents app from hanging on exit
        t.start();
    }

    @FXML
    private void handleResetAI() {
        this.aiRecommendedRestaurantIds.clear();
        aiInputField.clear();
        aiResponseLabel.setText("L'IA analysera tous les menus des restaurants pour trouver la correspondance parfaite.");
        aiStatusLabel.setText("En attente de votre demande...");
        aiProgress.setProgress(0);
        applyDeepFilters(); // Show all restaurants again
    }

    @FXML
    private void handleQuickSuggestion(ActionEvent event) { // Ensure javafx.event.ActionEvent
        Button btn = (Button) event.getSource();
        aiInputField.setText(btn.getText());
        aiAutomationTimer.stop(); // Add this to prevent double execution
        runAIWorkflow();
    }

    private void fetchAutocomplete(String query) {
        new Thread(() -> {
            try {
                String url = "https://nominatim.openstreetmap.org/search?format=json&q="
                        + java.net.URLEncoder.encode(query, "UTF-8");

                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("User-Agent", "GoVacate-App")
                        .GET().build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    org.json.JSONArray jsonArray = new org.json.JSONArray(response.body());

                    // Use a temporary map to avoid clearing the main one while the user is clicking
                    Map<String, String> tempResults = new HashMap<>();

                    for (int i = 0; i < jsonArray.length(); i++) {
                        org.json.JSONObject obj = jsonArray.getJSONObject(i);
                        tempResults.put(obj.getString("display_name"), obj.getString("lat") + "," + obj.getString("lon"));
                    }

                    Platform.runLater(() -> {
                        searchResults.clear();
                        searchResults.putAll(tempResults); // Update the main map only when ready
                        listLocationResults.getItems().setAll(searchResults.keySet());

                        boolean hasData = !searchResults.isEmpty();
                        listLocationResults.setVisible(hasData);
                        listLocationResults.setManaged(hasData);
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    private void handleOpenLocationSearch() {
        locationSearchOverlay.setVisible(true);
        locationSearchOverlay.setManaged(true);
        locationSearchOverlay.toFront();
        txtLocationSearch.requestFocus(); // Focus the text field automatically
    }

    @FXML
    private void handleCloseLocationSearch() {
        locationSearchOverlay.setVisible(false);
        locationSearchOverlay.setManaged(false);
        txtLocationSearch.clear();
        listLocationResults.setVisible(false);
        listLocationResults.setManaged(false);
    }

    @FXML
    private void handleConfirmLocation() {
        String selected = txtLocationSearch.getText();
        System.out.println("DEBUG: Confirming position for text: [" + selected + "]");

        if (selected != null && !selected.isEmpty()) {
            String coords = searchResults.get(selected);

            if (coords != null) {
                // SUCCESS CASE
                if (currentUser != null) {
                    // NEW FORMAT: Storing coordinates and name together in the same column
                    // Example saved string: "36.8,10.1|Tunis, Tunisia"
                    String formattedPosition = coords + "|" + selected;

                    currentUser.setPosition(formattedPosition);
                    userService.updatePosition(currentUser.getId(), formattedPosition);

                    System.out.println("DEBUG: Saved in combined format: " + formattedPosition);
                }
                handleCloseLocationSearch();
                if (aiStatusLabel != null) aiStatusLabel.setText("📍 Position : " + selected);
            } else {
                // If the map doesn't have the key, fallback to the first result
                if (!searchResults.isEmpty()) {
                    String firstKey = searchResults.keySet().iterator().next();
                    String firstCoords = searchResults.get(firstKey);

                    // Construct format for the fallback case
                    String formattedFallback = firstCoords + "|" + firstKey;

                    if (currentUser != null) {
                        currentUser.setPosition(formattedFallback);
                        userService.updatePosition(currentUser.getId(), formattedFallback);
                    }

                    handleCloseLocationSearch();
                    if (aiStatusLabel != null) aiStatusLabel.setText("📍 Position : " + firstKey);
                } else {
                    System.out.println("DEBUG: Key not found in map. Current Map size: " + searchResults.size());
                    txtLocationSearch.getStyleClass().add("input-error");
                }
            }
        }
    }
    private void applyNewPosition(String coords, String label) {
        if (currentUser != null) {
            currentUser.setPosition(coords);
            userService.updatePosition(currentUser.getId(), coords);
            System.out.println("DEBUG: Position saved: " + coords);
        }

        handleCloseLocationSearch();

        if (aiStatusLabel != null) {
            aiStatusLabel.setText("📍 Position réglée : " + label);
        }
    }
}