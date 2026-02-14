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
import tn.esprit.projet.services.*;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.SQLException;
import java.util.*;

public class RestaurantFormController implements Initializable {

    // --- FXML UI Components ---
    @FXML private VBox rootPane;
    @FXML private Label lblTitle;
    @FXML private TextField txtName, txtAddress, txtEmail, txtPhone;
    @FXML private ComboBox<String> cbCategory, cbStatus;
    @FXML private ComboBox<Destination> cbDestination;
    @FXML private Spinner<Integer> spnCapacity;
    @FXML private FlowPane imageFlowPane;

    @FXML private TableView<Menu> menuTable;
    @FXML private TableColumn<Menu, String> colMenuName;
    @FXML private TableColumn<Menu, String> colMenuDesc; // New
    @FXML private TableColumn<Menu, BigDecimal> colMenuPrice;
    @FXML private TableColumn<Menu, String> colMenuStatus; // New
    @FXML private TableColumn<Menu, Void> colMenuImage; // New
    @FXML private TableColumn<Menu, Void> colMenuActions;
    @FXML private TextField menuSearchField;
    @FXML private ComboBox<String> menuStatusFilter;

    // --- Services ---
    private final RestaurantService rs = new RestaurantService();
    private final RestaurantImageService ris = new RestaurantImageService();
    private final MenuService ms = new MenuService();
    private final MenuImageService mis = new MenuImageService();
    private final DestinationService ds = new DestinationService();

    // --- State Management ---
    private Restaurant currentRestaurant;
    private AdminController mainController;

    private final ObservableList<Menu> tempMenuList = FXCollections.observableArrayList();
    private final Map<Menu, List<MenuImage>> menuImageMap = new HashMap<>();

    private FilteredList<Menu> filteredMenuList;
    private final List<File> selectedFiles = new ArrayList<>();
    private final List<Integer> imagesToDelete = new ArrayList<>();
    private final List<Menu> menusToDelete = new ArrayList<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Setup Combo Boxes
        cbStatus.setItems(FXCollections.observableArrayList("OPEN", "CLOSED", "SUSPENDED"));
        cbStatus.setValue("OPEN");
        cbCategory.setItems(FXCollections.observableArrayList(
                "Gastronomique", "Bistro", "Fast Food", "Pizzeria", "Cuisine Tunisienne",
                "Cuisine Italienne", "Cuisine Française", "Cuisine Asiatique", "Fruits de Mer",
                "Steakhouse", "Végétarien / Vegan", "Halel", "Brunch", "Street Food"
        ));

