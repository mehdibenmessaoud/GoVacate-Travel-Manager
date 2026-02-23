package tn.esprit.projet.test;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import tn.esprit.projet.utils.SceneManager;
import tn.esprit.projet.utils.govacate_connect; // Using Yassine's new class

import java.io.IOException;
import java.net.URL;

public class MainGovacate extends Application {

    @Override
    public void start(Stage stage) {
        // Init friend's SceneManager so other buttons don't crash
        SceneManager.setPrimaryStage(stage);

        try {
            // YOUR loading logic
            URL fxmlLocation = getClass().getResource("/GoVacate.fxml");
            FXMLLoader loader = new FXMLLoader(fxmlLocation);
            Parent root = loader.load();
            Scene scene = new Scene(root);

            // Setting window properties (Mixing both styles)
            stage.setTitle("GoVacate - Gestion de Voyages");
            stage.setMinWidth(1000);
            stage.setMinHeight(700);
            stage.setScene(scene);
            stage.show();

            // Friend's cleanup logic (Updated to use govacate_connect)
            stage.setOnCloseRequest(event -> {
                try {
                    govacate_connect.getInstance().getConnection().close();
                } catch (Exception e) {
                    System.err.println("Erreur fermeture connexion: " + e.getMessage());
                }
            });

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}