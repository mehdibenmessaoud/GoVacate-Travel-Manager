package tn.esprit.projet.gui.admin;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import tn.esprit.projet.entities.Role;
import tn.esprit.projet.entities.User;
import tn.esprit.projet.services.RoleService;
import tn.esprit.projet.services.UserService;
import tn.esprit.projet.utils.SceneManager;
import tn.esprit.projet.utils.SessionManager;
import tn.esprit.projet.utils.ValidationUtils;

import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UserManagementController implements Initializable {

    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, Integer> colId;
    @FXML private TableColumn<User, String> colNom;
    @FXML private TableColumn<User, String> colEmail;
    @FXML private TableColumn<User, String> colRole;
    @FXML private TableColumn<User, String> colStatus;
    @FXML private TableColumn<User, String> colTelephone;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> roleFilter;
    @FXML private ComboBox<String> statusFilter;

    @FXML private TextField nomField;
    @FXML private TextField emailField;
    @FXML private TextField telephoneField;
    @FXML private DatePicker dateNaissancePicker;
    @FXML private ComboBox<Role> roleCombo;
    @FXML private ComboBox<String> statusCombo;
    @FXML private PasswordField passwordField;

    @FXML private Label formTitle;
    @FXML private Button saveButton;
    @FXML private Button deleteButton;
    @FXML private Label loadingLabel;

    private UserService userService;
    private RoleService roleService;
    private ObservableList<User> usersList;
    private User selectedUser = null;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Vérification de sécurité - seul un admin peut accéder
        if (!SessionManager.isAdmin()) {
            SceneManager.goToLogin();
            return;
        }
        
        // Initialiser les services de manière sécurisée
        try {
            userService = new UserService();
            roleService = new RoleService();
        } catch (Exception e) {
            showAlert("Erreur", "Erreur d'initialisation: " + e.getMessage(), Alert.AlertType.ERROR);
            return;
        }
        
        initializeTable();
        initializeFilters();
        initializeForm();
        
        // Charger les utilisateurs sur un thread séparé
        loadUsersAsync();
    }

    private void initializeTable() {
        if (userTable == null || colId == null || colNom == null || colEmail == null 
            || colRole == null || colStatus == null || colTelephone == null) {
            System.err.println("Composants TableView non chargés");
            return;
        }
        
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("roleName"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colTelephone.setCellValueFactory(new PropertyValueFactory<>("telephone"));

        // Style pour la colonne status
        colStatus.setCellFactory(column -> new TableCell<User, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(status);
                    if ("actif".equals(status)) {
                        setStyle("-fx-text-fill: #27AE60; -fx-font-weight: bold;");
                    } else if ("inactif".equals(status)) {
                        setStyle("-fx-text-fill: #E74C3C;");
                    } else {
                        setStyle("-fx-text-fill: #FF8210;");
                    }
                }
            }
        });

        userTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                selectedUser = newVal;
                fillForm(newVal);
            }
        });
    }

    private void initializeFilters() {
        if (roleFilter == null || statusFilter == null || searchField == null) {
            return;
        }
        
        roleFilter.setItems(FXCollections.observableArrayList("Tous", "ADMIN", "CLIENT"));
        roleFilter.setValue("Tous");
        
        statusFilter.setItems(FXCollections.observableArrayList("Tous", "actif", "inactif", "en_attente"));
        statusFilter.setValue("Tous");
        
        searchField.textProperty().addListener((obs, o, n) -> filterUsers());
        roleFilter.valueProperty().addListener((obs, o, n) -> filterUsers());
        statusFilter.valueProperty().addListener((obs, o, n) -> filterUsers());
    }

    private void initializeForm() {
        if (roleCombo == null || statusCombo == null) {
            return;
        }
        
        Role admin = roleService.getByName("ADMIN");
        Role client = roleService.getByName("CLIENT");
        roleCombo.setItems(FXCollections.observableArrayList(admin, client));
        statusCombo.setItems(FXCollections.observableArrayList("actif", "inactif", "en_attente"));
        statusCombo.setValue("actif");
        resetForm();
    }

    /**
     * Charge les utilisateurs de manière asynchrone pour ne pas bloquer l'UI
     */
    @FXML
    private void loadUsersAsync() {
        if (loadingLabel != null) {
            loadingLabel.setVisible(true);
            loadingLabel.setText("Chargement des utilisateurs...");
        }
        
        // Désactiver les interactions pendant le chargement
        if (userTable != null) {
            userTable.setDisable(true);
        }

        Task<List<User>> task = new Task<List<User>>() {
            @Override
            protected List<User> call() throws Exception {
                return userService.recupererUser();
            }
            
            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    usersList = FXCollections.observableArrayList(getValue());
                    userTable.setItems(usersList);
                    userTable.setDisable(false);
                    
                    if (loadingLabel != null) {
                        loadingLabel.setVisible(false);
                    }
                });
            }
            
            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    showAlert("Erreur", "Impossible de charger les utilisateurs: " + getException().getMessage(), 
                              Alert.AlertType.ERROR);
                    userTable.setDisable(false);
                    
                    if (loadingLabel != null) {
                        loadingLabel.setText("Erreur de chargement");
                    }
                });
            }
        };
        
        executor.execute(task);
    }

    /**
     * Ancienne méthode conservée pour compatibilité - redirige vers version async
     */
    @FXML
    private void loadUsers() {
        loadUsersAsync();
    }

    private void filterUsers() {
        if (usersList == null || userTable == null) {
            return;
        }
        
        String search = searchField != null ? searchField.getText().toLowerCase() : "";
        String role = roleFilter != null ? roleFilter.getValue() : "Tous";
        String status = statusFilter != null ? statusFilter.getValue() : "Tous";
        
        ObservableList<User> filtered = usersList.filtered(user -> {
            boolean matchesSearch = true;
            if (search != null && !search.isEmpty()) {
                matchesSearch = (user.getNom() != null && user.getNom().toLowerCase().contains(search)) ||
                        (user.getEmail() != null && user.getEmail().toLowerCase().contains(search));
            }
            
            boolean matchesRole = "Tous".equals(role) || 
                    (user.getRoleName() != null && user.getRoleName().equals(role));
            
            boolean matchesStatus = "Tous".equals(status) || 
                    (user.getStatus() != null && user.getStatus().equals(status));
            
            return matchesSearch && matchesRole && matchesStatus;
        });
        
        userTable.setItems(filtered);
    }

    private void fillForm(User user) {
        if (formTitle == null || nomField == null || emailField == null) {
            return;
        }
        
        formTitle.setText("Modifier Utilisateur #" + user.getId());
        nomField.setText(user.getNom());
        emailField.setText(user.getEmail());
        
        if (telephoneField != null) {
            telephoneField.setText(user.getTelephone());
        }
        
        if (dateNaissancePicker != null) {
            dateNaissancePicker.setValue(user.getDateNaissance());
        }
        
        if (roleCombo != null) {
            roleCombo.setValue(user.getRole());
        }
        
        if (statusCombo != null) {
            statusCombo.setValue(user.getStatus());
        }
        
        if (passwordField != null) {
            passwordField.clear();
            passwordField.setPromptText("Laisser vide pour garder l'actuel");
        }
        
        if (saveButton != null) {
            saveButton.setText("METTRE À JOUR");
        }
        
        if (deleteButton != null) {
            deleteButton.setVisible(true);
        }
    }

    @FXML
    private void handleSave() {
        // Vérification des composants
        if (nomField == null || emailField == null || roleCombo == null) {
            showAlert("Erreur", "Composants du formulaire non chargés", Alert.AlertType.ERROR);
            return;
        }
        
        String nom = nomField.getText();
        String email = emailField.getText();
        String telephone = telephoneField != null ? telephoneField.getText() : "";
        LocalDate dateN = dateNaissancePicker != null ? dateNaissancePicker.getValue() : null;
        Role role = roleCombo.getValue();
        String status = statusCombo != null ? statusCombo.getValue() : "actif";
        String password = passwordField != null ? passwordField.getText() : "";

        // ========== VALIDATION ==========
        
        // Validation nom
        if (!ValidationUtils.isNotEmpty(nom)) {
            showAlert("Erreur", "Le nom est obligatoire", Alert.AlertType.ERROR);
            return;
        }
        
        if (!ValidationUtils.isValidNom(nom)) {
            showAlert("Erreur", ValidationUtils.getNomErrorMessage(), Alert.AlertType.ERROR);
            return;
        }
        
        // Validation email
        if (!ValidationUtils.isNotEmpty(email)) {
            showAlert("Erreur", "L'email est obligatoire", Alert.AlertType.ERROR);
            return;
        }
        
        if (!ValidationUtils.isValidEmail(email)) {
            showAlert("Erreur", ValidationUtils.getEmailErrorMessage(), Alert.AlertType.ERROR);
            return;
        }
        
        // Validation téléphone (optionnel)
        if (ValidationUtils.isNotEmpty(telephone) && !ValidationUtils.isValidTelephone(telephone)) {
            showAlert("Erreur", ValidationUtils.getTelephoneErrorMessage(), Alert.AlertType.ERROR);
            return;
        }
        
        // Validation date de naissance
        if (!ValidationUtils.isValidDateDeNaissance(dateN)) {
            showAlert("Erreur", "La date de naissance ne peut pas être dans le futur", Alert.AlertType.ERROR);
            return;
        }
        
        // Validation mot de passe pour nouvel utilisateur
        if (selectedUser == null && !ValidationUtils.isNotEmpty(password)) {
            showAlert("Erreur", "Le mot de passe est obligatoire pour un nouvel utilisateur", Alert.AlertType.ERROR);
            return;
        }
        
        if (ValidationUtils.isNotEmpty(password) && !ValidationUtils.isValidPassword(password)) {
            showAlert("Erreur", ValidationUtils.getPasswordErrorMessage(), Alert.AlertType.ERROR);
            return;
        }

        // ========== SAUVEGARDE ==========
        
        try {
            if (selectedUser == null) {
                // Créer nouvel utilisateur
                if (userService.emailExiste(email.trim())) {
                    showAlert("Erreur", "Cet email est déjà utilisé", Alert.AlertType.ERROR);
                    return;
                }
                User newUser = new User(nom.trim(), email.trim(), password, telephone.trim(),
                                        dateN, role != null ? role.getId() : 2, status);
                userService.ajouterUser(newUser);
                showAlert("Succès", "Utilisateur créé avec succès", Alert.AlertType.INFORMATION);
            } else {
                // Modifier utilisateur existant
                if (!email.trim().equalsIgnoreCase(selectedUser.getEmail())
                        && userService.emailExiste(email.trim())) {
                    showAlert("Erreur", "Cet email est déjà utilisé par un autre utilisateur", Alert.AlertType.ERROR);
                    return;
                }
                selectedUser.setNom(nom.trim());
                selectedUser.setEmail(email.trim());
                selectedUser.setTelephone(telephone.trim());
                selectedUser.setDateNaissance(dateN);
                selectedUser.setRole(role);
                selectedUser.setStatus(status);
                userService.modifierUser(selectedUser);
                if (ValidationUtils.isNotEmpty(password)) {
                    userService.changerMotDePasse(selectedUser.getId(), password);
                }
                showAlert("Succès", "Utilisateur mis à jour", Alert.AlertType.INFORMATION);
            }
            resetForm();
            loadUsersAsync();
        } catch (java.sql.SQLException e) {
            showAlert("Erreur", e.getMessage() != null ? e.getMessage() : "Erreur lors de la sauvegarde", Alert.AlertType.ERROR);
            System.err.println("Erreur handleSave: " + e.getMessage());
        } catch (Exception e) {
            showAlert("Erreur", "Erreur lors de la sauvegarde: " + e.getMessage(), Alert.AlertType.ERROR);
            System.err.println("Erreur handleSave: " + e.getMessage());
        }
    }

    @FXML
    private void handleDelete() {
        if (selectedUser == null) {
            showAlert("Avertissement", "Veuillez sélectionner un utilisateur à supprimer", Alert.AlertType.WARNING);
            return;
        }
        
        // Empêcher l'auto-suppression
        if (selectedUser.getId() == SessionManager.getCurrentUserId()) {
            showAlert("Erreur", "Vous ne pouvez pas supprimer votre propre compte", Alert.AlertType.ERROR);
            return;
        }
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer l'utilisateur");
        confirm.setContentText("Êtes-vous sûr de vouloir supprimer " + selectedUser.getNom() + " ?");
        
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                userService.supprimerUser(selectedUser.getId());
                showAlert("Succès", "Utilisateur supprimé", Alert.AlertType.INFORMATION);
                resetForm();
                loadUsersAsync();
            } catch (java.sql.SQLException e) {
                showAlert("Erreur", e.getMessage() != null ? e.getMessage() : "Erreur lors de la suppression", Alert.AlertType.ERROR);
            } catch (Exception e) {
                showAlert("Erreur", "Erreur: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    @FXML
    private void handleCancel() {
        resetForm();
    }

    @FXML
    private void handleNew() {
        resetForm();
        if (userTable != null) {
            userTable.getSelectionModel().clearSelection();
        }
    }

    @FXML
    private void handleBack() {
        SceneManager.loadAdminContent("/fxml/admin/AdminDashboard.fxml");
    }

    private void resetForm() {
        selectedUser = null;
        
        if (formTitle != null) {
            formTitle.setText("Nouvel Utilisateur");
        }
        
        if (nomField != null) nomField.clear();
        if (emailField != null) emailField.clear();
        if (telephoneField != null) telephoneField.clear();
        if (dateNaissancePicker != null) dateNaissancePicker.setValue(null);
        if (roleCombo != null) roleCombo.setValue(null);
        if (statusCombo != null) statusCombo.setValue("actif");
        if (passwordField != null) {
            passwordField.clear();
            passwordField.setPromptText("Mot de passe");
        }
        
        if (saveButton != null) {
            saveButton.setText("CRÉER");
        }
        
        if (deleteButton != null) {
            deleteButton.setVisible(false);
        }
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        try {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
        } catch (Exception e) {
            System.err.println("Erreur affichage alerte: " + e.getMessage());
        }
    }

    @FXML
    private void handleEdit() {
        User selected = userTable != null ? userTable.getSelectionModel().getSelectedItem() : null;

        if (selected != null) {
            this.selectedUser = selected;
            fillForm(selected);
            if (nomField != null) {
                nomField.requestFocus();
            }
            System.out.println("Mode édition activé pour : " + selected.getNom());
        } else {
            showAlert("Sélection requise",
                    "Veuillez sélectionner un utilisateur dans le tableau pour le modifier.",
                    Alert.AlertType.WARNING);
        }
    }
    
    /**
     * Nettoie les ressources lors de la fermeture du contrôleur
     */
    public void cleanup() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}

