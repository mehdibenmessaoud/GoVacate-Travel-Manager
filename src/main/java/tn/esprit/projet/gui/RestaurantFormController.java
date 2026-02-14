package tn.esprit.projet.gui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;
import tn.esprit.projet.entities.*;
import tn.esprit.projet.entities.Menu;
import tn.esprit.projet.services.DestinationService;
import tn.esprit.projet.services.MenuService;
import tn.esprit.projet.services.RestaurantImageService;
import tn.esprit.projet.services.RestaurantService;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class RestaurantFormController implements Initializable {

    @FXML private VBox rootPane;
    @FXML private Label lblTitle;
    @FXML private TextField txtName, txtAddress, txtEmail, txtPhone;
    @FXML private ComboBox<String> cbCategory, cbStatus;
    @FXML private ComboBox<Destination> cbDestination;
    @FXML private Spinner<Integer> spnCapacity;
    @FXML private FlowPane imageFlowPane;

    // Menu Table and Filtering
    @FXML private TableView<Menu> menuTable;
    @FXML private TableColumn<Menu, String> colMenuName;
    @FXML private TableColumn<Menu, BigDecimal> colMenuPrice;
    @FXML private TableColumn<Menu, Void> colMenuActions;
    @FXML private TextField menuSearchField;
    @FXML private ComboBox<String> menuStatusFilter;

    private final RestaurantService rs = new RestaurantService();
    private final RestaurantImageService ris = new RestaurantImageService();
    private final MenuService ms = new MenuService();
    private final DestinationService ds = new DestinationService();

    private Restaurant currentRestaurant;
    private AdminController mainController;

    private final ObservableList<Menu> tempMenuList = FXCollections.observableArrayList();
    private FilteredList<Menu> filteredMenuList;
    private final List<File> selectedFiles = new ArrayList<>();
    private final List<Integer> imagesToDelete = new ArrayList<>();
    private final List<Menu> menusToDelete = new ArrayList<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        cbStatus.setItems(FXCollections.observableArrayList("OPEN", "CLOSED", "SUSPENDED"));
        cbStatus.setValue("OPEN");
        cbCategory.setItems(FXCollections.observableArrayList("Gastronomique", "Bistro", "Fast Food", "Pizzeria",
                "Cuisine Tunisienne", "Cuisine Italienne", "Cuisine Française",
                "Cuisine Asiatique", "Fruits de Mer", "Steakhouse",
                "Végétarien / Vegan", "Halel", "Brunch", "Street Food"));
        spnCapacity.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 1000, 20));

        setupMenuTable();
        loadDestinations();
        setupMenuFilters();
    }

    private void loadDestinations() {
        try {
            List<Destination> list = ds.getAll();
            cbDestination.setItems(FXCollections.observableArrayList(list));
            cbDestination.setConverter(new StringConverter<Destination>() {
                @Override
                public String toString(Destination d) { return (d == null) ? "" : d.getNameDestination(); }
                @Override
                public Destination fromString(String s) { return null; }
            });
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void setupMenuTable() {
        colMenuName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colMenuPrice.setCellValueFactory(new PropertyValueFactory<>("price"));

        colMenuActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnDel = new Button("×");
            private final Button btnEdit = new Button("Editer");
            private final HBox container = new HBox(btnEdit, btnDel);
            {
                container.setSpacing(5);
                btnDel.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-cursor: hand;");
                btnEdit.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-cursor: hand;");

                btnDel.setOnAction(e -> {
                    Menu m = getTableView().getItems().get(getIndex());
                    if (m.getId() != 0) menusToDelete.add(m);
                    tempMenuList.remove(m);
                });

                btnEdit.setOnAction(e -> openMenuDetailForm(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : container);
            }
        });
    }

    private void setupMenuFilters() {
        if (menuStatusFilter != null) {
            menuStatusFilter.setItems(FXCollections.observableArrayList("Tous", "AVAILABLE", "UNAVAILABLE"));
            menuStatusFilter.setValue("Tous");
        }

        filteredMenuList = new FilteredList<>(tempMenuList, p -> true);

        if (menuSearchField != null) {
            menuSearchField.textProperty().addListener((obs, old, newVal) -> applyMenuPredicate());
        }
        if (menuStatusFilter != null) {
            menuStatusFilter.valueProperty().addListener((obs, old, newVal) -> applyMenuPredicate());
        }

        SortedList<Menu> sortedMenuData = new SortedList<>(filteredMenuList);
        sortedMenuData.comparatorProperty().bind(menuTable.comparatorProperty());
        menuTable.setItems(sortedMenuData);
    }

    private void applyMenuPredicate() {
        String search = (menuSearchField.getText() == null) ? "" : menuSearchField.getText().toLowerCase().trim();
        String status = menuStatusFilter.getValue();

        filteredMenuList.setPredicate(m -> {
            boolean matchesStatus = status.equals("Tous") || (m.getStatus() != null && m.getStatus().equals(status));
            boolean matchesSearch = search.isEmpty() || (m.getName() != null && m.getName().toLowerCase().contains(search));
            return matchesStatus && matchesSearch;
        });
    }

    @FXML private void sortMenuByName() {
        menuTable.getSortOrder().clear();
        colMenuName.setSortType(TableColumn.SortType.ASCENDING);
        menuTable.getSortOrder().add(colMenuName);
    }

    @FXML private void sortMenuByPriceDesc() {
        menuTable.getSortOrder().clear();
        colMenuPrice.setSortType(TableColumn.SortType.DESCENDING);
        menuTable.getSortOrder().add(colMenuPrice);
    }

    @FXML private void sortMenuByRecent() {
        tempMenuList.sort((m1, m2) -> Integer.compare(m2.getId(), m1.getId()));
        menuTable.refresh();
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

        for (Destination d : cbDestination.getItems()) {
            if (d.getId() == r.getDestinationId()) {
                cbDestination.setValue(d);
                break;
            }
        }
        loadExistingData(r.getId());
    }

    private void loadExistingData(int id) {
        try {
            tempMenuList.setAll(ms.getAll().stream().filter(m -> m.getRestaurantId() == id).toList());
            ris.getByRestaurantId(id).forEach(img -> addThumbnail(img.getImageUrl(), img.getId()));
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @FXML
    private void btnAddMenuItem() {
        openMenuDetailForm(null);
    }

    private void openMenuDetailForm(Menu menu) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/RestaurantMenuFormView.fxml"));
            Parent menuFormRoot = loader.load();
            RestaurantMenuFormController controller = loader.getController();
            controller.setParentController(this);
            controller.setMenuData(menu);
            if (mainController != null) {
                mainController.getMainBorderPane().setCenter(menuFormRoot);
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML
    private void handleUploadImage() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Sélectionner des images");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.jpg", "*.jpeg", "*.png"));
        List<File> files = fc.showOpenMultipleDialog(null);
        if (files != null) {
            files.forEach(f -> {
                if (selectedFiles.stream().noneMatch(ef -> ef.getAbsolutePath().equals(f.getAbsolutePath()))) {
                    selectedFiles.add(f);
                    addThumbnail(f.toURI().toString(), null);
                }
            });
        }
    }

    private void addThumbnail(String url, Integer dbId) {
        StackPane container = new StackPane();
        container.setPrefSize(100, 100);
        container.setStyle("-fx-border-color: #ddd; -fx-background-radius: 5;");

        ImageView iv = new ImageView(new Image(url));
        iv.setFitWidth(90); iv.setFitHeight(90); iv.setPreserveRatio(true);

        Button btnDel = new Button("×");
        btnDel.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-background-radius: 50%;");
        StackPane.setAlignment(btnDel, Pos.TOP_RIGHT);

        btnDel.setOnAction(e -> {
            imageFlowPane.getChildren().remove(container);
            if (dbId != null) imagesToDelete.add(dbId);
            else selectedFiles.removeIf(f -> f.toURI().toString().equals(url));
        });

        container.getChildren().addAll(iv, btnDel);
        imageFlowPane.getChildren().add(container);
    }

    @FXML
    private void handleSave() {
        try {
            if (txtName.getText().isEmpty() || cbDestination.getValue() == null) {
                new Alert(Alert.AlertType.WARNING, "Nom et Destination sont obligatoires.").show();
                return;
            }

            if (currentRestaurant == null) currentRestaurant = new Restaurant();

            currentRestaurant.setName(txtName.getText());
            currentRestaurant.setCategory(cbCategory.getValue());
            currentRestaurant.setAddress(txtAddress.getText());
            currentRestaurant.setPhone(txtPhone.getText());
            currentRestaurant.setEmail(txtEmail.getText());
            currentRestaurant.setCapacity(spnCapacity.getValue());
            currentRestaurant.setStatus(cbStatus.getValue());
            currentRestaurant.setDestinationId(cbDestination.getValue().getId());

            if (currentRestaurant.getId() == 0) {
                rs.create(currentRestaurant);
                currentRestaurant = rs.getAll().stream()
                        .filter(res -> res.getName().equals(txtName.getText()))
                        .findFirst().orElse(null);
            } else {
                rs.update(currentRestaurant);
            }

            if (currentRestaurant == null) return;
            int restaurantId = currentRestaurant.getId();

            for (Menu m : menusToDelete) ms.delete(m.getId());
            for (Menu m : tempMenuList) {
                m.setRestaurantId(restaurantId);
                if (m.getId() == 0) ms.create(m); else ms.update(m);
            }

            for (Integer imgId : imagesToDelete) ris.delete(imgId);
            for (File file : selectedFiles) {
                RestaurantImage newImg = new RestaurantImage();
                newImg.setRestaurantId(restaurantId);
                newImg.setImageUrl(file.toURI().toString());
                ris.create(newImg);
            }

            handleBack();
        } catch (SQLException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Erreur BD: " + e.getMessage()).show();
        }
    }

    @FXML private void handleBack() {
        if(mainController != null) mainController.loadSection("/RestaurantAdminView.fxml");
    }

    public void addOrUpdateMenuItem(Menu menu, List<MenuImage> images) {
        if (!tempMenuList.contains(menu)) {
            tempMenuList.add(menu);
        }
        menuTable.refresh();
        closeMenuForm();
    }

    public void closeMenuForm() {
        if (mainController != null) {
            mainController.getMainBorderPane().setCenter(rootPane);
        }
    }
}