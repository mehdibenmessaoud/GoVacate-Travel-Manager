package tn.esprit.projet.gui;

// --- JavaFX Core ---
import javafx.application.Platform; // 👈 Zid hadhi (Bech tna7i erreur Platform.runLater)
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.event.ActionEvent;

// --- Entities & Services ---
import tn.esprit.projet.entities.Pack;
import tn.esprit.projet.entities.Excursion;
import tn.esprit.projet.services.PackService;
import tn.esprit.projet.services.ExcursionService;
import tn.esprit.projet.utils.SessionManager;

// --- WebSocket & Network ---
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import java.net.URI; // 👈 Zid hadhi (Bech tna7i erreur URI)
import java.net.URL;

// --- IO & Time ---
import java.io.*;
import java.time.LocalDateTime; // 👈 Zid hadhi (Bech tna7i erreur LocalDateTime)
import java.time.format.DateTimeFormatter;
import java.util.List;public class ClientDashboardController2 {

    @FXML private ScrollPane explorerView;
    @FXML private VBox contentArea; // Container for dynamic views
    @FXML private FlowPane packsContainer;
    private WebSocketClient webSocketClient;
    private String lastProcessedMessage = "";
    private VBox chatMessageContainer; // Dynamic container
    private TextField chatInput;
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM");
    private final PackService packService = new PackService();

    @FXML
    public void initialize() {
        // Load packs by default on startup
        showExplorer();
    }

    // --- PACK EXPLORER LOGIC ---
    private void loadPacks() {
        try {
            List<Pack> allPacks = packService.getAll();
            packsContainer.getChildren().clear();
            for (Pack p : allPacks) {
                packsContainer.getChildren().add(createPackCard(p));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private VBox createPackCard(Pack p) {
        VBox card = new VBox(12);
        card.setStyle("-fx-background-color: rgba(255, 255, 255, 0.07); -fx-background-radius: 20; -fx-padding: 15; -fx-border-color: rgba(255, 255, 255, 0.1); -fx-border-radius: 20;");
        card.setPrefWidth(280);

        // Image Handling
        ImageView img = new ImageView();
        try {
            String path = "/images/" + p.getImageName();
            URL imgUrl = getClass().getResource(path);
            if (imgUrl != null) {
                img.setImage(new Image(imgUrl.toExternalForm()));
            }
        } catch (Exception e) { System.err.println("Image not found: " + p.getImageName()); }

        img.setFitWidth(250); img.setFitHeight(150);
        Rectangle clip = new Rectangle(250, 150); clip.setArcWidth(30); clip.setArcHeight(30);
        img.setClip(clip);

        Label title = new Label(p.getName());
        title.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 18px;");

        Label price = new Label(p.getPrix() + " DT");
        price.setStyle("-fx-text-fill: #FF8210; -fx-font-weight: bold; -fx-font-size: 16px;");

        Button btnBook = new Button("Réserver");
        btnBook.setStyle("-fx-background-color: #FF8210; -fx-text-fill: white; -fx-background-radius: 10; -fx-cursor: hand; -fx-font-weight: bold;");
        btnBook.setMaxWidth(Double.MAX_VALUE);
        btnBook.setOnAction(e -> openBookingForm(p));

        card.getChildren().addAll(img, title, price, btnBook);
        return card;
    }

    private void openBookingForm(Pack p) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/reservationpackform.fxml"));
            Parent view = loader.load();
            PackBookingController controller = loader.getController();
            controller.setPackDataFromEntity(p);

            explorerView.setVisible(false);
            contentArea.getChildren().setAll(view);
            contentArea.setVisible(true);
        } catch (IOException e) { e.printStackTrace(); }
    }

    // --- NAVIGATION ---
    @FXML
    private void showExplorer() {
        contentArea.setVisible(false);
        explorerView.setVisible(true);
        loadPacks();
    }

    @FXML
    private void showReservations() {
        try {
            explorerView.setVisible(false);
            contentArea.getChildren().clear();

            // ⚠️ El esm mte3ek fih espace w é accentué
            String fxmlName = "/Mes Réservation.fxml";
            URL url = getClass().getResource(fxmlName);

            if (url == null) {
                // Jarreb el path b-el package kenek 7attou dakhil gui
                url = getClass().getResource("/Mes Réservations.fxml");
            }

            if (url != null) {
                FXMLLoader loader = new FXMLLoader(url);
                Parent view = loader.load();
                contentArea.getChildren().add(view);
                contentArea.setVisible(true);
                System.out.println("✅ Mes Réservation chargé avec succès !");
            } else {
                System.err.println("❌ Erreur: Impossible de trouver el file [" + fxmlName + "]");
                System.err.println("💡 Thabbet mel Espace w el é (accent) f-el esm mta el file f-el Resources!");
            }
        } catch (IOException e) {
            System.err.println("❌ Erreur FXMLLoader: " + e.getMessage());
            e.printStackTrace();
        }
    }
    @FXML
    private void showFactures() {
        try {
            explorerView.setVisible(false);
            contentArea.getChildren().clear();

            // Path to your existing Invoices FXML
            URL url = getClass().getResource("/MesFactures.fxml");
            if (url == null) url = getClass().getResource("/tn/esprit/projet/gui/MesFactures.fxml");

            if (url != null) {
                Parent view = new FXMLLoader(url).load();
                contentArea.getChildren().add(view);
                contentArea.setVisible(true);
                System.out.println("✅ Mes Factures chargées avec succès !");
            } else {
                System.err.println("❌ Erreur: MesFactures.fxml introuvable.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @FXML
    private void showExcursions() {
        try {
            // 1. Visibilité mrigla bech tchouf el contentArea
            explorerView.setVisible(false);
            contentArea.setVisible(true);
            contentArea.getChildren().clear();

            // 2. Titre mta el page
            Label title = new Label("Explorez nos Excursions");
            title.setStyle("-fx-text-fill: white; -fx-font-size: 32px; -fx-font-weight: bold; -fx-padding: 0 0 20 0;");
            contentArea.getChildren().add(title);

            // 3. Grid mta el cards
            FlowPane excursionGrid = new FlowPane();
            excursionGrid.setHgap(30);
            excursionGrid.setVgap(30);
            excursionGrid.setPrefWrapLength(1000);

            // 4. Jib el data mel Database
            ExcursionService es = new ExcursionService();
            List<Excursion> list = es.getAll();

            // Dakhil showExcursions() f-el ClientDashboardController2
            for (Excursion e : list) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/ExcursionCard.fxml"));
                Parent card = loader.load();

                ExcursionCardController ctrl = loader.getController();

                // 🔥 Hné el riga: ki y-cliqui 3al card (ou bouton), y-hezou lel Booking mouch Détails
                ctrl.setData(e, selected -> {
                    openExcursionBooking(selected);
                });

                excursionGrid.getChildren().add(card);
            }
            contentArea.getChildren().add(excursionGrid);

        } catch (Exception ex) {
            System.err.println("❌ Erreur showExcursions: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void showExcursionDetails(Excursion e) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ExcursionDetails.fxml"));
            Parent detailsView = loader.load();

            // ⚠️ Thabbet f-el ExcursionDetailsController lezem el méthode esmha setExcursionData
            ExcursionDetailsController ctrl = loader.getController();
            ctrl.setExcursionData(e);

            contentArea.getChildren().clear();
            contentArea.getChildren().add(detailsView);
        } catch (IOException ex) {
            System.err.println("❌ Erreur showExcursionDetails: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
    private void openExcursionBooking(Excursion e) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ExcursionBooking.fxml"));
            Parent view = loader.load();

            // 🟢 Passi el data lel Controller mta el Booking
            ExcursionBookingController ctrl = loader.getController();
            ctrl.setExcursionData(e);

            // 🟢 Affichi f-el contentArea
            explorerView.setVisible(false);
            contentArea.getChildren().setAll(view);
            contentArea.setVisible(true);

            System.out.println("✅ Formulaire de réservation chargé pour: " + e.getName());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    @FXML
    private void showProfile(ActionEvent event) {
        System.out.println("Naviguer vers le profil du voyageur");
        // Hne t-zid el code bech t-affichi el profil ken t7eb
    }
    @FXML
    private void handleLogout() {
        System.exit(0);
    }
    @FXML
    public void showChat() {
        explorerView.setVisible(false);
        contentArea.setVisible(true);
        contentArea.getChildren().clear();

        VBox chatLayout = new VBox(10);
        VBox.setVgrow(chatLayout, Priority.ALWAYS);
        chatLayout.setPadding(new Insets(20));

        Label title = new Label("Support & Chat Direct");
        title.setStyle("-fx-text-fill: #FF8210; -fx-font-size: 24px; -fx-font-weight: bold;");

        chatMessageContainer = new VBox(15);
        chatMessageContainer.setPadding(new Insets(10));
        ScrollPane scroll = new ScrollPane(chatMessageContainer);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        chatMessageContainer.heightProperty().addListener((obs, oldV, newV) -> scroll.setVvalue(1.0));

        HBox inputArea = new HBox(10);
        chatInput = new TextField();
        chatInput.setPromptText("Tapez votre message...");
        chatInput.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-text-fill: white; -fx-background-radius: 20; -fx-padding: 10;");
        HBox.setHgrow(chatInput, Priority.ALWAYS);

        Button btnSend = new Button("Envoyer");
        btnSend.setStyle("-fx-background-color: #FF8210; -fx-text-fill: white; -fx-background-radius: 20; -fx-padding: 10 20; -fx-font-weight: bold; -fx-cursor: hand;");
        btnSend.setOnAction(e -> handleSendMessage());

        inputArea.getChildren().addAll(chatInput, btnSend);
        chatLayout.getChildren().addAll(title, new Separator(), scroll, inputArea);
        contentArea.getChildren().add(chatLayout);

        loadChatHistory();
        connectToChatServer();
    }

    private void handleSendMessage() {
        String msg = chatInput.getText().trim();
        if (msg.isEmpty() || !SessionManager.isLoggedIn()) return;

        String time = LocalDateTime.now().format(timeFormatter);
        String date = LocalDateTime.now().format(dateFormatter);
        String payload = SessionManager.getCurrentUserId() + "|CLIENT|" + SessionManager.getCurrentUserName() + "|" + msg + "|" + time + "|" + date;

        if (webSocketClient != null && webSocketClient.isOpen()) {
            webSocketClient.send(payload);
            chatInput.clear();
        }
    }

    private void connectToChatServer() {
        try {
            if (webSocketClient != null && webSocketClient.isOpen()) return;
            webSocketClient = new WebSocketClient(new URI("ws://localhost:8887")) {
                @Override public void onOpen(ServerHandshake h) { System.out.println("✅ Online"); }

                @Override
                public void onMessage(String m) {
                    Platform.runLater(() -> {
                        System.out.println("DEBUG CLIENT: Jeni message -> " + m);
                        String[] p = m.split("\\|");
                        if (p.length < 6) return;

                        String targetId = p[0];

                        // 1️⃣ Thabbet elli el message ma3ni bih enti
                        if (targetId.equals(String.valueOf(SessionManager.getCurrentUserId()))) {

                            // 2️⃣ FIX DUPLICATE: Ken el message jé mel Server (Broadcast)
                            // w houwa bidou e5er message t-afficha, t3addeh (Skip)
                            if (m.equals(lastProcessedMessage)) {
                                System.out.println("⚠️ Duplicate détecté, ignore l'affichage.");
                                return;
                            }

                            // 3️⃣ Affichi w sajjel e5er message processed
                            lastProcessedMessage = m;
                            addMessageToUI(m);
                        }
                    });
                }
                @Override public void onClose(int i, String s, boolean b) {}
                @Override public void onError(Exception e) { e.printStackTrace(); }
            };
            webSocketClient.connect();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void addMessageToUI(String message) {
        saveMessageToFile(message);
        renderHistoricalMessage(message);
    }

    private void renderHistoricalMessage(String message) {
        String[] p = message.split("\\|");
        if (p.length < 6) return;

        String role = p[1], name = p[2], content = p[3], time = p[4], date = p[5];
        VBox bubble = new VBox(3);
        Label nameLbl = new Label(name);
        nameLbl.setStyle("-fx-font-size: 9px; -fx-text-fill: #bdc3c7;");
        Label msgLbl = new Label(content);
        msgLbl.setWrapText(true); msgLbl.setMaxWidth(300);
        Label timeLbl = new Label(date + " " + time);
        timeLbl.setStyle("-fx-font-size: 8px; -fx-text-fill: #95a5a6;");

        HBox row = new HBox();
        row.setPadding(new Insets(5, 0, 5, 0));
        if ("CLIENT".equals(role)) {
            row.setAlignment(Pos.CENTER_RIGHT);
            bubble.setAlignment(Pos.TOP_RIGHT);
            msgLbl.setStyle("-fx-background-color: #FF8210; -fx-text-fill: white; -fx-padding: 8 12; -fx-background-radius: 15 15 2 15;");
        } else {
            row.setAlignment(Pos.CENTER_LEFT);
            bubble.setAlignment(Pos.TOP_LEFT);
            msgLbl.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-text-fill: white; -fx-padding: 8 12; -fx-background-radius: 15 15 15 2;");
        }
        bubble.getChildren().addAll(nameLbl, msgLbl, timeLbl);
        row.getChildren().add(bubble);
        chatMessageContainer.getChildren().add(row);
    }

    private void saveMessageToFile(String raw) {
        String fileName = "chat_history_user_" + SessionManager.getCurrentUserId() + ".txt";
        try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(fileName, true)))) {
            out.println(raw);
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void loadChatHistory() {
        String fileName = "chat_history_user_" + SessionManager.getCurrentUserId() + ".txt";
        File f = new File(fileName);
        if (f.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                String line;
                while ((line = br.readLine()) != null) renderHistoricalMessage(line);
            } catch (IOException e) { e.printStackTrace(); }
        }
    }
}