package tn.esprit.projet.gui;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import tn.esprit.projet.entities.User;
import tn.esprit.projet.utils.MySrcModuleLauncher;
import tn.esprit.projet.utils.SessionManager;
import tn.esprit.projet.utils.SceneManager;

import java.net.URL;
import java.util.ResourceBundle;

public class ClientDashboardController implements Initializable {

    @FXML private Label welcomeLabel;
    @FXML private Label clientNameLabel;
    @FXML private Label avatarInitials;

    /**
     * Initialisation du Dashboard
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupUserSession();
    }

    /**
     * Charge les données de l'utilisateur connecté depuis le SessionManager
     */
    private void setupUserSession() {
        // Vérification de la session via SessionManager
        if (SessionManager.isLoggedIn()) {
            User user = SessionManager.getCurrentUser();

            // Personnalisation du message de bienvenue [cite: 2]
            welcomeLabel.setText("Bonjour, " + user.getNom() + " !");

            // Mise à jour du badge de profil [cite: 4, 5]
            clientNameLabel.setText(user.getNom() + " " + user.getNom());

            // Génération dynamique des initiales pour le cercle de l'avatar
            String initials = generateInitials(user.getNom(), user.getNom());
            if (avatarInitials != null) {
                avatarInitials.setText(initials);
            }
        } else {
            // Si aucune session n'est détectée, retour forcé au login
            handleLogout();
        }
    }

    /**
     * Logique de génération des initiales (ex: "John Doe" -> "JD")
     */
    private String generateInitials(String nom, String prenom) {
        StringBuilder sb = new StringBuilder();
        if (nom != null && !nom.isEmpty()) sb.append(nom.substring(0, 1).toUpperCase());
        if (prenom != null && !prenom.isEmpty()) sb.append(prenom.substring(0, 1).toUpperCase());
        return sb.length() > 0 ? sb.toString() : "?";
    }

    /**
     * Action lors du clic sur la carte "Mes Voyages" [cite: 6]
     */
    @FXML
    private void handleReservations() {
        System.out.println("Chargement de la vue Réservations...");
        // On charge le contenu dans la zone dynamique sans changer la sidebar
        SceneManager.loadClientContent("/tn/esprit/projet/gui/client/MyReservations.fxml");
    }

    /**
     * Action lors du clic sur la carte "Explorer" [cite: 13, 14]
     */
    @FXML
    private void handleExplore() {
        System.out.println("Chargement de la vue Exploration...");
        SceneManager.loadClientContent("/resources/ClientDestination.fxml");
    }

    @FXML
    private void openMySrcClientModule() {
        MySrcModuleLauncher.openWindow("/mysrc/Client.fxml", "GoVacate - Hotels Client");
    }

    /**
     * Redirection vers le profil
     */
    @FXML
    private void handleProfile() {
        // Selon votre choix, soit on reste avec la sidebar :
        SceneManager.loadClientContent("/resources/Profile.fxml");

        // Soit on passe en plein écran (comme sur vos captures) :
        // SceneManager.switchTo("/tn/esprit/projet/gui/profile/Profile.fxml");
    }

    /**
     * Déconnexion (Bouton en bas du dashboard ou sécurité) [cite: 22]
     */
    @FXML
    private void handleLogout() {
        // Nettoyage complet via la méthode statique
        SessionManager.clearSession();

        // Retour à l'écran de login (Plein écran obligatoire ici)
        SceneManager.switchTo("/Auth.fxml");
    }
}
