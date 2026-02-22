package tn.esprit.projet.test;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import tn.esprit.projet.utils.ChatServer; // N'oublie pas l'import !

import java.io.IOException;
import java.net.URL;

public class ClientTestLauncher extends Application {

    private ChatServer chatServer;

    @Override
    public void start(Stage stage) {
        try {
            // --- NOUVEAU : Démarrer le serveur de chat en arrière-plan ---
            startChatServer();

            // 1. Chargement du fichier FXML (Espace Client)
            URL fxmlLocation = getClass().getResource("/ClientDashboard.fxml");
            if (fxmlLocation == null) {
                System.err.println("ERREUR : Fichier ClientDashboard.fxml non trouvé dans /resources");
                return;
            }

            FXMLLoader fxmlLoader = new FXMLLoader(fxmlLocation);
            StackPane root = fxmlLoader.load();

            // 2. Création de la scène avec la taille Liquid (1100x750)
            Scene scene = new Scene(root, 1100, 750);

            // 3. Application du CSS
            URL cssLocation = getClass().getResource("/css/style.css");
            if (cssLocation != null) {
                scene.getStylesheets().add(cssLocation.toExternalForm());
            }

            stage.setTitle("GoVacate - Mode Test Client");
            stage.setScene(scene);
            stage.setResizable(true);

            // Sécurité : Arrêter le serveur quand on ferme la fenêtre
            stage.setOnCloseRequest(event -> {
                try {
                    if (chatServer != null) chatServer.stop();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });

            stage.show();
            System.out.println("Test lancé avec succès !");

        } catch (IOException e) {
            System.err.println("Erreur lors du chargement du test : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void startChatServer() {
        // On lance le serveur sur le port 8887
        chatServer = new ChatServer(8887);
        chatServer.start();
        System.out.println("🚀 [Serveur] WebSocket Server démarré sur le port 8887");
    }

    public static void main(String[] args) {
        launch(args);
    }
}