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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/RestaurantBookingView.fxml"));
            Parent root = loader.load();

            // 1. Force a large resolution to give the UI room to breathe
            Scene scene = new Scene(root, 1280, 800);

            primaryStage.setScene(scene);
            primaryStage.setTitle("GoVacate - Gastronomie Luxe");

            // 2. Make sure it's centered on your monitor
            primaryStage.setResizable(true);
            primaryStage.centerOnScreen();

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