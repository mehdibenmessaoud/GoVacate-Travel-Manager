package tn.esprit.projet.gui.profile;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import tn.esprit.projet.entities.User;
import tn.esprit.projet.services.UserService;
import tn.esprit.projet.utils.SceneManager;
import tn.esprit.projet.utils.SessionManager;
import tn.esprit.projet.utils.ValidationUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProfileController implements Initializable {

    // === Elements FXML Nom Complet ===
    @FXML private Label lblFullName;
    @FXML private Label lblDisplayFullName;
    @FXML private TextField txtPrenom;
    @FXML private TextField txtNom;
    @FXML private TextField txtFullName;
    @FXML private TextField fieldFullName;
    @FXML private HBox hboxEditName;
    @FXML private Label avatarInitials;
    @FXML private ImageView avatarImage;

    // === Autres elements ===
    @FXML private Label lblEmail;
    @FXML private Label lblRole;
    @FXML private Label lblMemberSince;
    @FXML private TextField fieldEmail;
    @FXML private TextField fieldTelephone;
    @FXML private DatePicker dpDateNaissance;
    @FXML private Label lblStatus;
    @FXML private Button btnEdit;
    @FXML private HBox hboxActions;
    @FXML private Label messageLabel;

    // === Securite ===
    @FXML private PasswordField currentPasswordField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;

    // === Tabs ===
    @FXML private Button tabInformations;
    @FXML private Button tabSecurite;
    @FXML private Button tabPreferences;
    @FXML private VBox cardInformations;
    @FXML private VBox cardSecurite;
    @FXML private HBox statusBox;

    private boolean isEditing = false;
    private User currentUser;
    private UserService userService;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (!SessionManager.isLoggedIn()) {
            SceneManager.goToLogin();
            return;
        }
        
        try {
            userService = new UserService();
        } catch (Exception e) {
            showMessage("Erreur d'initialisation: " + e.getMessage(), "error");
            return;
        }
        
        currentUser = SessionManager.getCurrentUser();
        if (currentUser != null) {
            loadUserDataAsync();
        }
    }

    private void loadUserDataAsync() {
        executor.execute(() -> {
            try {
                User user = userService.getById(currentUser.getId());
                Platform.runLater(() -> {
                    if (user != null) {
                        currentUser = user;
                        displayUserData();
                    } else {
                        showMessage("Impossible de charger les donnees utilisateur", "error");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showMessage("Erreur: " + e.getMessage(), "error");
                });
            }
        });
    }

    private void displayUserData() {
        if (currentUser == null) return;

        String fullName = currentUser.getNom() != null ? currentUser.getNom() : "";
        String[] nameParts = fullName.split(" ", 2);
        String prenom = nameParts.length > 0 ? nameParts[0] : "";
        String nom = nameParts.length > 1 ? nameParts[1] : "";

        String imageUrl = currentUser.getImageUrl();
        if (avatarImage != null && avatarInitials != null) {
            if (imageUrl != null && !imageUrl.isEmpty() && !"default.png".equals(imageUrl)) {
                File f = new File(imageUrl);
                if (f.exists()) {
                    try {
                        Image img = new Image(f.toURI().toString());
                        avatarImage.setImage(img);
                        Circle clip = new Circle(60, 60, 60);
                        avatarImage.setClip(clip);
                        avatarImage.setVisible(true);
                        avatarImage.setManaged(true);
                        avatarInitials.setVisible(false);
                        avatarInitials.setManaged(false);
                    } catch (Exception e) {
                        showInitialsOnly(prenom, nom);
                    }
                } else {
                    showInitialsOnly(prenom, nom);
                }
            } else {
                showInitialsOnly(prenom, nom);
            }
        }

        if (lblFullName != null) {
            lblFullName.setText(fullName.isEmpty() ? "Utilisateur" : fullName);
        }
        if (lblDisplayFullName != null) {
            lblDisplayFullName.setText(fullName.isEmpty() ? "Utilisateur" : fullName);
        }

        if (txtPrenom != null) txtPrenom.setText(prenom);
        if (txtNom != null) txtNom.setText(nom);
        if (fieldFullName != null) fieldFullName.setText(fullName);
        if (txtFullName != null) txtFullName.setText(fullName);

        String email = currentUser.getEmail() != null ? currentUser.getEmail() : "";
        if (lblEmail != null) lblEmail.setText(email);
        if (fieldEmail != null) fieldEmail.setText(email);

        if (fieldTelephone != null) {
            fieldTelephone.setText(currentUser.getTelephone() != null ? currentUser.getTelephone() : "");
        }

        if (dpDateNaissance != null) {
            dpDateNaissance.setValue(currentUser.getDateNaissance());
        }

        boolean isAdmin = currentUser.isAdmin();
        if (lblRole != null) {
            lblRole.setText(isAdmin ? "ADMINISTRATEUR" : "CLIENT");
            lblRole.getStyleClass().removeAll("profile-role-badge-admin", "profile-role-badge-client");
            lblRole.getStyleClass().add(isAdmin ? "profile-role-badge-admin" : "profile-role-badge-client");
        }

        if (lblStatus != null) {
            lblStatus.setText(currentUser.isActive() ? "Actif" : currentUser.getStatus());
        }

        if (lblMemberSince != null) {
            lblMemberSince.setText("Membre depuis " + formatDate(currentUser.getCreatedAt()));
        }
    }

    @FXML
    private void toggleEditMode() {
        isEditing = !isEditing;

        if (isEditing) {
            if (btnEdit != null) {
                btnEdit.setText("Annuler");
                btnEdit.getStyleClass().remove("profile-card-edit-btn");
                btnEdit.getStyleClass().add("profile-btn-cancel");
            }

            if (lblDisplayFullName != null) {
                lblDisplayFullName.setVisible(false);
                lblDisplayFullName.setManaged(false);
            }
            if (fieldFullName != null) {
                fieldFullName.setVisible(true);
                fieldFullName.setManaged(true);
                fieldFullName.setText(currentUser.getNom() != null ? currentUser.getNom() : "");
            }

            if (hboxActions != null) {
                hboxActions.setVisible(true);
                hboxActions.setManaged(true);
            }

        } else {
            cancelEdit();
        }
    }

    @FXML
    private void cancelEdit() {
        isEditing = false;
        if (btnEdit != null) {
            btnEdit.setText("Modifier");
            btnEdit.getStyleClass().remove("profile-btn-cancel");
            if (!btnEdit.getStyleClass().contains("profile-card-edit-btn")) {
                btnEdit.getStyleClass().add("profile-card-edit-btn");
            }
        }

        if (lblDisplayFullName != null) {
            lblDisplayFullName.setVisible(true);
            lblDisplayFullName.setManaged(true);
        }
        if (fieldFullName != null) {
            fieldFullName.setVisible(false);
            fieldFullName.setManaged(false);
        }

        if (hboxActions != null) {
            hboxActions.setVisible(false);
            hboxActions.setManaged(false);
        }

        displayUserData();
    }

    @FXML
    private void saveProfile() {
        if (fieldFullName == null || fieldTelephone == null || dpDateNaissance == null) {
            showMessage("Erreur: Composants non charges", "error");
            return;
        }
        
        String nouveauNom = fieldFullName.getText();
        String telephone = fieldTelephone.getText();
        LocalDate dateN = dpDateNaissance.getValue();

        if (!ValidationUtils.isNotEmpty(nouveauNom)) {
            showMessage("Le nom est obligatoire", "error");
            return;
        }
        
        if (!ValidationUtils.isValidNom(nouveauNom)) {
            showMessage(ValidationUtils.getNomErrorMessage(), "error");
            return;
        }

        if (ValidationUtils.isNotEmpty(telephone) && !ValidationUtils.isValidTelephone(telephone)) {
            showMessage(ValidationUtils.getTelephoneErrorMessage(), "error");
            return;
        }

        if (!ValidationUtils.isValidDateDeNaissance(dateN)) {
            showMessage("La date de naissance ne peut pas etre dans le futur", "error");
            return;
        }

        currentUser.setNom(nouveauNom.trim());
        currentUser.setTelephone(telephone != null ? telephone.trim() : "");
        currentUser.setDateNaissance(dateN);

        executor.execute(() -> {
            try {
                userService.update(currentUser);
                Platform.runLater(() -> {
                    showMessage("Profil mis a jour avec succes", "success");
                    SessionManager.login(currentUser);
                    cancelEdit();
                });
            } catch (java.sql.SQLException e) {
                Platform.runLater(() -> {
                    showMessage(e.getMessage() != null ? e.getMessage() : "Impossible de mettre a jour le profil", "error");
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showMessage("Erreur: " + e.getMessage(), "error");
                });
            }
        });
    }

    private void showInitialsOnly(String prenom, String nom) {
        if (avatarInitials != null) {
            String initials = (prenom.isEmpty() ? "" : prenom.substring(0, 1)) +
                             (nom.isEmpty() ? "" : nom.substring(0, 1));
            avatarInitials.setText(initials.toUpperCase().isEmpty() ? "?" : initials.toUpperCase());
            avatarInitials.setVisible(true);
            avatarInitials.setManaged(true);
        }
        if (avatarImage != null) {
            avatarImage.setImage(null);
            avatarImage.setVisible(false);
            avatarImage.setManaged(false);
        }
    }

    @FXML
    private void handleChangeAvatar() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choisir une photo de profil");
        fc.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Images", "*.jpg", "*.jpeg", "*.png", "*.gif")
        );
        File selected = fc.showOpenDialog(avatarImage != null ? avatarImage.getScene().getWindow() : null);
        if (selected == null) return;

        long fileSize = selected.length();
        long maxSize = 20 * 1024 * 1024; // 20 MB
        if (fileSize > maxSize) {
            showMessage("L'image est trop volumineuse (max 20 Mo)", "error");
            return;
        }

        Path uploadsDir = Paths.get(System.getProperty("user.dir"), "uploads", "profiles");
        try {
            Files.createDirectories(uploadsDir);
        } catch (IOException e) {
            showMessage("Impossible de creer le dossier: " + e.getMessage(), "error");
            return;
        }

        String ext = "";
        String name = selected.getName();
        int i = name.lastIndexOf('.');
        if (i > 0) ext = name.substring(i);

        String destFileName = "user_" + currentUser.getId() + ext;
        Path dest = uploadsDir.resolve(destFileName);
        try {
            Files.copy(selected.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            showMessage("Erreur lors de la copie: " + e.getMessage(), "error");
            return;
        }

        String absPath = dest.toAbsolutePath().toString();
        currentUser.setImageUrl(absPath);

        executor.execute(() -> {
            try {
                boolean ok = userService.updateImageUrl(currentUser.getId(), absPath);
                Platform.runLater(() -> {
                    if (ok) {
                        SessionManager.login(currentUser);
                        displayUserData();
                        showMessage("Photo de profil mise a jour", "success");
                    } else {
                        showMessage("Impossible de sauvegarder la photo en base", "error");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showMessage("Erreur: " + e.getMessage(), "error"));
            }
        });
    }

    @FXML
    private void showInformations() {
        if (cardInformations != null) { cardInformations.setVisible(true); cardInformations.setManaged(true); }
        if (cardSecurite != null) { cardSecurite.setVisible(false); cardSecurite.setManaged(false); }
        if (tabInformations != null && !tabInformations.getStyleClass().contains("profile-tab-btn-active")) {
            tabInformations.getStyleClass().add("profile-tab-btn-active");
        }
        if (tabSecurite != null) tabSecurite.getStyleClass().remove("profile-tab-btn-active");
    }

    @FXML
    private void showSecurite() {
        if (cardInformations != null) { cardInformations.setVisible(false); cardInformations.setManaged(false); }
        if (cardSecurite != null) { cardSecurite.setVisible(true); cardSecurite.setManaged(true); }
        if (tabSecurite != null && !tabSecurite.getStyleClass().contains("profile-tab-btn-active")) {
            tabSecurite.getStyleClass().add("profile-tab-btn-active");
        }
        if (tabInformations != null) tabInformations.getStyleClass().remove("profile-tab-btn-active");
    }

    @FXML
    private void showPreferences() {
    }

    @FXML
    private void updatePassword() {
        if (currentPasswordField == null || newPasswordField == null || confirmPasswordField == null) {
            showMessage("Erreur: Composants non charges", "error");
            return;
        }
        
        String current = currentPasswordField.getText();
        String nouveau = newPasswordField.getText();
        String confirmation = confirmPasswordField.getText();

        if (!ValidationUtils.isNotEmpty(current) || !ValidationUtils.isNotEmpty(nouveau) || !ValidationUtils.isNotEmpty(confirmation)) {
            showMessage("Veuillez remplir tous les champs", "error");
            return;
        }

        if (userService == null) {
            userService = new UserService();
        }
        
        User verified = userService.authenticate(currentUser.getEmail(), current);
        if (verified == null) {
            showMessage("Mot de passe actuel incorrect", "error");
            return;
        }

        if (!ValidationUtils.isValidPassword(nouveau)) {
            showMessage(ValidationUtils.getPasswordErrorMessage(), "error");
            return;
        }

        if (!ValidationUtils.matchesPassword(nouveau, confirmation)) {
            showMessage("Les mots de passe ne correspondent pas", "error");
            confirmPasswordField.clear();
            return;
        }

        executor.execute(() -> {
            try {
                boolean success = userService.changerMotDePasse(currentUser.getId(), nouveau);
                Platform.runLater(() -> {
                    if (success) {
                        showMessage("Mot de passe change avec succes", "success");
                        currentPasswordField.clear();
                        newPasswordField.clear();
                        confirmPasswordField.clear();
                    } else {
                        showMessage("Erreur lors du changement", "error");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showMessage("Erreur: " + e.getMessage(), "error");
                });
            }
        });
    }

    @FXML
    private void handleBack() {
        if (SessionManager.isAdmin()) {
            SceneManager.switchTo("/fxml/admin/AdminDashboardLayout.fxml");
        } else {
            SceneManager.switchTo("/fxml/client/ClientDashboardLayout.fxml");
        }
    }

    private String formatDate(java.time.LocalDateTime dateTime) {
        if (dateTime == null) return "N/A";
        return dateTime.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH));
    }

    private void showMessage(String message, String type) {
        if (messageLabel != null) {
            messageLabel.setText(message);
            messageLabel.setStyle("success".equals(type) ? "-fx-text-fill: #27AE60;" : "-fx-text-fill: #E74C3C;");
        }
    }
    
    public void cleanup() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}
