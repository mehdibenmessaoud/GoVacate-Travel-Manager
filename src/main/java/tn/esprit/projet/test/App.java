/*
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
}*/

package tn.esprit.projet.test;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import tn.esprit.projet.utils.SceneManager;

import java.net.URL;

public class App extends Application {

    /*@Override
    public void start(Stage primaryStage) throws Exception {
        // Comme tu as mis tes FXML directement dans 'resources', le chemin est "/Nom_du_fichier.fxml"
        URL fxmlLocation = getClass().getResource("/Auth.fxml");

        if (fxmlLocation == null) {
            System.err.println("❌ ERREUR : Le fichier Auth.fxml est introuvable !");
            System.err.println("Assurez-vous qu'il est bien à la racine de : src/main/resources/");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(fxmlLocation);
            Parent root = loader.load();

            // Paramètres de la fenêtre pour l'authentification
            primaryStage.setTitle("GoVacate - Connexion");
            Scene scene = new Scene(root);

            // Si tu as un fichier CSS pour l'auth, assure-toi qu'il est chargé ici ou dans le FXML
            // scene.getStylesheets().add(getClass().getResource("/auth-style.css").toExternalForm());

            primaryStage.setScene(scene);
            primaryStage.setResizable(false); // Souvent préférable pour l'écran de login
            primaryStage.show();

        } catch (Exception e) {
            System.err.println("❌ Erreur critique lors du chargement de l'authentification : " + e.getMessage());
            e.printStackTrace();
        }
    }*/

    @Override
    public void start(Stage stage) throws Exception {
        // 1. LIGNE INDISPENSABLE : On initialise le SceneManager avec le stage de JavaFX
        SceneManager.setPrimaryStage(stage);

        // 2. On charge la page de login au début
        SceneManager.switchTo("/Auth.fxml");

        stage.setTitle("GoVacate - Authentification");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}