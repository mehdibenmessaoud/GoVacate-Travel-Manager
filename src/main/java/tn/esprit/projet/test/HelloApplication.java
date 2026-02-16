package tn.esprit.projet.test;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        // Remplace "GoVacate.fxml" par le nom exact de ton fichier FXML
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/GoVacate.fxml"));

        // Taille correspondant à ton FXML (1100x750)
        Scene scene = new Scene(fxmlLoader.load(), 1100, 750);

        stage.setTitle("GoVacate - Surf the Wave!");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}