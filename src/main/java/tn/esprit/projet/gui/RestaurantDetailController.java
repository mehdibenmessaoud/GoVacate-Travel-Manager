package tn.esprit.projet.gui;

import javafx.animation.FadeTransition;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
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
    @FXML private TableView<Menu> menuTable;
    @FXML private TableColumn<Menu, String> colMenuName;
    @FXML private TableColumn<Menu, BigDecimal> colMenuPrice;

    private AdminController mainController;
    private Restaurant currentRestaurant;
    private final MenuService ms = new MenuService();
    private final RestaurantImageService ris = new RestaurantImageService();

    // Carousel state
    private final List<String> imageList = new ArrayList<>();
    private int currentIndex = 0;

    public void setMainController(AdminController controller) {
        this.mainController = controller;
    }

    /**
     * Entry point to populate the view.
     */
    public void setRestaurantData(Restaurant restaurant) {
        this.currentRestaurant = restaurant;

        // Populate Text Fields safely
        lblName.setText(restaurant.getName() != null ? restaurant.getName() : "N/A");
        lblCategory.setText(restaurant.getCategory() != null ? restaurant.getCategory().toUpperCase() : "NON CLASSÉ");
        lblAddress.setText(restaurant.getAddress() != null ? restaurant.getAddress() : "N/A");
        lblEmail.setText(restaurant.getEmail() != null ? restaurant.getEmail() : "N/A");
        lblPhone.setText(restaurant.getPhone() != null ? restaurant.getPhone() : "N/A");
        lblStatus.setText(restaurant.getStatus() != null ? restaurant.getStatus() : "OPEN");
        lblCapacity.setText(String.valueOf(restaurant.getCapacity()));

        // Load Data
        loadMenuData(restaurant.getId());
        loadCarouselData(restaurant.getId());
    }

    private void loadMenuData(int restaurantId) {
        try {
            colMenuName.setCellValueFactory(new PropertyValueFactory<>("name"));
            colMenuPrice.setCellValueFactory(new PropertyValueFactory<>("price"));

            menuTable.setItems(FXCollections.observableArrayList(
                    ms.getAll().stream()
                            .filter(m -> m.getRestaurantId() == restaurantId)
                            .toList()
            ));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadCarouselData(int restaurantId) {
        try {
            imageList.clear();
            ris.getByRestaurantId(restaurantId).forEach(img -> imageList.add(img.getImageUrl()));

            if (!imageList.isEmpty()) {
                currentIndex = 0;
                displayImage(currentIndex);
            } else {
                // Set placeholder if no images found
                mainCarouselImageView.setImage(new Image(getClass().getResourceAsStream("/images/placeholder.png")));
                lblImageCounter.setText("0/0");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles the cross-fade animation between images.
     */
    private void displayImage(int index) {
        if (imageList.isEmpty()) return;

        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), mainCarouselImageView);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.1);

        fadeOut.setOnFinished(e -> {
            try {
                Image newImg = new Image(imageList.get(index), true); // true = load in background
                mainCarouselImageView.setImage(newImg);
                lblImageCounter.setText((index + 1) + " / " + imageList.size());

                FadeTransition fadeIn = new FadeTransition(Duration.millis(300), mainCarouselImageView);
                fadeIn.setFromValue(0.1);
                fadeIn.setToValue(1.0);
                fadeIn.play();
            } catch (Exception ex) {
                System.err.println("Error loading image: " + imageList.get(index));
            }
        });

        fadeOut.play();
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
    private void handleBack() {
        if (mainController != null) {
            mainController.loadSection("/RestaurantAdminView.fxml");
        }
    }
}