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
import tn.esprit.projet.entities.Restaurant;
import tn.esprit.projet.services.RestaurantService;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class RestaurantAdminController implements Initializable {

    @FXML private TableView<Restaurant> restaurantTable;
    @FXML private TableColumn<Restaurant, String> colName, colCategory, colDestination, colAddress, colPhone, colStatus;
    @FXML private TableColumn<Restaurant, Integer> colCapacity;
    @FXML private TableColumn<Restaurant, Void> colActions;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> categoryFilter;
    @FXML private ComboBox<String> statusFilter;

    private final RestaurantService rs = new RestaurantService();
    private final ObservableList<Restaurant> masterData = FXCollections.observableArrayList();
    private FilteredList<Restaurant> filteredData;
    private AdminController mainAdminController;

    public void setMainAdminController(AdminController controller) {
        this.mainAdminController = controller;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupColumns();
        loadData();
        setupSearchAndFilters();
    }

    private void setupColumns() {
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        colDestination.setCellValueFactory(new PropertyValueFactory<>("destinationName"));
        colAddress.setCellValueFactory(new PropertyValueFactory<>("address"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colCapacity.setCellValueFactory(new PropertyValueFactory<>("capacity"));

        // Status Column with CSS Class triggers
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                // Important: Clean up old classes to prevent color bleeding on scroll
                getStyleClass().removeAll("status-open", "status-closed", "status-suspended");

                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.toUpperCase());
                    switch (item.toUpperCase()) {
                        case "OPEN" -> getStyleClass().add("status-open");
                        case "CLOSED" -> getStyleClass().add("status-closed");
                        case "SUSPENDED" -> getStyleClass().add("status-suspended");
                    }
                }
            }
        });

        // Actions Column
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnView = new Button("Détails");
            private final Button btnEdit = new Button("Modifier");
            private final Button btnDelete = new Button("X");
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
        });
    }

    private void setupSearchAndFilters() {
        statusFilter.setItems(FXCollections.observableArrayList("Tous les statuts", "OPEN", "CLOSED", "SUSPENDED"));
        statusFilter.setValue("Tous les statuts");

        filteredData = new FilteredList<>(masterData, p -> true);

        // Listeners for all filter inputs
        searchField.textProperty().addListener((obs, old, newVal) -> applyPredicate());
        categoryFilter.valueProperty().addListener((obs, old, newVal) -> applyPredicate());
        statusFilter.valueProperty().addListener((obs, old, newVal) -> applyPredicate());

        SortedList<Restaurant> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(restaurantTable.comparatorProperty());
        restaurantTable.setItems(sortedData);
    }

    private void applyPredicate() {
        String search = (searchField.getText() == null) ? "" : searchField.getText().toLowerCase().trim();
        String cat = categoryFilter.getValue();
        String stat = statusFilter.getValue();

        filteredData.setPredicate(r -> {
            // Category Filter
            boolean matchesCategory = (cat == null || cat.equals("Toutes les catégories") ||
                    (r.getCategory() != null && r.getCategory().equals(cat)));

            // Status Filter
            boolean matchesStatus = (stat == null || stat.equals("Tous les statuts") ||
                    (r.getStatus() != null && r.getStatus().equalsIgnoreCase(stat)));

            if (!matchesCategory || !matchesStatus) return false;
            if (search.isEmpty()) return true;

            // Search text logic
            String name = (r.getName() != null) ? r.getName().toLowerCase() : "";
            String dest = (r.getDestinationName() != null) ? r.getDestinationName().toLowerCase() : "";

            return name.contains(search) || dest.contains(search) || r.getPhone().contains(search);
        });
    }

    private void loadData() {
        try {
            masterData.setAll(rs.getAll());
            updateCategoryFilterOptions();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateCategoryFilterOptions() {
        Set<String> catSet = new TreeSet<>(); // TreeSet keeps them alphabetical
        catSet.add("Toutes les catégories");

        // Add existing ones from DB
        masterData.forEach(r -> {
            if (r.getCategory() != null && !r.getCategory().isEmpty()) catSet.add(r.getCategory());
        });

        categoryFilter.setItems(FXCollections.observableArrayList(catSet));
        categoryFilter.setValue("Toutes les catégories");
    }

    // Navigation Methods
    @FXML private void handleAddRestaurant() { switchToForm(null); }

    private void switchToForm(Restaurant r) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/RestaurantFormView.fxml"));
            Parent root = loader.load();
            RestaurantFormController formCtrl = loader.getController();
            formCtrl.setMainController(this.mainAdminController);
            if (r != null) formCtrl.setRestaurantData(r);
            mainAdminController.getMainBorderPane().setCenter(root);
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void handleView(Restaurant r) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/RestaurantDetailView.fxml"));
            Parent root = loader.load();
            RestaurantDetailController detailCtrl = loader.getController();

            detailCtrl.setMainController(this.mainAdminController);
            detailCtrl.setRestaurantData(r);

            mainAdminController.getMainBorderPane().setCenter(root);
        } catch (IOException e) {
            System.err.println("Error loading Detail View: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleDelete(Restaurant r) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer '" + r.getName() + "' ?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(res -> {
            if (res == ButtonType.YES) {
                try {
                    rs.delete(r.getId());
                    loadData();
                } catch (SQLException e) { e.printStackTrace(); }
            }
        });
    }

    // Sort Menu Actions for FXML
    @FXML private void sortByNameAsc() {
        restaurantTable.getSortOrder().clear();
        colName.setSortType(TableColumn.SortType.ASCENDING);
        restaurantTable.getSortOrder().add(colName);
    }

    @FXML private void sortByCapacityDesc() {
        restaurantTable.getSortOrder().clear();
        colCapacity.setSortType(TableColumn.SortType.DESCENDING);
        restaurantTable.getSortOrder().add(colCapacity);
    }

    @FXML private void sortByDateDesc() { /* If your entity has a date field */ }

    @FXML private void resetFilters() {
        searchField.clear();
        categoryFilter.setValue("Toutes les catégories");
        statusFilter.setValue("Tous les statuts");
    }
}