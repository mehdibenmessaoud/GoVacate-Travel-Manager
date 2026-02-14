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
import tn.esprit.projet.entities.Menu;
import tn.esprit.projet.entities.MenuImage;
import tn.esprit.projet.entities.Restaurant;
import tn.esprit.projet.services.MenuImageService;
import tn.esprit.projet.services.MenuService;
import tn.esprit.projet.services.RestaurantImageService;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class RestaurantDetailController {

    @FXML private Label lblName, lblCategory, lblStatus, lblAddress, lblEmail, lblPhone, lblCapacity, lblImageCounter, lblMenuCount;
    @FXML private ImageView mainCarouselImageView;
    @FXML private StackPane carouselContainer;
    @FXML private HBox thumbnailContainer, paginationContainer;
    @FXML private FlowPane menuFlowPane;
    @FXML private Button btnToggleAutoPlay;
    @FXML private TextField txtSearchMenu;
    @FXML private ComboBox<String> comboSort;

    private final MenuService ms = new MenuService();
    private final MenuImageService mis = new MenuImageService();
    private final RestaurantImageService ris = new RestaurantImageService();

    private AdminController mainController;
    private final List<String> imageList = new ArrayList<>();
    private List<Menu> allMenus = new ArrayList<>();
    private int currentIndex = 0;
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
    }

    public void setRestaurantData(Restaurant restaurant) {
        if (restaurant == null) return;

        // Populate Labels with Null Checks to avoid NPE
        if (lblName != null) lblName.setText(restaurant.getName());
        if (lblCategory != null) lblCategory.setText(restaurant.getCategory() != null ? restaurant.getCategory().toUpperCase() : "NON CLASSÉ");
        if (lblAddress != null) lblAddress.setText(restaurant.getAddress());
        if (lblEmail != null) lblEmail.setText(restaurant.getEmail());
        if (lblPhone != null) lblPhone.setText(restaurant.getPhone());
        if (lblCapacity != null) lblCapacity.setText(restaurant.getCapacity() + " Places");

        // Status Styling
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
        setupAutoPlay();
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
            if (!imageList.isEmpty()) displayImage(0);
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void addThumbnail(String url, int index) {
        ImageView thumb = new ImageView(new Image(url, 120, 80, true, true));
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
        mainCarouselImageView.setImage(new Image(imageList.get(index)));
        lblImageCounter.setText((index + 1) + " / " + imageList.size());
        for (int i = 0; i < paginationContainer.getChildren().size(); i++) {
            ((Circle) paginationContainer.getChildren().get(i)).setFill(i == index ? Color.web("#FF8210") : Color.GRAY);
        }
    }

    @FXML
    private void toggleAutoPlay() {
        isAutoPlaying = !isAutoPlaying;
        if (isAutoPlaying) {
            autoPlayTimeline.play();
            btnToggleAutoPlay.setText("⏸ Auto-Play");
            btnToggleAutoPlay.setStyle("-fx-background-color: #FF8210; -fx-text-fill: white;");
        } else {
            autoPlayTimeline.stop();
            btnToggleAutoPlay.setText("▶ Auto-Play");
            btnToggleAutoPlay.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-text-fill: white;");
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
        VBox card = new VBox(0);
        card.getStyleClass().add("water-card");
        card.setPrefWidth(260);

        StackPane imgContainer = new StackPane();
        imgContainer.setPrefHeight(150);
        ImageView iv = new ImageView();
        iv.setFitWidth(260); iv.setFitHeight(150); iv.setPreserveRatio(true);

        try {
            List<MenuImage> mImgs = mis.getByMenuId(menu.getId());
            if (!mImgs.isEmpty()) iv.setImage(new Image(mImgs.get(0).getImageUrl(), true));
        } catch (SQLException e) { e.printStackTrace(); }

        Button btnFull = new Button("⛶");
        btnFull.getStyleClass().add("btn-full-glass");
        btnFull.setOnAction(e -> handleFullScreen(iv.getImage()));
        StackPane.setAlignment(btnFull, Pos.TOP_RIGHT);
        StackPane.setMargin(btnFull, new Insets(10));

        imgContainer.getChildren().addAll(iv, btnFull);

        VBox info = new VBox(8);
        info.setPadding(new Insets(15));
        Label name = new Label(menu.getName());
        name.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16;");
        Label desc = new Label(menu.getDescription());
        desc.setStyle("-fx-text-fill: #679AC1; -fx-font-size: 11;");
        desc.setWrapText(true);
        desc.setPrefHeight(35);

        HBox footer = new HBox();
        Label price = new Label(menu.getPrice() + " TND");
        price.setStyle("-fx-text-fill: #FF8210; -fx-font-weight: bold;");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Label status = new Label(menu.getStatus());
        status.setStyle("-fx-font-size: 9; -fx-padding: 2 6; -fx-background-color: rgba(255,255,255,0.1); -fx-text-fill: white; -fx-background-radius: 5;");

        footer.getChildren().addAll(price, sp, status);
        info.getChildren().addAll(name, desc, footer);
        card.getChildren().addAll(imgContainer, info);
        return card;
    }

    @FXML private void handleFullScreen() { if (mainCarouselImageView.getImage() != null) handleFullScreen(mainCarouselImageView.getImage()); }

    private void handleFullScreen(Image img) {
        if (img == null) return;
        Stage stage = new Stage(StageStyle.UNDECORATED);
        ImageView iv = new ImageView(img); iv.setPreserveRatio(true);
        StackPane root = new StackPane(iv); root.setStyle("-fx-background-color: black;");
        iv.fitWidthProperty().bind(stage.widthProperty()); iv.fitHeightProperty().bind(stage.heightProperty());
        Scene scene = new Scene(root);
        scene.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ESCAPE) stage.close(); });
        stage.setScene(scene); stage.setFullScreen(true); stage.setFullScreenExitHint(""); stage.show();
    }

    @FXML private void handleBack() {
        if (autoPlayTimeline != null) autoPlayTimeline.stop();
        if (mainController != null) mainController.loadSection("/RestaurantAdminView.fxml");
    }
}