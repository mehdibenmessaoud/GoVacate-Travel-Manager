package tn.esprit.projet.gui;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.util.Duration;
import tn.esprit.projet.entities.Restaurant;
import tn.esprit.projet.entities.Menu;
import tn.esprit.projet.entities.RestaurantReview;
import tn.esprit.projet.services.RestaurantService;
import tn.esprit.projet.services.MenuService;
import tn.esprit.projet.services.RestaurantReviewService;

import java.math.BigDecimal;
import java.net.URL;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class AdminController implements Initializable {
    // FXML COMPONENTS - Root & Layout
    @FXML private StackPane rootStack;
    @FXML private BorderPane mainRoot;
    @FXML private VBox modalOverlay, restaurantModal, menuModal;
    @FXML private ToggleButton themeToggle;
    
    // Navigation
    @FXML private Button navRestaurants, navMenus, navReviews;
    @FXML private Label viewTitle;
    
    // Content Areas
    @FXML private VBox restaurantsContent, menusContent, reviewsContent;
    
    // Restaurants Section
    @FXML private TableView<Restaurant> restaurantsTable;
    @FXML private TableColumn<Restaurant, Integer> resColId;
    @FXML private TableColumn<Restaurant, String> resColName, resColCategory, resColAddress, resColStatus;
    @FXML private TableColumn<Restaurant, Integer> resColCapacity;
    @FXML private TableColumn<Restaurant, Void> resColActions;
    @FXML private TextField resSearchField;
    
    // Restaurant Modal
    @FXML private Text modalTitle;
    @FXML private TextField resModalName, resModalCategory, resModalAddress, resModalPhone, resModalEmail;
    @FXML private Spinner<Integer> resModalCapacity;
    @FXML private ComboBox<String> resModalStatus;
    @FXML private Button btnSaveRestaurant;
    
    // Menus Section
    @FXML private TableView<Menu> menusTable;
    @FXML private TableColumn<Menu, Integer> menuColId;
    @FXML private TableColumn<Menu, String> menuColName, menuColRestaurant, menuColDescription, menuColStatus;
    @FXML private TableColumn<Menu, BigDecimal> menuColPrice;
    @FXML private TableColumn<Menu, Void> menuColActions;
    @FXML private TextField menuSearchField;
    
    // Menu Modal
    @FXML private Text menuModalTitle;
    @FXML private ComboBox<String> menuModalRestaurant;
    @FXML private TextField menuModalName;
    @FXML private TextArea menuModalDescription;
    @FXML private Spinner<Double> menuModalPrice;
    @FXML private ComboBox<String> menuModalStatus;
    @FXML private Button btnSaveMenu;
    
    // Reviews Section
    @FXML private TableView<RestaurantReview> reviewsTable;
    @FXML private TableColumn<RestaurantReview, Integer> revColId;
    @FXML private TableColumn<RestaurantReview, String> revColRestaurant;
    @FXML private TableColumn<RestaurantReview, Integer> revColRating;
    @FXML private TableColumn<RestaurantReview, String> revColComment;
    @FXML private TableColumn<RestaurantReview, String> revColDate;
    @FXML private TableColumn<RestaurantReview, Void> revColActions;
    @FXML private TextField reviewSearchField;
    
    // Services
    private RestaurantService restaurantService;
    private MenuService menuService;
    private RestaurantReviewService reviewService;
    
    // Data
    private ObservableList<Restaurant> restaurantsList = FXCollections.observableArrayList();
    private ObservableList<Menu> menusList = FXCollections.observableArrayList();
    private ObservableList<RestaurantReview> reviewsList = FXCollections.observableArrayList();
    
    // State
    private Restaurant selectedRestaurant = null;
    private Menu selectedMenu = null;
    private boolean isEditingRestaurant = false;
    private boolean isEditingMenu = false;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            System.out.println("Initializing Admin Dashboard...");
            
            // Initialize services
            restaurantService = new RestaurantService();
            menuService = new MenuService();
            reviewService = new RestaurantReviewService();
            
            // Initialize UI
            setupTableColumns();
            setupStatusComboBoxes();
            setupSpinners();
            setupSearchFields();
            
            // Load initial data
            loadRestaurantsTab();
            
            System.out.println("✓ Dashboard initialized successfully");
            
        } catch (NullPointerException e) {
            System.err.println("✗ NullPointerException during initialization: " + e.getMessage());
            showError("Erreur de base de données: Assurez-vous que MySQL est démarré\n\n" + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("✗ Error during initialization: " + e.getMessage());
            showError("Erreur d'initialisation:\n" + e.getMessage());
            e.printStackTrace();
        }
    }

    // ==================== INITIALIZATION METHODS ====================

    private void setupTableColumns() {
        // Restaurants Table
        resColId.setCellValueFactory(new PropertyValueFactory<>("id"));
        resColName.setCellValueFactory(new PropertyValueFactory<>("name"));
        resColCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        resColAddress.setCellValueFactory(new PropertyValueFactory<>("address"));
        resColStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        resColCapacity.setCellValueFactory(new PropertyValueFactory<>("capacity"));
        resColActions.setCellFactory(col -> createRestaurantActionCell());
        
        // Menus Table
        menuColId.setCellValueFactory(new PropertyValueFactory<>("id"));
        menuColName.setCellValueFactory(new PropertyValueFactory<>("name"));
        menuColDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        menuColPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        menuColStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        menuColRestaurant.setCellFactory(col -> createRestaurantNameCell());
        menuColActions.setCellFactory(col -> createMenuActionCell());
        
        // Reviews Table
        revColId.setCellValueFactory(new PropertyValueFactory<>("id"));
        revColRestaurant.setCellFactory(col -> createRestaurantNameForReviewCell());
        revColRating.setCellValueFactory(new PropertyValueFactory<>("rating"));
        revColComment.setCellValueFactory(new PropertyValueFactory<>("comment"));
        revColDate.setCellFactory(col -> createDateCell());
        revColActions.setCellFactory(col -> createReviewActionCell());
    }

    private void setupStatusComboBoxes() {
        ObservableList<String> statusOptions = FXCollections.observableArrayList("OPEN", "CLOSED", "SUSPENDED");
        resModalStatus.setItems(statusOptions);
        menuModalStatus.setItems(FXCollections.observableArrayList("AVAILABLE", "UNAVAILABLE"));
    }

    private void setupSpinners() {
        SpinnerValueFactory.IntegerSpinnerValueFactory capacityFactory = 
            new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 1000, 50);
        resModalCapacity.setValueFactory(capacityFactory);
        
        SpinnerValueFactory.DoubleSpinnerValueFactory priceFactory = 
            new SpinnerValueFactory.DoubleSpinnerValueFactory(0.0, 10000.0, 0.0, 0.5);
        menuModalPrice.setValueFactory(priceFactory);
    }

    private void setupSearchFields() {
        resSearchField.textProperty().addListener((obs, old, newVal) -> filterRestaurants(newVal));
        menuSearchField.textProperty().addListener((obs, old, newVal) -> filterMenus(newVal));
        reviewSearchField.textProperty().addListener((obs, old, newVal) -> filterReviews(newVal));
    }

    // ==================== NAVIGATION ====================

    @FXML private void loadRestaurantsTab() {
        switchTab(restaurantsContent, navRestaurants, "🏨 Restaurants");
        refreshRestaurants();
    }

    @FXML private void loadMenusTab() {
        switchTab(menusContent, navMenus, "🍴 Menus");
        refreshMenus();
    }

    @FXML private void loadReviewsTab() {
        switchTab(reviewsContent, navReviews, "⭐ Reviews");
        refreshReviews();
    }

    private void switchTab(VBox content, Button navBtn, String title) {
        restaurantsContent.setVisible(false);
        menusContent.setVisible(false);
        reviewsContent.setVisible(false);
        
        navRestaurants.getStyleClass().removeAll("nav-active");
        navMenus.getStyleClass().removeAll("nav-active");
        navReviews.getStyleClass().removeAll("nav-active");
        
        content.setVisible(true);
        navBtn.getStyleClass().add("nav-active");
        viewTitle.setText(title);
    }

    @FXML private void refreshData() {
        if (restaurantsContent.isVisible()) refreshRestaurants();
        else if (menusContent.isVisible()) refreshMenus();
        else if (reviewsContent.isVisible()) refreshReviews();
    }

    // ==================== RESTAURANTS CRUD ====================

    private void refreshRestaurants() {
        new Thread(() -> {
            try {
                List<Restaurant> restaurants = restaurantService.getAll();
                Platform.runLater(() -> {
                    restaurantsList.clear();
                    restaurantsList.addAll(restaurants);
                    restaurantsTable.setItems(restaurantsList);
                });
            } catch (SQLException e) {
                Platform.runLater(() -> showError("Erreur lors du chargement: " + e.getMessage()));
            }
        }).start();
    }

    @FXML private void showRestaurantForm() {
        isEditingRestaurant = false;
        selectedRestaurant = null;
        clearRestaurantForm();
        modalTitle.setText("Nouveau Restaurant");
        restaurantModal.setVisible(true);
        menuModal.setVisible(false);
        showModal();
    }

    private void editRestaurant(Restaurant restaurant) {
        isEditingRestaurant = true;
        selectedRestaurant = restaurant;
        resModalName.setText(restaurant.getName());
        resModalCategory.setText(restaurant.getCategory() != null ? restaurant.getCategory() : "");
        resModalAddress.setText(restaurant.getAddress() != null ? restaurant.getAddress() : "");
        resModalPhone.setText(restaurant.getPhone() != null ? restaurant.getPhone() : "");
        resModalEmail.setText(restaurant.getEmail() != null ? restaurant.getEmail() : "");
        resModalCapacity.getValueFactory().setValue(restaurant.getCapacity());
        resModalStatus.setValue(restaurant.getStatus());
        
        modalTitle.setText("Modifier Restaurant");
        restaurantModal.setVisible(true);
        menuModal.setVisible(false);
        showModal();
    }

    @FXML private void handleSaveRestaurant() {
        if (!validateRestaurantForm()) return;
        
        new Thread(() -> {
            try {
                if (isEditingRestaurant) {
                    selectedRestaurant.setName(resModalName.getText());
                    selectedRestaurant.setCategory(resModalCategory.getText());
                    selectedRestaurant.setAddress(resModalAddress.getText());
                    selectedRestaurant.setPhone(resModalPhone.getText());
                    selectedRestaurant.setEmail(resModalEmail.getText());
                    selectedRestaurant.setCapacity(resModalCapacity.getValue());
                    selectedRestaurant.setStatus(resModalStatus.getValue());
                    restaurantService.update(selectedRestaurant);
                    Platform.runLater(() -> showSuccess("Restaurant modifié avec succès"));
                } else {
                    Restaurant newRestaurant = new Restaurant();
                    newRestaurant.setName(resModalName.getText());
                    newRestaurant.setCategory(resModalCategory.getText());
                    newRestaurant.setAddress(resModalAddress.getText());
                    newRestaurant.setPhone(resModalPhone.getText());
                    newRestaurant.setEmail(resModalEmail.getText());
                    newRestaurant.setCapacity(resModalCapacity.getValue());
                    newRestaurant.setStatus(resModalStatus.getValue());
                    newRestaurant.setDestinationId(1);
                    restaurantService.create(newRestaurant);
                    Platform.runLater(() -> showSuccess("Restaurant créé avec succès"));
                }
                Platform.runLater(() -> {
                    hideModal();
                    refreshRestaurants();
                });
            } catch (SQLException e) {
                Platform.runLater(() -> showError("Erreur: " + e.getMessage()));
            }
        }).start();
    }

    private void deleteRestaurant(Restaurant restaurant) {
        if (showConfirm("Supprimer ce restaurant?")) {
            new Thread(() -> {
                try {
                    restaurantService.delete(restaurant.getId());
                    Platform.runLater(() -> {
                        showSuccess("Restaurant supprimé");
                        refreshRestaurants();
                    });
                } catch (SQLException e) {
                    Platform.runLater(() -> showError("Erreur: " + e.getMessage()));
                }
            }).start();
        }
    }

    private boolean validateRestaurantForm() {
        if (resModalName.getText().trim().isEmpty() || 
            resModalCategory.getText().trim().isEmpty() ||
            resModalAddress.getText().trim().isEmpty() ||
            resModalStatus.getValue() == null) {
            showError("Veuillez remplir tous les champs requis");
            return false;
        }
        return true;
    }

    private void clearRestaurantForm() {
        resModalName.clear();
        resModalCategory.clear();
        resModalAddress.clear();
        resModalPhone.clear();
        resModalEmail.clear();
        resModalCapacity.getValueFactory().setValue(50);
        resModalStatus.setValue(null);
    }

    private void filterRestaurants(String query) {
        if (query == null || query.isEmpty()) {
            restaurantsTable.setItems(restaurantsList);
        } else {
            String lowerQuery = query.toLowerCase();
            ObservableList<Restaurant> filtered = restaurantsList.filtered(r -> 
                r.getName().toLowerCase().contains(lowerQuery) ||
                r.getCategory().toLowerCase().contains(lowerQuery) ||
                r.getAddress().toLowerCase().contains(lowerQuery)
            );
            restaurantsTable.setItems(filtered);
        }
    }

    // ==================== MENUS CRUD ====================

    private void refreshMenus() {
        new Thread(() -> {
            try {
                List<Menu> menus = menuService.getAll();
                Platform.runLater(() -> {
                    menusList.clear();
                    menusList.addAll(menus);
                    menusTable.setItems(menusList);
                    updateRestaurantComboBox();
                });
            } catch (SQLException e) {
                Platform.runLater(() -> showError("Erreur: " + e.getMessage()));
            }
        }).start();
    }

    private void updateRestaurantComboBox() {
        try {
            List<Restaurant> restaurants = restaurantService.getAll();
            ObservableList<String> options = FXCollections.observableArrayList();
            restaurants.forEach(r -> options.add(r.getId() + " - " + r.getName()));
            menuModalRestaurant.setItems(options);
        } catch (SQLException e) {
            showError("Erreur: " + e.getMessage());
        }
    }

    @FXML private void showMenuForm() {
        isEditingMenu = false;
        selectedMenu = null;
        clearMenuForm();
        menuModalTitle.setText("Nouveau Menu");
        restaurantModal.setVisible(false);
        menuModal.setVisible(true);
        updateRestaurantComboBox();
        showModal();
    }

    private void editMenu(Menu menu) {
        isEditingMenu = true;
        selectedMenu = menu;
        menuModalName.setText(menu.getName());
        menuModalDescription.setText(menu.getDescription() != null ? menu.getDescription() : "");
        menuModalPrice.getValueFactory().setValue(menu.getPrice().doubleValue());
        menuModalStatus.setValue(menu.getStatus());
        updateRestaurantComboBox();
        
        String selected = restaurantsList.stream()
            .filter(r -> r.getId() == menu.getRestaurantId())
            .map(r -> r.getId() + " - " + r.getName())
            .findFirst().orElse(null);
        menuModalRestaurant.setValue(selected);
        
        menuModalTitle.setText("Modifier Menu");
        restaurantModal.setVisible(false);
        menuModal.setVisible(true);
        showModal();
    }

    @FXML private void handleSaveMenu() {
        if (!validateMenuForm()) return;
        
        new Thread(() -> {
            try {
                String[] selectedRes = menuModalRestaurant.getValue().split(" - ");
                int restaurantId = Integer.parseInt(selectedRes[0]);
                
                if (isEditingMenu) {
                    selectedMenu.setName(menuModalName.getText());
                    selectedMenu.setDescription(menuModalDescription.getText());
                    selectedMenu.setPrice(BigDecimal.valueOf(menuModalPrice.getValue()));
                    selectedMenu.setStatus(menuModalStatus.getValue());
                    selectedMenu.setRestaurantId(restaurantId);
                    menuService.update(selectedMenu);
                    Platform.runLater(() -> showSuccess("Menu modifié"));
                } else {
                    Menu newMenu = new Menu();
                    newMenu.setName(menuModalName.getText());
                    newMenu.setDescription(menuModalDescription.getText());
                    newMenu.setPrice(BigDecimal.valueOf(menuModalPrice.getValue()));
                    newMenu.setStatus(menuModalStatus.getValue());
                    newMenu.setRestaurantId(restaurantId);
                    menuService.create(newMenu);
                    Platform.runLater(() -> showSuccess("Menu créé"));
                }
                Platform.runLater(() -> {
                    hideModal();
                    refreshMenus();
                });
            } catch (Exception e) {
                Platform.runLater(() -> showError("Erreur: " + e.getMessage()));
            }
        }).start();
    }

    private void deleteMenu(Menu menu) {
        if (showConfirm("Supprimer ce menu?")) {
            new Thread(() -> {
                try {
                    menuService.delete(menu.getId());
                    Platform.runLater(() -> {
                        showSuccess("Menu supprimé");
                        refreshMenus();
                    });
                } catch (SQLException e) {
                    Platform.runLater(() -> showError("Erreur: " + e.getMessage()));
                }
            }).start();
        }
    }

    private boolean validateMenuForm() {
        if (menuModalName.getText().trim().isEmpty() ||
            menuModalRestaurant.getValue() == null ||
            menuModalStatus.getValue() == null) {
            showError("Veuillez remplir tous les champs requis");
            return false;
        }
        return true;
    }

    private void clearMenuForm() {
        menuModalName.clear();
        menuModalDescription.clear();
        menuModalPrice.getValueFactory().setValue(0.0);
        menuModalStatus.setValue(null);
        menuModalRestaurant.setValue(null);
    }

    private void filterMenus(String query) {
        if (query == null || query.isEmpty()) {
            menusTable.setItems(menusList);
        } else {
            String lowerQuery = query.toLowerCase();
            ObservableList<Menu> filtered = menusList.filtered(m -> 
                m.getName().toLowerCase().contains(lowerQuery) ||
                (m.getDescription() != null && m.getDescription().toLowerCase().contains(lowerQuery))
            );
            menusTable.setItems(filtered);
        }
    }

    // ==================== REVIEWS MANAGEMENT ====================

    private void refreshReviews() {
        new Thread(() -> {
            try {
                List<RestaurantReview> reviews = reviewService.getAll();
                Platform.runLater(() -> {
                    reviewsList.clear();
                    reviewsList.addAll(reviews);
                    reviewsTable.setItems(reviewsList);
                });
            } catch (SQLException e) {
                Platform.runLater(() -> showError("Erreur: " + e.getMessage()));
            }
        }).start();
    }

    private void deleteReview(RestaurantReview review) {
        if (showConfirm("Supprimer cet avis?")) {
            new Thread(() -> {
                try {
                    reviewService.delete(review.getId());
                    Platform.runLater(() -> {
                        showSuccess("Avis supprimé");
                        refreshReviews();
                    });
                } catch (SQLException e) {
                    Platform.runLater(() -> showError("Erreur: " + e.getMessage()));
                }
            }).start();
        }
    }

    private void filterReviews(String query) {
        if (query == null || query.isEmpty()) {
            reviewsTable.setItems(reviewsList);
        } else {
            String lowerQuery = query.toLowerCase();
            ObservableList<RestaurantReview> filtered = reviewsList.filtered(r -> 
                (r.getComment() != null && r.getComment().toLowerCase().contains(lowerQuery))
            );
            reviewsTable.setItems(filtered);
        }
    }

    // ==================== CELL FACTORIES ====================

    private TableCell<Restaurant, Void> createRestaurantActionCell() {
        return new TableCell<Restaurant, Void>() {
            private final Button editBtn = new Button("✏️");
            private final Button deleteBtn = new Button("🗑️");
            
            {
                editBtn.setStyle("-fx-font-size: 12; -fx-padding: 5;");
                deleteBtn.setStyle("-fx-font-size: 12; -fx-padding: 5;");
                editBtn.setCursor(javafx.scene.Cursor.HAND);
                deleteBtn.setCursor(javafx.scene.Cursor.HAND);
                
                editBtn.setOnAction(e -> editRestaurant(getTableView().getItems().get(getIndex())));
                deleteBtn.setOnAction(e -> deleteRestaurant(getTableView().getItems().get(getIndex())));
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox hbox = new HBox(5);
                    hbox.setAlignment(Pos.CENTER);
                    hbox.getChildren().addAll(editBtn, deleteBtn);
                    setGraphic(hbox);
                }
            }
        };
    }

    private TableCell<Menu, Void> createMenuActionCell() {
        return new TableCell<Menu, Void>() {
            private final Button editBtn = new Button("✏️");
            private final Button deleteBtn = new Button("🗑️");
            
            {
                editBtn.setStyle("-fx-font-size: 12; -fx-padding: 5;");
                deleteBtn.setStyle("-fx-font-size: 12; -fx-padding: 5;");
                editBtn.setCursor(javafx.scene.Cursor.HAND);
                deleteBtn.setCursor(javafx.scene.Cursor.HAND);
                
                editBtn.setOnAction(e -> editMenu(getTableView().getItems().get(getIndex())));
                deleteBtn.setOnAction(e -> deleteMenu(getTableView().getItems().get(getIndex())));
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox hbox = new HBox(5);
                    hbox.setAlignment(Pos.CENTER);
                    hbox.getChildren().addAll(editBtn, deleteBtn);
                    setGraphic(hbox);
                }
            }
        };
    }

    private TableCell<RestaurantReview, Void> createReviewActionCell() {
        return new TableCell<RestaurantReview, Void>() {
            private final Button deleteBtn = new Button("🗑️");
            
            {
                deleteBtn.setStyle("-fx-font-size: 12; -fx-padding: 5;");
                deleteBtn.setCursor(javafx.scene.Cursor.HAND);
                deleteBtn.setOnAction(e -> deleteReview(getTableView().getItems().get(getIndex())));
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : deleteBtn);
            }
        };
    }

    private TableCell<Menu, String> createRestaurantNameCell() {
        return new TableCell<Menu, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                } else {
                    Menu menu = getTableView().getItems().get(getIndex());
                    String name = restaurantsList.stream()
                        .filter(r -> r.getId() == menu.getRestaurantId())
                        .map(Restaurant::getName)
                        .findFirst().orElse("Unknown");
                    setText(name);
                }
            }
        };
    }

    private TableCell<RestaurantReview, String> createRestaurantNameForReviewCell() {
        return new TableCell<RestaurantReview, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                } else {
                    RestaurantReview review = getTableView().getItems().get(getIndex());
                    String name = restaurantsList.stream()
                        .filter(r -> r.getId() == review.getRestaurantId())
                        .map(Restaurant::getName)
                        .findFirst().orElse("Unknown");
                    setText(name);
                }
            }
        };
    }

    private TableCell<RestaurantReview, String> createDateCell() {
        return new TableCell<RestaurantReview, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                } else {
                    RestaurantReview review = getTableView().getItems().get(getIndex());
                    String dateStr = review.getCreatedAt() != null ? 
                        review.getCreatedAt().format(DATE_FORMATTER) : "N/A";
                    setText(dateStr);
                }
            }
        };
    }

    // ==================== THEME MANAGEMENT ====================

    @FXML private void handleThemeSwitch() {
        rootStack.getStylesheets().clear();
        String theme = themeToggle.isSelected() ? "admin_dark.css" : "admin_light.css";
        String cssResource = getClass().getResource("/css/" + theme).toExternalForm();
        rootStack.getStylesheets().add(cssResource);
    }

    // ==================== MODAL MANAGEMENT ====================

    private void showModal() {
        modalOverlay.setVisible(true);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), modalOverlay);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();
    }

    @FXML private void hideModal() {
        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), modalOverlay);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> modalOverlay.setVisible(false));
        fadeOut.play();
    }

    @FXML private void consumeEvent(javafx.scene.input.MouseEvent event) {
        event.consume();
    }

    // ==================== NOTIFICATIONS ====================

    private void showSuccess(String message) {
        showNotification(message, "success");
    }

    private void showError(String message) {
        showNotification(message, "error");
    }

    private boolean showConfirm(String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText(null);
        alert.setContentText(message);
        return alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    private void showNotification(String message, String type) {
        Alert alert = new Alert(type.equals("error") ? Alert.AlertType.ERROR : Alert.AlertType.INFORMATION);
        alert.setTitle(type.equals("error") ? "Erreur" : "Succès");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}