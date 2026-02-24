package tn.esprit.projet.gui;

import javafx.animation.Interpolator;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.util.Duration;
import org.java_websocket.WebSocket;
import tn.esprit.projet.entities.Reservation;
import tn.esprit.projet.services.ReservationServiceImpl;
import tn.esprit.projet.utils.ChatServer;

import java.io.*;
import java.net.URL;
import java.util.ResourceBundle;

public class GoVacateControllor implements Initializable {

    @FXML private Pane slidingPane;
    @FXML private Button btnExplorer, btnVoyages, btnMessages;
    @FXML private ScrollPane explorerView, adminChatScrollPane;
    @FXML private VBox reservationView, supportView, adminChatMessageContainer;
    @FXML private TableView<Reservation> tableMesReservations;
    @FXML private TableColumn<Reservation, String> colUserId, colType, colDate;
    @FXML private TableColumn<Reservation, Object> colStatut;
    @FXML private TableColumn<Reservation, Double> colPrix;
    @FXML private TableColumn<Reservation, Void> colAction;
    @FXML private TextField txtSearch, adminChatInput;
    @FXML private ComboBox<String> comboStatut;

    private Button currentActiveBtn = null;
    private final ReservationServiceImpl reservationService = new ReservationServiceImpl();
    private ChatServer chatServer;
    private static final String HISTORY_FILE = "chat_history.txt";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTable();
        setupFilters();

        // Auto-scroll logic
        if (adminChatMessageContainer != null) {
            adminChatMessageContainer.heightProperty().addListener((obs, old, newVal) ->
                    adminChatScrollPane.setVvalue(1.0));
        }

        startChatServer();
        slidingPane.setMouseTransparent(true);

