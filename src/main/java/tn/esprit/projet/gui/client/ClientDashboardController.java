package tn.esprit.projet.gui.client;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import tn.esprit.projet.utils.SceneManager;
import tn.esprit.projet.utils.SessionManager;

import java.net.URL;
import java.util.ResourceBundle;

public class ClientDashboardController implements Initializable {

    @FXML private Label welcomeLabel;
    @FXML private Label clientNameLabel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (!SessionManager.isClient()) {
            SceneManager.goToLogin();
            return;
        }
        String userName = SessionManager.getCurrentUserName();
        if (welcomeLabel != null) {
            welcomeLabel.setText("Bienvenue, " + userName + " !");
        }
        if (clientNameLabel != null) {
            clientNameLabel.setText(userName);
        }
    }

    @FXML
    private void handleReservations() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Mes Voyages");
        alert.setHeaderText(null);
        alert.setContentText("Fonctionnalité à venir : gestion des réservations.");
        alert.showAndWait();
    }

    @FXML
    private void handleExplore() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Explorer");
        alert.setHeaderText(null);
        alert.setContentText("Fonctionnalité à venir : exploration des vols, hôtels et activités.");
        alert.showAndWait();
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
}
