package tn.esprit.projet.GUI;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;

public class AdminChatController {

    @FXML private TextArea adminChatDisplay; // L'ID doit être identique dans le FXML
    @FXML private TextField adminChatInput;   // L'ID doit être identique dans le FXML

    private WebSocketClient adminClient;
    private static final String HISTORY_FILE = "chat_history.txt";

    @FXML
    public void initialize() {
        // 1. Charger les anciens messages sauvegardés
        loadHistory();

        // 2. Se connecter au serveur de chat
        connectToWebSocket();
    }

    /**
     * Lit le fichier chat_history.txt et l'affiche dans la zone de texte
     */
    private void loadHistory() {
        File file = new File(HISTORY_FILE);
        if (file.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = br.readLine()) != null) {
                    adminChatDisplay.appendText(line + "\n");
                }
                adminChatDisplay.appendText("--- Historique chargé ---\n");
            } catch (IOException e) {
                System.err.println("Erreur lors du chargement de l'historique : " + e.getMessage());
            }
        }
    }

    /**
     * Initialise la connexion WebSocket vers le serveur
     */
    private void connectToWebSocket() {
        try {
            adminClient = new WebSocketClient(new URI("ws://localhost:8887")) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    Platform.runLater(() -> adminChatDisplay.appendText("[Système] Connecté au serveur de support.\n"));
                }

                @Override
                public void onMessage(String message) {
                    // Reçoit les messages du client en temps réel
                    Platform.runLater(() -> adminChatDisplay.appendText(message + "\n"));
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    Platform.runLater(() -> adminChatDisplay.appendText("[Système] Déconnecté du serveur.\n"));
                }

                @Override
                public void onError(Exception ex) {
                    System.err.println("Erreur WebSocket Admin : " + ex.getMessage());
                }
            };
            adminClient.connect();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    /**
     * Méthode liée au bouton "Envoyer" ou à la touche Entrée du TextField
     */
    @FXML
    private void handleAdminSend() {
        if (adminClient != null && adminClient.isOpen()) {
            String msg = adminChatInput.getText().trim();
            if (!msg.isEmpty()) {
                // Envoie le message au serveur (qui l'enregistrera et le diffusera)
                adminClient.send("ADMIN: " + msg);
                adminChatInput.clear();
            }
        } else {
            adminChatDisplay.appendText("[Erreur] Connexion perdue avec le serveur.\n");
        }
    }
}