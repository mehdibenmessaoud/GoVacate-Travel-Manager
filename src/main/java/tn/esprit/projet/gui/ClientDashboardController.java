package tn.esprit.projet.gui;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import tn.esprit.projet.utils.SessionManager;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ClientDashboardController {

    @FXML private ScrollPane explorerView;
    @FXML private VBox clientReservationView;
    @FXML private VBox clientFactureView;
    @FXML private VBox clientChatView;
    @FXML private VBox chatMessageContainer;
    @FXML private ScrollPane chatScrollPane;
    @FXML private TextField chatInput;

    private WebSocketClient webSocketClient;
    private String lastProcessedMessage = ""; // 🎯 Anti-Duplication
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("EEEE, d MMM");

    private String getUserHistoryFile() {
        return "chat_history_user_" + SessionManager.getCurrentUserId() + ".txt";
    }

    @FXML
    public void initialize() {
        // Initialiser la vue par défaut
        showExplorer();

        // Auto-scroll pour le chat
        chatMessageContainer.heightProperty().addListener((obs, oldVal, newVal) ->
                chatScrollPane.setVvalue(1.0));

        if (SessionManager.isLoggedIn()) {
            loadChatHistory();
            connectToChatServer();
        }
        addDeleteButtonToUI();
    }

    // --- LOGIC CHAT (VERSION FINALE) ---

    @FXML
    private void handleSendMessage() {
        String msg = chatInput.getText().trim();
        if (msg.isEmpty() || !SessionManager.isLoggedIn()) return;

        if (webSocketClient == null || !webSocketClient.isOpen()) {
            System.out.println("🔄 Tentative de reconnexion...");
            connectToChatServer();
        }

        if (webSocketClient == null || !webSocketClient.isOpen()) {
            System.err.println("❌ Serveur indisponible.");
            return;
        }

        String time = LocalDateTime.now().format(timeFormatter);
        String date = LocalDateTime.now().format(dateFormatter);

        // Protocol: TARGET_ID|ROLE|NAME|CONTENT|TIME|DATE
        String payload = SessionManager.getCurrentUserId() + "|CLIENT|" +
                SessionManager.getCurrentUserName() + "|" +
                msg + "|" + time + "|" + date;

        try {
            webSocketClient.send(payload);
            chatInput.clear();
            // L'affichage sera géré par onMessage (Relay) pour éviter les doublons
        } catch (Exception e) {
            System.err.println("❌ Erreur d'envoi : " + e.getMessage());
        }
    }

    private void connectToChatServer() {
        try {
            if (webSocketClient != null && webSocketClient.isOpen()) return;

            webSocketClient = new WebSocketClient(new URI("ws://localhost:8887")) {
                @Override
                public void onOpen(ServerHandshake h) {
                    System.out.println("✅ [CLIENT] Online");
                }

                @Override
                public void onMessage(String message) {
                    Platform.runLater(() -> {
                        // Anti-duplication check
                        if (message.equals(lastProcessedMessage)) return;
                        lastProcessedMessage = message;

                        String[] parts = message.split("\\|");
                        if (parts.length >= 4) {
                            String targetId = parts[0];
                            // On n'affiche que si c'est pour nous (Admin -> Moi ou Mon Relay)
                            if (targetId.equals(String.valueOf(SessionManager.getCurrentUserId()))) {
                                addMessageToUI(message);
                            }
                        }
                    });
                }

                @Override public void onClose(int c, String r, boolean rem) {}
                @Override public void onError(Exception ex) {}
            };

            new Thread(() -> {
                try {
                    webSocketClient.connectBlocking();
                } catch (InterruptedException e) { e.printStackTrace(); }
            }).start();

        } catch (URISyntaxException e) { e.printStackTrace(); }
    }

    private void loadChatHistory() {
        chatMessageContainer.getChildren().clear();
        File file = new File(getUserHistoryFile());
        if (file.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = br.readLine()) != null) addMessageToUI(line);
            } catch (IOException e) { e.printStackTrace(); }
        }
    }

    private void addMessageToUI(String message) {
        if (message == null || message.isEmpty()) return;
        Platform.runLater(() -> {
            String content = message, role = "ADMIN", name = "Admin", time = "00:00", date = "";
            if (message.contains("|")) {
                String[] p = message.split("\\|");
                if (p.length >= 6) { role = p[1]; name = p[2]; content = p[3]; time = p[4]; date = p[5]; }
            }
            checkAndAddDateSeparator(date);

            HBox row = new HBox();
            VBox v = new VBox(2);
            Label b = new Label(content);
            b.setWrapText(true); b.setMaxWidth(400);
            Label info = new Label(name + " • " + time);
            info.setStyle("-fx-font-size: 9px; -fx-text-fill: #bdc3c7;");

            if ("CLIENT".equalsIgnoreCase(role)) {
                row.setAlignment(Pos.CENTER_RIGHT); v.setAlignment(Pos.CENTER_RIGHT);
                b.setStyle("-fx-background-color: #00FFCC; -fx-text-fill: #1a1a1a; -fx-padding: 10 15; -fx-background-radius: 15 15 2 15; -fx-font-weight: bold;");
            } else {
                row.setAlignment(Pos.CENTER_LEFT); v.setAlignment(Pos.CENTER_LEFT);
                b.setStyle("-fx-background-color: #3d3d3d; -fx-text-fill: #ecf0f1; -fx-padding: 10 15; -fx-background-radius: 15 15 15 2;");
            }
            v.getChildren().addAll(b, info);
            row.getChildren().add(v);
            chatMessageContainer.getChildren().add(row);
        });
    }

    // --- OLD METHODS (NAVIGATION & UI) ---

    private void checkAndAddDateSeparator(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return;
        boolean exists = false;
        for (Node node : chatMessageContainer.getChildren()) {
            if (node instanceof HBox) {
                HBox h = (HBox) node;
                if (!h.getChildren().isEmpty() && h.getChildren().get(0) instanceof Label) {
                    if (((Label) h.getChildren().get(0)).getText().equals(dateStr)) { exists = true; break; }
                }
            }
        }
        if (!exists) {
            Label l = new Label(dateStr);
            l.setStyle("-fx-background-color: #2c3e50; -fx-text-fill: #95a5a6; -fx-padding: 3 10; -fx-background-radius: 10; -fx-font-size: 10px;");
            HBox r = new HBox(l); r.setAlignment(Pos.CENTER);
            VBox.setMargin(r, new javafx.geometry.Insets(10, 0, 10, 0));
            chatMessageContainer.getChildren().add(r);
        }
    }

    @FXML
    private void handleClearHistory() {
        File f = new File(getUserHistoryFile());
        if (f.exists()) f.delete();
        chatMessageContainer.getChildren().clear();
    }

    private void addDeleteButtonToUI() {
        Button btn = new Button("🗑 Supprimer l'historique");
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #FF4B5C; -fx-cursor: hand; -fx-font-size: 11px;");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setOnAction(e -> handleClearHistory());
        if (clientChatView != null) clientChatView.getChildren().add(btn);
    }

    private void hideAllViews() {
        explorerView.setVisible(false);
        if(clientReservationView != null) clientReservationView.setVisible(false);
        if(clientFactureView != null) clientFactureView.setVisible(false);
        if(clientChatView != null) clientChatView.setVisible(false);
    }

    @FXML private void showExplorer() { hideAllViews(); explorerView.setVisible(true); }
    @FXML public void showChat() { hideAllViews(); clientChatView.setVisible(true); }

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
            URL u = getClass().getResource("/MesFactures.fxml");
            if (u != null) {
                clientFactureView.getChildren().add(new FXMLLoader(u).load());
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