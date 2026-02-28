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
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import tn.esprit.projet.entities.Reservation;
import tn.esprit.projet.services.ReservationServiceImpl;
import tn.esprit.projet.utils.MyDBConnexion;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class GoVacateControllor implements Initializable {

    @FXML private Pane slidingPane;
    @FXML private Button btnExplorer, btnVoyages, btnMessages;
    @FXML private ScrollPane explorerView, adminChatScrollPane;
    @FXML private VBox reservationView, supportView, adminChatMessageContainer, inboxClientList;
    @FXML private Label chatHeaderLabel;

    // Table & Columns
    @FXML private TableView<Reservation> tableMesReservations;
    @FXML private TableColumn<Reservation, String> colUserId, colType, colDate;
    @FXML private TableColumn<Reservation, Object> colStatut;
    @FXML private TableColumn<Reservation, Double> colPrix;
    @FXML private TableColumn<Reservation, Void> colAction;

    // Filters & Chat Input
    @FXML private TextField txtSearch, adminChatInput;
    @FXML private ComboBox<String> comboStatut;

    private Button currentActiveBtn = null;
    private final ReservationServiceImpl reservationService = new ReservationServiceImpl();
    private WebSocketClient webSocketClient;
    private String lastActiveClientId = null;
    private String lastProcessedMessage = "";

    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("EEEE, d MMM");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTable();
        setupFilters();
        connectToChatServer();

        if (adminChatMessageContainer != null) {
            adminChatMessageContainer.heightProperty().addListener((obs, old, newVal) ->
                    adminChatScrollPane.setVvalue(1.0));
        }

        slidingPane.setMouseTransparent(true);
        Platform.runLater(() -> {
            showExplorer(); // Vue par défaut au démarrage
        });
    }

    // ==========================================
    // 1. TABLE DESIGN & LOGIC
    // ==========================================
    private void setupTable() {
        if (tableMesReservations == null) return;

        colUserId.setCellValueFactory(new PropertyValueFactory<>("commentaire_client"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type_res"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("date_debut"));
        colPrix.setCellValueFactory(new PropertyValueFactory<>("prix_total"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));

        colPrix.setCellFactory(column -> new TableCell<>() {
            @Override protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) setText(null);
                else { setText(String.format("%.2f €", price)); setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold;"); }
            }
        });

        colStatut.setCellFactory(column -> new TableCell<>() {
            @Override protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setGraphic(null);
                else {
                    String val = item.toString().toUpperCase();
                    Label badge = new Label(val); badge.setPrefWidth(100); badge.setAlignment(Pos.CENTER);
                    String style = "-fx-padding: 5 10; -fx-background-radius: 15; -fx-text-fill: white; -fx-font-weight: bold;";
                    if (val.contains("CONFIRMEE")) badge.setStyle(style + "-fx-background-color: #27ae60;");
                    else if (val.contains("ATTENTE")) badge.setStyle(style + "-fx-background-color: #f39c12;");
                    else badge.setStyle(style + "-fx-background-color: #e74c3c;");
                    setGraphic(badge); setAlignment(Pos.CENTER);
                }
            }
        });

        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button btn = new Button("Supprimer");
            {
                btn.setStyle("-fx-background-color: #34495e; -fx-text-fill: white; -fx-background-radius: 20; -fx-cursor: hand;");
                btn.setOnAction(event -> handleDelete(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setGraphic(null); else setGraphic(btn);
            }
        });
    }

    // ==========================================
    // 2. NAVIGATION & RESERVATIONS
    // ==========================================
    @FXML
    public void showReservations() {
        // 1. Navigation & Visibility (Kima el l-9dim)
        hideAllViews();
        reservationView.setVisible(true);
        reservationView.setManaged(true);
        moveBubble(btnVoyages);

        // 2. Fetch Data (Ista3mel getAllReservations kima el l-9dim!)
        // Thabbet elli getAllReservations() maktouba s7i7 fi ReservationServiceImpl
        ObservableList<Reservation> data = FXCollections.observableArrayList(reservationService.getAllReservations());

        // 3. Filtering Logic (Simplified)
        FilteredList<Reservation> filteredData = new FilteredList<>(data, b -> true);

        // Add Listeners ONLY if they exist
        if (txtSearch != null) {
            txtSearch.textProperty().addListener((o, old, nv) -> applyFilter(filteredData));
        }
        if (comboStatut != null) {
            comboStatut.valueProperty().addListener((o, old, nv) -> applyFilter(filteredData));
        }

        // 4. Sorting & Display
        SortedList<Reservation> sortedData = new SortedList<>(filteredData);
        if (tableMesReservations != null) {
            sortedData.comparatorProperty().bind(tableMesReservations.comparatorProperty());
            tableMesReservations.setItems(sortedData);
            tableMesReservations.refresh();
        }
    }

    private void applyFilter(FilteredList<Reservation> fd) {
        fd.setPredicate(res -> {
            String t = (txtSearch == null) ? "" : txtSearch.getText().toLowerCase().trim();
            String s = (comboStatut == null) ? "Tous" : comboStatut.getValue();
            boolean mt = t.isEmpty() || res.getType_res().toLowerCase().contains(t) || String.valueOf(res.getCommentaire_client()).contains(t);
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

    // ==========================================
    // 3. CHAT & INBOX LOGIC
    // ==========================================
    private void connectToChatServer() {
        try {
            webSocketClient = new WebSocketClient(new URI("ws://localhost:8887")) {
                @Override public void onOpen(ServerHandshake h) { System.out.println("✅ Admin Connected to WebSocket"); }
                @Override public void onMessage(String m) {
                    Platform.runLater(() -> {
                        if (m.equals(lastProcessedMessage)) return;
                        lastProcessedMessage = m;
                        String[] p = m.split("\\|");
                        refreshInbox();
                        if (p.length >= 4 && p[0].equals(lastActiveClientId)) addMessageToUI(m);
                    });
                }
                @Override public void onClose(int c, String r, boolean rem) {}
                @Override public void onError(Exception ex) { System.err.println("❌ WS Error: " + ex.getMessage()); }
            };
            webSocketClient.connect();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void refreshInbox() {
        if (inboxClientList == null) return;
        inboxClientList.getChildren().clear();
        String query = "SELECT id, nom FROM user WHERE role_id = 2 AND status = 'actif'";
        try (Connection conn = MyDBConnexion.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                String id = String.valueOf(rs.getInt("id"));
                String nom = rs.getString("nom");
                Button btn = new Button(nom + " (#" + id + ")");
                btn.setMaxWidth(Double.MAX_VALUE);
                btn.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-text-fill: white; -fx-alignment: CENTER_LEFT; -fx-padding: 12;");
                btn.setOnAction(e -> loadSpecificChat(id));
                inboxClientList.getChildren().add(btn);
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void loadSpecificChat(String clientId) {
        this.lastActiveClientId = clientId;
        if (chatHeaderLabel != null) chatHeaderLabel.setText("Discussion avec : " + clientId);
        adminChatMessageContainer.getChildren().clear();
        File f = new File("chat_history_user_" + clientId + ".txt");
        if (f.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                String l; while ((l = br.readLine()) != null) addMessageToUI(l);
            } catch (IOException e) { e.printStackTrace(); }
        }
    }

    @FXML private void handleAdminReply() {
        String msg = adminChatInput.getText().trim();
        if (lastActiveClientId == null || msg.isEmpty()) return;
        String time = LocalDateTime.now().format(timeFormatter);
        String date = LocalDateTime.now().format(dateFormatter);
        String payload = lastActiveClientId + "|ADMIN|Admin|" + msg + "|" + time + "|" + date;
        if (webSocketClient != null && webSocketClient.isOpen()) {
            webSocketClient.send(payload);
            adminChatInput.clear();
        }
    }

    @FXML private void handleClearHistory() {
        if (lastActiveClientId != null) {
            File f = new File("chat_history_user_" + lastActiveClientId + ".txt");
            if (f.exists()) f.delete();
            adminChatMessageContainer.getChildren().clear();
        }
    }

    private void addMessageToUI(String message) {
        Platform.runLater(() -> {
            String[] parts = message.split("\\|");
            if (parts.length < 6) return;
            String role = parts[1], name = parts[2], content = parts[3], time = parts[4];

            HBox row = new HBox();
            VBox v = new VBox(2);
            Label b = new Label(content);
            b.setWrapText(true); b.setMaxWidth(400);
            Label info = new Label(name + " • " + time);
            info.setStyle("-fx-font-size: 9px; -fx-text-fill: #bdc3c7;");

            if ("ADMIN".equalsIgnoreCase(role)) {
                row.setAlignment(Pos.CENTER_RIGHT); v.setAlignment(Pos.CENTER_RIGHT);
                b.setStyle("-fx-background-color: #FF8210; -fx-text-fill: white; -fx-padding: 10 15; -fx-background-radius: 15 15 2 15;");
            } else {
                row.setAlignment(Pos.CENTER_LEFT); v.setAlignment(Pos.CENTER_LEFT);
                b.setStyle("-fx-background-color: #3d3d3d; -fx-text-fill: #ecf0f1; -fx-padding: 10 15; -fx-background-radius: 15 15 15 2;");
            }
            v.getChildren().addAll(b, info); row.getChildren().add(v);
            adminChatMessageContainer.getChildren().add(row);
        });
    }

    // ==========================================
    // UTILS & NAVIGATION
    // ==========================================
    @FXML
    public void showSupport() {
        hideAllViews();
        supportView.setVisible(true);
        supportView.setManaged(true); // 🎯 Zid hadhi
        moveBubble(btnMessages);
        refreshInbox();
    }

    @FXML
    public void showExplorer() {
        hideAllViews();
        explorerView.setVisible(true);
        explorerView.setManaged(true); // 🎯 Zid hadhi
        moveBubble(btnExplorer);
    }
    private void hideAllViews() {
        if (explorerView != null) { explorerView.setVisible(false); explorerView.setManaged(false); }
        if (reservationView != null) { reservationView.setVisible(false); reservationView.setManaged(false); }
        if (supportView != null) { supportView.setVisible(false); supportView.setManaged(false); }
    }
    private void handleDelete(Reservation res) {
        reservationService.delete(res.getId());
        showReservations();
    }
    private void moveBubble(Button b) {
        if (b == null || slidingPane == null) return;
        TranslateTransition tt = new TranslateTransition(Duration.millis(300), slidingPane);
        tt.setToY(b.getLayoutY()); tt.play(); currentActiveBtn = b;
    }
}