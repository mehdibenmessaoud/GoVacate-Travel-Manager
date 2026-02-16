package tn.esprit.projet.test;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class MainGovacate extends Application {

    @Override
    public void start(Stage stage) {
        try {
            // Chargement simple
            URL fxmlLocation = getClass().getResource("/GoVacate.fxml");
            FXMLLoader loader = new FXMLLoader(fxmlLocation);

            // PLUS BESOIN DE loader.setController(this) !

            Parent root = loader.load();
            Scene scene = new Scene(root);

            stage.setTitle("GoVacate");
            stage.setMaximized(true);
            stage.setScene(scene);
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}