package tn.esprit.projet.utils;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

public final class MySrcModuleLauncher {

    private MySrcModuleLauncher() {
    }

    public static void openWindow(String fxmlPath, String title) {
        try {
            Parent root = FXMLLoader.load(MySrcModuleLauncher.class.getResource(fxmlPath));
            Stage stage = new Stage();
            stage.setTitle(title);
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText("Impossible d'ouvrir le module _mysrc");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }
}
