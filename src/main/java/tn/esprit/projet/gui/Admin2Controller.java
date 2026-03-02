package tn.esprit.projet.gui;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableCell;
import javafx.scene.control.Button;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import tn.esprit.projet.entities.Reservation;
import javafx.animation.TranslateTransition;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import tn.esprit.projet.entities.Pack;
import tn.esprit.projet.services.PackService;
import org.java_websocket.client.WebSocketClient;
import tn.esprit.projet.utils.MyDBConnexion;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.io.*;
import java.sql.*;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.scene.layout.Priority;
public class Admin2Controller implements Initializable {
    @FXML
    private BorderPane mainLayout;
    @FXML
    private StackPane rootPane;
    @FXML
    private Pane slidingPane;
    @FXML
    private TextField searchField;

    @FXML
    private TableView<Pack> packTable;
    @FXML
    private TableColumn<Pack, String> colNom;
    @FXML
    private TableColumn<Pack, String> colDesc;
    @FXML
    private TableColumn<Pack, String> colCategorie;
    @FXML
    private TableColumn<Pack, Double> colPrix;
    @FXML
    private TableColumn<Pack, String> colStatut;
    @FXML
    private TableColumn<Pack, Void> colActions;
    // UI IDs (Lezem ykounou s7a7 fel FXML mte3ek)
    @FXML
    private VBox adminChatMessageContainer, inboxClientList;
    @FXML
    private ScrollPane adminChatScrollPane;
    @FXML
    private TextField adminChatInput;
    @FXML
    private Label chatHeaderLabel;
    private static String lastProcessedMessage = "";
    // Logic Fields
    private WebSocketClient webSocketClient;

    private String lastActiveClientId = null;
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
    private final tn.esprit.projet.services.ReservationServiceImpl reservationService = new tn.esprit.projet.services.ReservationServiceImpl();
    // Suppression de colDuree, colDateDep, colDateArr car elles ne sont pas dans votre FXML

    @FXML
    private Button btnExplorer, btnVoyages, btnFavoris, btnMessages, btnParametres, btnLogout,
            btnHotelsModule, btnRoomsModule, btnRoomReservationsModule;

    private Parent adminHotelModuleRoot;
    private AdminController3 adminHotelModuleController;

    private final PackService packService = new PackService();
    private ObservableList<Pack> masterData = FXCollections.observableArrayList();
    // Ã€ ajouter au dÃ©but de la classe avec les autres attributs @FXML
    private ObservableList<Pack> packList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Liaison des colonnes (Uniquement celles prÃ©sentes dans le FXML)
        if (colNom != null) {
            colNom.setCellValueFactory(new PropertyValueFactory<>("name"));
            colDesc.setCellValueFactory(new PropertyValueFactory<>("description"));
            colCategorie.setCellValueFactory(new PropertyValueFactory<>("categorie"));
            colPrix.setCellValueFactory(new PropertyValueFactory<>("prix"));
            colStatut.setCellValueFactory(new PropertyValueFactory<>("status"));

            setupActionsColumn();
            loadPackData();
        }

        // Gestion de la recherche
        if (searchField != null) {
            searchField.textProperty().addListener((observable, oldValue, newValue) -> {
                handleSearch(); // Appelle la recherche Ã  chaque changement de texte
            });
        }

        // Animations de la barre latÃ©rale
        setupSidebarAnimations();

        // --- PARTIE CHAT & SUPPORT ---
        // 1. Initialisation de la connexion WebSocket
        connectToChatServer();

        // 2. Gestion de l'Auto-scroll pour le conteneur de messages
        if (adminChatScrollPane != null && adminChatMessageContainer != null) {
            adminChatMessageContainer.heightProperty().addListener((obs, oldVal, newVal) -> {
                adminChatScrollPane.setVvalue(1.0); // Descendre tout en bas Ã  chaque nouveau message
            });
        }
        // ----------------------------

