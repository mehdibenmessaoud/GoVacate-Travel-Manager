package tn.esprit.projet.gui;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;
import tn.esprit.projet.entities.Menu;
import tn.esprit.projet.entities.Restaurant;
import tn.esprit.projet.services.MenuService;
import tn.esprit.projet.services.RestaurantImageService;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RestaurantDetailController {

    @FXML private Label lblName, lblCategory, lblStatus, lblAddress, lblEmail, lblPhone, lblCapacity, lblImageCounter;
    @FXML private ImageView mainCarouselImageView;
    @FXML private StackPane carouselContainer;
    @FXML private HBox thumbnailContainer, paginationContainer;
    @FXML private TableView<Menu> menuTable;
    @FXML private TableColumn<Menu, String> colMenuName;
    @FXML private TableColumn<Menu, BigDecimal> colMenuPrice;
    @FXML private Button btnToggleAutoPlay;

    private AdminController mainController;
    private final MenuService ms = new MenuService();
    private final RestaurantImageService ris = new RestaurantImageService();
    private final List<String> imageList = new ArrayList<>();

    private int currentIndex = 0;
    private Timeline autoPlayTimeline;
    private boolean isAutoPlaying = false;

    public void setMainController(AdminController controller) {
        this.mainController = controller;
    }

    public void setRestaurantData(Restaurant restaurant) {
        // Basic Info
        lblName.setText(restaurant.getName() != null ? restaurant.getName() : "N/A");
        lblCategory.setText(restaurant.getCategory() != null ? restaurant.getCategory().toUpperCase() : "NON CLASSÉ");
        lblAddress.setText(restaurant.getAddress() != null ? restaurant.getAddress() : "N/A");
        lblEmail.setText(restaurant.getEmail() != null ? restaurant.getEmail() : "N/A");
        lblPhone.setText(restaurant.getPhone() != null ? restaurant.getPhone() : "N/A");
        lblCapacity.setText(String.valueOf(restaurant.getCapacity()));

        // Enhanced Status Logic
        String status = restaurant.getStatus() != null ? restaurant.getStatus().toUpperCase() : "OPEN";
        lblStatus.setText(status);

        // Clear all possible status classes first
        lblStatus.getStyleClass().removeAll("status-open", "status-closed", "status-suspended");

        // Apply the specific class
        if (status.equalsIgnoreCase("OPEN")) {
            lblStatus.getStyleClass().add("status-open");
        } else if (status.equalsIgnoreCase("SUSPENDED")) {
            lblStatus.getStyleClass().add("status-suspended");
        } else {
            lblStatus.getStyleClass().add("status-closed");
        }

        loadMenuData(restaurant.getId());
        loadCarouselData(restaurant.getId());
        setupAutoPlay();
    }

    private void setupAutoPlay() {
        if (autoPlayTimeline != null) autoPlayTimeline.stop();
        autoPlayTimeline = new Timeline(new KeyFrame(Duration.seconds(3), event -> handleNextImage()));
        autoPlayTimeline.setCycleCount(Timeline.INDEFINITE);
    }

    @FXML
    private void toggleAutoPlay() {
        if (isAutoPlaying) {
            autoPlayTimeline.stop();
            btnToggleAutoPlay.setText("▶ Auto-Play");
            btnToggleAutoPlay.getStyleClass().remove("btn-autoplay-active");
        } else {
            autoPlayTimeline.play();
            btnToggleAutoPlay.setText("⏸ Pause");
            btnToggleAutoPlay.getStyleClass().add("btn-autoplay-active");
        }
        isAutoPlaying = !isAutoPlaying;
    }

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
            if (!imageList.isEmpty()) { currentIndex = 0; displayImage(currentIndex); }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void addThumbnail(String url, int index) {
        ImageView thumb = new ImageView(new Image(url, 80, 60, true, true));
        thumb.getStyleClass().add("gallery-thumb");
        thumb.setOnMouseClicked(e -> {
            if(isAutoPlaying) toggleAutoPlay(); // Stop autoplay if user interacts
            currentIndex = index;
            displayImage(currentIndex);
        });
        thumbnailContainer.getChildren().add(thumb);
    }

    private void addDot() {
        Circle dot = new Circle(4, Color.GRAY);
        paginationContainer.getChildren().add(dot);
    }

    private void displayImage(int index) {
        if (imageList.isEmpty()) return;
        FadeTransition fadeOut = new FadeTransition(Duration.millis(250), mainCarouselImageView);
        fadeOut.setFromValue(1.0); fadeOut.setToValue(0.2);
        fadeOut.setOnFinished(e -> {
            mainCarouselImageView.setImage(new Image(imageList.get(index)));
            lblImageCounter.setText((index + 1) + " / " + imageList.size());
            updateVisuals();
            FadeTransition fadeIn = new FadeTransition(Duration.millis(250), mainCarouselImageView);
            fadeIn.setFromValue(0.2); fadeIn.setToValue(1.0); fadeIn.play();
        });
        fadeOut.play();
    }

    private void updateVisuals() {
        for (int i = 0; i < paginationContainer.getChildren().size(); i++) {
            ((Circle) paginationContainer.getChildren().get(i)).setFill(i == currentIndex ? Color.web("#FF8210") : Color.GRAY);
        }
        for (int i = 0; i < thumbnailContainer.getChildren().size(); i++) {
            thumbnailContainer.getChildren().get(i).setOpacity(i == currentIndex ? 1.0 : 0.5);
        }
    }

    @FXML
    private void handleNextImage() {
        if (imageList.size() > 1) {
            currentIndex = (currentIndex + 1) % imageList.size();
            displayImage(currentIndex);
        }
    }

    @FXML
    private void handlePrevImage() {
        if (imageList.size() > 1) {
            currentIndex = (currentIndex - 1 + imageList.size()) % imageList.size();
            displayImage(currentIndex);
        }
    }

    @FXML
    private void handleFullScreen() {
        if (imageList.isEmpty()) return;
        Stage stage = new Stage();
        ImageView iv = new ImageView(new Image(imageList.get(currentIndex)));
        iv.setPreserveRatio(true);
        StackPane root = new StackPane(iv);
        root.setStyle("-fx-background-color: black;");
        iv.fitWidthProperty().bind(stage.widthProperty());
        iv.fitHeightProperty().bind(stage.heightProperty());

        stage.setScene(new Scene(root));
        stage.setFullScreen(true);
        stage.setFullScreenExitHint("");
        stage.fullScreenProperty().addListener((obs, old, isFull) -> { if (!isFull) stage.close(); });
        stage.show();
    }

    private void loadMenuData(int restaurantId) {
        try {
            colMenuName.setCellValueFactory(new PropertyValueFactory<>("name"));
            colMenuPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
            menuTable.setItems(FXCollections.observableArrayList(
                    ms.getAll().stream().filter(m -> m.getRestaurantId() == restaurantId).toList()
            ));
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @FXML private void handleBack() {
        if (autoPlayTimeline != null) autoPlayTimeline.stop();
        if (mainController != null) mainController.loadSection("/RestaurantAdminView.fxml");
    }
}