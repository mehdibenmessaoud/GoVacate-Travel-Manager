package tn.esprit.projet.gui;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;

import tn.esprit.projet.services.RestaurantService;
import tn.esprit.projet.entities.Restaurant;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RestaurantController {

    @FXML private ComboBox<String> categoryFilter;
    @FXML private Slider ratingFilter;
    @FXML private ComboBox<String> sortBox;
    @FXML private Button applyFilters, clearFilters;
    @FXML private FlowPane restaurantList;
    @FXML private ScrollPane listScroll;
    @FXML private Button backBtn;
    @FXML private Button prevPage, nextPage;
    @FXML private Label pageLabel;

    private RestaurantService restaurantService;
    private List<Restaurant> allItems = new ArrayList<>();
    private List<Restaurant> filteredItems = new ArrayList<>();
    private int pageSize = 8;
    private int currentPage = 0;

    public void initialize() {
        setupFilters();
        restaurantService = new RestaurantService();
        loadRestaurants();
        applyFilters.setOnAction(e -> { currentPage = 0; loadRestaurants(); });
        clearFilters.setOnAction(e -> { categoryFilter.getSelectionModel().clearSelection(); ratingFilter.setValue(0); loadRestaurants(); });
        if (backBtn != null) backBtn.setOnAction(e -> goBack());
        if (prevPage != null) prevPage.setOnAction(e -> { if (currentPage > 0) { currentPage--; showPage(); } });
        if (nextPage != null) nextPage.setOnAction(e -> { currentPage++; showPage(); });
    }

    // allow external callers to preselect a category and refresh the list
    public void applyInitialCategory(String cat) {
        if (cat == null || categoryFilter == null) return;
        try {
            if (categoryFilter.getItems().contains(cat)) categoryFilter.getSelectionModel().select(cat);
            else { /* add and select */ categoryFilter.getItems().add(cat); categoryFilter.getSelectionModel().select(cat); }
        } catch (Exception ignored) {}
        loadRestaurants();
    }

    private void setupFilters() {
        try {
            categoryFilter.getItems().addAll("Toutes", "Italienne", "Halal", "Végétarienne", "Locale");
            categoryFilter.getSelectionModel().selectFirst();
        } catch (Exception ignored) {}
    }

    private void loadRestaurants() {
        restaurantList.getChildren().clear();
        allItems.clear(); filteredItems.clear();
        try {
            allItems = restaurantService.getAll();
        } catch (SQLException ex) {
            // fallback to sample if DB unavailable
            allItems = new ArrayList<>();
            allItems.add(new Restaurant(0, "La Bella Italia", "Italienne", "Via Roma 12", "", "", 0, "OPEN", 0));
            allItems.add(new Restaurant(0, "Chez Habib", "Halal", "Rue Centrale 5", "", "", 0, "OPEN", 0));
            allItems.add(new Restaurant(0, "Green Table", "Végétarienne", "Markt 3", "", "", 0, "OPEN", 0));
        }

        String selectedCat = (categoryFilter != null && categoryFilter.getSelectionModel().getSelectedItem() != null)
                ? categoryFilter.getSelectionModel().getSelectedItem() : "Toutes";

        for (Restaurant r : allItems) {
            if (!"Toutes".equalsIgnoreCase(selectedCat) && r.getCategory() != null && !r.getCategory().equalsIgnoreCase(selectedCat)) continue;
            filteredItems.add(r);
        }

        currentPage = Math.max(0, Math.min(currentPage, (filteredItems.size() - 1) / pageSize));
        showPage();
    }

    private void showPage() {
        restaurantList.getChildren().clear();
        int from = currentPage * pageSize;
        int to = Math.min(from + pageSize, filteredItems.size());
        for (int i = from; i < to; i++) restaurantList.getChildren().add(createCard(filteredItems.get(i)));

        int totalPages = Math.max(1, (int) Math.ceil((double) filteredItems.size() / pageSize));
        if (pageLabel != null) pageLabel.setText("Page " + (currentPage + 1) + " / " + totalPages);
        if (prevPage != null) prevPage.setDisable(currentPage <= 0);
        if (nextPage != null) nextPage.setDisable(currentPage >= totalPages - 1);
    }

    private VBox createCard(Restaurant r) {
        VBox card = new VBox(10);
        card.getStyleClass().add("restaurant-card");
        card.setPadding(new Insets(14));
        card.setPrefWidth(340);

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        Circle avatar = new Circle(30);
        try {
            var res = getClass().getResource("/images/restaurant_default.png");
            if (res != null) avatar.setFill(new ImagePattern(new Image(res.toExternalForm(), 60, 60, true, true)));
            else avatar.setFill(javafx.scene.paint.Color.web("#e6eef8"));
        } catch (Exception ignored) { avatar.setFill(javafx.scene.paint.Color.web("#e6eef8")); }

        VBox info = new VBox(4);
        Text n = new Text(r.getName()); n.getStyleClass().add("deal-title");
        Text loc = new Text((r.getAddress() != null ? r.getAddress() : "") + " • " + (r.getCategory() != null ? r.getCategory() : "")); loc.getStyleClass().add("section-subtitle");
        info.getChildren().addAll(n, loc);

        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);

        Label status = new Label(r.getStatus() != null ? r.getStatus() : "OPEN");
        status.getStyleClass().add("status-badge");

        header.getChildren().addAll(avatar, info, spacer, status);

        HBox actions = new HBox(8);
        Button viewBtn = new Button("Voir"); viewBtn.getStyleClass().add("add-btn");
        Button bookBtn = new Button("Réserver"); bookBtn.getStyleClass().addAll("btn-cta");
        actions.getChildren().addAll(viewBtn, bookBtn);

        card.getChildren().addAll(header, actions);
        return card;
    }

    private void goBack() {
        try {
            var res = getClass().getResource("/ClientHome.fxml");
            if (res == null) return;
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(res);
            javafx.scene.Parent page = loader.load();
            // replace root of current scene
            if (backBtn != null && backBtn.getScene() != null) backBtn.getScene().setRoot(page);
        } catch (Exception ex) { ex.printStackTrace(); }
    }
}