        // Chargement du Dashboard par dÃ©faut au dÃ©marrage
        javafx.application.Platform.runLater(this::handleShowDashboard);
    }

    private void loadPackData() {
        try {
            List<Pack> list = packService.getAll();
            masterData = FXCollections.observableArrayList(list);
            packTable.setItems(masterData);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /*private void loadPackData() {
        try {
            List<Pack> list = packService.getAll();
            // On remplit masterData
            masterData.setAll(list);
            // On lie le tableau Ã  masterData
            packTable.setItems(masterData);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }*/


    private void setupActionsColumn() {
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnDetails = new Button("DÃ©tails");
            private final Button btnEdit = new Button("Modifier");
            private final Button btnDelete = new Button("Supprimer");
            private final HBox container = new HBox(12, btnDetails, btnEdit, btnDelete);

            {
                container.setAlignment(Pos.CENTER);

                // Application des styles dÃ©finis dans votre admin.css
                btnDetails.getStyleClass().add("btn-table-details"); // Style bleu translucide
                btnEdit.getStyleClass().add("btn-table-edit");
                btnDelete.getStyleClass().add("btn-table-delete"); // Style orange translucide

                // Actions
                btnDetails.setOnAction(event -> handleViewDetails(getTableView().getItems().get(getIndex())));
                btnEdit.setOnAction(event -> handleEditPack(getTableView().getItems().get(getIndex())));
                btnDelete.setOnAction(event -> handleDeletePack(getTableView().getItems().get(getIndex())));

                // Curseur main pour une meilleure UX
                btnDetails.setCursor(javafx.scene.Cursor.HAND);
                btnEdit.setCursor(javafx.scene.Cursor.HAND);
                btnDelete.setCursor(javafx.scene.Cursor.HAND);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(container);
                }
            }
        });
    }

    @FXML
    private void handleShowPacks() {
        try {
            // 1. Charger la vue du tableau des packs
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/PackTable.fxml"));
            Parent packTableView = loader.load();

            // 2. L'injecter au centre du BorderPane
            if (mainLayout != null) {
                mainLayout.setCenter(packTableView);
                System.out.println("âœ… Retour Ã  la gestion des packs.");
            }
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger la table des packs.");
        }
    }

    /*@FXML
    private void handleSearch() {
        String query = (searchField != null) ? searchField.getText().toLowerCase().trim() : "";
        if (query.isEmpty()) {
            packTable.setItems(masterData);
            return;
        }
        FilteredList<Pack> filteredData = new FilteredList<>(masterData, p ->
                p.getName().toLowerCase().contains(query) || p.getCategorie().toLowerCase().contains(query)
        );
        packTable.setItems(filteredData);
    }*/

    @FXML
    private void handleSearch() {
        String query = (searchField != null) ? searchField.getText().toLowerCase().trim() : "";

        // Si le champ est vide, on rÃ©affiche tout
        if (query.isEmpty()) {
            packTable.setItems(masterData);
            return;
        }

        // Filtrage dynamique
        FilteredList<Pack> filteredData = new FilteredList<>(masterData, p -> {
            // On vÃ©rifie le nom ou la catÃ©gorie (tu peux ajouter la description ici aussi)
            return p.getName().toLowerCase().contains(query) ||
                    p.getCategorie().toLowerCase().contains(query) ||
                    p.getDescription().toLowerCase().contains(query);
        });

        packTable.setItems(filteredData);
    }

    @FXML
    private void onAddButtonClicked() {
        try {
            // 1. Charger le fichier FXML du formulaire
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/PackForm.fxml"));
            Parent form = loader.load();

            // 2. Trouver dynamiquement le BorderPane principal (mainLayout)
            // On utilise la table (packTable) pour remonter jusqu'Ã  la scÃ¨ne actuelle
            BorderPane layout = (BorderPane) packTable.getScene().lookup("#mainLayout");

            if (layout != null) {
                // 3. Remplacer la table des packs par le formulaire au centre
                layout.setCenter(form);
                System.out.println("âœ… Formulaire chargÃ© au centre du layout.");
            } else {
                // Si vraiment on ne trouve pas mainLayout, on affiche une alerte
                showAlert("Erreur de Navigation", "Le conteneur principal 'mainLayout' est introuvable.");
            }
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger le formulaire : " + e.getMessage());
        }
    }

    /*private void handleViewDetails(Pack pack) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/PackDetails.fxml"));
            Parent detailsView = loader.load();

            PackDetailsController controller = loader.getController();
            controller.setPackData(pack);

            // On injecte la vue au centre du BorderPane
            BorderPane mainLayout = (BorderPane) rootPane.getChildren().stream()
                    .filter(node -> node instanceof BorderPane)
                    .findFirst()
                    .orElse(null);

            if (mainLayout != null) {
                mainLayout.setCenter(detailsView);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }*/
    private void handleViewDetails(Pack pack) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/PackDetails.fxml"));
            Parent detailsView = loader.load();

            PackDetailsController controller = loader.getController();
            controller.setPackData(pack);

            // Utiliser directement mainLayout au lieu de rootPane
            if (mainLayout != null) {
                mainLayout.setCenter(detailsView);
            } else {
                // SÃ©curitÃ© : si mainLayout est null, on tente de le trouver via la scÃ¨ne
                BorderPane layout = (BorderPane) packTable.getScene().lookup("#mainLayout");
                if (layout != null) layout.setCenter(detailsView);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /*private void handleEditPack(Pack p) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/PackForm.fxml"));
            Parent formView = loader.load();

            // RÃ©cupÃ©rer le contrÃ´leur du formulaire et lui passer le pack
            PackFormController controller = loader.getController();
            controller.setUpdateMode(p); // On active le mode modif ici

            // Afficher le formulaire au centre
            BorderPane mainLayout = (BorderPane) rootPane.getChildren().stream()
                    .filter(node -> node instanceof BorderPane)
                    .findFirst()
                    .orElse(null);

            if (mainLayout != null) {
                mainLayout.setCenter(formView);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }*/

    private void handleEditPack(Pack p) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/PackForm.fxml"));
            Parent formView = loader.load();

            PackFormController controller = loader.getController();
            controller.setUpdateMode(p);

            // Utiliser directement mainLayout
            if (mainLayout != null) {
                mainLayout.setCenter(formView);
            } else {
                BorderPane layout = (BorderPane) packTable.getScene().lookup("#mainLayout");
                if (layout != null) layout.setCenter(formView);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleDeletePack(Pack p) {
        if (p == null) {
            showAlert("Avertissement", "Aucun pack sÃ©lectionnÃ©.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation de suppression");
        confirm.setHeaderText("Supprimer le pack : " + p.getName());
        confirm.setContentText("Voulez-vous vraiment supprimer ce pack ?");

        applyCustomStyle(confirm);

        if (confirm.showAndWait().get() == ButtonType.OK) {
            try {
                // 1. Suppression base de donnÃ©es
                packService.delete(p.getId());

                // 2. CORRECTION : On supprime de masterData car c'est elle qui est liÃ©e au TableView
                masterData.remove(p);

                System.out.println("âœ… Pack supprimÃ© de la base et de l'interface.");

            } catch (SQLException e) {
                showAlert("Erreur SQL", "Impossible de supprimer le pack : " + e.getMessage());
                e.printStackTrace();
            }
        }
    }


    @FXML
    private void handleShowDestinations() {
        try {
            // 1. Charger le nouveau fichier FXML (le tableau des destinations)
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/DestinationTable.fxml"));
            Parent destinationView = loader.load();

            // 2. AccÃ©der au BorderPane qui est Ã  l'intÃ©rieur du rootPane
            BorderPane mainLayout = (BorderPane) rootPane.getChildren().stream()
                    .filter(node -> node instanceof BorderPane)
                    .findFirst()
                    .orElse(null);

            if (mainLayout != null) {
                // 3. Remplacer le contenu central par la vue Destination
                mainLayout.setCenter(destinationView);
            }
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger la vue des destinations.");
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);

        applyCustomStyle(alert);

        alert.showAndWait();
    }

    @FXML
    private void handleShowExcursions() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ExcursionTable.fxml"));
            Parent view = loader.load();
            mainLayout.setCenter(view); // Utilise le BorderPane dÃ©jÃ  configurÃ©
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger la vue des excursions.");
        }
    }

    private void setupSidebarAnimations() {
        if (slidingPane != null) slidingPane.setMouseTransparent(true);
        Button[] buttons = {btnExplorer, btnVoyages, btnFavoris, btnMessages, btnHotelsModule,
                btnRoomsModule, btnRoomReservationsModule, btnParametres, btnLogout};
        for (Button btn : buttons) {
            if (btn != null) {
                btn.setOnMouseEntered(e -> {
                    TranslateTransition tt = new TranslateTransition(Duration.millis(200), slidingPane);
                    tt.setToY(btn.getLayoutY());
                    tt.play();
                });
            }
        }
    }

    @FXML
    private void handleShowHotelsModule() {
        showAdminHotelsModule(AdminController3::goToHotels);
    }

    @FXML
    private void handleShowRoomsModule() {
        showAdminHotelsModule(AdminController3::goToRooms);
    }

    @FXML
    private void handleShowRoomReservationsModule() {
        showAdminHotelsModule(AdminController3::goToReservations);
    }

    private void showAdminHotelsModule(java.util.function.Consumer<AdminController3> navigator) {
        try {
            if (adminHotelModuleRoot == null || adminHotelModuleController == null) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/Admin.fxml"));
                adminHotelModuleRoot = loader.load();
                adminHotelModuleController = loader.getController();

                if (adminHotelModuleRoot instanceof StackPane moduleStack) {
                    for (javafx.scene.Node child : moduleStack.getChildren()) {
                        if (child instanceof BorderPane moduleLayout) {
                            // Keep global sidebar from adminView.fxml only.
                            moduleLayout.setLeft(null);
                            break;
                        }
                    }
                }
            }

            if (mainLayout != null) {
                mainLayout.setCenter(adminHotelModuleRoot);
            }
            if (adminHotelModuleController != null && navigator != null) {
                navigator.accept(adminHotelModuleController);
            }
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger la gestion des hotels.");
        }
    }

    private void applyCustomStyle(Alert alert) {
        DialogPane dialogPane = alert.getDialogPane();
        // On rÃ©cupÃ¨re le CSS que tu utilises dÃ©jÃ  pour ton interface admin
        String css = getClass().getResource("/css/admin.css").toExternalForm();
        dialogPane.getStylesheets().add(css);
        dialogPane.getStyleClass().add("custom-alert");
    }

    @FXML

    private void handleShowDashboard() {
        try {
            // Charge le nouveau fichier FXML du Dashboard
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Admin_Dashboard.fxml"));
            Parent dashboardView = loader.load();

            // L'insÃ¨re au centre du layout principal
            if (mainLayout != null) {
                mainLayout.setCenter(dashboardView);
            }
        } catch (IOException e) {
            System.err.println("Erreur chargement Dashboard: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleShowReservations() {
        // 1. Nehchiw kol chay fi wast Runnable bech n-garantiw el UI Thread
        javafx.application.Platform.runLater(() -> {
            try {
                // CrÃ©ation du conteneur principal
                VBox container = new VBox(25);
                container.setPadding(new Insets(40, 50, 50, 50));

                // Header
                VBox header = new VBox(5);
                Label title = new Label("Gestion des RÃ©servations");
                title.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: white;");
                Label subTitle = new Label("Suivi et modÃ©ration des rÃ©servations clients");
                subTitle.setStyle("-fx-font-size: 14px; -fx-text-fill: #679AC1;");
                header.getChildren().addAll(title, subTitle);

                // 2. CrÃ©ation de la TableView
                TableView<Reservation> table = new TableView<>();
                table.setPrefHeight(500);

                TableColumn<Reservation, String> colUser = new TableColumn<>("Username");
                colUser.setCellValueFactory(new PropertyValueFactory<>("commentaire_client"));

                TableColumn<Reservation, String> colType = new TableColumn<>("EXPÃ‰RIENCE");
                colType.setCellValueFactory(new PropertyValueFactory<>("type_res"));

                TableColumn<Reservation, String> colDate = new TableColumn<>("DATE");
                colDate.setCellValueFactory(new PropertyValueFactory<>("date_debut"));

                TableColumn<Reservation, Double> colPrix = new TableColumn<>("PRIX (DT)");
                colPrix.setCellValueFactory(new PropertyValueFactory<>("prix_total"));

                TableColumn<Reservation, Object> colStatut = new TableColumn<>("STATUT");
                colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));

                // CellFactory pour les badges
                colStatut.setCellFactory(column -> new TableCell<>() {
                    @Override
                    protected void updateItem(Object item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) setGraphic(null);
                        else {
                            String val = item.toString().toUpperCase();
                            Label badge = new Label(val);
                            badge.setPrefWidth(110);
                            badge.setAlignment(Pos.CENTER);
                            // El style el jdid: Glass effect + Border Neon + Letter Spacing
                            String style = "-fx-padding: 6 12; " +
                                    "-fx-background-radius: 10; " +
                                    "-fx-border-radius: 10; " +
                                    "-fx-border-width: 1.5; " + // Border ybayen el badge pro
                                    "-fx-font-weight: 900; " +
                                    "-fx-font-size: 10px; " +
                                    "-fx-letter-spacing: 0.5px;";

                            if (val.contains("CONFIRMEE")) {
                                // Vert Neon (Glass)
                                badge.setStyle(style + "-fx-background-color: rgba(46, 204, 113, 0.15); -fx-text-fill: #2ecc71; -fx-border-color: #2ecc71;");
                            } else if (val.contains("ATTENTE")) {
                                // Orange/Gold Sunset (Glass)
                                badge.setStyle(style + "-fx-background-color: rgba(255, 130, 16, 0.15); -fx-text-fill: #FF8210; -fx-border-color: #FF8210;");
                            } else {
                                // Rouge Passion (Glass)
                                badge.setStyle(style + "-fx-background-color: rgba(231, 76, 60, 0.15); -fx-text-fill: #e74c3c; -fx-border-color: #e74c3c;");
                            }
                            setGraphic(badge);
                        }
                    }
                });

                // Action Supprimer
                TableColumn<Reservation, Void> colAction = new TableColumn<>("ACTIONS");
                colAction.setCellFactory(param -> new TableCell<>() {
                    private final Button btnDelete = new Button("Supprimer");

                    {
                        btnDelete.setStyle(
                                "-fx-background-color: rgba(231, 76, 60, 0.15); " + // Rouge transparent
                                        "-fx-text-fill: #e74c3c; " +                      // Ktaba rouge fati7
                                        "-fx-border-color: #e74c3c; " +                   // Border rouge
                                        "-fx-border-radius: 15; " +
                                        "-fx-background-radius: 15; " +
                                        "-fx-font-weight: bold; " +
                                        "-fx-font-size: 11px; " +
                                        "-fx-cursor: hand;"
                        );

                        btnDelete.setOnAction(event -> {
                            Reservation res = getTableView().getItems().get(getIndex());
                            try {
                                reservationService.delete(res.getId().intValue());
                                handleShowReservations();
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        });
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        setGraphic(empty ? null : btnDelete);
                    }
                });

                table.getColumns().addAll(colUser, colType, colDate, colPrix, colStatut, colAction);

                // --- EL BLOCK EL HASSES ---
                // Hna IntelliJ ykhÃ¢f, donc nasta3mlou try/catch s7i7
                List<Reservation> list = reservationService.getAllReservations();
                table.setItems(FXCollections.observableArrayList(list));

                // 3. Card Styling
                VBox card = new VBox(table);
                card.getStyleClass().add("glass-card");
                card.setPadding(new Insets(15));
                container.getChildren().addAll(header, card);

                // 4. Update UI
                if (mainLayout != null) {
                    mainLayout.setCenter(container);
                }

            } catch (Exception e) {
                // Hedhi bech t-capturi el SQLException bessif
                e.printStackTrace();
            }
        });
    }

    @FXML
    private void handleShowUsers() {
        try {
            // Nesta3mlou getClass().getResource() b-path kaamel mel root mta el resources
            // Thabbet mli7 fel ism (UserTable.fxml) w thabbet f-el dossier (ghalban ykoun /tn/esprit/projet/gui/...)
            URL fxmlLocation = getClass().getResource("/UserManagement.fxml");

            if (fxmlLocation == null) {
                // Ken hna t-printi "NULL", rahou el path ghalet wala el fichier mouch f-el blasa s7i7a
                System.err.println("âŒ Erreur: Fichier UserTable.fxml introuvable!");
                return;
            }

            FXMLLoader loader = new FXMLLoader(fxmlLocation);
            Parent view = loader.load();

            if (mainLayout != null) {
                mainLayout.setCenter(view);
                System.out.println("âœ… Gestion des Utilisateurs chargÃ©e avec succÃ¨s.");
            }
        } catch (IOException e) {
            System.err.println("âŒ Erreur de chargement FXML: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void connectToChatServer() {
        try {
            if (webSocketClient != null && webSocketClient.isOpen()) return; // Connection d-dÃ©jÃ  ma7loula

            webSocketClient = new org.java_websocket.client.WebSocketClient(new java.net.URI("ws://localhost:8887")) {
                @Override public void onOpen(org.java_websocket.handshake.ServerHandshake h) { System.out.println("âœ… Admin Connected"); }

                @Override
                public void onMessage(String m) {
                    Platform.runLater(() -> {
                        if (m.equals(lastProcessedMessage)) return; // ðŸš€ Anti-Duplicate
                        lastProcessedMessage = m;

                        String[] p = m.split("\\|");
                        if (p.length < 6) return;
                        String msgClientId = p[0];

                        // âŒ NA7I saveMessageToFile(msgClientId, m); <--- HEDHI HIYA EL 3ELLA

                        if (msgClientId.equals(lastActiveClientId)) {
                            renderMessageFromHistory(m); // Juste affichage
                        }
                    });
                }
                @Override public void onClose(int i, String s, boolean b) { System.out.println("ðŸ›‘ Admin Offline"); }
                @Override public void onError(Exception e) { e.printStackTrace(); }
            };
            webSocketClient.connect();
        } catch (Exception e) { e.printStackTrace(); }
    }
    @FXML
    private void handleShowSupport() {
        try {
            // 1. El Conteneur el kbir (HBox bech n9esmouh Inbox w Chat)
            HBox supportContainer = new HBox();
            supportContainer.setStyle("-fx-background-color: transparent;");

            // --- PARTIE INBOX (Listet el clients 3al lisar) ---
            VBox inboxContainer = new VBox(15);
            inboxContainer.setPrefWidth(300);
            inboxContainer.setPadding(new Insets(20));
            inboxContainer.setStyle("-fx-background-color: rgba(255, 255, 255, 0.05); -fx-border-color: rgba(255,255,255,0.1); -fx-border-width: 0 1 0 0;");

            Label inboxTitle = new Label("Messages Clients");
            inboxTitle.setStyle("-fx-font-size: 20px; -fx-text-fill: white; -fx-font-weight: bold;");

            inboxClientList = new VBox(10);
            ScrollPane inboxScroll = new ScrollPane(inboxClientList);
            inboxScroll.setFitToWidth(true);
            inboxScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-padding: 0;");

            inboxContainer.getChildren().addAll(inboxTitle, new Separator(), inboxScroll);

            // --- PARTIE CHAT AREA (El conversation fel wast) ---
            VBox chatArea = new VBox(0);
            HBox.setHgrow(chatArea, Priority.ALWAYS); // Bech el chat yetjabed 3al kobar mte3 el window

            // Header mta3 el chat
            chatHeaderLabel = new Label("SÃ©lectionnez une discussion");
            chatHeaderLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: #FF8210; -fx-padding: 20; -fx-font-weight: bold;");

            // El blasa fin yjiw el messages
            adminChatMessageContainer = new VBox(15);
            adminChatMessageContainer.setPadding(new Insets(20));
            adminChatScrollPane = new ScrollPane(adminChatMessageContainer);
            adminChatScrollPane.setFitToWidth(true);
            VBox.setVgrow(adminChatScrollPane, Priority.ALWAYS); // El Scroll ye5ou el blasa el fergha el kol
            adminChatScrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-padding: 0;");

            // Input area (TextField + Envoyer + Supprimer)
            HBox inputArea = new HBox(10);
            inputArea.setPadding(new Insets(20));
            inputArea.setAlignment(Pos.CENTER_LEFT);
            inputArea.setStyle("-fx-background-color: rgba(0,0,0,0.2);");

            adminChatInput = new TextField();
            adminChatInput.setPromptText("Tapez votre message...");
            HBox.setHgrow(adminChatInput, Priority.ALWAYS);
            adminChatInput.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-text-fill: white; -fx-padding: 10; -fx-background-radius: 20;");

            // Bouton Envoyer (Orange)
            Button btnSend = new Button("Envoyer");
            btnSend.setStyle("-fx-background-color: #FF8210; -fx-text-fill: white; -fx-background-radius: 20; -fx-padding: 10 20; -fx-font-weight: bold; -fx-cursor: hand;");
            btnSend.setOnAction(e -> handleAdminReply());

            // Bouton Supprimer (Rouge)
            Button btnClear = new Button("Supprimer");
            btnClear.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-background-radius: 20; -fx-padding: 10 20; -fx-font-weight: bold; -fx-cursor: hand;");
            btnClear.setOnAction(e -> handleClearHistory());
            inputArea.getChildren().addAll(adminChatInput, btnSend, btnClear);
            chatArea.getChildren().addAll(chatHeaderLabel, adminChatScrollPane, inputArea);

            // Assembly
            supportContainer.getChildren().addAll(inboxContainer, chatArea);

            // Affichage fel center mta3 el BorderPane (mainLayout)
            if (mainLayout != null) {
                mainLayout.setCenter(supportContainer);
                refreshInbox(); // Load listet el clients mel base
            }

        } catch (Exception e) {
            System.err.println("âŒ Erreur fel handleShowSupport: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void refreshInbox() {
        if (inboxClientList == null) return;
        inboxClientList.getChildren().clear();

        // Query mrigla b-ism el table "utilisateur"
        // Thabbet barka ken el colonnes esmhom "id" w "nom" s7a7 f-el base
        String query = "SELECT id, nom FROM utilisateur WHERE role_id = 2";

        try (Connection conn = MyDBConnexion.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                String id = String.valueOf(rs.getInt("id"));
                String nom = rs.getString("nom");

                Button btn = new Button(nom );
                btn.setMaxWidth(Double.MAX_VALUE);

                // Style mezyn bech yji kima el sidebar
                btn.setStyle("-fx-background-color: transparent; " +
                        "-fx-text-fill: white; " +
                        "-fx-alignment: CENTER_LEFT; " +
                        "-fx-padding: 10; " +
                        "-fx-cursor: hand; " +
                        "-fx-border-color: rgba(255,255,255,0.05); " +
                        "-fx-border-width: 0 0 1 0;");

                // Hover effect sghir
                btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: rgba(255,130,16,0.1); -fx-text-fill: #FF8210; -fx-alignment: CENTER_LEFT; -fx-padding: 10;"));
                btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-alignment: CENTER_LEFT; -fx-padding: 10; -fx-border-color: rgba(255,255,255,0.05); -fx-border-width: 0 0 1 0;"));

                btn.setOnAction(e -> loadSpecificChat(id, nom));

                inboxClientList.getChildren().add(btn);
            }

            if (inboxClientList.getChildren().isEmpty()) {
                Label noClient = new Label("Aucun client trouvÃ©.");
                noClient.setStyle("-fx-text-fill: gray; -fx-padding: 10;");
                inboxClientList.getChildren().add(noClient);
            }

        } catch (SQLException e) {
            System.err.println("âŒ Erreur SQL fel refreshInbox: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadSpecificChat(String clientId, String clientName) {
        this.lastActiveClientId = clientId;
        if (chatHeaderLabel != null) chatHeaderLabel.setText("Chat avec: " + clientName);

        if (adminChatMessageContainer != null) {
            // ðŸš€ FIX: Nadhaf el UI qbal ma tloady mel fichier
            adminChatMessageContainer.getChildren().clear();

            File file = new File("chat_history_user_" + clientId + ".txt");
            if (file.exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        renderMessageFromHistory(line); // Affichi mel history
                    }
                } catch (IOException e) { e.printStackTrace(); }
            }
        }
    }

    @FXML
    private void handleAdminReply() {
        String msg = adminChatInput.getText().trim();
        if (lastActiveClientId == null || msg.isEmpty()) return;

        String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        String currentDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM"));

        // Protocol: CLIENT_ID|ROLE|NAME|CONTENT|TIME|DATE
        String payload = lastActiveClientId + "|ADMIN|Admin|" + msg + "|" + currentTime + "|" + currentDate;

        if (webSocketClient != null && webSocketClient.isOpen()) {
            webSocketClient.send(payload);
            adminChatInput.clear();
            // âœ… UI bech tet-updata wa7edha dakhil onMessage()
        }
    }
    private void addMessageToUI(String message) {
        String[] parts = message.split("\\|");
        if (parts.length < 6 || adminChatMessageContainer == null) return;

        String role = parts[1];
        String senderName = parts[2]; // L-Esm mta3 el ba3eth
        String content = parts[3];
        String time = parts[4];       // HH:mm
        String date = parts[5];       // Today wala Date s7iha

        // ðŸ’¾ Persistance (kima l-marra l-fatet)
        saveMessageToFile(parts[0], message);

        // ðŸŽ¨ UI Construction
        VBox bubbleContainer = new VBox(3);

        // ðŸ‘¤ L-Esm mel fou9 sghir
        Label nameLabel = new Label(senderName);
        nameLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #bdc3c7; -fx-font-weight: bold;");

        // ðŸ’¬ El Message
        Label msgLabel = new Label(content);
        msgLabel.setWrapText(true);
        msgLabel.setMaxWidth(300);

        // â° El Wa9t wel Date mel louta
        Label timeAndDateLabel = new Label(date + " Ã  " + time);
        timeAndDateLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: #ecf0f1; -fx-opacity: 0.6;");

        HBox row = new HBox();
        row.setPadding(new Insets(8, 0, 8, 0));

        if ("ADMIN".equals(role)) {
            row.setAlignment(Pos.CENTER_RIGHT);
            bubbleContainer.setAlignment(Pos.TOP_RIGHT);
            msgLabel.setStyle("-fx-background-color: #FF8210; -fx-text-fill: white; -fx-padding: 8 12; -fx-background-radius: 15 15 2 15;");
        } else {
            row.setAlignment(Pos.CENTER_LEFT);
            bubbleContainer.setAlignment(Pos.TOP_LEFT);
            msgLabel.setStyle("-fx-background-color: rgba(255, 255, 255, 0.1); -fx-text-fill: white; -fx-padding: 8 12; -fx-background-radius: 15 15 15 2;");
        }

        bubbleContainer.getChildren().addAll(nameLabel, msgLabel, timeAndDateLabel);
        row.getChildren().add(bubbleContainer);

        Platform.runLater(() -> adminChatMessageContainer.getChildren().add(row));
        renderMessageFromHistory(message);
    }

    private void renderMessageFromHistory(String message) {
        String[] parts = message.split("\\|");
        if (parts.length < 6 || adminChatMessageContainer == null) return;

        String role = parts[1], senderName = parts[2], content = parts[3], time = parts[4], date = parts[5];

        VBox bubbleContainer = new VBox(3);
        Label nameLabel = new Label(senderName);
        nameLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #bdc3c7; -fx-font-weight: bold;");

        Label msgLabel = new Label(content);
        msgLabel.setWrapText(true); msgLabel.setMaxWidth(300);

        Label timeAndDateLabel = new Label(date + " Ã  " + time);
        timeAndDateLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: #ecf0f1; -fx-opacity: 0.6;");

        HBox row = new HBox();
        row.setPadding(new Insets(8, 0, 8, 0));

        if ("ADMIN".equals(role)) {
            row.setAlignment(Pos.CENTER_RIGHT);
            bubbleContainer.setAlignment(Pos.TOP_RIGHT);
            msgLabel.setStyle("-fx-background-color: #FF8210; -fx-text-fill: white; -fx-padding: 8 12; -fx-background-radius: 15 15 2 15;");
        } else {
            row.setAlignment(Pos.CENTER_LEFT);
            bubbleContainer.setAlignment(Pos.TOP_LEFT);
            msgLabel.setStyle("-fx-background-color: rgba(255, 255, 255, 0.1); -fx-text-fill: white; -fx-padding: 8 12; -fx-background-radius: 15 15 15 2;");
        }

        bubbleContainer.getChildren().addAll(nameLabel, msgLabel, timeAndDateLabel);
        row.getChildren().add(bubbleContainer);
        adminChatMessageContainer.getChildren().add(row);
    }

    private void saveMessageToFile(String clientId, String rawMessage) {
        try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("chat_history_user_" + clientId + ".txt", true)))) {
            out.println(rawMessage);
        } catch (IOException e) { e.printStackTrace(); }
    }
    @FXML
    private void handleClearHistory() {
        // ðŸš€ Hedha houwa el fix: fassa5 el source mta3 el chat
        File f = new File("chat_history_user_" + lastActiveClientId + ".txt");
        if (f.exists()) {
            f.delete();
        }
        adminChatMessageContainer.getChildren().clear();
    }
}


