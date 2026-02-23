package tn.esprit.projet.gui.admin;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import tn.esprit.projet.utils.SceneManager;
import tn.esprit.projet.utils.SessionManager;

import java.net.URL;
import java.util.ResourceBundle;

public class AdminDashboardController implements Initializable {

    @FXML private StackPane contentArea;
    @FXML private Button btnDashboard;
    @FXML private Button btnUsers;
    @FXML private HBox actionButtonsContainer;
    @FXML private Button addButton;
    @FXML private Button editButton;
    @FXML private Button deleteButton;
    @FXML private Circle avatarCircle;
    @FXML private Label adminName;
    @FXML private Label adminNameLabel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (!SessionManager.isAdmin()) {
            SceneManager.goToLogin();
            return;
        }
        String userName = SessionManager.getCurrentUserName();
        if (adminName != null) {
            adminName.setText(userName);
        }
        if (adminNameLabel != null) {
            adminNameLabel.setText(userName);
        }
        /* Layout FXML injects contentArea; content FXML does not */
        if (contentArea != null) {
            SceneManager.setAdminContentPane(contentArea);
            showHome(); // Load home by default
        }
    }

    @FXML
    private void showHome() {
        setActiveButton(btnDashboard);
        if (actionButtonsContainer != null) {
            actionButtonsContainer.setVisible(false);
        }
        SceneManager.loadAdminContent("/fxml/admin/AdminDashboard.fxml");
    }

    @FXML
    private void showUsers() {
        setActiveButton(btnUsers);
        if (actionButtonsContainer != null) {
            actionButtonsContainer.setVisible(true);
        }
        SceneManager.loadAdminContent("/fxml/admin/UserManagement.fxml");
    }

    /** Ouvre la gestion utilisateurs en mode ajout (formulaire vide). */
    @FXML
    private void handleNew() {
        // Logic for adding a new user can be implemented here or passed to the controller
        // For now, just ensuring we are on the user management view
        showUsers();
    }

    /** Ouvre la gestion utilisateurs (modifier = sélectionner un utilisateur puis Modifier). */
    @FXML
    private void handleSave() {
        // Logic for saving/editing
        showUsers();
    }

    /** Ouvre la gestion utilisateurs (supprimer = sélectionner un utilisateur puis Supprimer). */
    @FXML
    private void handleDelete() {
        // Logic for deleting
        showUsers();
    }

    @FXML
    private void handleUserManagement() {
        showUsers();
    }

    @FXML
    private void handleStatistics() {
        setActiveButton(null); // No specific sidebar button for stats in this layout yet, or create one
        if (actionButtonsContainer != null) {
            actionButtonsContainer.setVisible(false);
        }
        SceneManager.loadAdminContent("/fxml/admin/UserStatistics.fxml");
    }

    @FXML
    private void handleProfile() {
        SceneManager.goToProfile();
    }

    @FXML
    private void handleLogout() {
        SessionManager.logout();
        SceneManager.goToLogin();
    }

    private void setActiveButton(Button activeButton) {
        if (btnDashboard != null) btnDashboard.getStyleClass().remove("active");
        if (btnUsers != null) btnUsers.getStyleClass().remove("active");

        if (activeButton != null) {
            activeButton.getStyleClass().add("active");
        }
    }
}
