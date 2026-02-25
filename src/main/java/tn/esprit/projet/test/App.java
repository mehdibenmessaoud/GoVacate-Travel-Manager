package tn.esprit.projet.test;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class App extends Application {
    private static Stage primaryStage;

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        // Start with the Client view by default for now
        showClientView();
    }

    public static void showClientView() {
        loadView("/ClientMainView.fxml", "GoVacate - Client Portal", "/css/client_style.css");
    }

    public static void showAdminView() {
        loadView("/AdminDashboard.fxml", "GoVacate - Admin Panel", "/css/admin_style.css");
    }

    private static void loadView(String fxmlPath, String title, String cssPath) {
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource(fxmlPath));
            Parent root = loader.load();

            Scene scene = (primaryStage.getScene() == null)
                    ? new Scene(root)
                    : primaryStage.getScene();

            scene.setRoot(root);
            scene.getStylesheets().clear();
            scene.getStylesheets().add(App.class.getResource(cssPath).toExternalForm());

            primaryStage.setTitle(title);
            primaryStage.setScene(scene);
            primaryStage.show();

            System.out.println("✓ Loaded: " + fxmlPath);
        } catch (IOException e) {
            System.err.println("✗ Error loading " + fxmlPath);
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}