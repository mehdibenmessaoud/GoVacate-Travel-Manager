package tn.esprit.projet.gui;

import javafx.application.Platform;
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
import tn.esprit.projet.entities.Menu;
import tn.esprit.projet.entities.Restaurant;
import tn.esprit.projet.entities.RestaurantImage;
import tn.esprit.projet.entities.RestaurantReview;
import tn.esprit.projet.services.MenuService;
import tn.esprit.projet.services.RestaurantService;
import tn.esprit.projet.services.RestaurantImageService;
import tn.esprit.projet.services.RestaurantReviewService;
import tn.esprit.projet.utils.CuisineWikiService;

import java.io.IOException;
import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class ClientRestaurantController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> categoryFilter;
    @FXML private ComboBox<String> scoreFilter;
    @FXML private FlowPane restaurantGrid;

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

    @FXML
    public void initialize() {
        loadDataFromDatabase();

        searchField.textProperty().addListener((obs, old, val) -> applyDeepFilters());
        categoryFilter.valueProperty().addListener((obs, old, val) -> applyDeepFilters());
        scoreFilter.valueProperty().addListener((obs, old, val) -> applyDeepFilters());
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

    private void applyDeepFilters() {
        String query = (searchField.getText() == null) ? "" : searchField.getText().toLowerCase().trim();
        String selectedCat = (categoryFilter.getValue() == null) ? "Toutes les catégories" : categoryFilter.getValue();
        String selectedScore = (scoreFilter.getValue() == null) ? "Toutes les notes" : scoreFilter.getValue();

        List<Restaurant> filtered = allRestaurants.stream()
                .filter(r -> {
                    boolean matchCat = selectedCat.equals("Toutes les catégories") ||
                            (r.getCategory() != null && r.getCategory().equalsIgnoreCase(selectedCat));
                    if (!matchCat) return false;

                    double avg = calculateAverage(r.getId());
                    if (selectedScore.equals("4.5+ ⭐") && avg < 4.5) return false;
                    if (selectedScore.equals("4.0+ ⭐") && avg < 4.0) return false;
                    if (selectedScore.equals("3.0+ ⭐") && avg < 3.0) return false;

                    if (query.isEmpty()) return true;

                    boolean basicMatch = (r.getName() != null && r.getName().toLowerCase().contains(query)) ||
                            (r.getDestinationName() != null && r.getDestinationName().toLowerCase().contains(query)) ||
                            (r.getAddress() != null && r.getAddress().toLowerCase().contains(query));
                    if (basicMatch) return true;

                    return allMenusCache.stream()
                            .filter(m -> m.getRestaurantId() == r.getId())
                            .anyMatch(m -> (m.getName() != null && m.getName().toLowerCase().contains(query)) ||
                                    (m.getDescription() != null && m.getDescription().toLowerCase().contains(query)));
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
            for (Restaurant r : list) {
                restaurantGrid.getChildren().add(buildCard(r));
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
}