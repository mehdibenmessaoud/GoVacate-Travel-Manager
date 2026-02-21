package tn.esprit.projet.gui;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button; // AJOUTÉ
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public class ClientDashboardController {

    @FXML private ScrollPane explorerView;
    @FXML private VBox clientReservationView;
    @FXML private VBox clientFactureView;
    @FXML private VBox clientChatView;
    @FXML private TextArea chatDisplay;
    @FXML private TextField chatInput;

    private WebSocketClient webSocketClient;
    private static final String HISTORY_FILE = "chat_history.txt";

    @FXML
    public void initialize() {
        showExplorer();
        loadChatHistory();
        connectToChatServer();

        // --- AJOUT DYNAMIQUE DU BOUTON SUPPRIMER (Pas besoin de FXML) ---
        addDeleteButtonToUI();
    }

    private void addDeleteButtonToUI() {
        Button btnDelete = new Button("🗑 Supprimer l'historique");
        btnDelete.setStyle("-fx-background-color: #c0392b; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        btnDelete.setMaxWidth(Double.MAX_VALUE);

        // Action du bouton
        btnDelete.setOnAction(e -> handleClearHistory());

        // On l'ajoute à la fin de la vue chat
        if (clientChatView != null) {
            clientChatView.setSpacing(10);
            clientChatView.getChildren().add(btnDelete);
        }
    }

    // --- LOGIQUE DU CHAT ---

    private void loadChatHistory() {
        File file = new File(HISTORY_FILE);
        if (file.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = br.readLine()) != null) {
                    chatDisplay.appendText(line + "\n");
                }
            } catch (IOException e) { e.printStackTrace(); }
        }
    }

    private void connectToChatServer() {
        try {
            webSocketClient = new WebSocketClient(new URI("ws://localhost:8887")) {
                @Override
                public void onOpen(ServerHandshake h) {
                    Platform.runLater(() -> chatDisplay.appendText("[Système] Connecté au support.\n"));
                }

                @Override
                public void onMessage(String message) {
                    Platform.runLater(() -> {
                        if (message.equals("[EFFACER_TOUT]")) {
                            chatDisplay.clear();
                            chatDisplay.appendText("[Système] L'historique a été réinitialisé.\n");
                        } else {
                            chatDisplay.appendText(message + "\n");
                        }
                    });
                }

                @Override public void onClose(int c, String r, boolean rem) {}
                @Override public void onError(Exception ex) {}
            };
            webSocketClient.connect();
        } catch (URISyntaxException e) { e.printStackTrace(); }
    }

    @FXML
    private void handleSendMessage() {
        String msg = chatInput.getText().trim();
        if (webSocketClient != null && webSocketClient.isOpen() && !msg.isEmpty()) {
            webSocketClient.send("Client: " + msg);
            chatInput.clear();
        }
    }

    @FXML
    private void handleClearHistory() {
        File file = new File(HISTORY_FILE);
        if (file.exists()) file.delete();

        chatDisplay.clear();
        if (webSocketClient != null && webSocketClient.isOpen()) {
            webSocketClient.send("[EFFACER_TOUT]");
        }
    }

    // --- NAVIGATION (Fix LoadExceptions) ---

    private void hideAllViews() {
        if (explorerView != null) explorerView.setVisible(false);
        if (clientReservationView != null) clientReservationView.setVisible(false);
        if (clientFactureView != null) clientFactureView.setVisible(false);
        if (clientChatView != null) clientChatView.setVisible(false);
    }

    @FXML private void showExplorer() { hideAllViews(); if (explorerView != null) explorerView.setVisible(true); }

    @FXML private void showChat() { hideAllViews(); if (clientChatView != null) clientChatView.setVisible(true); }

    @FXML
    private void showReservations() {
        try {
            hideAllViews();
            clientReservationView.getChildren().clear();
            URL fxmlUrl = getClass().getResource("/Mes Réservations.fxml");
            if (fxmlUrl == null) fxmlUrl = getClass().getResource("/MesReservations.fxml");
            if (fxmlUrl != null) {
                FXMLLoader loader = new FXMLLoader(fxmlUrl);
                clientReservationView.getChildren().add(loader.load());
                clientReservationView.setVisible(true);
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML
    private void showFactures() {
        try {
            hideAllViews();
            clientFactureView.getChildren().clear();
            URL fxmlUrl = getClass().getResource("/MesFactures.fxml");
            if (fxmlUrl != null) {
                FXMLLoader loader = new FXMLLoader(fxmlUrl);
                clientFactureView.getChildren().add(loader.load());
                clientFactureView.setVisible(true);
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        if (webSocketClient != null) webSocketClient.close();
        Platform.exit();
        System.exit(0);
    }
}