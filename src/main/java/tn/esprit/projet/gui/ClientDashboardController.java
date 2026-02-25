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
import javafx.stage.Stage;
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
    private static final String HISTORY_FILE = "chat_history.txt";

    // Formatting patterns
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("EEEE, d MMM");

    @FXML
    public void initialize() {
        showExplorer();
        chatMessageContainer.heightProperty().addListener((obs, oldVal, newVal) ->
                chatScrollPane.setVvalue(1.0));

        loadChatHistory();
        connectToChatServer();
        addDeleteButtonToUI();
    }

    // --- MODERN UI MESSAGE BUBBLES WITH DATE & TIME ---

    private void addMessageToUI(String message) {
        if (message.contains("[EFFACER_TOUT]")) {
            chatMessageContainer.getChildren().clear();
            return;
        }

        String messageContent = message;
        String displayTime = LocalDateTime.now().format(timeFormatter);
        String displayDate = LocalDateTime.now().format(dateFormatter);

        // Parse saved format: "Sender: Content|HH:mm|Date"
        if (message.contains("|")) {
            String[] parts = message.split("\\|");
            messageContent = parts[0];
            if (parts.length > 1) displayTime = parts[1];
            if (parts.length > 2) displayDate = parts[2];
        }

        // --- Logic for Date Separator ---
        // We only show the Date Label if it's different from the last message's date
        checkAndAddDateSeparator(displayDate);

        HBox row = new HBox();
        VBox bubbleContainer = new VBox(2);

        String cleanMsg = messageContent.replace("Client: ", "").replace("Admin: ", "").replace("ADMIN: ", "");
        Label bubble = new Label(cleanMsg);
        bubble.setWrapText(true);
        bubble.setMaxWidth(400);

        // Time label now shows the time captured
        Label timeLabel = new Label(displayTime);
        timeLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: #bdc3c7;");

        if (messageContent.startsWith("Client:")) {
            row.setAlignment(Pos.CENTER_RIGHT);
            bubbleContainer.setAlignment(Pos.CENTER_RIGHT);
            bubble.setStyle("-fx-background-color: linear-gradient(to bottom right, #00FFCC, #00cca3); " +
                    "-fx-text-fill: #1a1a1a; -fx-padding: 10 15; -fx-background-radius: 15 15 2 15; " +
                    "-fx-font-size: 13px; -fx-font-weight: bold;");
        } else if (messageContent.startsWith("[Système]")) {
            row.setAlignment(Pos.CENTER);
            bubbleContainer.setAlignment(Pos.CENTER);
            bubble.setStyle("-fx-text-fill: #679AC1; -fx-font-style: italic; -fx-font-size: 11px;");
            timeLabel.setVisible(false);
        } else {
            row.setAlignment(Pos.CENTER_LEFT);
            bubbleContainer.setAlignment(Pos.CENTER_LEFT);
            bubble.setStyle("-fx-background-color: #3d3d3d; -fx-text-fill: #ecf0f1; " +
                    "-fx-padding: 10 15; -fx-background-radius: 15 15 15 2; -fx-font-size: 13px;");
        }

        bubbleContainer.getChildren().addAll(bubble, timeLabel);
        row.getChildren().add(bubbleContainer);
        VBox.setMargin(row, new javafx.geometry.Insets(0, 0, 8, 0));
        chatMessageContainer.getChildren().add(row);
    }

    private void checkAndAddDateSeparator(String dateStr) {
        // Simple logic: If the last node isn't this date, add a label
        boolean needsDate = true;
        for (Node node : chatMessageContainer.getChildren()) {
            if (node instanceof Label && ((Label) node).getText().equals(dateStr)) {
                needsDate = false;
                break;
            }
        }

        // This is a simplified check. In a real app, you'd check the *last* separator added.
        // For now, we will just add a small centered date label if the container is empty
        // or if the date isn't already there as a header.
        if (needsDate) {
            Label dateSeparator = new Label(dateStr);
            dateSeparator.setStyle("-fx-background-color: #2c3e50; -fx-text-fill: #95a5a6; " +
                    "-fx-padding: 3 10; -fx-background-radius: 10; -fx-font-size: 10px;");
            HBox dateRow = new HBox(dateSeparator);
            dateRow.setAlignment(Pos.CENTER);
            VBox.setMargin(dateRow, new javafx.geometry.Insets(10, 0, 10, 0));
            chatMessageContainer.getChildren().add(dateRow);
        }
    }

    @FXML
    private void handleSendMessage() {
        String msg = chatInput.getText().trim();
        if (webSocketClient != null && webSocketClient.isOpen() && !msg.isEmpty()) {
            // Capture Current Date and Time
            LocalDateTime now = LocalDateTime.now();
            String time = now.format(timeFormatter);
            String date = now.format(dateFormatter);

            // Format: "Client: Hello|14:30|Wednesday, 25 Feb"
            String fullMsgWithDateTime = "Client: " + msg + "|" + time + "|" + date;

            webSocketClient.send(fullMsgWithDateTime);
            saveMessageToFile(fullMsgWithDateTime);

            chatInput.clear();
        }
    }

    // --- REST OF METHODS REMAIN UNCHANGED ---

    private void saveMessageToFile(String m) {
        try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(HISTORY_FILE, true)))) {
            out.println(m);
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void loadChatHistory() {
        File file = new File(HISTORY_FILE);
        if (file.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = br.readLine()) != null) {
                    addMessageToUI(line);
                }
            } catch (IOException e) { e.printStackTrace(); }
        }
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

    private void addDeleteButtonToUI() {
        Button btnDelete = new Button("🗑 Supprimer l'historique");
        btnDelete.setStyle("-fx-background-color: transparent; -fx-text-fill: #FF4B5C; -fx-cursor: hand; -fx-font-size: 11px;");
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