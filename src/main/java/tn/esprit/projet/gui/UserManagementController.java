package tn.esprit.projet.gui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import tn.esprit.projet.entities.Role;
import tn.esprit.projet.entities.User;
import tn.esprit.projet.services.UserService;

import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class UserManagementController implements Initializable {

    // Éléments de structure (Views)
    @FXML private VBox listView;
    @FXML private VBox formView;
    @FXML private Label formTitle;

    // Table
    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, String> colNom, colEmail, colRole, colStatus;
    @FXML private TableColumn<User, Void> colAction;

    // Filtres
    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilterCombo;
    @FXML private ComboBox<String> roleFilterCombo;

    // Champs du formulaire
    @FXML private TextField nomField, emailField, telField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<String> roleCombo, statusCombo;

    private final UserService userService = new UserService();
    private ObservableList<User> masterData = FXCollections.observableArrayList();
    private User selectedUser = null; // null = ajout, sinon = modification

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupTable();
        initFilterCombos();
        initFormCombos();
        loadUserData();
        setupDynamicFilters();
    }

    private void setupTable() {
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colRole.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getRoleName()));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button btnEdit = new Button("Modifier");
            private final Button btnDelete = new Button("Supprimer");
            private final HBox pane = new HBox(10, btnEdit, btnDelete);
            {
                pane.setAlignment(Pos.CENTER);
                btnEdit.getStyleClass().add("btn-table-edit");
                btnDelete.getStyleClass().add("btn-table-delete");
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    User user = getTableView().getItems().get(getIndex());
                    btnEdit.setOnAction(e -> handleEditUser(user));
                    btnDelete.setOnAction(e -> handleDeleteUser(user));
                    setGraphic(pane);
                }
            }
        });
    }

    private void initFilterCombos() {
        statusFilterCombo.setItems(FXCollections.observableArrayList("Tous", "actif", "inactif"));
        statusFilterCombo.setValue("Tous");
        roleFilterCombo.setItems(FXCollections.observableArrayList("Tous", "ADMIN", "CLIENT"));
        roleFilterCombo.setValue("Tous");
    }

    private void initFormCombos() {
        roleCombo.setItems(FXCollections.observableArrayList("ADMIN", "CLIENT"));
        statusCombo.setItems(FXCollections.observableArrayList("actif", "inactif"));
    }

    private void loadUserData() {
        try {
            masterData.setAll(userService.getAll());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // --- NAVIGATION ---

    @FXML
    private void handleAddNew() {
        this.selectedUser = null;
        formTitle.setText("Ajouter un nouveau client");
        nomField.clear();
        emailField.clear();
        telField.clear();
        passwordField.clear();
        roleCombo.setValue("CLIENT");
        statusCombo.setValue("actif");

        listView.setVisible(false);
        formView.setVisible(true);
    }

    private void handleEditUser(User user) {
        this.selectedUser = user;
        formTitle.setText("Modifier l'utilisateur : " + user.getNom());
        nomField.setText(user.getNom());
        emailField.setText(user.getEmail());
        telField.setText(user.getTelephone());
        passwordField.clear(); // On ne montre pas le hash du mot de passe
        roleCombo.setValue(user.getRoleName());
        statusCombo.setValue(user.getStatus());

        listView.setVisible(false);
        formView.setVisible(true);
    }

    @FXML
    private void showList() {
        formView.setVisible(false);
        listView.setVisible(true);
        loadUserData();
    }

    // --- ACTIONS BDD ---

    @FXML
    private void handleSaveUser() {
        if (nomField.getText().isEmpty() || emailField.getText().isEmpty()) {
            showAlert("Erreur", "Veuillez remplir les champs obligatoires.");
            return;
        }

        try {
            if (selectedUser == null) {
                // Mode AJOUT
                int roleId = roleCombo.getValue().equals("ADMIN") ? 1 : 2;
                User newUser = new User(nomField.getText(), emailField.getText(), passwordField.getText(),
                        telField.getText(), null, roleId, statusCombo.getValue());
                userService.create(newUser);
            } else {
                // Mode MODIFICATION
                selectedUser.setNom(nomField.getText());
                selectedUser.setEmail(emailField.getText());
                selectedUser.setTelephone(telField.getText());
                selectedUser.setStatus(statusCombo.getValue());
                int roleId = roleCombo.getValue().equals("ADMIN") ? 1 : 2;
                selectedUser.setRole(new Role(roleId, roleCombo.getValue()));

                // Si on a tapé un nouveau password
                if (!passwordField.getText().isEmpty()) {
                    selectedUser.setPassword(passwordField.getText());
                }

                userService.update(selectedUser);
            }
            showList();
        } catch (SQLException e) {
            showAlert("Erreur BDD", e.getMessage());
        }
    }

    private void handleDeleteUser(User user) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer " + user.getNom() + " ?", ButtonType.YES, ButtonType.NO);
        if (alert.showAndWait().get() == ButtonType.YES) {
            try {
                userService.delete(user.getId());
                loadUserData();
            } catch (SQLException e) {
                showAlert("Erreur", "Impossible de supprimer.");
            }
        }
    }

    // --- RECHERCHE DYNAMIQUE ---

    private void setupDynamicFilters() {
        FilteredList<User> filteredData = new FilteredList<>(masterData, p -> true);
        Runnable updateFilter = () -> {
            String searchText = (searchField.getText() == null) ? "" : searchField.getText().toLowerCase();
            String statusF = statusFilterCombo.getValue();
            String roleF = roleFilterCombo.getValue();

            filteredData.setPredicate(user -> {
                boolean matchesText = user.getNom().toLowerCase().contains(searchText) || user.getEmail().toLowerCase().contains(searchText);
                boolean matchesStatus = statusF.equals("Tous") || user.getStatus().equalsIgnoreCase(statusF);
                boolean matchesRole = roleF.equals("Tous") || user.getRoleName().equalsIgnoreCase(roleF);
                return matchesText && matchesStatus && matchesRole;
            });
        };
        searchField.textProperty().addListener((o, old, newVal) -> updateFilter.run());
        statusFilterCombo.valueProperty().addListener((o, old, newVal) -> updateFilter.run());
        roleFilterCombo.valueProperty().addListener((o, old, newVal) -> updateFilter.run());

        SortedList<User> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(userTable.comparatorProperty());
        userTable.setItems(sortedData);
    }

    private void showAlert(String title, String content) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setContentText(content);
        a.showAndWait();
    }
}