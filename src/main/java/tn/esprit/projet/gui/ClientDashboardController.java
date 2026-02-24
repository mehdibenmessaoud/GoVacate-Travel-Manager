package tn.esprit.projet.gui;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
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

    // UI components for the modern chat
    @FXML private VBox chatMessageContainer; // Replace TextArea with this in FXML
    @FXML private ScrollPane chatScrollPane;
    @FXML private TextField chatInput;

    private WebSocketClient webSocketClient;
    private static final String HISTORY_FILE = "chat_history.txt";

    @FXML
    public void initialize() {
        showExplorer();

        // Auto-scroll chat to bottom when new messages arrive
        chatMessageContainer.heightProperty().addListener((obs, oldVal, newVal) ->
                chatScrollPane.setVvalue(1.0));

        loadChatHistory();
        connectToChatServer();
        addDeleteButtonToUI();
    }

    // --- MODERN UI MESSAGE BUBBLES ---

    private void addMessageToUI(String message) {
        if (message.contains("[EFFACER_TOUT]")) {
            chatMessageContainer.getChildren().clear();
            return;
        }

        String messageContent = message;
        String messageTime = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));

        // Check if the message has a timestamp attached (e.g., "Client: Hello|14:30")
        if (message.contains("|")) {
            String[] parts = message.split("\\|");
            messageContent = parts[0];
            messageTime = parts[1];
        }

        HBox row = new HBox();
        VBox bubbleContainer = new VBox(2);

        String cleanMsg = messageContent.replace("Client: ", "").replace("Admin: ", "").replace("ADMIN: ", "");
        Label bubble = new Label(cleanMsg);
        bubble.setWrapText(true);
        bubble.setMaxWidth(400);

        // DISPLAY THE EXTRACTED TIME
        Label timeLabel = new Label(messageTime);
        timeLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: #bdc3c7;");

        if (messageContent.startsWith("Client:")) {
            row.setAlignment(Pos.CENTER_RIGHT);
            bubbleContainer.setAlignment(Pos.CENTER_RIGHT);
            bubble.setStyle("-fx-background-color: linear-gradient(to bottom right, #00FFCC, #00cca3); " +
                    "-fx-text-fill: #1a1a1a; -fx-padding: 12 18; -fx-background-radius: 20 20 5 20; " +
                    "-fx-font-size: 14px; -fx-font-weight: bold;");
        } else if (messageContent.startsWith("[Système]")) {
            row.setAlignment(Pos.CENTER);
            bubbleContainer.setAlignment(Pos.CENTER);
            bubble.setStyle("-fx-text-fill: #679AC1; -fx-font-style: italic; -fx-font-size: 12px;");
            timeLabel.setVisible(false);
        } else {
            row.setAlignment(Pos.CENTER_LEFT);
            bubbleContainer.setAlignment(Pos.CENTER_LEFT);
            bubble.setStyle("-fx-background-color: #3d3d3d; -fx-text-fill: #ecf0f1; " +
                    "-fx-padding: 12 18; -fx-background-radius: 20 20 20 5; -fx-font-size: 14px;");
        }

        bubbleContainer.getChildren().addAll(bubble, timeLabel);
        row.getChildren().add(bubbleContainer);
        VBox.setMargin(row, new javafx.geometry.Insets(0, 0, 10, 0));
        chatMessageContainer.getChildren().add(row);
    }

    private void connectToChatServer() {
        try {
            webSocketClient = new WebSocketClient(new URI("ws://localhost:8887")) {
                @Override
                public void onOpen(ServerHandshake h) {
                    Platform.runLater(() -> addMessageToUI("[Système] Connecté au support."));
                }

                @Override
                public void onMessage(String message) {
                    Platform.runLater(() -> {
                        if (message.equals("[EFFACER_TOUT]")) {
                            chatMessageContainer.getChildren().clear();
                            addMessageToUI("[Système] Historique réinitialisé.");
                        } else {
                            addMessageToUI(message);
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
            // 1. Capture the time right now
            String time = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));

            // 2. Format the message with the timestamp
            String fullMsgWithTime = "Client: " + msg + "|" + time;

            // 3. Send and Save
            webSocketClient.send(fullMsgWithTime);
            saveMessageToFile(fullMsgWithTime);

            chatInput.clear();
        }
    }
    private void saveMessageToFile(String m) {
        try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(HISTORY_FILE, true)))) {
            out.println(m);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void loadChatHistory() {
        File file = new File(HISTORY_FILE);
        if (file.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = br.readLine()) != null) {
                    final String msg = line;
                    addMessageToUI(msg);
                }
            } catch (IOException e) { e.printStackTrace(); }
        }
    }

    private void addDeleteButtonToUI() {
        Button btnDelete = new Button("🗑 Supprimer l'historique");
        btnDelete.getStyleClass().add("delete-button");
        btnDelete.setMaxWidth(Double.MAX_VALUE);
        btnDelete.setOnAction(e -> handleClearHistory());
        if (clientChatView != null) clientChatView.getChildren().add(btnDelete);
    }

    @FXML
    private void handleClearHistory() {
        File file = new File(HISTORY_FILE);
        if (file.exists()) file.delete();
        chatMessageContainer.getChildren().clear();
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