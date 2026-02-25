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

    @FXML private VBox mainContainer;
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

        // Real-time numeric validation for Price (allows digits and one decimal point)
        txtPrice.textProperty().addListener((obs, old, newVal) -> {
            if (!newVal.matches("\\d*(\\.\\d*)?")) {
                txtPrice.setText(old);
            }
        });
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

            if (parentController != null && parentController.getImagesForMenu(menu) != null) {
                List<MenuImage> memoryImages = parentController.getImagesForMenu(menu);
                if (!memoryImages.isEmpty()) {
                    this.singleImage = memoryImages.get(0);
                    updatePreview(singleImage.getImageUrl());
                    return;
                }
            }

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
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg")
        );

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
            iv.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 10, 0, 0, 2);");
            imagePreviewContainer.getChildren().add(iv);
        } catch (Exception e) {
            System.out.println("Preview failed: " + url);
        }
    }

    @FXML
    private void handleSave() {
        if (!isInputValid()) return;

        if (currentMenu == null) currentMenu = new Menu();
        currentMenu.setName(txtName.getText().trim());
        currentMenu.setDescription(txtDescription.getText().trim());
        currentMenu.setPrice(new BigDecimal(txtPrice.getText().trim()));
        currentMenu.setStatus(cbStatus.getValue());

        List<MenuImage> list = new ArrayList<>();
        if (singleImage != null) {
            list.add(singleImage);
        }

        parentController.addOrUpdateMenuItem(currentMenu, list);
    }

    // --- VALIDATION LOGIC ---
    private boolean isInputValid() {
        String errorMessage = "";

        if (txtName.getText() == null || txtName.getText().trim().isEmpty()) {
            errorMessage += "- Le nom du plat est obligatoire.\n";
        }

        if (txtPrice.getText() == null || txtPrice.getText().trim().isEmpty()) {
            errorMessage += "- Le prix est obligatoire.\n";
        } else {
            try {
                BigDecimal price = new BigDecimal(txtPrice.getText().trim());
                if (price.compareTo(BigDecimal.ZERO) <= 0) {
                    errorMessage += "- Le prix doit être supérieur à 0.\n";
                }
            } catch (NumberFormatException e) {
                errorMessage += "- Le prix doit être un nombre valide.\n";
            }
        }

        if (singleImage == null) {
            errorMessage += "- Une image du plat est obligatoire.\n";
        }

        if (errorMessage.isEmpty()) {
            return true;
        } else {
            showAlert(Alert.AlertType.WARNING, "Validation Échouée", errorMessage);
            return false;
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML private void handleCancel() { parentController.closeMenuForm(); }
}