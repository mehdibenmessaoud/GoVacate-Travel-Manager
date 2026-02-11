package tn.esprit.projet.test;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        try {
            System.out.println("Loading FXML...");
            Parent root = FXMLLoader.load(getClass().getResource("/Auth.fxml"));
            
            System.out.println("Creating scene...");
            Scene scene = new Scene(root);

            // Load initial light theme
            String cssResource = getClass().getResource("/css/style.css").toExternalForm();
            scene.getStylesheets().add(cssResource);
            System.out.println("✓ CSS loaded: admin_light.css");

            stage.setTitle("GoVacate Admin Panel");
            stage.setWidth(1400);
            stage.setHeight(800);
            stage.setScene(scene);
            stage.show();
            
            System.out.println("✓ Application started successfully");
            
        } catch (NullPointerException e) {
            System.err.println("✗ FXML Resource not found: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } catch (Exception e) {
            System.err.println("✗ Error starting application: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        System.out.println("GoVacate Application Starting...");
        launch(args);
    }
}