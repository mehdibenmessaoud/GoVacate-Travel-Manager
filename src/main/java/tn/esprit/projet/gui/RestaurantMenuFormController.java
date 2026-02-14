package tn.esprit.projet.gui;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import tn.esprit.projet.entities.Menu;
import tn.esprit.projet.entities.MenuImage;
import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class RestaurantMenuFormController {

    @FXML private Label lblTitle;
    @FXML private TextField txtName, txtPrice;
    @FXML private TextArea txtDescription;
    @FXML private ComboBox<String> cbStatus;
    @FXML private VBox imagePreviewContainer;

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
        }
    }

    @FXML
    private void handleUploadImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg"));
        File file = fileChooser.showOpenDialog(txtName.getScene().getWindow());

        if (file != null) {
            singleImage = new MenuImage();
            singleImage.setImageUrl(file.toURI().toString());

            imagePreviewContainer.getChildren().clear();
            ImageView imageView = new ImageView(new Image(file.toURI().toString()));
            imageView.setFitWidth(150);
            imageView.setPreserveRatio(true);
            imagePreviewContainer.getChildren().add(imageView);
        }
    }

    @FXML
    private void handleSave() {
        if (validateInput()) {
            if (currentMenu == null) currentMenu = new Menu();

            currentMenu.setName(txtName.getText());
            currentMenu.setDescription(txtDescription.getText());
            currentMenu.setPrice(new BigDecimal(txtPrice.getText()));
            currentMenu.setStatus(cbStatus.getValue());

            List<MenuImage> images = new ArrayList<>();
            if (singleImage != null) images.add(singleImage);

            parentController.addOrUpdateMenuItem(currentMenu, images);
        }
    }

    private boolean validateInput() {
        try {
            if (txtName.getText().isEmpty()) return false;
            new BigDecimal(txtPrice.getText());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @FXML
    private void handleCancel() {
        parentController.closeMenuForm();
    }
}