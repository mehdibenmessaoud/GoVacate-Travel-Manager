    package tn.esprit.projet.gui;

    import javafx.collections.FXCollections;
    import javafx.fxml.FXML;
    import javafx.fxml.FXMLLoader;
    import javafx.fxml.Initializable;
    import javafx.geometry.Pos;
    import javafx.scene.Parent;
    import javafx.scene.control.*;
    import javafx.scene.control.ScrollPane;
    import javafx.scene.image.Image;
    import javafx.scene.image.ImageView;
    import javafx.scene.layout.*;
    import javafx.scene.shape.Rectangle;
    import tn.esprit.projet.entities.Pack;
    import tn.esprit.projet.services.PackService;
    import javafx.event.ActionEvent;
    import javafx.scene.Node;
    import javafx.scene.Scene;
    import javafx.scene.layout.BorderPane;
    import javafx.fxml.FXMLLoader;
    import javafx.scene.Parent;
    import java.io.IOException;

    import javafx.stage.Stage;
    // --- JavaFX Core ---
    import javafx.application.Platform;
    import javafx.collections.FXCollections;
    import javafx.event.ActionEvent;
    import javafx.fxml.FXML;
    import javafx.fxml.FXMLLoader;
    import javafx.fxml.Initializable;
    import javafx.geometry.Insets;
    import javafx.geometry.Pos;
    import javafx.scene.Parent;
    import javafx.scene.Scene;
    import javafx.scene.control.*;
    import javafx.scene.image.Image;
    import javafx.scene.image.ImageView;
    import javafx.scene.layout.*;
    import javafx.scene.shape.Rectangle;
    import javafx.stage.Stage;

    // --- WebSocket & Network ---
    import org.java_websocket.client.WebSocketClient;
    import org.java_websocket.handshake.ServerHandshake;
    import java.net.URI;
    import java.net.URL;

    // --- IO & Persistence ---
    import java.io.*;
    import java.util.List;
    import java.util.ResourceBundle;
    import java.util.stream.Collectors;

    // --- Time & Formatting ---
    import java.time.LocalDateTime;
    import java.time.format.DateTimeFormatter;

    // --- Entities & Services (Thabbet f-el paths mte3ek) ---
    import tn.esprit.projet.entities.Pack;
    import tn.esprit.projet.entities.Excursion;
    import tn.esprit.projet.services.PackService;
    import tn.esprit.projet.services.ExcursionService;
    import tn.esprit.projet.utils.SceneManager;
    import tn.esprit.projet.utils.SessionManager;

    import javafx.scene.layout.AnchorPane;
    import javafx.scene.layout.BorderPane;
    import javafx.scene.layout.StackPane;
    import tn.esprit.projet.utils.SceneManager;
    import tn.esprit.projet.utils.SessionManager;

    import java.io.IOException;

    import java.io.IOException;
    import java.net.URL;
    import java.util.List;
    import java.util.ResourceBundle;
    import java.util.stream.Collectors;

    public class ClientPackController implements Initializable {

        @FXML private StackPane rootPane; // Le StackPane racine défini dans le FXML
        @FXML private FlowPane packsContainer;
        @FXML private TextField searchField;
        @FXML private ComboBox<String> categoryFilter;
        @FXML private ScrollPane mainContentArea; // Ajoute cette ligne

        @FXML private Button btnExplorer;
        @FXML private Button btnExplorerDest;
        @FXML private Button btnHotelsModule;
        @FXML private Button btnRoomsModule;
        @FXML private Button btnRoomReservationsModule;

        @FXML private VBox packContainer;
        @FXML private BorderPane mainLayout; // 👈 ZID HADHI HNA!
        @FXML private Pane monPane;
        // --- Chat & Network Variables (Ziydet) ---
        private static org.java_websocket.client.WebSocketClient webSocketClient;
        private static String lastProcessedMessage = "";
        private VBox chatMessageContainer;
        private TextField chatInput;
        private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM");
        private final PackService sp = new PackService();
        private List<Pack> allPacks;
        private Parent clientHotelModuleRoot;
        private ClientController3 clientHotelModuleController;

        @Override

        public void initialize(URL location, ResourceBundle resources) {
            // 1. Initialisation des filtres
            SceneManager.setClientContentPane(mainLayout);
            categoryFilter.setItems(FXCollections.observableArrayList("Toutes", "Individual", "Couple", "Familialle"));
            categoryFilter.setValue("Toutes");

            // 2. RENDRE LA RECHERCHE DYNAMIQUE
            // On ajoute un listener sur le champ de texte
            searchField.textProperty().addListener((observable, oldValue, newValue) -> {
                handleSearch(); // Se déclenche à chaque lettre tapée
            });

            // On ajoute aussi un listener sur la ComboBox pour que le filtre soit instantané
            categoryFilter.valueProperty().addListener((observable, oldValue, newValue) -> {
                handleSearch();
            });

            // 3. Chargement initial
            loadPacks();
        }

        private void loadPacks() {
            try {
                allPacks = sp.getAll();
                displayPacks(allPacks);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void displayPacks(List<Pack> packs) {
            packsContainer.getChildren().clear();
            for (Pack p : packs) {
                packsContainer.getChildren().add(createPackCard(p));
            }
        }

        private VBox createPackCard(Pack p) {
            VBox card = new VBox();
            card.getStyleClass().add("water-card");
            card.setPrefWidth(280);
            card.setSpacing(0);

            // --- Header Image ---
            StackPane imageHeader = new StackPane();
            imageHeader.getStyleClass().add("card-image-header");

            ImageView imageView = new ImageView();
            try {
                String imagePath = "/images/" + p.getImageName();
                if (getClass().getResource(imagePath) != null) {
                    Image img = new Image(getClass().getResource(imagePath).toExternalForm());
                    imageView.setImage(img);
                }
            } catch (Exception e) {
                System.err.println("Image introuvable : " + p.getImageName());
            }

            imageView.setFitWidth(280);
            imageView.setFitHeight(160);
            imageView.setPreserveRatio(false);

            // Arrondir les coins supérieurs de l'image
            Rectangle clip = new Rectangle(280, 160);
            clip.setArcWidth(40);
            clip.setArcHeight(40);
            imageView.setClip(clip);

            imageHeader.getChildren().add(imageView);

            // --- Contenu des infos ---
            VBox content = new VBox(10);
            content.getStyleClass().add("glass-card");
            content.setPadding(new javafx.geometry.Insets(15));

            Label category = new Label(p.getCategorie().toUpperCase());
            category.getStyleClass().add("card-sub");

            Label name = new Label(p.getName());
            name.getStyleClass().add("card-title");

            Label price = new Label(p.getPrix() + " DT");
            price.setStyle("-fx-text-fill: #FF8210; -fx-font-size: 18px; -fx-font-weight: bold;");

            // --- Ligne d'actions ---
            HBox actionBox = new HBox(10);
            actionBox.setAlignment(Pos.CENTER_LEFT);

            // Badge de Statut (Vert ou Rouge)
            Label statusBadge = new Label();
            statusBadge.getStyleClass().add("status-badge");
            String currentStatus = p.getStatus() != null ? p.getStatus().toLowerCase() : "";

            if (currentStatus.equals("disponible")) {
                statusBadge.setText("Disponible");
                statusBadge.getStyleClass().add("status-available");
            } else {
                statusBadge.setText("Indisponible");
                statusBadge.getStyleClass().add("status-unavailable");
            }

            Button btnDetails = new Button("Détails");
            btnDetails.getStyleClass().add("btn-details-small");

            // ACTION : Afficher les détails sans changer de fenêtre
            btnDetails.setOnAction(event -> showPackDetails(p));

            Button btnBook = new Button("Réserver");
            btnBook.getStyleClass().add("btn-reserve-small");
            if (!currentStatus.equals("disponible")) btnBook.setDisable(true);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            actionBox.getChildren().addAll(statusBadge, spacer, btnDetails, btnBook);

            content.getChildren().addAll(category, name, price, actionBox);
            card.getChildren().addAll(imageHeader, content);

            return card;
        }

        /**
         * Change le contenu central pour afficher les détails du pack
         */
        private void showPackDetails(Pack p) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/PackDetails.fxml"));
                Parent detailsView = loader.load();

                PackDetailsController controller = loader.getController();
                controller.setPackData(p);

                // Au lieu de chercher le BorderPane, on remplace directement
                // le contenu du ScrollPane ou du centre du BorderPane
                BorderPane bp = (BorderPane) rootPane.getChildren().get(1);
                bp.setCenter(detailsView);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /*@FXML
        private void handleSearch() {
            String query = searchField.getText().toLowerCase();
            String cat = categoryFilter.getValue();

            List<Pack> filtered = allPacks.stream().filter(p -> {
                boolean matchesSearch = p.getName().toLowerCase().contains(query) ||
                        p.getDescription().toLowerCase().contains(query);
                boolean matchesCat = (cat == null || cat.equals("Toutes") || p.getCategorie().equals(cat));
                return matchesSearch && matchesCat;
            }).collect(Collectors.toList());

            displayPacks(filtered);
        }*/
        @FXML
        private void handleSearch() {
            // On récupère le texte, même s'il est vide
            String query = (searchField.getText() == null) ? "" : searchField.getText().toLowerCase().trim();
            String cat = categoryFilter.getValue();

            if (allPacks == null) return;

            List<Pack> filtered = allPacks.stream().filter(p -> {
                boolean matchesSearch = p.getName().toLowerCase().contains(query) ||
                        p.getDescription().toLowerCase().contains(query);

                boolean matchesCat = (cat == null || cat.equals("Toutes") || p.getCategorie().equalsIgnoreCase(cat));

                return matchesSearch && matchesCat;
            }).collect(Collectors.toList());

            displayPacks(filtered);
        }

        /*@FXML
        private void handleShowPacks() {
            try {
                // ... (votre code de chargement actuel)
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/ClientPackView.fxml"));
                Parent packView = loader.load();
                BorderPane loadedBp = (BorderPane) ((StackPane) packView).getChildren().get(1);

                ((BorderPane) rootPane.getChildren().get(1)).setCenter(loadedBp.getCenter());
                loadPacks();

                // --- GESTION DU STYLE ---
                btnExplorer.getStyleClass().add("liquid-btn-active"); // On allume Packs
                btnExplorerDest.getStyleClass().remove("liquid-btn-active"); // On éteint Destinations

            } catch (IOException e) {
                e.printStackTrace();
            }
        }*/

        @FXML
        private void handleShowPacks(ActionEvent event) {
            try {
                // On charge UNIQUEMENT le contenu central
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/PacksContent.fxml"));
                Parent view = loader.load();

                // On accède au mainLayout défini dans adminView.fxml
                // Note: scene.lookup("#mainLayout") fonctionne si l'ID est bien présent
                BorderPane mainLayout = (BorderPane) rootPane.getScene().lookup("#mainLayout");

                if (mainLayout != null) {
                    mainLayout.setCenter(view);
                }
            } catch (IOException e) {
                System.err.println("Erreur de navigation : " + e.getMessage());
                e.printStackTrace();
            }
        }

        @FXML
        private void handleShowDestinations() {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/ClientDestinationView.fxml"));
                Parent destinationView = loader.load();

                // Ici rootPane existe car on est dans le contrôleur principal
                BorderPane bp = (BorderPane) rootPane.getChildren().get(1);
                bp.setCenter(destinationView);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @FXML
        private void handleShowExcursions() {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/ClientExcursionView.fxml"));
                Parent excursionView = loader.load();

                if (mainLayout != null) {
                    mainLayout.setCenter(excursionView);
                } else {
                    BorderPane bp = (BorderPane) rootPane.getChildren().get(1);
                    bp.setCenter(excursionView);
                }
            } catch (IOException e) {
                System.err.println("Erreur chargement Excursions: " + e.getMessage());
                e.printStackTrace();
            }
        }

        @FXML
        private void handleShowHotelsModule() {
            showClientHotelsModule(ClientController3::goToHotels);
        }

        @FXML
        private void handleShowRoomsModule() {
            showClientHotelsModule(ClientController3::goToRooms);
        }

        @FXML
        private void handleShowRoomReservationsModule() {
            showClientHotelsModule(ClientController3::goToReservations);
        }

        private void showClientHotelsModule(java.util.function.Consumer<ClientController3> navigator) {
            try {
                if (clientHotelModuleRoot == null || clientHotelModuleController == null) {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/Client.fxml"));
                    clientHotelModuleRoot = loader.load();
                    clientHotelModuleController = loader.getController();

                    if (clientHotelModuleRoot instanceof StackPane moduleStack) {
                        for (Node child : moduleStack.getChildren()) {
                            if (child instanceof BorderPane moduleLayout) {
                                // Keep global sidebar from ClientPackView.fxml only.
                                moduleLayout.setLeft(null);
                                break;
                            }
                        }
                    }
                }

                if (mainLayout != null) {
                    mainLayout.setCenter(clientHotelModuleRoot);
                } else {
                    BorderPane bp = (BorderPane) rootPane.getChildren().get(1);
                    bp.setCenter(clientHotelModuleRoot);
                }
                if (clientHotelModuleController != null && navigator != null) {
                    navigator.accept(clientHotelModuleController);
                }
            } catch (IOException e) {
                System.err.println("Erreur chargement Hotels: " + e.getMessage());
                e.printStackTrace();
            }
        }
        @FXML
        private void handleDetails(Pack selectedPack) {
            try {
                // 1. Charger le fichier FXML des détails [cite: 20]
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/tn/esprit/projet/gui/PackDetails.fxml"));
                Parent detailsView = loader.load();

                // 2. Récupérer le contrôleur de la vue de détails
                PackDetailsController controller = loader.getController();

                // 3. Passer les données du pack au nouveau contrôleur
                controller.setPackData(selectedPack);

                // 4. Accéder au BorderPane principal pour changer le centre
                // On remonte l'arborescence à partir d'un élément existant (ex: packContainer)
                StackPane root = (StackPane) packContainer.getScene().getRoot();
                BorderPane mainLayout = (BorderPane) root.lookup("#mainLayout"); // Utilise l'ID défini dans adminView

                if (mainLayout != null) {
                    mainLayout.setCenter(detailsView);
                }

            } catch (IOException e) {
                System.err.println("Erreur lors du chargement des détails : " + e.getMessage());
                e.printStackTrace();
            }
        }

        @FXML
        private void openAIChat() {
            try {
                // Chargement du FXML du Chat
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/ChatAI.fxml"));
                Parent root = loader.load();

                // Création d'une nouvelle fenêtre (Stage)
                Stage chatStage = new Stage();
                chatStage.setTitle("Assistant Intelligent GoVacate");

                // On rend la fenêtre non redimensionnable pour garder le design propre
                chatStage.setResizable(false);

                // On peut la rendre "toujours au dessus" pour que le client puisse lire et naviguer
                chatStage.setAlwaysOnTop(true);

                chatStage.setScene(new Scene(root));
                chatStage.show();

            } catch (IOException e) {
                System.err.println("Erreur lors de l'ouverture du chat IA : " + e.getMessage());
                e.printStackTrace();
            }
        }

        @FXML
        private void handleProfileClick() {
            // On utilise la méthode de chargement dynamique de ton SceneManager
            SceneManager.loadClientContent("/Profile.fxml");
        }


        @FXML
        private void handleLogout() {
            // 1. On vide les données de l'utilisateur
            SessionManager.clearSession();

            // 2. On redirige vers la page de login
            SceneManager.switchTo("/Auth.fxml");

            System.out.println("Déconnexion réussie.");
        }

        @FXML
        private void handleDashboard(ActionEvent event) {
            try {
                // On utilise SceneManager pour charger le contenu du Dashboard
                // dans la zone centrale de votre application
                SceneManager.loadClientContent("/ClientDashboard.fxml");

                System.out.println("Affichage du Dashboard client...");
            } catch (Exception e) {
                System.err.println("Erreur lors du chargement du Dashboard : " + e.getMessage());
                e.printStackTrace();
            }
        }
        @FXML
        public void showChat() {
            if (mainContentArea != null) mainContentArea.setVisible(false);

            VBox chatLayout = new VBox(15);
            chatLayout.setPadding(new Insets(20));
            chatLayout.setAlignment(Pos.TOP_CENTER);
            VBox.setVgrow(chatLayout, Priority.ALWAYS);

            // 🎨 BG Jdid: Gradient Bleu Nuit Pro (mouch aswed ghamre9)
            chatLayout.setStyle("-fx-background-color: linear-gradient(to bottom right, #1a2a3a, #0f172a); " +
                    "-fx-background-radius: 25; " +
                    "-fx-border-color: rgba(255,255,255,0.1); " +
                    "-fx-border-width: 1;");

            // Header mezyn
            Label title = new Label("Support GoVacate");
            title.setStyle("-fx-text-fill: #FF8210; -fx-font-size: 22px; -fx-font-weight: bold; -fx-font-family: 'Arial Rounded MT Bold';");

            chatMessageContainer = new VBox(15);
            chatMessageContainer.setPadding(new Insets(10));
            chatMessageContainer.setStyle("-fx-background-color: transparent;");

            ScrollPane scroll = new ScrollPane(chatMessageContainer);
            scroll.setFitToWidth(true);
            scroll.setPrefHeight(500);
            // Style ScrollPane kima el 9dim ama transparent
            scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-border-color: transparent;");
            VBox.setVgrow(scroll, Priority.ALWAYS);
            chatMessageContainer.heightProperty().addListener((obs, oldV, newV) -> scroll.setVvalue(1.0));

            // --- Bottom Bar (Input Area) ---
            HBox inputArea = new HBox(12);
            inputArea.setAlignment(Pos.CENTER);
            inputArea.setPadding(new Insets(15, 10, 10, 10));
            inputArea.setStyle("-fx-background-color: rgba(255, 255, 255, 0.05); -fx-background-radius: 30;");

            chatInput = new TextField();
            chatInput.setPromptText("Écrivez un message...");
            chatInput.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-prompt-text-fill: #7f8c8d; -fx-font-size: 14px;");
            HBox.setHgrow(chatInput, Priority.ALWAYS);

            // Envoyer Style
            Button btnSend = new Button("Envoyer");
            btnSend.setStyle("-fx-background-color: #FF8210; -fx-text-fill: white; -fx-background-radius: 20; -fx-padding: 8 20; -fx-font-weight: bold; -fx-cursor: hand;");
            btnSend.setOnAction(e -> handleSendMessage());

            // Supprimer Style (Glass Red)
            Button btnClear = new Button("🗑");
            btnClear.setStyle("-fx-background-color: rgba(231, 76, 60, 0.2); -fx-text-fill: #e74c3c; -fx-background-radius: 50; -fx-padding: 8 12; -fx-border-color: #e74c3c; -fx-cursor: hand;");
            btnClear.setOnAction(e -> handleClearHistory());

            inputArea.getChildren().addAll(chatInput, btnSend, btnClear);
            chatLayout.getChildren().addAll(title, new Separator(), scroll, inputArea);

            if (mainLayout != null) {
                mainLayout.setCenter(chatLayout);
            } else {
                BorderPane bp = (BorderPane) rootPane.getChildren().get(1);
                bp.setCenter(chatLayout);
            }

            loadChatHistory();
            connectToChatServer();
        }
        private void handleSendMessage() {
            String msg = chatInput.getText().trim();
            if (msg.isEmpty() || !SessionManager.isLoggedIn()) return;

            String time = LocalDateTime.now().format(timeFormatter);
            String date = LocalDateTime.now().format(dateFormatter);

            // Protocol kima el 9dim
            String payload = SessionManager.getCurrentUserId() + "|CLIENT|" +
                    SessionManager.getCurrentUserName() + "|" + msg + "|" + time + "|" + date;

            if (webSocketClient != null && webSocketClient.isOpen()) {
                webSocketClient.send(payload);
                chatInput.clear();
            }
        }
        private void connectToChatServer() {
            try {
                if (webSocketClient != null && webSocketClient.isOpen()) return;

                webSocketClient = new org.java_websocket.client.WebSocketClient(new URI("ws://localhost:8887")) {
                    @Override
                    public void onOpen(org.java_websocket.handshake.ServerHandshake h) {
                        System.out.println("✅ [CLIENT] Online");
                    }

                    @Override
                    public void onMessage(String message) {
                        Platform.runLater(() -> {
                            // 🎯 Anti-duplication check (Static)
                            if (message.equals(lastProcessedMessage)) return;
                            lastProcessedMessage = message;

                            String[] parts = message.split("\\|");
                            if (parts.length >= 4) {
                                String targetId = parts[0];
                                // Affichi ken klem li jey l-identifiant mte3i
                                if (targetId.equals(String.valueOf(SessionManager.getCurrentUserId()))) {
                                    addMessageToUI(message);
                                }
                            }
                        });
                    }

                    @Override public void onClose(int c, String r, boolean rem) {}
                    @Override public void onError(Exception ex) { ex.printStackTrace(); }
                };
                webSocketClient.connect();
            } catch (Exception e) { e.printStackTrace(); }
        }
        private void addMessageToUI(String message) {
            if (message == null || message.isEmpty()) return;
            Platform.runLater(() -> {
                String content = message, role = "ADMIN", name = "Admin", time = "00:00", date = "";

                if (message.contains("|")) {
                    String[] p = message.split("\\|");
                    if (p.length >= 6) {
                        role = p[1]; name = p[2]; content = p[3]; time = p[4]; date = p[5];
                    }
                }

                // 🚀 Zid el séparateur de date kima el 9dim
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

        // 🚀 El Méthode mta3 el Date separator mel code el 9dim
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
            if (chatMessageContainer == null) return;
            chatMessageContainer.getChildren().clear();

            String fileName = "chat_history_user_" + SessionManager.getCurrentUserId() + ".txt";
            File f = new File(fileName);
            if (f.exists()) {
                try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        addMessageToUI(line);
                    }
                } catch (IOException e) { e.printStackTrace(); }
            }
        }
        @FXML
        private void handleShowFactures() {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/MesFactures.fxml"));
                Parent view = loader.load();

                // 🚀 NAB3ATH EL USER ID
                MesFacturesController controller = loader.getController();
                controller.loadFacturesForUser(SessionManager.getCurrentUserId());

                mainLayout.setCenter(view);
            } catch (IOException e) { e.printStackTrace(); }
        }
        @FXML
        private void handleShowReservations() {
            try {
                // On charge la vue des réservations
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/Mes Réservations.fxml"));
                Parent view = loader.load();

                // Wrap in ScrollPane so the payment button is always visible
                ScrollPane scroll = new ScrollPane(view);
                scroll.setFitToWidth(true);
                scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
                scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
                scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

                // On l'injecte dans le centre du mainLayout (BorderPane)
                if (mainLayout != null) {
                    mainLayout.setCenter(scroll);
                    System.out.println("✅ Mes Réservations chargées dans le center.");
                }
            } catch (IOException e) {
                System.err.println("❌ Erreur chargement Réservations: " + e.getMessage());
                e.printStackTrace();
            }
        }
        @FXML
        private void handleClearHistory() {
            String fileName = "chat_history_user_" + SessionManager.getCurrentUserId() + ".txt";
            File f = new File(fileName);
            if (f.exists()) {
                if (f.delete()) {
                    System.out.println("🗑️ Historique supprimé définitivement.");
                }
            }
            if (chatMessageContainer != null) {
                chatMessageContainer.getChildren().clear();
            }
            // 🚀 Reset anti-duplicate
            lastProcessedMessage = "";
        }

    }