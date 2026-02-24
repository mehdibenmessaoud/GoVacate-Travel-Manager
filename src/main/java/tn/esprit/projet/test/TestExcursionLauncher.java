package tn.esprit.projet.test;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import tn.esprit.projet.utils.ChatServer; // <--- ADD THIS IMPORT

public class TestExcursionLauncher extends Application {

    private ChatServer chatServer; // <--- ADD THIS FIELD

    @Override
    public void start(Stage primaryStage) {
        try {
            // --- FIX: START THE CHAT SERVER HERE TOO ---
            chatServer = new ChatServer(8887);
            chatServer.start();
            System.out.println("🚀 [Serveur] WebSocket Server démarré pour le test Excursion");

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ExcursionBooking.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root, 1200, 800);
            scene.setFill(Color.TRANSPARENT);

            primaryStage.setTitle("Travel Agency - Test Réservation Excursion");
            primaryStage.setScene(scene);

            primaryStage.setResizable(true);
            primaryStage.centerOnScreen();

            // --- SAFETY: Stop server when window closes ---
            primaryStage.setOnCloseRequest(event -> {
                try {
                    if (chatServer != null) chatServer.stop();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });

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