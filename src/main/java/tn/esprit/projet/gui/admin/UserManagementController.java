package tn.esprit.projet.gui.admin;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import tn.esprit.projet.entities.Role;
import tn.esprit.projet.entities.User;
import tn.esprit.projet.services.RoleService;
import tn.esprit.projet.services.UserService;
import tn.esprit.projet.utils.SceneManager;

import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UserManagementController implements Initializable {

    private static final Logger LOG = Logger.getLogger(UserManagementController.class.getName());

    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, Integer> colIndex;
    @FXML private TableColumn<User, String> colNom;
    @FXML private TableColumn<User, String> colEmail;
    @FXML private TableColumn<User, String> colTelephone;
    @FXML private TableColumn<User, String> colRole;
    @FXML private TableColumn<User, String> colStatus;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> roleFilter;
    @FXML private ComboBox<String> statusFilter;
    @FXML private Label loadingLabel;

    @FXML private Label formTitle;
    @FXML private TextField nomField;
    @FXML private TextField emailField;
    @FXML private TextField telephoneField;
    @FXML private DatePicker dateNaissancePicker;
    @FXML private ComboBox<Role> roleCombo;
    @FXML private ComboBox<String> statusCombo;
    @FXML private PasswordField passwordField;
    @FXML private Button saveButton;
    @FXML private Button deleteButton;

    private final UserService userService = new UserService();
    private final RoleService roleService = new RoleService();
    private ObservableList<User> userList = FXCollections.observableArrayList();
    private FilteredList<User> filteredData;
    private User selectedUser = null;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTableColumns();
        loadRoles();
        loadUsers();
        setupFilters();
        
        // Listen for selection changes
        userTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> {
                if (newSelection != null) {
                    selectUserForEdit(newSelection);
                }
            }
        );
    }

    private void setupTableColumns() {
        // Configure index column to show row number (1-based)
        colIndex.setCellValueFactory(column -> new ReadOnlyObjectWrapper<>(userTable.getItems().indexOf(column.getValue()) + 1));
        colIndex.setSortable(false);

        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colTelephone.setCellValueFactory(new PropertyValueFactory<>("telephone"));
        
        colRole.setCellValueFactory(cellData -> {
            Role r = cellData.getValue().getRole();
            return javafx.beans.binding.Bindings.createStringBinding(
                () -> r != null ? r.getNomRole() : "N/A"
            );
        });

        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colStatus.setCellFactory(column -> new TableCell<User, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if ("actif".equalsIgnoreCase(item)) {
                        setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                    } else if ("banni".equalsIgnoreCase(item)) {
                        setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: orange; -fx-font-weight: bold;");
                    }
                }
            }
        });
    }

    private void loadRoles() {
        try {
            List<Role> roles = roleService.getAll();
            roleCombo.setItems(FXCollections.observableArrayList(roles));
            
            // Pour le filtre
            ObservableList<String> roleNames = FXCollections.observableArrayList("Tous");
            roles.forEach(r -> roleNames.add(r.getNomRole()));
            roleFilter.setItems(roleNames);
            roleFilter.setValue("Tous");
            
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Erreur chargement roles", e);
            showAlert("Erreur", "Impossible de charger les rôles.", Alert.AlertType.ERROR);
        }
    }

    private void loadUsers() {
        if (loadingLabel != null) loadingLabel.setVisible(true);
        
        new Thread(() -> {
            try {
                List<User> users = userService.getAll();
                Platform.runLater(() -> {
                    userList.setAll(users);
                    filteredData = new FilteredList<>(userList, p -> true);
                    SortedList<User> sortedData = new SortedList<>(filteredData);
                    sortedData.comparatorProperty().bind(userTable.comparatorProperty());
                    userTable.setItems(sortedData);
                    if (loadingLabel != null) loadingLabel.setVisible(false);
                });
            } catch (SQLException e) {
                Platform.runLater(() -> {
                    LOG.log(Level.SEVERE, "Erreur chargement utilisateurs", e);
                    if (loadingLabel != null) loadingLabel.setVisible(false);
                    showAlert("Erreur", "Impossible de charger les utilisateurs.", Alert.AlertType.ERROR);
                });
            }
        }).start();
    }

    private void setupFilters() {
        statusFilter.setItems(FXCollections.observableArrayList("Tous", "actif", "inactif", "banni"));
        statusFilter.setValue("Tous");
        statusCombo.setItems(FXCollections.observableArrayList("actif", "inactif", "banni"));

        searchField.textProperty().addListener((observable, oldValue, newValue) -> updateFilters());
        roleFilter.valueProperty().addListener((observable, oldValue, newValue) -> updateFilters());
        statusFilter.valueProperty().addListener((observable, oldValue, newValue) -> updateFilters());
    }

    private void updateFilters() {
        if (filteredData == null) return;
        
        filteredData.setPredicate(user -> {
            String searchText = searchField.getText().toLowerCase();
            String roleSel = roleFilter.getValue();
            String statusSel = statusFilter.getValue();

            boolean matchesSearch = searchText.isEmpty() || 
                (user.getNom() != null && user.getNom().toLowerCase().contains(searchText)) || 
                (user.getEmail() != null && user.getEmail().toLowerCase().contains(searchText));

            boolean matchesRole = "Tous".equals(roleSel) || roleSel == null ||
                (user.getRole() != null && user.getRole().getNomRole().equals(roleSel));

            boolean matchesStatus = "Tous".equals(statusSel) || statusSel == null ||
                (user.getStatus() != null && user.getStatus().equalsIgnoreCase(statusSel));

            return matchesSearch && matchesRole && matchesStatus;
        });
    }

    private void selectUserForEdit(User user) {
        selectedUser = user;
        formTitle.setText("Modifier Utilisateur");
        saveButton.setText("Mettre à jour");
        deleteButton.setVisible(true);
        
        nomField.setText(user.getNom());
        emailField.setText(user.getEmail());
        passwordField.setText(""); // On ne montre pas le mot de passe hashé
        passwordField.setPromptText("Laisser vide pour ne pas changer");
        telephoneField.setText(user.getTelephone());
        dateNaissancePicker.setValue(user.getDateNaissance());
        statusCombo.setValue(user.getStatus());
        
        // Sélectionner le bon rôle dans la ComboBox
        if (user.getRole() != null) {
            for (Role r : roleCombo.getItems()) {
                if (r.getId() == user.getRoleId()) {
                    roleCombo.setValue(r);
                    break;
                }
            }
        }
    }

    @FXML
    private void handleNew() {
        handleCancel(); // Clears the form
    }

    @FXML
    private void handleCancel() {
        selectedUser = null;
        formTitle.setText("Nouvel Utilisateur");
        saveButton.setText("CRÉER");
        deleteButton.setVisible(false);
        userTable.getSelectionModel().clearSelection();
        
        nomField.clear();
        emailField.clear();
        passwordField.clear();
        passwordField.setPromptText("Mot de passe");
        telephoneField.clear();
        dateNaissancePicker.setValue(null);
        roleCombo.getSelectionModel().clearSelection();
        statusCombo.setValue("actif");
    }

    @FXML
    private void handleSave() {
        if (!validateForm()) return;

        try {
            if (selectedUser == null) {
                // Création
                Role selectedRole = roleCombo.getValue();
                String status = statusCombo.getValue() != null ? statusCombo.getValue() : "actif";
                User newUser = new User(
                    nomField.getText(),
                    emailField.getText(),
                    passwordField.getText(),
                    telephoneField.getText(),
                    dateNaissancePicker.getValue(),
                    selectedRole.getId(),
                    status
                );
                newUser.setRole(selectedRole);
                userService.create(newUser);
                showAlert("Succès", "Utilisateur ajouté avec succès.", Alert.AlertType.INFORMATION);
            } else {
                // Modification
                selectedUser.setNom(nomField.getText());
                selectedUser.setEmail(emailField.getText());
                if (!passwordField.getText().isEmpty()) {
                    userService.changerMotDePasse(selectedUser.getId(), passwordField.getText());
                }
                selectedUser.setTelephone(telephoneField.getText());
                selectedUser.setDateNaissance(dateNaissancePicker.getValue());
                selectedUser.setRole(roleCombo.getValue());
                selectedUser.setRoleId(roleCombo.getValue().getId());
                selectedUser.setStatus(statusCombo.getValue());
                
                userService.update(selectedUser);
                showAlert("Succès", "Utilisateur mis à jour avec succès.", Alert.AlertType.INFORMATION);
            }
            handleCancel();
            loadUsers();
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Erreur sauvegarde utilisateur", e);
            showAlert("Erreur", "Une erreur est survenue lors de la sauvegarde: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleDelete() {
        if (selectedUser == null) return;
        
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Supprimer l'utilisateur");
        alert.setContentText("Voulez-vous vraiment supprimer l'utilisateur " + selectedUser.getNom() + " ?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                userService.delete(selectedUser.getId());
                userList.remove(selectedUser);
                handleCancel();
                showAlert("Succès", "Utilisateur supprimé.", Alert.AlertType.INFORMATION);
            } catch (SQLException e) {
                LOG.log(Level.SEVERE, "Erreur suppression utilisateur", e);
                showAlert("Erreur", "Impossible de supprimer l'utilisateur.", Alert.AlertType.ERROR);
            }
        }
    }

    @FXML
    private void handleBack() {
        SceneManager.switchTo("/fxml/admin/AdminDashboard.fxml");
    }

    private boolean validateForm() {
        StringBuilder errors = new StringBuilder();
        
        if (nomField.getText() == null || nomField.getText().trim().isEmpty()) errors.append("- Le nom est requis.\n");
        if (emailField.getText() == null || emailField.getText().trim().isEmpty()) errors.append("- L'email est requis.\n");
        if (selectedUser == null && (passwordField.getText() == null || passwordField.getText().trim().isEmpty())) errors.append("- Le mot de passe est requis.\n");
        if (roleCombo.getValue() == null) errors.append("- Le rôle est requis.\n");
        
        if (errors.length() > 0) {
            showAlert("Validation", "Veuillez corriger les erreurs suivantes :\n" + errors.toString(), Alert.AlertType.WARNING);
            return false;
        }
        return true;
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
