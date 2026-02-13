package tn.esprit.projet.gui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import tn.esprit.projet.entities.Menu;
import tn.esprit.projet.entities.Restaurant;
import tn.esprit.projet.entities.RestaurantImage;
import tn.esprit.projet.services.MenuService;
import tn.esprit.projet.services.RestaurantImageService;
import tn.esprit.projet.services.RestaurantService;

import java.io.File;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class RestaurantFormController implements Initializable {

    @FXML private Label lblTitle;
    @FXML private TextField txtName, txtAddress, txtEmail, txtPhone, txtMenuItemName, txtMenuItemPrice;
    @FXML private ComboBox<String> cbCategory, cbStatus;
    @FXML private Spinner<Integer> spnCapacity;
    @FXML private FlowPane imageFlowPane;
    @FXML private Button btnAddMenuAction; // Rename fx:id if it conflicts with method name

    @FXML private TableView<Menu> menuTable;
    @FXML private TableColumn<Menu, String> colMenuName;
    @FXML private TableColumn<Menu, BigDecimal> colMenuPrice;
    @FXML private TableColumn<Menu, Void> colMenuActions;

    private final RestaurantService rs = new RestaurantService();
    private final RestaurantImageService ris = new RestaurantImageService();
    private final MenuService ms = new MenuService();

    private Restaurant currentRestaurant;
    private AdminController mainController;

    private final ObservableList<Menu> tempMenuList = FXCollections.observableArrayList();
    private final List<File> selectedFiles = new ArrayList<>();
    private final List<Integer> imagesToDelete = new ArrayList<>();
    private final List<Menu> menusToDelete = new ArrayList<>();
    private Menu editingMenu = null;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        cbStatus.setItems(FXCollections.observableArrayList("OPEN", "CLOSED", "SUSPENDED"));
        cbStatus.setValue("OPEN");
        cbCategory.setItems(FXCollections.observableArrayList("Gastronomique", "Fast Food", "Pizzeria", "Café", "Bistro"));
        spnCapacity.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 1000, 20));

        setupMenuTable();
    }

    private void setupMenuTable() {
        colMenuName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colMenuPrice.setCellValueFactory(new PropertyValueFactory<>("price"));

        colMenuActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnDel = new Button("×");
            {
                btnDel.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-cursor: hand;");
                btnDel.setOnAction(e -> {
                    Menu m = getTableView().getItems().get(getIndex());
                    if (m.getId() != 0) menusToDelete.add(m);
                    tempMenuList.remove(m);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setGraphic(null);
                else setGraphic(btnDel);
            }
        });
        menuTable.setItems(tempMenuList);
    }

    public void setMainController(AdminController controller) {
        this.mainController = controller;
    }

    public void setRestaurantData(Restaurant r) {
        this.currentRestaurant = r;
        if(lblTitle != null) lblTitle.setText("Modifier " + r.getName());
        txtName.setText(r.getName());
        txtAddress.setText(r.getAddress());
        txtEmail.setText(r.getEmail());
        txtPhone.setText(r.getPhone());
        cbCategory.setValue(r.getCategory());
        cbStatus.setValue(r.getStatus());
        spnCapacity.getValueFactory().setValue(r.getCapacity());

        loadExistingData(r.getId());
    }

    private void loadExistingData(int id) {
        try {
            tempMenuList.setAll(ms.getAll().stream().filter(m -> m.getRestaurantId() == id).toList());
            ris.getByRestaurantId(id).forEach(img -> addThumbnail(img.getImageUrl(), img.getId()));
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // THIS METHOD NAME MUST MATCH FXML onAction='#btnAddMenuItem'
    @FXML
    private void btnAddMenuItem() {
        try {
            String name = txtMenuItemName.getText();
            String priceStr = txtMenuItemPrice.getText();

            if (name.isEmpty() || priceStr.isEmpty()) return;

            BigDecimal price = new BigDecimal(priceStr);
            tempMenuList.add(new Menu(0, name, "", price, "OPEN", 0));

            txtMenuItemName.clear();
            txtMenuItemPrice.clear();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Prix invalide").show();
        }
    }

    @FXML
    private void handleUploadImage() {
        FileChooser fc = new FileChooser();
        List<File> files = fc.showOpenMultipleDialog(null);
        if (files != null) {
            files.forEach(f -> {
                selectedFiles.add(f);
                addThumbnail(f.toURI().toString(), null);
            });
        }
    }

    private void addThumbnail(String url, Integer dbId) {
        // 1. Create the container
        StackPane container = new StackPane();
        container.setPrefSize(100, 100);
        container.setStyle("-fx-border-color: #ddd; -fx-border-radius: 5; -fx-background-radius: 5; -fx-padding: 2;");

        // 2. Setup the Image
        ImageView iv = new ImageView(new Image(url));
        iv.setFitWidth(95);
        iv.setFitHeight(95);
        iv.setPreserveRatio(true);

        // 3. Setup the Delete Button (Red X)
        Button btnDel = new Button("×");
        // Styling the button to be a small red circle
        btnDel.setStyle(
                "-fx-background-color: #e74c3c; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-weight: bold; " +
                        "-fx-background-radius: 50%; " +
                        "-fx-min-width: 22px; " +
                        "-fx-min-height: 22px; " +
                        "-fx-max-width: 22px; " +
                        "-fx-max-height: 22px; " +
                        "-fx-padding: 0; " +
                        "-fx-cursor: hand;"
        );

        // 4. Position the button in the TOP_RIGHT corner
        StackPane.setAlignment(btnDel, Pos.TOP_RIGHT);

        // Offset the button slightly so it overlaps the corner neatly
        btnDel.setTranslateX(5);
        btnDel.setTranslateY(-5);

        // 5. Delete Logic
        btnDel.setOnAction(e -> {
            imageFlowPane.getChildren().remove(container);
            if (dbId != null) {
                imagesToDelete.add(dbId);
            } else {
                selectedFiles.removeIf(f -> f.toURI().toString().equals(url));
            }
        });

        // 6. Assemble
        container.getChildren().addAll(iv, btnDel);
        imageFlowPane.getChildren().add(container);
    }
    @FXML
    private void handleSave() {
        try {
            if (currentRestaurant == null) currentRestaurant = new Restaurant();
            currentRestaurant.setName(txtName.getText());
            currentRestaurant.setCategory(cbCategory.getValue());
            currentRestaurant.setAddress(txtAddress.getText());
            currentRestaurant.setPhone(txtPhone.getText());
            currentRestaurant.setEmail(txtEmail.getText());
            currentRestaurant.setCapacity(spnCapacity.getValue());
            currentRestaurant.setStatus(cbStatus.getValue());

            if (currentRestaurant.getId() == 0) {
                rs.create(currentRestaurant);
                // Simple way to get the generated ID if your service doesn't return it
                currentRestaurant = rs.getAll().stream().filter(res -> res.getName().equals(txtName.getText())).findFirst().orElse(null);
            } else {
                rs.update(currentRestaurant);
            }

            // Sync Menus
            for (Menu m : menusToDelete) ms.delete(m.getId());
            for (Menu m : tempMenuList) {
                m.setRestaurantId(currentRestaurant.getId());
                if (m.getId() == 0) ms.create(m); else ms.update(m);
            }

            handleBack();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @FXML private void handleBack() {
        if(mainController != null) mainController.loadSection("/RestaurantAdminView.fxml");
    }
}