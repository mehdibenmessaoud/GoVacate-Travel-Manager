package tn.esprit.projet.gui;

import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("EEEE, d MMM");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTable();
        setupFilters();

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

    // ==========================================
    // MODERN TABLE DESIGN LOGIC
    // ==========================================

    private void setupTable() {
        colUserId.setCellValueFactory(new PropertyValueFactory<>("commentaire_client"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type_res"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("date_debut"));
        colPrix.setCellValueFactory(new PropertyValueFactory<>("prix_total"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));

        // 1. Price Column Formatting
        colPrix.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) setText(null);
                else {
                    setText(String.format("%.2f €", price));
                    setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold; -fx-alignment: CENTER-RIGHT;");
                }
            }
        });

        // 2. Status Badges
        colStatut.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    String val = item.toString().toUpperCase();
                    Label badge = new Label(val);
                    badge.setPrefWidth(100);
                    badge.setAlignment(Pos.CENTER);
                    String baseStyle = "-fx-padding: 5 10; -fx-background-radius: 15; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold;";

                    if (val.contains("CONFIRMEE")) badge.setStyle(baseStyle + "-fx-background-color: #27ae60;");
                    else if (val.contains("ATTENTE")) badge.setStyle(baseStyle + "-fx-background-color: #f39c12;");
                    else badge.setStyle(baseStyle + "-fx-background-color: #e74c3c;");

                    setGraphic(badge);
                    setAlignment(Pos.CENTER);
                }
            }
        });

        // 3. Modern Action Buttons
        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button btn = new Button("Supprimer");
            {
                btn.setPrefWidth(90);
                String normalStyle = "-fx-background-color: #34495e; -fx-text-fill: white; -fx-background-radius: 20; -fx-cursor: hand; -fx-font-weight: bold;";
                String hoverStyle = "-fx-background-color: #FF4B5C; -fx-text-fill: white; -fx-background-radius: 20; -fx-cursor: hand; -fx-font-weight: bold;";
                btn.setStyle(normalStyle);
                btn.setOnMouseEntered(e -> btn.setStyle(hoverStyle));
                btn.setOnMouseExited(e -> btn.setStyle(normalStyle));
                btn.setOnAction(event -> handleDelete(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setGraphic(null);
                else { setGraphic(btn); setAlignment(Pos.CENTER); }
            }
        });
    }

    // ==========================================
    // CHAT & DATE LOGIC
    // ==========================================

    private void startChatServer() {
        chatServer = new ChatServer(8887) {
            @Override
            public void onOpen(WebSocket conn, org.java_websocket.handshake.ClientHandshake handshake) {
                Platform.runLater(() -> addMessageToUI("[Système] Un voyageur a rejoint la session."));
            }
            @Override
            public void onMessage(WebSocket conn, String message) {
                Platform.runLater(() -> addMessageToUI(message));
                super.onMessage(conn, message);
            }
            @Override public void onClose(WebSocket conn, int code, String reason, boolean rem) {
                Platform.runLater(() -> addMessageToUI("[Système] Voyageur déconnecté."));
            }
            @Override public void onError(WebSocket conn, Exception ex) {}
        };
        chatServer.start();
        Platform.runLater(() -> addMessageToUI("[Système] Serveur de support démarré."));
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
            String displayTime = LocalDateTime.now().format(timeFormatter);
            String displayDate = LocalDateTime.now().format(dateFormatter);

            if (message.contains("|")) {
                String[] parts = message.split("\\|");
                content = parts[0];
                if (parts.length > 1) displayTime = parts[1];
                if (parts.length > 2) displayDate = parts[2];
            }

            checkAndAddDateSeparator(displayDate);

            HBox row = new HBox();
            VBox bubbleContainer = new VBox(2);
            boolean isSystem = content.contains("[Système]");
            String cleanMsg = isSystem ? content : content.replace("Client: ", "").replace("Admin: ", "").replace("ADMIN: ", "");

            Label bubble = new Label(cleanMsg);
            bubble.setWrapText(true);
            bubble.setMaxWidth(400);

            if (isSystem) {
                row.setAlignment(Pos.CENTER);
                bubble.setStyle("-fx-text-fill: #679AC1; -fx-font-style: italic; -fx-font-size: 12px;");
            } else if (content.startsWith("Admin:") || content.startsWith("ADMIN:")) {
                row.setAlignment(Pos.CENTER_RIGHT);
                bubble.setStyle("-fx-background-color: linear-gradient(to bottom right, #FF8210, #e67e22); -fx-text-fill: white; -fx-padding: 10 15; -fx-background-radius: 15 15 2 15; -fx-font-weight: bold;");
            } else {
                row.setAlignment(Pos.CENTER_LEFT);
                bubble.setStyle("-fx-background-color: #3d3d3d; -fx-text-fill: #ecf0f1; -fx-padding: 10 15; -fx-background-radius: 15 15 15 2;");
            }

            bubbleContainer.getChildren().add(bubble);
            if (!isSystem) {
                Label timeL = new Label(displayTime);
                timeL.setStyle("-fx-font-size: 9px; -fx-text-fill: #bdc3c7;");
                bubbleContainer.setAlignment(content.startsWith("Admin") ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
                bubbleContainer.getChildren().add(timeL);
            }
            row.getChildren().add(bubbleContainer);
            VBox.setMargin(row, new javafx.geometry.Insets(0, 0, 8, 0));
            adminChatMessageContainer.getChildren().add(row);
        });
    }

    private void checkAndAddDateSeparator(String dateStr) {
        boolean dateExists = false;
        for (Node node : adminChatMessageContainer.getChildren()) {
            if (node instanceof HBox) {
                HBox hb = (HBox) node;
                if (!hb.getChildren().isEmpty() && hb.getChildren().get(0) instanceof Label) {
                    if (dateStr.equals(((Label) hb.getChildren().get(0)).getText())) {
                        dateExists = true; break;
                    }
                }
            }
        }
        if (!dateExists) {
            Label dl = new Label(dateStr);
            dl.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-text-fill: #95a5a6; -fx-padding: 3 12; -fx-background-radius: 10; -fx-font-size: 10px;");
            HBox dr = new HBox(dl); dr.setAlignment(Pos.CENTER);
            VBox.setMargin(dr, new javafx.geometry.Insets(15, 0, 10, 0));
            adminChatMessageContainer.getChildren().add(dr);
        }
    }

    @FXML
    private void handleAdminReply() {
        String msg = adminChatInput.getText().trim();
        if (!msg.isEmpty()) {
            LocalDateTime n = LocalDateTime.now();
            String fullMsg = "Admin: " + msg + "|" + n.format(timeFormatter) + "|" + n.format(dateFormatter);
            chatServer.broadcast(fullMsg);
            addMessageToUI(fullMsg);
            saveMessageToFile(fullMsg);
            adminChatInput.clear();
        }
    }

    // ==========================================
    // FILTER & NAVIGATION LOGIC
    // ==========================================

    @FXML public void showReservations() {
        hideAllViews(); reservationView.setVisible(true); moveBubble(btnVoyages);
        ObservableList<Reservation> data = FXCollections.observableArrayList(reservationService.getAllReservations());
        FilteredList<Reservation> filteredData = new FilteredList<>(data, b -> true);
        if (txtSearch != null) {
            txtSearch.textProperty().addListener((o, old, nv) -> applyFilter(filteredData));
            comboStatut.valueProperty().addListener((o, old, nv) -> applyFilter(filteredData));
        }
        tableMesReservations.setItems(new SortedList<>(filteredData));
    }

    private void applyFilter(FilteredList<Reservation> fd) {
        fd.setPredicate(res -> {
            String t = (txtSearch == null) ? "" : txtSearch.getText().toLowerCase().trim();
            String s = (comboStatut == null) ? "Tous" : comboStatut.getValue();
            boolean mt = t.isEmpty() || (res.getType_res() != null && res.getType_res().toLowerCase().contains(t));
            boolean ms = s.equals("Tous") || (res.getStatut() != null && res.getStatut().toString().equals(s));
            return mt && ms;
        });
    }

    private void setupFilters() {
        if (comboStatut != null) {
            comboStatut.setItems(FXCollections.observableArrayList("Tous", "EN_ATTENTE", "CONFIRMEE", "ANNULEE"));
            comboStatut.setValue("Tous");
        }
    }

    private void handleDelete(Reservation res) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer la réservation #" + res.getId() + " ?", ButtonType.YES, ButtonType.NO);
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                reservationService.delete(res.getId());
                showReservations();
            }
        });
    }

    @FXML private void handleClearHistory() {
        new File(HISTORY_FILE).delete();
        adminChatMessageContainer.getChildren().clear();
        if (chatServer != null) chatServer.broadcast("[EFFACER_TOUT]");
        addMessageToUI("[Système] Historique réinitialisé.");
    }

    @FXML public void showSupport() { hideAllViews(); supportView.setVisible(true); moveBubble(btnMessages); }
    @FXML private void showExplorer() { hideAllViews(); explorerView.setVisible(true); moveBubble(btnExplorer); }
    private void hideAllViews() { explorerView.setVisible(false); reservationView.setVisible(false); supportView.setVisible(false); }

    private void moveBubble(Button target) {
        if (currentActiveBtn == target || target == null) return;
        TranslateTransition tt = new TranslateTransition(Duration.millis(300), slidingPane);
        tt.setToY(target.getLayoutY()); tt.play();
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
            } catch (IOException e) { e.printStackTrace(); }
        }
    }
}