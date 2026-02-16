package tn.esprit.projet.test;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class TestExcursionLauncher extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            // Utilisation d'un chemin relatif robuste
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ExcursionBooking.fxml"));
            Parent root = loader.load();

            // Taille adaptée à ton design (Excursion est souvent plus étroit que la liste)
            Scene scene = new Scene(root);

            // On active la transparence si ton CSS utilise des bordures arrondies (border-radius)
            scene.setFill(Color.TRANSPARENT);
            // primaryStage.initStyle(StageStyle.TRANSPARENT); // Décommenter si tu as un fond personnalisé

            primaryStage.setTitle("Travel Agency - Test Réservation");
            primaryStage.setScene(scene);

            // Empêcher le redimensionnement pour garder l'aspect "App Mobile"
            primaryStage.setResizable(false);

            primaryStage.show();

        } catch (Exception e) {
            System.err.println("Erreur de chargement du FXML : " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}