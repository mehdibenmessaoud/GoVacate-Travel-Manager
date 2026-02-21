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
import java.util.Optional;
import java.util.ResourceBundle;

public class GoVacateControllor implements Initializable {

    // --- ÉLÉMENTS FXML ---
    @FXML private Pane slidingPane;
    @FXML private Button btnExplorer, btnVoyages, btnMessages;
    @FXML private ScrollPane explorerView;
    @FXML private VBox reservationView, supportView;
    @FXML private TableView<Reservation> tableMesReservations;
    @FXML private TableColumn<Reservation, String> colUserId, colType, colDate;
    @FXML private TableColumn<Reservation, Object> colStatut; // Changé en Object pour éviter le crash Enum
    @FXML private TableColumn<Reservation, Double> colPrix;
    @FXML private TableColumn<Reservation, Void> colAction;
    @FXML private TextField txtSearch, adminChatInput;
    @FXML private ComboBox<String> comboStatut;
    @FXML private TextArea adminChatDisplay;

    // --- LOGIQUE ---
    private Button currentActiveBtn = null;
    private final ReservationServiceImpl reservationService = new ReservationServiceImpl();
    private ChatServer chatServer;
    private static final String HISTORY_FILE = "chat_history.txt";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTable();
        setupFilters();
        startChatServer();
        slidingPane.setMouseTransparent(true);

        Platform.runLater(() -> {
            if (btnExplorer != null) {
                slidingPane.setTranslateY(btnExplorer.getLayoutY());
                currentActiveBtn = btnExplorer;
            }
        });
    }

    private void setupFilters() {
        if (comboStatut != null) {
            comboStatut.setItems(FXCollections.observableArrayList("Tous", "EN_ATTENTE", "CONFIRMEE", "ANNULEE"));
            comboStatut.setValue("Tous");
        }
    }

    private void setupTable() {
        colUserId.setCellValueFactory(new PropertyValueFactory<>("commentaire_client"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type_res"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("date_debut"));
        colPrix.setCellValueFactory(new PropertyValueFactory<>("prix_total"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));

        // --- DESIGN COLONNE STATUT (FIX CLASSCASTEXCEPTION) ---
        colStatut.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    String val = item.toString();
                    setText(val);
                    String style = "-fx-font-weight: bold; -fx-alignment: CENTER;";
                    if (val.equalsIgnoreCase("CONFIRMEE")) setStyle(style + "-fx-text-fill: #2ecc71;");
                    else if (val.equalsIgnoreCase("EN_ATTENTE")) setStyle(style + "-fx-text-fill: #f1c40f;");
                    else if (val.equalsIgnoreCase("ANNULEE")) setStyle(style + "-fx-text-fill: #e74c3c;");
                }
            }
        });

        // --- DESIGN BOUTON SUPPRIMER ---
        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button btn = new Button("Supprimer");
            {
                btn.setStyle("-fx-background-color: linear-gradient(to right, #FF8210, #ffb347); -fx-text-fill: white; -fx-background-radius: 15; -fx-font-weight: bold; -fx-cursor: hand;");
                btn.setPrefWidth(90);
                btn.setOnAction(event -> handleDelete(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
                if (!empty) setAlignment(javafx.geometry.Pos.CENTER);
            }
        });
    }

    @FXML
    public void showReservations() {
        hideAllViews();
        reservationView.setVisible(true);
        moveBubble(btnVoyages);

        ObservableList<Reservation> data = FXCollections.observableArrayList(reservationService.getAllReservations());
        FilteredList<Reservation> filteredData = new FilteredList<>(data, b -> true);

        if (txtSearch != null) {
            txtSearch.textProperty().addListener((obs, old, nv) -> applyFilter(filteredData));
            comboStatut.valueProperty().addListener((obs, old, nv) -> applyFilter(filteredData));
        }

        SortedList<Reservation> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tableMesReservations.comparatorProperty());
        tableMesReservations.setItems(sortedData);
    }

    private void applyFilter(FilteredList<Reservation> filteredData) {
        filteredData.setPredicate(res -> {
            String text = (txtSearch == null) ? "" : txtSearch.getText().toLowerCase().trim();
            String status = (comboStatut == null) ? "Tous" : comboStatut.getValue();

            boolean matchesText = text.isEmpty() ||
                    (res.getType_res() != null && res.getType_res().toLowerCase().contains(text)) ||
                    (res.getCommentaire_client() != null && res.getCommentaire_client().toLowerCase().contains(text));

            boolean matchesStatus = status.equals("Tous") || (res.getStatut() != null && res.getStatut().toString().equals(status));
            return matchesText && matchesStatus;
        });
    }

    // --- LOGIQUE CHAT ---
    private void startChatServer() {
        chatServer = new ChatServer(8887) {
            @Override
            public void onMessage(WebSocket conn, String msg) {
                Platform.runLater(() -> {
                    if (msg.equals("[EFFACER_TOUT]")) adminChatDisplay.clear();
                    else adminChatDisplay.appendText(msg + "\n");
                });
                super.onMessage(conn, msg);
            }
        };
        chatServer.start();
        loadChatHistory();
    }

    @FXML private void handleAdminReply() {
        String msg = adminChatInput.getText().trim();
        if (!msg.isEmpty()) {
            chatServer.broadcast("Admin: " + msg);
            adminChatDisplay.appendText("Moi: " + msg + "\n");
            saveMessageToFile("Admin: " + msg);
            adminChatInput.clear();
        }
    }

    @FXML private void handleClearHistory() {
        new File(HISTORY_FILE).delete();
        adminChatDisplay.clear();
        chatServer.broadcast("[EFFACER_TOUT]");
    }

    // --- NAVIGATION ---
    @FXML public void showSupport() { hideAllViews(); supportView.setVisible(true); moveBubble(btnMessages); }
    @FXML private void showExplorer() { hideAllViews(); explorerView.setVisible(true); moveBubble(btnExplorer); }
    private void hideAllViews() { explorerView.setVisible(false); reservationView.setVisible(false); supportView.setVisible(false); }

    private void moveBubble(Button target) {
        if (currentActiveBtn == target || target == null) return;
        TranslateTransition tt = new TranslateTransition(Duration.millis(300), slidingPane);
        tt.setToY(target.getLayoutY());
        tt.setInterpolator(Interpolator.EASE_BOTH);
        tt.play();
        currentActiveBtn = target;
    }

    private void handleDelete(Reservation res) {
        reservationService.delete(res.getId());
        showReservations();
    }

    private void saveMessageToFile(String m) {
        try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(HISTORY_FILE, true)))) { out.println(m); } catch (IOException e) {}
    }

    private void loadChatHistory() {
        File f = new File(HISTORY_FILE);
        if (f.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                String l; while ((l = br.readLine()) != null) adminChatDisplay.appendText(l + "\n");
            } catch (IOException e) {}
        }
    }
}