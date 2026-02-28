package tn.esprit.projet.gui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import tn.esprit.projet.utils.SessionManager; // IMPORTED
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AdminChatController {

    @FXML private TextArea adminChatDisplay;
    @FXML private TextField adminChatInput;

    private WebSocketClient adminClient;
    private static final String HISTORY_FILE = "chat_history.txt";
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("EEEE, d MMM");

    @FXML
    public void initialize() {
        loadHistory();
        connectToWebSocket();
    }

    private void loadHistory() {
        File file = new File(HISTORY_FILE);
        if (file.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = br.readLine()) != null) {
                    // We parse the history so the Admin sees "Clean" text, not the | pipes
                    adminChatDisplay.appendText(formatMessageForDisplay(line) + "\n");
                }
                adminChatDisplay.appendText("--- Historique chargé ---\n");
            } catch (IOException e) {
                System.err.println("Erreur : " + e.getMessage());
            }
        }
    }

    /**
     * Converts "ID|ROLE|NAME|CONTENT|TIME|DATE" into "Name: Content (Time)"
     */
    private String formatMessageForDisplay(String rawMessage) {
        if (rawMessage.contains("|")) {
            String[] parts = rawMessage.split("\\|");
            if (parts.length >= 5) {
                String name = parts[2];
                String content = parts[3];
                String time = parts[4];
                return "[" + time + "] " + name + " : " + content;
            }
        }
        return rawMessage;
    }

    private void connectToWebSocket() {
        try {
            adminClient = new WebSocketClient(new URI("ws://localhost:8887")) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    Platform.runLater(() -> adminChatDisplay.appendText("[Système] Connecté en tant qu'ADMIN.\n"));
                }

                @Override
                public void onMessage(String message) {
                    Platform.runLater(() -> adminChatDisplay.appendText(formatMessageForDisplay(message) + "\n"));
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    Platform.runLater(() -> adminChatDisplay.appendText("[Système] Déconnecté.\n"));
                }

                @Override
                public void onError(Exception ex) {
                    System.err.println("Erreur : " + ex.getMessage());
                }
            };
            adminClient.connect();
        } catch (URISyntaxException e) { e.printStackTrace(); }
    }

    @FXML
    private void handleAdminSend() {
        if (adminClient != null && adminClient.isOpen() && SessionManager.isAdmin()) {
            String msg = adminChatInput.getText().trim();
            if (!msg.isEmpty()) {

                // 1. Get Admin info from Session
                int id = SessionManager.getCurrentUserId();
                String name = SessionManager.getCurrentUserName();
                String role = "ADMIN";
                String time = LocalDateTime.now().format(timeFormatter);
                String date = LocalDateTime.now().format(dateFormatter);

                // 2. Build the Protocol: "ID|ROLE|NAME|CONTENT|TIME|DATE"
                // This ensures the Client can parse the message correctly!
                String payload = id + "|" + role + "|" + name + "|" + msg + "|" + time + "|" + date;

                adminClient.send(payload);
                adminChatInput.clear();
            }
        } else {
            adminChatDisplay.appendText("[Erreur] Accès refusé ou serveur hors ligne.\n");
        }
    }
}