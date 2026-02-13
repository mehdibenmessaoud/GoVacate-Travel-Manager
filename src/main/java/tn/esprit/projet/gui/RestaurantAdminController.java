package tn.esprit.projet.gui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.util.Callback;
import tn.esprit.projet.entities.Restaurant;
import tn.esprit.projet.services.RestaurantService;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class RestaurantAdminController implements Initializable {

    @FXML private TableView<Restaurant> restaurantTable;
    @FXML private TableColumn<Restaurant, String> colName, colCategory, colAddress, colPhone, colStatus;
    @FXML private TableColumn<Restaurant, Integer> colCapacity;
    @FXML private TableColumn<Restaurant, Void> colActions;
    @FXML private TextField searchField;

    private final RestaurantService rs = new RestaurantService();
    private final ObservableList<Restaurant> masterData = FXCollections.observableArrayList();

    // Reference to main AdminController for page switching
    private AdminController mainAdminController;

    public void setMainAdminController(AdminController controller) {
        this.mainAdminController = controller;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupColumns();
        loadData();
        setupSearch();
    }

    private void setupColumns() {
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        colAddress.setCellValueFactory(new PropertyValueFactory<>("address"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colCapacity.setCellValueFactory(new PropertyValueFactory<>("capacity"));

        // Improved Status Column with dynamic CSS colors
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    // Match your ENUM values
                    switch (item.toUpperCase()) {
                        case "OPEN" -> setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold;");
                        case "CLOSED" -> setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                        case "SUSPENDED" -> setStyle("-fx-text-fill: #f1c40f; -fx-font-weight: bold;");
                        default -> setStyle("-fx-text-fill: white;");
                    }
                }
            }
        });

        Callback<TableColumn<Restaurant, Void>, TableCell<Restaurant, Void>> cellFactory = param -> new TableCell<>() {
            private final Button btnView = new Button("Détails");
            private final Button btnEdit = new Button("Modifier");
            private final Button btnDelete = new Button("Supprimer");
            private final HBox container = new HBox(btnView, btnEdit, btnDelete);

            {
                container.setSpacing(8);
                btnView.getStyleClass().add("btn-action-view");
                btnEdit.getStyleClass().add("btn-action-edit");
                btnDelete.getStyleClass().add("btn-action-delete");

                btnView.setOnAction(e -> handleView(getTableView().getItems().get(getIndex())));
                btnEdit.setOnAction(e -> switchToForm(getTableView().getItems().get(getIndex())));
                btnDelete.setOnAction(e -> handleDelete(getTableView().getItems().get(getIndex())));
            }

            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : container);
            }
        };
        colActions.setCellFactory(cellFactory);
    }

    private void loadData() {
        try {
            masterData.setAll(rs.getAll());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void setupSearch() {
        // Wrap masterData in FilteredList
        FilteredList<Restaurant> filteredData = new FilteredList<>(masterData, p -> true);

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredData.setPredicate(r -> {
                if (newVal == null || newVal.isEmpty()) return true;

                String filter = newVal.toLowerCase();

                // FIX: Null-safe string conversion to prevent NullPointerException
                String name = (r.getName() != null) ? r.getName().toLowerCase() : "";
                String category = (r.getCategory() != null) ? r.getCategory().toLowerCase() : "";
                String address = (r.getAddress() != null) ? r.getAddress().toLowerCase() : "";

                return name.contains(filter) || category.contains(filter) || address.contains(filter);
            });
        });

        // IMPROVEMENT: Wrap in SortedList so table sorting still works while filtering
        SortedList<Restaurant> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(restaurantTable.comparatorProperty());
        restaurantTable.setItems(sortedData);
    }

    @FXML
    private void handleAddRestaurant() {
        switchToForm(null);
    }

    private void switchToForm(Restaurant r) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/RestaurantFormView.fxml"));
            Parent root = loader.load();
            RestaurantFormController formCtrl = loader.getController();

            // Pass the main controller for navigation back
            formCtrl.setMainController(this.mainAdminController);

            if (r != null) {
                formCtrl.setRestaurantData(r);
            }

            // Replace the center content of your dashboard
            mainAdminController.getMainBorderPane().setCenter(root);
        } catch (IOException e) {
            System.err.println("Error loading RestaurantFormView: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleView(Restaurant r) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/RestaurantDetailView.fxml"));
            Parent root = loader.load();

            RestaurantDetailController detailCtrl = loader.getController();
            detailCtrl.setMainController(this.mainAdminController);
            detailCtrl.setRestaurantData(r);

            // Switch the view just like you do for Edit/Add
            mainAdminController.getMainBorderPane().setCenter(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleDelete(Restaurant r) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Voulez-vous vraiment supprimer le restaurant '" + r.getName() + "' ?", ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirmation de suppression");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                try {
                    rs.delete(r.getId());
                    loadData(); // Refresh the masterData list
                } catch (SQLException e) {
                    e.printStackTrace();
                    new Alert(Alert.AlertType.ERROR, "Erreur lors de la suppression").show();
                }
            }
        });
    }
}