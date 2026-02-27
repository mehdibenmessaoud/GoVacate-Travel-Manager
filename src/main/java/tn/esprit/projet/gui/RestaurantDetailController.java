package tn.esprit.projet.gui;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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
import tn.esprit.projet.entities.*;
import tn.esprit.projet.entities.Menu;
import tn.esprit.projet.services.*;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import java.io.File;
import java.net.URL;

public class RestaurantDetailController {

    @FXML private Label lblName, lblCategory, lblStatus, lblAddress, lblEmail, lblPhone, lblCapacity, lblImageCounter, lblMenuCount, lblReviewStats;
    @FXML private ImageView mainCarouselImageView;
    @FXML private StackPane carouselContainer;
    @FXML private HBox thumbnailContainer, paginationContainer;
    @FXML private FlowPane menuFlowPane;
    @FXML private VBox reviewsContainer;
    @FXML private Button btnToggleAutoPlay;
    @FXML private TextField txtSearchMenu;
    @FXML private ComboBox<String> comboSort, comboSortReviews;

    private final MenuService ms = new MenuService();
    private final MenuImageService mis = new MenuImageService();
    private final RestaurantImageService ris = new RestaurantImageService();
    private final RestaurantReviewService rrs = new RestaurantReviewService();
    private final ReviewImageService ris_review = new ReviewImageService();

    private AdminController mainController;
    private final List<String> imageList = new ArrayList<>();
    private List<Menu> allMenus = new ArrayList<>();
    private List<RestaurantReview> allReviews = new ArrayList<>();
    private int currentIndex = 0;
    private int currentRestaurantId;
    private Timeline autoPlayTimeline;
    private boolean isAutoPlaying = true;

    public void setMainController(AdminController controller) {
        this.mainController = controller;
    }

    @FXML
    public void initialize() {
        if (txtSearchMenu != null) txtSearchMenu.textProperty().addListener((obs, old, val) -> applyFilters());

        if (comboSort != null) {
            comboSort.setItems(FXCollections.observableArrayList("Nom: A-Z", "Prix: Croissant", "Prix: Décroissant"));
            comboSort.setOnAction(e -> applyFilters());
        }

        if (comboSortReviews != null) {
            comboSortReviews.setItems(FXCollections.observableArrayList("Plus récents", "Plus anciens", "Meilleures notes", "Moins bonnes notes"));
            comboSortReviews.setOnAction(e -> applyReviewSorting());
        }
    }

    public void setRestaurantData(Restaurant restaurant) {
        if (restaurant == null) return;
        this.currentRestaurantId = restaurant.getId();

        if (lblName != null) lblName.setText(restaurant.getName());
        if (lblCategory != null) lblCategory.setText(restaurant.getCategory() != null ? restaurant.getCategory().toUpperCase() : "NON CLASSÉ");
        if (lblAddress != null) lblAddress.setText(restaurant.getAddress());
        if (lblEmail != null) lblEmail.setText(restaurant.getEmail());
        if (lblPhone != null) lblPhone.setText(restaurant.getPhone());
        if (lblCapacity != null) lblCapacity.setText(restaurant.getCapacity() + " Places");

        if (lblStatus != null) {
            String status = restaurant.getStatus() != null ? restaurant.getStatus().toUpperCase() : "OPEN";
            lblStatus.setText(status);
            lblStatus.getStyleClass().removeAll("status-open", "status-closed", "status-suspended");
            if (status.equals("OPEN")) lblStatus.getStyleClass().add("status-open");
            else if (status.equals("SUSPENDED")) lblStatus.getStyleClass().add("status-suspended");
            else lblStatus.getStyleClass().add("status-closed");
        }

        loadCarouselData(restaurant.getId());
        loadMenuData(restaurant.getId());
        loadReviewData(restaurant.getId());
        setupAutoPlay();
    }

