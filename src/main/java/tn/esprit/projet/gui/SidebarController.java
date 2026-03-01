package tn.esprit.projet.gui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import tn.esprit.projet.utils.SceneManager;
import tn.esprit.projet.utils.SessionManager;

import java.net.URL;
import java.util.ResourceBundle;

public class SidebarController implements Initializable {

    @FXML private Button btnDashboard;
    @FXML private Button btnReservations;
    @FXML private Button btnExplore;
    @FXML private Button btnProfile;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Optionnel : Définir le bouton Dashboard comme actif par défaut
        highlightActiveButton(btnDashboard);
    }

    /**
     * Affiche le Dashboard dans la zone de contenu principale
     */
    @FXML
    private void handleDashboard(ActionEvent event) {
        highlightActiveButton(btnDashboard);
        // On charge uniquement le contenu central
        SceneManager.loadClientContent("/tn/esprit/projet/gui/client/ClientDashboard.fxml");
    }

    /**
     * Affiche la liste des réservations
     */
    @FXML
    private void handleReservations(ActionEvent event) {
        highlightActiveButton(btnReservations);
        // SceneManager.loadClientContent("/tn/esprit/projet/gui/client/Reservations.fxml");
        System.out.println("Navigation vers les réservations...");
    }

    /**
     * Affiche l'exploration des destinations
     */
    @FXML
    private void handleExplore(ActionEvent event) {
        highlightActiveButton(btnExplore);
        // SceneManager.loadClientContent("/tn/esprit/projet/gui/client/Explore.fxml");
        System.out.println("Navigation vers l'exploration...");
    }

    /**
     * Affiche le Profil
     * Note : Si votre profil est en plein écran (sans sidebar),
     * utilisez switchTo au lieu de loadClientContent.
     */
    @FXML
    private void handleProfile(ActionEvent event) {
        highlightActiveButton(btnProfile);
        // Si vous voulez garder la sidebar :
        SceneManager.loadClientContent("/tn/esprit/projet/gui/profile/Profile.fxml");

        // SI vous voulez le profil en PLEIN ÉCRAN (sans sidebar) :
        // SceneManager.switchTo("/tn/esprit/projet/gui/profile/Profile.fxml");
    }

    /**
     * Déconnexion sécurisée via le SessionManager
     */
    @FXML
    private void handleLogout(ActionEvent event) {
        // Utilisation de la méthode de nettoyage que nous avons créée
        SessionManager.clearSession();

        // Retour à l'écran de login (Plein écran)
        SceneManager.switchTo("/tn/esprit/projet/gui/auth/Login.fxml");

        System.out.println("Utilisateur déconnecté via Sidebar.");
    }

    /**
     * Gère l'apparence visuelle du bouton sélectionné
     */
    private void highlightActiveButton(Button activeBtn) {
        // Réinitialiser tous les boutons (enlever la classe active)
        btnDashboard.getStyleClass().remove("profile-tab-btn-active");
        btnReservations.getStyleClass().remove("profile-tab-btn-active");
        btnExplore.getStyleClass().remove("profile-tab-btn-active");
        btnProfile.getStyleClass().remove("profile-tab-btn-active");

        // Ajouter la classe au bouton cliqué
        activeBtn.getStyleClass().add("profile-tab-btn-active");
    }
}