        Platform.runLater(() -> {
            if (btnExplorer != null) {
                slidingPane.setTranslateY(btnExplorer.getLayoutY());
                currentActiveBtn = btnExplorer;
            }
        });
    }

    private void startChatServer() {
        chatServer = new ChatServer(8887) {
            @Override
            public void onOpen(WebSocket conn, org.java_websocket.handshake.ClientHandshake handshake) {
                // This triggers ONLY when a client actually joins
                Platform.runLater(() -> addMessageToUI("[Système] Un voyageur a rejoint la session."));
            }

            @Override
            public void onMessage(WebSocket conn, String message) {
                Platform.runLater(() -> addMessageToUI(message));
                super.onMessage(conn, message);
            }

            @Override public void onClose(WebSocket conn, int code, String reason, boolean remote) {
                Platform.runLater(() -> addMessageToUI("[Système] Voyageur déconnecté."));
            }
            @Override public void onError(WebSocket conn, Exception ex) {}
        };

        chatServer.start();

        // --- THIS IS THE FIX ---
        // This forces the message to appear immediately for the Admin
        Platform.runLater(() -> addMessageToUI("[Système] Connecté au support."));

        loadChatHistory();
    }
    private void addMessageToUI(String message) {
        if (message == null || message.isEmpty()) return;

        Platform.runLater(() -> {
            if (message.contains("[EFFACER_TOUT]")) {
                adminChatMessageContainer.getChildren().clear();
                return;
            }

            String content = message;
            String time = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));

            if (message.contains("|")) {
                String[] parts = message.split("\\|");
                content = parts[0];
                time = parts[1];
            }

            HBox row = new HBox();
            VBox bubbleContainer = new VBox(2);

            boolean isSystem = content.contains("[Système]") || content.contains("[Systeme]");
            String cleanMsg = isSystem ? content : content.replace("Client: ", "").replace("Admin: ", "").replace("ADMIN: ", "");

            Label bubble = new Label(cleanMsg);
            bubble.setWrapText(true);
            bubble.setMaxWidth(400);

            // Styling logic
            if (isSystem) {
                row.setAlignment(Pos.CENTER);
                bubbleContainer.setAlignment(Pos.CENTER);
                bubble.setStyle("-fx-text-fill: #679AC1; -fx-font-style: italic; -fx-font-size: 13px; -fx-background-color: rgba(255,255,255,0.05); -fx-padding: 5 15; -fx-background-radius: 10;");
            } else if (content.startsWith("Admin:") || content.startsWith("ADMIN:")) {
                row.setAlignment(Pos.CENTER_RIGHT);
                bubbleContainer.setAlignment(Pos.CENTER_RIGHT);
                bubble.setStyle("-fx-background-color: linear-gradient(to bottom right, #FF8210, #e67e22); -fx-text-fill: white; -fx-padding: 10 15; -fx-background-radius: 15 15 2 15; -fx-font-weight: bold;");
            } else {
                row.setAlignment(Pos.CENTER_LEFT);
                bubbleContainer.setAlignment(Pos.CENTER_LEFT);
                bubble.setStyle("-fx-background-color: #3d3d3d; -fx-text-fill: #ecf0f1; -fx-padding: 10 15; -fx-background-radius: 15 15 15 2;");
            }

            bubbleContainer.getChildren().add(bubble);
            if (!isSystem) {
                Label timeLabel = new Label(time);
                timeLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: #bdc3c7;");
                bubbleContainer.getChildren().add(timeLabel);
            }

            row.getChildren().add(bubbleContainer);
            adminChatMessageContainer.getChildren().add(row);
        });
    }

    @FXML
    private void handleAdminReply() {
        String msg = adminChatInput.getText().trim();
        if (!msg.isEmpty()) {
            String time = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
            String fullMsg = "Admin: " + msg + "|" + time;
            chatServer.broadcast(fullMsg);
            addMessageToUI(fullMsg);
            saveMessageToFile(fullMsg);
            adminChatInput.clear();
        }
    }

    @FXML
    private void handleClearHistory() {
        // 1. Delete the physical file
        new File(HISTORY_FILE).delete();

        // 2. Clear the UI container
        adminChatMessageContainer.getChildren().clear();

        // 3. BROADCAST to the client so their screen clears too
        if (chatServer != null) {
            chatServer.broadcast("[EFFACER_TOUT]");
        }

        // 4. THE FIX: Show the system message on the Admin screen immediately
        addMessageToUI("[Système] Historique réinitialisé.");
    }
    private void setupTable() {
        // FIXED: PropertyValueFactory instead of PropertyFactory
        colUserId.setCellValueFactory(new PropertyValueFactory<>("commentaire_client"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type_res"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("date_debut"));
        colPrix.setCellValueFactory(new PropertyValueFactory<>("prix_total"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));

        colStatut.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); }
                else {
                    String val = item.toString();
                    setText(val);
                    setStyle("-fx-font-weight: bold; -fx-alignment: CENTER; " +
                            (val.equalsIgnoreCase("CONFIRMEE") ? "-fx-text-fill: #2ecc71;" :
                                    val.equalsIgnoreCase("EN_ATTENTE") ? "-fx-text-fill: #f1c40f;" : "-fx-text-fill: #e74c3c;"));
                }
            }
        });

        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button btn = new Button("Supprimer");
            {
                btn.setStyle("-fx-background-color: #FF8210; -fx-text-fill: white; -fx-background-radius: 15; -fx-cursor: hand;");
                btn.setOnAction(event -> handleDelete(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
                if (!empty) setAlignment(Pos.CENTER);
            }
        });
    }

    private void setupFilters() {
        if (comboStatut != null) {
            comboStatut.setItems(FXCollections.observableArrayList("Tous", "EN_ATTENTE", "CONFIRMEE", "ANNULEE"));
            comboStatut.setValue("Tous");
        }
    }

    @FXML public void showReservations() {
        hideAllViews(); reservationView.setVisible(true); moveBubble(btnVoyages);
        ObservableList<Reservation> data = FXCollections.observableArrayList(reservationService.getAllReservations());
        FilteredList<Reservation> filteredData = new FilteredList<>(data, b -> true);
        if (txtSearch != null) {
            txtSearch.textProperty().addListener((obs, old, nv) -> applyFilter(filteredData));
            comboStatut.valueProperty().addListener((obs, old, nv) -> applyFilter(filteredData));
        }
        tableMesReservations.setItems(new SortedList<>(filteredData));
    }

    private void applyFilter(FilteredList<Reservation> filteredData) {
        filteredData.setPredicate(res -> {
            String text = (txtSearch == null) ? "" : txtSearch.getText().toLowerCase().trim();
            String status = (comboStatut == null) ? "Tous" : comboStatut.getValue();
            boolean matchesText = text.isEmpty() || (res.getType_res() != null && res.getType_res().toLowerCase().contains(text));
            boolean matchesStatus = status.equals("Tous") || (res.getStatut() != null && res.getStatut().toString().equals(status));
            return matchesText && matchesStatus;
        });
    }

    private void handleDelete(Reservation res) { reservationService.delete(res.getId()); showReservations(); }
    @FXML public void showSupport() { hideAllViews(); supportView.setVisible(true); moveBubble(btnMessages); }
    @FXML private void showExplorer() { hideAllViews(); explorerView.setVisible(true); moveBubble(btnExplorer); }
    private void hideAllViews() { explorerView.setVisible(false); reservationView.setVisible(false); supportView.setVisible(false); }

    private void moveBubble(Button target) {
        if (currentActiveBtn == target || target == null) return;
        TranslateTransition tt = new TranslateTransition(Duration.millis(300), slidingPane);
        tt.setToY(target.getLayoutY());
        tt.play();
        currentActiveBtn = target;
    }

    private void saveMessageToFile(String m) {
        try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(HISTORY_FILE, true)))) { out.println(m); }
        catch (IOException e) { e.printStackTrace(); }
    }

    private void loadChatHistory() {
        File f = new File(HISTORY_FILE);
        if (f.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                String l; while ((l = br.readLine()) != null) addMessageToUI(l);
            } catch (IOException e) {}
        }
    }
}