    // --- CAROUSEL LOGIC ---
    private void loadCarouselData(int restaurantId) {
        try {
            imageList.clear();
            thumbnailContainer.getChildren().clear();
            paginationContainer.getChildren().clear();
            ris.getByRestaurantId(restaurantId).forEach(img -> {
                imageList.add(img.getImageUrl());
                addThumbnail(img.getImageUrl(), imageList.size() - 1);
                addDot();
            });
            if (!imageList.isEmpty()) displayImage(0);
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void addThumbnail(String url, int index) {
        // UPDATED: Using the smart loader
        Image img = loadSmartImage(url);
        ImageView thumb = new ImageView(img);
        thumb.setFitWidth(120);
        thumb.setFitHeight(80);
        thumb.setPreserveRatio(true);
        thumb.getStyleClass().add("gallery-thumb");
        thumb.setOnMouseClicked(e -> { currentIndex = index; displayImage(currentIndex); });
        thumbnailContainer.getChildren().add(thumb);
    }

    private void addDot() {
        Circle dot = new Circle(4, Color.GRAY);
        paginationContainer.getChildren().add(dot);
    }

    private void displayImage(int index) {
        if (imageList.isEmpty()) return;
        mainCarouselImageView.setImage(new Image(imageList.get(index), 0, 0, true, true, true));
        lblImageCounter.setText((index + 1) + " / " + imageList.size());
        for (int i = 0; i < paginationContainer.getChildren().size(); i++) {
            ((Circle) paginationContainer.getChildren().get(i)).setFill(i == index ? Color.web("#FF8210") : Color.GRAY);
        }
    }

    @FXML private void toggleAutoPlay() {
        isAutoPlaying = !isAutoPlaying;
        if (isAutoPlaying) {
            autoPlayTimeline.play();
            btnToggleAutoPlay.setText("⏸ Auto-Play");
        } else {
            autoPlayTimeline.stop();
            btnToggleAutoPlay.setText("▶ Auto-Play");
        }
    }

    private void setupAutoPlay() {
        if (autoPlayTimeline != null) autoPlayTimeline.stop();
        autoPlayTimeline = new Timeline(new KeyFrame(Duration.seconds(4), e -> handleNextImage()));
        autoPlayTimeline.setCycleCount(Timeline.INDEFINITE);
        if (isAutoPlaying) autoPlayTimeline.play();
    }

    @FXML private void handleNextImage() {
        if (imageList.isEmpty()) return;
        currentIndex = (currentIndex + 1) % imageList.size();
        displayImage(currentIndex);
    }

    @FXML private void handlePrevImage() {
        if (imageList.isEmpty()) return;
        currentIndex = (currentIndex - 1 + imageList.size()) % imageList.size();
        displayImage(currentIndex);
    }

    // --- MENU LOGIC ---
    private void loadMenuData(int restaurantId) {
        try {
            allMenus = ms.getAll().stream().filter(m -> m.getRestaurantId() == restaurantId).collect(Collectors.toList());
            applyFilters();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void applyFilters() {
        String search = txtSearchMenu.getText().toLowerCase();
        List<Menu> filtered = allMenus.stream()
                .filter(m -> m.getName().toLowerCase().contains(search) || m.getDescription().toLowerCase().contains(search))
                .collect(Collectors.toList());

        String sort = comboSort.getValue();
        if ("Prix: Croissant".equals(sort)) filtered.sort(Comparator.comparing(Menu::getPrice));
        else if ("Prix: Décroissant".equals(sort)) filtered.sort((a, b) -> b.getPrice().compareTo(a.getPrice()));
        else filtered.sort(Comparator.comparing(Menu::getName));

        menuFlowPane.getChildren().clear();
        for (Menu m : filtered) menuFlowPane.getChildren().add(createMenuCard(m));
        lblMenuCount.setText(filtered.size() + " Plats");
    }

    private VBox createMenuCard(Menu menu) {
        VBox card = new VBox(0); card.getStyleClass().add("water-card"); card.setPrefWidth(260);
        StackPane imgContainer = new StackPane(); imgContainer.setPrefHeight(150);
        ImageView iv = new ImageView(); iv.setFitWidth(260); iv.setFitHeight(150); iv.setPreserveRatio(true);

        try {
            List<MenuImage> mImgs = mis.getByMenuId(menu.getId());
            if (!mImgs.isEmpty()) {
                // UPDATED: Using the smart loader
                Image img = loadSmartImage(mImgs.get(0).getImageUrl());
                if (img != null) {
                    iv.setImage(img);
                    iv.setOnMouseClicked(e -> handleFullScreen(iv.getImage()));
                    iv.setStyle("-fx-cursor: hand;");
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }

        imgContainer.getChildren().add(iv);

        VBox info = new VBox(8); info.setPadding(new Insets(15));
        Label name = new Label(menu.getName()); name.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        Label price = new Label(menu.getPrice() + " TND"); price.setStyle("-fx-text-fill: #FF8210;");
        info.getChildren().addAll(name, price);
        card.getChildren().addAll(imgContainer, info);
        return card;
    }

    // --- REVIEW SYSTEM LOGIC ---
    private void loadReviewData(int restaurantId) {
        try {
            allReviews = rrs.getAll().stream()
                    .filter(r -> r.getRestaurantId() == restaurantId)
                    .collect(Collectors.toList());
            updateReviewStats();
            applyReviewSorting();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void updateReviewStats() {
        if (allReviews.isEmpty()) {
            lblReviewStats.setText("Aucun avis pour le moment");
            return;
        }
        double avg = allReviews.stream().mapToInt(RestaurantReview::getRating).average().orElse(0.0);
        lblReviewStats.setText(String.format("Note moyenne: %.1f ★ (%d avis)", avg, allReviews.size()));
    }

    private void applyReviewSorting() {
        String sort = comboSortReviews.getValue();
        if ("Plus récents".equals(sort)) allReviews.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        else if ("Plus anciens".equals(sort)) allReviews.sort(Comparator.comparing(RestaurantReview::getCreatedAt));
        else if ("Meilleures notes".equals(sort)) allReviews.sort((a, b) -> Integer.compare(b.getRating(), a.getRating()));
        else if ("Moins bonnes notes".equals(sort)) allReviews.sort(Comparator.comparingInt(RestaurantReview::getRating));

        displayReviews();
    }

    private void displayReviews() {
        reviewsContainer.getChildren().clear();
        for (RestaurantReview review : allReviews) {
            reviewsContainer.getChildren().add(createReviewCard(review));
        }
    }

    private VBox createReviewCard(RestaurantReview review) {
        VBox card = new VBox(10);
        card.getStyleClass().add("water-card");
        card.setPadding(new Insets(15));

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        Label stars = new Label("★".repeat(review.getRating()) + "☆".repeat(Math.max(0, 5 - review.getRating())));
        stars.setStyle("-fx-text-fill: #FF8210; -fx-font-size: 16;");

        Label date = new Label(review.getCreatedAt() != null ? review.getCreatedAt().toString().split("T")[0] : "");
        date.setStyle("-fx-text-fill: gray; -fx-font-size: 11;");

        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);

        Button btnDel = new Button("Supprimer");
        btnDel.getStyleClass().add("btn-action-delete");
        btnDel.setOnAction(e -> handleDeleteReview(review));
        header.getChildren().addAll(stars, date, sp, btnDel);

        Label comment = new Label(review.getComment());
        comment.setWrapText(true);
        comment.setStyle("-fx-text-fill: white;");

        HBox imgBox = new HBox(10);
        try {
            List<ReviewImage> allReviewImages = ris_review.getAll();
            List<ReviewImage> reviewImages = allReviewImages.stream()
                    .filter(i -> i.getReviewId() == review.getId())
                    .collect(Collectors.toList());

            for (ReviewImage ri : reviewImages) {
                String url = ri.getImageUrl();
                if (url != null && !url.trim().isEmpty()) {
                    try {
                        // LOAD AT FULL RESOLUTION (0, 0) for crisp FullScreen
                        Image img = new Image(url, 0, 0, true, true, true);
                        ImageView iv = new ImageView(img);
                        iv.setFitWidth(100);
                        iv.setFitHeight(100);
                        iv.setPreserveRatio(true);
                        iv.setSmooth(true);
                        iv.setStyle("-fx-cursor: hand; -fx-background-radius: 10;");

                        iv.setOnMouseClicked(e -> {
                            if (iv.getImage() != null && !iv.getImage().isError()) {
                                handleFullScreen(iv.getImage());
                            }
                        });

                        imgBox.getChildren().add(iv);
                    } catch (Exception e) {
                        System.err.println("Skipping invalid image URL: " + url);
                    }
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }

        card.getChildren().addAll(header, comment, imgBox);
        return card;
    }

    private void handleDeleteReview(RestaurantReview review) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer cet avis ?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(res -> {
            if (res == ButtonType.YES) {
                try {
                    rrs.delete(review.getId());
                    allReviews.remove(review);
                    updateReviewStats();
                    displayReviews();
                } catch (SQLException e) { e.printStackTrace(); }
            }
        });
    }

    // --- FULL SCREEN LOGIC ---
    @FXML private void handleFullScreen() {
        if (mainCarouselImageView.getImage() != null) handleFullScreen(mainCarouselImageView.getImage());
    }

    private void handleFullScreen(Image img) {
        if (img == null || img.isError()) return;

        Stage stage = new Stage(StageStyle.UNDECORATED);
        ImageView iv = new ImageView(img);
        iv.setPreserveRatio(true);
        iv.setSmooth(true);

        iv.fitWidthProperty().bind(stage.widthProperty());
        iv.fitHeightProperty().bind(stage.heightProperty());

        StackPane root = new StackPane(iv);
        root.setStyle("-fx-background-color: rgba(0,0,0,0.95);");

        Scene scene = new Scene(root);
        scene.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ESCAPE) stage.close(); });
        root.setOnMouseClicked(e -> stage.close());

        stage.setScene(scene);
        stage.setFullScreen(true);
        // REMOVED HINT HERE
        stage.setFullScreenExitHint("");
        stage.show();
    }

    @FXML private void handleBack() {
        if (autoPlayTimeline != null) autoPlayTimeline.stop();
        if (mainController != null) mainController.loadSection("/RestaurantAdminView.fxml");
    }


    private Image loadSmartImage(String path) {
        if (path == null || path.isEmpty()) return null;

        try {
            if (path.startsWith("http")) {
                return new Image(path, true);
            }

            // 1. Try absolute disk path (for real-time newly added images)
            File file = new File("src/main/resources" + path);
            if (file.exists()) {
                return new Image(file.toURI().toString(), true);
            }

            // 2. Fallback to Classpath (for images already in target/classes)
            URL resource = getClass().getResource(path);
            if (resource != null) {
                return new Image(resource.toExternalForm(), true);
            }
        } catch (Exception e) {
            System.err.println("Error loading image at: " + path);
        }
        return null;
    }
}