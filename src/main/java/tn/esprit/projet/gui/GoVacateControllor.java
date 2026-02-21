package tn.esprit.projet.gui;

import javafx.animation.Interpolator;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
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

    // --- ÉLÉMENTS FXML EXISTANTS ---
    @FXML private Pane slidingPane;
    @FXML private Button btnExplorer, btnVoyages, btnMessages;
    @FXML private ScrollPane explorerView;
    @FXML private VBox reservationView;
    @FXML private TableView<Reservation> tableMesReservations;
    @FXML private TableColumn<Reservation, String> colUserId, colType, colDate, colStatut;
    @FXML private TableColumn<Reservation, Double> colPrix;
    @FXML private TableColumn<Reservation, Void> colAction;
    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> comboStatut;

    // --- NOUVEAUX ÉLÉMENTS FXML (SUPPORT CHAT) ---
    @FXML private VBox supportView;
    @FXML private TextArea adminChatDisplay;
    @FXML private TextField adminChatInput;

    // --- VARIABLES LOGIQUES ---
    private Button currentActiveBtn = null;
    private final ReservationServiceImpl reservationService = new ReservationServiceImpl();
    private ChatServer chatServer;
    private static final String HISTORY_FILE = "chat_history.txt";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialisation de la table et des filtres
        setupTable();
        setupFilters();

        // Démarrage du serveur de chat intégré
        startChatServer();

        slidingPane.setMouseTransparent(true);

        Platform.runLater(() -> {
            if (btnExplorer != null) {
                slidingPane.setTranslateY(btnExplorer.getLayoutY());
                currentActiveBtn = btnExplorer;
            }
        });
    }

    // --- SECTION SUPPORT CHAT (SERVEUR) ---

    private void startChatServer() {
        chatServer = new ChatServer(8887) {
            @Override
            public void onMessage(WebSocket conn, String message) {
                Platform.runLater(() -> {
                    if (message.equals("[EFFACER_TOUT]")) {
                        adminChatDisplay.clear();
                        adminChatDisplay.appendText("[Système] L'historique a été effacé par un utilisateur.\n");
                    } else {
                        adminChatDisplay.appendText(message + "\n");
                    }
                });
                // Diffuse le message aux autres clients
                super.onMessage(conn, message);
            }
        };
        chatServer.start();
        loadChatHistory();
    }

    private void loadChatHistory() {
        File file = new File(HISTORY_FILE);
        if (file.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = br.readLine()) != null) {
                    adminChatDisplay.appendText(line + "\n");
                }
            } catch (IOException e) { e.printStackTrace(); }
        }
    }

    @FXML
    private void handleAdminReply() {
        String msg = adminChatInput.getText().trim();
        if (!msg.isEmpty()) {
            String formattedMsg = "Admin: " + msg;
            chatServer.broadcast(formattedMsg); // Envoie aux clients WebSockets
            adminChatDisplay.appendText("Moi: " + msg + "\n"); // Affiche sur l'écran admin
            saveMessageToFile(formattedMsg);
            adminChatInput.clear();
        }
    }

    private void saveMessageToFile(String message) {
        try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(HISTORY_FILE, true)))) {
            out.println(message);
        } catch (IOException e) { e.printStackTrace(); }
    }

    // --- SECTION GESTION RÉSERVATIONS (EXISTANTE) ---

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

        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button btn = new Button("Supprimer");
            {
                btn.getStyleClass().add("btn-orange-glow");
                btn.setPrefWidth(90);
                btn.setOnAction(event -> {
                    Optional.ofNullable(getTableView().getItems().get(getIndex()))
                            .ifPresent(res -> handleDelete(res));
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });
    }

    private void handleDelete(Reservation res) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Annuler la réservation ID: " + res.getId() + " ?", ButtonType.YES, ButtonType.NO);
        alert.setHeaderText(null);
        alert.showAndWait()
                .filter(response -> response == ButtonType.YES)
                .ifPresent(response -> {
                    reservationService.delete(res.getId());
                    showReservations();
                });
    }

    // --- SECTION NAVIGATION & ANIMATION ---

    @FXML
    private void showExplorer() {
        hideAllViews();
        explorerView.setVisible(true);
        moveBubble(btnExplorer);
    }

    @FXML
    public void showReservations() {
        hideAllViews();
        reservationView.setVisible(true);
        moveBubble(btnVoyages);

        ObservableList<Reservation> data = FXCollections.observableArrayList(
                reservationService.getAllReservations().stream().toList()
        );

        FilteredList<Reservation> filteredData = new FilteredList<>(data, b -> true);
        txtSearch.textProperty().addListener((obs, old, nv) -> applyFilter(filteredData));
        comboStatut.valueProperty().addListener((obs, old, nv) -> applyFilter(filteredData));

        SortedList<Reservation> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tableMesReservations.comparatorProperty());
        tableMesReservations.setItems(sortedData);
    }

    @FXML
    public void showSupport() {
        hideAllViews();
        if (supportView != null) supportView.setVisible(true);
        moveBubble(btnMessages);
    }

    private void hideAllViews() {
        explorerView.setVisible(false);
        reservationView.setVisible(false);
        if (supportView != null) supportView.setVisible(false);
    }

    private void applyFilter(FilteredList<Reservation> filteredData) {
        filteredData.setPredicate(res -> {
            String text = Optional.ofNullable(txtSearch.getText()).orElse("").toLowerCase().trim();
            String status = Optional.ofNullable(comboStatut.getValue()).orElse("Tous");

            boolean matchesText = text.isEmpty() ||
                    (res.getType_res() != null && res.getType_res().toLowerCase().contains(text)) ||
                    (res.getCommentaire_client() != null && res.getCommentaire_client().toLowerCase().contains(text));

            boolean matchesStatus = status.equals("Tous") ||
                    (res.getStatut() != null && res.getStatut().name().equals(status));

            return matchesText && matchesStatus;
        });
    }
    @FXML
    private void handleClearHistory() {
        File file = new File(HISTORY_FILE);
        if (file.exists()) file.delete(); // Supprime le fichier texte

        adminChatDisplay.clear();
        adminChatDisplay.appendText("[Système] Historique supprimé.\n");

        if (chatServer != null) {
            chatServer.broadcast("[EFFACER_TOUT]"); // Envoie l'ordre au client
        }
    }
    private void moveBubble(Button target) {
        if (currentActiveBtn == target || target == null) return;
        TranslateTransition tt = new TranslateTransition(Duration.millis(300), slidingPane);
        tt.setToY(target.getLayoutY());
        tt.setInterpolator(Interpolator.EASE_BOTH);
        tt.play();
        currentActiveBtn = target;
    }
}