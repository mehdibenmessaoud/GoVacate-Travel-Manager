package tn.esprit.projet.test;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.net.URL; // Import indispensable pour résoudre l'erreur 'URL'

public class App extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Le "/" au début indique la racine du dossier 'resources'
        URL fxmlLocation = getClass().getResource("/AdminView.fxml");

        if (fxmlLocation == null) {
            System.err.println("❌ ERREUR : Le fichier MainLayout.fxml est introuvable !");
            System.err.println("Vérifiez qu'il est bien dans src/main/resources/");
            // Affiche l'endroit où Java cherche les fichiers pour vous aider à débugger
            System.out.println("Classpath actuel : " + getClass().getProtectionDomain().getCodeSource().getLocation());
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(fxmlLocation);
            Parent root = loader.load();

            primaryStage.setTitle("GoVacate - Gestion des Packs");
            primaryStage.setScene(new Scene(root));

            // Empêche la fenêtre d'être trop petite au démarrage
            primaryStage.setMinWidth(1000);
            primaryStage.setMinHeight(700);

            primaryStage.show();
        } catch (Exception e) {
            System.err.println("❌ Erreur lors du chargement du FXML : " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}