        // Setup Spinner
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
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void setupMenuTable() {
        colMenuName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colMenuPrice.setCellValueFactory(new PropertyValueFactory<>("price"));

        // ADDED NEW COLUMN MAPPINGS
        if (colMenuDesc != null) colMenuDesc.setCellValueFactory(new PropertyValueFactory<>("description"));
        if (colMenuStatus != null) colMenuStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        // ADDED IMAGE COLUMN CELL FACTORY
        if (colMenuImage != null) {
            colMenuImage.setCellFactory(param -> new TableCell<>() {
                private final ImageView iv = new ImageView();
                {
                    iv.setFitHeight(50);
                    iv.setFitWidth(50);
                    iv.setPreserveRatio(true);
                }
                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        Menu m = getTableView().getItems().get(getIndex());
                        List<MenuImage> images = menuImageMap.get(m);
                        if (images != null && !images.isEmpty()) {
                            iv.setImage(new Image(images.get(0).getImageUrl(), true));
                            setGraphic(iv);
                        } else {
                            setGraphic(new Label("No Img"));
                        }
                    }
                }
            });
        }

        colMenuActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnDel = new Button("×");
            private final Button btnEdit = new Button("Editer");
            private final HBox container = new HBox(btnEdit, btnDel);
            {
                container.setSpacing(8);
                btnDel.getStyleClass().add("btn-delete");
                btnEdit.getStyleClass().add("btn-edit");

                btnDel.setOnAction(e -> {
                    Menu m = getTableView().getItems().get(getIndex());
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer ce plat ?", ButtonType.YES, ButtonType.NO);
                    alert.showAndWait().ifPresent(response -> {
                        if (response == ButtonType.YES) {
                            if (m.getId() != 0) menusToDelete.add(m);
                            tempMenuList.remove(m);
                            menuImageMap.remove(m);
                        }
                    });
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

        // Listeners for Search and Filter
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
        String status = (menuStatusFilter.getValue() == null) ? "Tous" : menuStatusFilter.getValue();

        filteredMenuList.setPredicate(m -> {
            boolean matchesStatus = status.equals("Tous") || (m.getStatus() != null && m.getStatus().equals(status));
            boolean matchesSearch = search.isEmpty() ||
                    (m.getName() != null && m.getName().toLowerCase().contains(search)) ||
                    (m.getDescription() != null && m.getDescription().toLowerCase().contains(search));
            return matchesStatus && matchesSearch;
        });
    }

    // --- Extra Sorting Methods Restored ---
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

        // Match Destination in ComboBox
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
            // Load Menus
            List<Menu> menus = ms.getAll().stream().filter(m -> m.getRestaurantId() == id).toList();
            tempMenuList.setAll(menus);

            // CRUCIAL: Load images for each menu into memory map
            for (Menu m : menus) {
                List<MenuImage> images = mis.getByMenuId(m.getId());
                if (!images.isEmpty()) {
                    menuImageMap.put(m, images);
                }
            }

            // Load Restaurant Images
            ris.getByRestaurantId(id).forEach(img -> addThumbnail(img.getImageUrl(), img.getId()));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML private void btnAddMenuItem() { openMenuDetailForm(null); }

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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML private void handleUploadImage() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Sélectionner des images");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.jpg", "*.jpeg", "*.png"));
        List<File> files = fc.showOpenMultipleDialog(txtName.getScene().getWindow());
        if (files != null) {
            files.forEach(f -> {
                selectedFiles.add(f);
                addThumbnail(f.toURI().toString(), null);
            });
        }
    }

    private void addThumbnail(String url, Integer dbId) {
        StackPane container = new StackPane();
        container.setPrefSize(100, 100);
        container.getStyleClass().add("image-thumbnail-container");

        ImageView iv = new ImageView(new Image(url));
        iv.setFitWidth(90); iv.setFitHeight(90); iv.setPreserveRatio(true);

        Button btnDel = new Button("×");
        btnDel.getStyleClass().add("btn-delete-small");
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
                new Alert(Alert.AlertType.WARNING, "Veuillez remplir les champs obligatoires.").show();
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

            // 1. Save Restaurant
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

            // 2. Handle Menus
            for (Menu m : menusToDelete) ms.delete(m.getId());
            for (Menu m : tempMenuList) {
                m.setRestaurantId(restaurantId);
                int menuId;
                if (m.getId() == 0) {
                    ms.create(m);
                    // Fetch to get ID
                    Menu savedMenu = ms.getAll().stream()
                            .filter(sm -> sm.getName().equals(m.getName()) && sm.getRestaurantId() == restaurantId)
                            .findFirst().orElse(null);
                    menuId = (savedMenu != null) ? savedMenu.getId() : 0;
                } else {
                    ms.update(m);
                    menuId = m.getId();
                }

                // SAVE THE LINKED IMAGES FOR THIS MENU
                if (menuId != 0 && menuImageMap.containsKey(m)) {
                    for (MenuImage mi : menuImageMap.get(m)) {
                        if (mi.getId() == 0) { // Only save new ones
                            mi.setMenuId(menuId);
                            mis.create(mi);
                        }
                    }
                }
            }

            // 3. Handle Restaurant Images
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
            new Alert(Alert.AlertType.ERROR, "Erreur lors de l'enregistrement : " + e.getMessage()).show();
        }
    }

    public void addOrUpdateMenuItem(Menu menu, List<MenuImage> images) {
        if (!tempMenuList.contains(menu)) {
            tempMenuList.add(menu);
        }
        menuImageMap.put(menu, images);
        menuTable.refresh();
        closeMenuForm();
    }

    public void closeMenuForm() {
        if (mainController != null) {
            mainController.getMainBorderPane().setCenter(rootPane);
        }
    }

    @FXML private void handleBack() {
        if(mainController != null) mainController.loadSection("/RestaurantAdminView.fxml");
    }

    // ADDED METHOD TO ALLOW CHILD FORM TO CHECK MEMORY
    public List<MenuImage> getImagesForMenu(Menu m) {
        return menuImageMap.get(m);
    }
}