package tn.esprit.projet.gui;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import tn.esprit.projet.entities.Menu;
import tn.esprit.projet.entities.MenuImage;
import tn.esprit.projet.services.MenuImageService;

import java.io.File;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RestaurantMenuFormController {

    @FXML private VBox mainContainer; // Linked to FXML root to resolve 'rootPane' error
    @FXML private Label lblTitle;
    @FXML private TextField txtName, txtPrice;
    @FXML private TextArea txtDescription;
    @FXML private ComboBox<String> cbStatus;
    @FXML private VBox imagePreviewContainer;

    private final MenuImageService mis = new MenuImageService();
    private Menu currentMenu;
    private MenuImage singleImage;
    private RestaurantFormController parentController;

    @FXML
    public void initialize() {
        cbStatus.getItems().addAll("AVAILABLE", "UNAVAILABLE");
        cbStatus.setValue("AVAILABLE");
        txtDescription.setWrapText(true);
    }

    public void setParentController(RestaurantFormController parent) {
        this.parentController = parent;
    }

    public void setMenuData(Menu menu) {
        this.currentMenu = menu;
        if (menu != null) {
            lblTitle.setText("Modifier le Plat");
            txtName.setText(menu.getName());
            txtDescription.setText(menu.getDescription());
            txtPrice.setText(menu.getPrice() != null ? menu.getPrice().toString() : "");
            cbStatus.setValue(menu.getStatus());

            // 1. Check memory first (for newly added meals)
            if (parentController != null && parentController.getImagesForMenu(menu) != null) {
                List<MenuImage> memoryImages = parentController.getImagesForMenu(menu);
                if (!memoryImages.isEmpty()) {
                    this.singleImage = memoryImages.get(0);
                    updatePreview(singleImage.getImageUrl());
                    return;
                }
            }

            // 2. Check DB for existing meals
            if (menu.getId() != 0) {
                try {
                    List<MenuImage> existing = mis.getByMenuId(menu.getId());
                    if (!existing.isEmpty()) {
                        this.singleImage = existing.get(0);
                        updatePreview(singleImage.getImageUrl());
                    }
                } catch (SQLException e) { e.printStackTrace(); }
            }
        }
    }

    @FXML
    private void handleUploadImage() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Sélectionner une photo du plat");

        // Restriction: Only PNG, JPG, and JPEG
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg")
        );

        // Uses mainContainer to get the current window
        File file = fc.showOpenDialog(mainContainer.getScene().getWindow());

        if (file != null) {
            singleImage = new MenuImage();
            singleImage.setImageUrl(file.toURI().toString());
            updatePreview(singleImage.getImageUrl());
        }
    }

    private void updatePreview(String url) {
        imagePreviewContainer.getChildren().clear();
        try {
            ImageView iv = new ImageView(new Image(url));
            iv.setFitWidth(180);
            iv.setPreserveRatio(true);
            // Apply a slight glow effect to the preview image
            iv.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 10, 0, 0, 2);");
            imagePreviewContainer.getChildren().add(iv);
        } catch (Exception e) {
            System.out.println("Preview failed: " + url);
        }
    }

    @FXML
    private void handleSave() {
        if (txtName.getText().isEmpty() || txtPrice.getText().isEmpty()) return;

        if (currentMenu == null) currentMenu = new Menu();
        currentMenu.setName(txtName.getText());
        currentMenu.setDescription(txtDescription.getText());

        try {
            currentMenu.setPrice(new BigDecimal(txtPrice.getText()));
        } catch (NumberFormatException e) {
            // Optional: Add an error alert here for invalid price
            return;
        }

        currentMenu.setStatus(cbStatus.getValue());

        List<MenuImage> list = new ArrayList<>();
        if (singleImage != null) {
            list.add(singleImage);
        }
        parentController.addOrUpdateMenuItem(currentMenu, list);
    }

    @FXML private void handleCancel() { parentController.closeMenuForm(); }
}