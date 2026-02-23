package tn.esprit.projet.gui.client;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.StackPane;
import tn.esprit.projet.utils.SceneManager;
import tn.esprit.projet.utils.SessionManager;

import java.net.URL;
import java.util.ResourceBundle;

public class ClientDashboardLayoutController implements Initializable {

    @FXML private StackPane contentPane;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (!SessionManager.isClient()) {
            SceneManager.goToLogin();
            return;
        }
        SceneManager.setClientContentPane(contentPane);
        SceneManager.loadClientContent("/fxml/client/ClientDashboard.fxml");
    }

    @FXML
    private void handleHome() {
        SceneManager.loadClientContent("/fxml/client/ClientDashboard.fxml");
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
