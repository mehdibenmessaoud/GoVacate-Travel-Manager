package tn.esprit.projet.gui.admin;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import tn.esprit.projet.entities.Role;
import tn.esprit.projet.entities.User;
import tn.esprit.projet.services.RoleService;
import tn.esprit.projet.services.UserService;
import tn.esprit.projet.utils.AlertUtils;

import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UserManagementController implements Initializable {

    private static final Logger LOG = Logger.getLogger(UserManagementController.class.getName());

    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, Integer> colId;
    @FXML private TableColumn<User, String> colNom;
    @FXML private TableColumn<User, String> colEmail;
    @FXML private TableColumn<User, String> colTelephone;
    @FXML private TableColumn<User, String> colRole;
    @FXML private TableColumn<User, String> colStatus;
    @FXML private TableColumn<User, Void> colActions;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> roleFilter;
    @FXML private ComboBox<String> statusFilter;

    @FXML private TextField nomField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private TextField telephoneField;
    @FXML private DatePicker dateNaissancePicker;
    @FXML private ComboBox<Role> roleComboBox;
    @FXML private ComboBox<String> statusComboBox;
    @FXML private Button btnSave;
    @FXML private Button btnClear;
    @FXML private VBox formContainer;
    @FXML private Label formTitle;

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
        setupForm();
    }

    private void setupTableColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
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

        setupActionButtons();
    }

    private void setupActionButtons() {
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button editBtn = new Button("Modifier");
            private final Button deleteBtn = new Button("Supprimer");
            private final HBox pane = new HBox(5, editBtn, deleteBtn);

            {
                editBtn.getStyleClass().add("button-primary-small");
                deleteBtn.getStyleClass().add("button-danger-small");
                
                editBtn.setOnAction(event -> {
                    User user = getTableView().getItems().get(getIndex());
                    selectUserForEdit(user);
                });

                deleteBtn.setOnAction(event -> {
                    User user = getTableView().getItems().get(getIndex());
                    handleDeleteUser(user);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });
    }

    private void loadRoles() {
        try {
            List<Role> roles = roleService.getAll();
            roleComboBox.setItems(FXCollections.observableArrayList(roles));
            
            // Pour le filtre
            ObservableList<String> roleNames = FXCollections.observableArrayList("Tous");
            roles.forEach(r -> roleNames.add(r.getNomRole()));
            roleFilter.setItems(roleNames);
            roleFilter.setValue("Tous");
            
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Erreur chargement roles", e);
            AlertUtils.showError("Erreur", "Impossible de charger les rôles.");
        }
    }

    private void loadUsers() {
        try {
            List<User> users = userService.getAll();
            userList.setAll(users);
            filteredData = new FilteredList<>(userList, p -> true);
            SortedList<User> sortedData = new SortedList<>(filteredData);
            sortedData.comparatorProperty().bind(usersTable.comparatorProperty());
            usersTable.setItems(sortedData);
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Erreur chargement utilisateurs", e);
            AlertUtils.showError("Erreur", "Impossible de charger les utilisateurs.");
        }
    }

    private void setupFilters() {
        statusFilter.setItems(FXCollections.observableArrayList("Tous", "actif", "inactif", "banni"));
        statusFilter.setValue("Tous");
        statusComboBox.setItems(FXCollections.observableArrayList("actif", "inactif", "banni"));

        searchField.textProperty().addListener((observable, oldValue, newValue) -> updateFilters());
        roleFilter.valueProperty().addListener((observable, oldValue, newValue) -> updateFilters());
        statusFilter.valueProperty().addListener((observable, oldValue, newValue) -> updateFilters());
    }

    private void updateFilters() {
        filteredData.setPredicate(user -> {
            String searchText = searchField.getText().toLowerCase();
            String roleSel = roleFilter.getValue();
            String statusSel = statusFilter.getValue();

            boolean matchesSearch = searchText.isEmpty() || 
                user.getNom().toLowerCase().contains(searchText) || 
                user.getEmail().toLowerCase().contains(searchText);

            boolean matchesRole = "Tous".equals(roleSel) || 
                (user.getRole() != null && user.getRole().getNomRole().equals(roleSel));

            boolean matchesStatus = "Tous".equals(statusSel) || 
                (user.getStatus() != null && user.getStatus().equalsIgnoreCase(statusSel));

            return matchesSearch && matchesRole && matchesStatus;
        });
    }

    private void setupForm() {
        btnSave.setOnAction(e -> handleSaveUser());
        btnClear.setOnAction(e -> clearForm());
    }

    private void selectUserForEdit(User user) {
        selectedUser = user;
        formTitle.setText("Modifier Utilisateur");
        btnSave.setText("Mettre à jour");
        
        nomField.setText(user.getNom());
        emailField.setText(user.getEmail());
        passwordField.setText(""); // On ne montre pas le mot de passe hashé
        passwordField.setPromptText("Laisser vide pour ne pas changer");
        telephoneField.setText(user.getTelephone());
        dateNaissancePicker.setValue(user.getDateNaissance());
        statusComboBox.setValue(user.getStatus());
        
        // Sélectionner le bon rôle dans la ComboBox
        if (user.getRole() != null) {
            for (Role r : roleComboBox.getItems()) {
                if (r.getId() == user.getRoleId()) {
                    roleComboBox.setValue(r);
                    break;
                }
            }
        }
    }

    private void clearForm() {
        selectedUser = null;
        formTitle.setText("Nouvel Utilisateur");
        btnSave.setText("Ajouter");
        
        nomField.clear();
        emailField.clear();
        passwordField.clear();
        passwordField.setPromptText("Mot de passe");
        telephoneField.clear();
        dateNaissancePicker.setValue(null);
        roleComboBox.getSelectionModel().clearSelection();
        statusComboBox.setValue("actif");
    }

    private void handleSaveUser() {
        if (!validateForm()) return;

        try {
            if (selectedUser == null) {
                // Création
                Role selectedRole = roleComboBox.getValue();
                String status = statusComboBox.getValue() != null ? statusComboBox.getValue() : "actif";
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
                AlertUtils.showInfo("Succès", "Utilisateur ajouté avec succès.");
            } else {
                // Modification
                selectedUser.setNom(nomField.getText());
                selectedUser.setEmail(emailField.getText());
                if (!passwordField.getText().isEmpty()) {
                    // Si le champ mot de passe n'est pas vide, on le met à jour (le service gère le hashage si besoin)
                    // Note: UserService.update ne gère pas le changement de mot de passe directement si on veut le hasher
                    // Il faudrait idéalement utiliser updatePassword ou gérer ça dans le service.
                    // Ici on suppose que update gère tout ou on fait un appel séparé si besoin.
                    // Pour simplifier, on met à jour le mot de passe dans l'objet, mais attention au hashage.
                    // UserService.update ne touche pas au mot de passe dans la requête SQL actuelle.
                    // On va utiliser changerMotDePasse si le champ n'est pas vide.
                    userService.changerMotDePasse(selectedUser.getId(), passwordField.getText());
                }
                selectedUser.setTelephone(telephoneField.getText());
                selectedUser.setDateNaissance(dateNaissancePicker.getValue());
                selectedUser.setRole(roleComboBox.getValue());
                selectedUser.setRoleId(roleComboBox.getValue().getId());
                selectedUser.setStatus(statusComboBox.getValue());
                
                userService.update(selectedUser);
                AlertUtils.showInfo("Succès", "Utilisateur mis à jour avec succès.");
            }
            clearForm();
            loadUsers();
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Erreur sauvegarde utilisateur", e);
            AlertUtils.showError("Erreur", "Une erreur est survenue lors de la sauvegarde: " + e.getMessage());
        }
    }

    private boolean validateForm() {
        StringBuilder errors = new StringBuilder();
        
        if (nomField.getText().trim().isEmpty()) errors.append("- Le nom est requis.\n");
        if (emailField.getText().trim().isEmpty()) errors.append("- L'email est requis.\n");
        if (selectedUser == null && passwordField.getText().trim().isEmpty()) errors.append("- Le mot de passe est requis.\n");
        if (roleComboBox.getValue() == null) errors.append("- Le rôle est requis.\n");
        
        if (errors.length() > 0) {
            AlertUtils.showWarning("Validation", "Veuillez corriger les erreurs suivantes :\n" + errors.toString());
            return false;
        }
        return true;
    }

    private void handleDeleteUser(User user) {
        Optional<ButtonType> result = AlertUtils.showConfirmation("Confirmation", "Voulez-vous vraiment supprimer l'utilisateur " + user.getNom() + " ?");
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                userService.delete(user.getId());
                userList.remove(user);
                AlertUtils.showInfo("Succès", "Utilisateur supprimé.");
            } catch (SQLException e) {
                LOG.log(Level.SEVERE, "Erreur suppression utilisateur", e);
                AlertUtils.showError("Erreur", "Impossible de supprimer l'utilisateur.");
            }
        }
    }
}
