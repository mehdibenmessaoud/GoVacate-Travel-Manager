package tn.esprit.projet.test;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            // VERIFIE BIEN CE CHEMIN : il doit correspondre à l'endroit où est ton FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/RestaurantBookingView.fxml"));
            Parent root = loader.load();
            primaryStage.setScene(new Scene(root));
            primaryStage.setTitle("Test Réservation");
            primaryStage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ASTUCE : On crée un deuxième main qui n'hérite pas de Application
    public static void main(String[] args) {
        launch(args);
    }
}