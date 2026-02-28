package tn.esprit.projet.gui;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import tn.esprit.projet.entities.Pack;
import tn.esprit.projet.services.PackService;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.Stage;
import tn.esprit.projet.utils.SceneManager;
import tn.esprit.projet.utils.SessionManager;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class ClientPackController implements Initializable {

    @FXML private StackPane rootPane;
    @FXML private FlowPane packsContainer;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> categoryFilter;
    @FXML private ScrollPane mainContentArea;
    @FXML private Button btnExplorer, btnExplorerDest;
    @FXML private VBox packContainer;
    @FXML private Pane monPane;

    private final PackService sp = new PackService();
    private List<Pack> allPacks;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        SceneManager.setClientContentPane(rootPane);
        categoryFilter.setItems(FXCollections.observableArrayList("Toutes", "Individual", "Couple", "Familialle"));
        categoryFilter.setValue("Toutes");

        searchField.textProperty().addListener((observable, oldValue, newValue) -> handleSearch());
        categoryFilter.valueProperty().addListener((observable, oldValue, newValue) -> handleSearch());

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

        Rectangle clip = new Rectangle(280, 160);
        clip.setArcWidth(40);
        clip.setArcHeight(40);
        imageView.setClip(clip);
        imageHeader.getChildren().add(imageView);

        VBox content = new VBox(10);
        content.getStyleClass().add("glass-card");
        content.setPadding(new javafx.geometry.Insets(15));

        Label category = new Label(p.getCategorie().toUpperCase());
        category.getStyleClass().add("card-sub");

        Label name = new Label(p.getName());
        name.getStyleClass().add("card-title");

        Label price = new Label(p.getPrix() + " DT");
        price.setStyle("-fx-text-fill: #FF8210; -fx-font-size: 18px; -fx-font-weight: bold;");

        HBox actionBox = new HBox(10);
        actionBox.setAlignment(Pos.CENTER_LEFT);

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
        btnDetails.setOnAction(event -> showPackDetails(p));

        Button btnBook = new Button("Réserver");
        btnBook.getStyleClass().add("btn-reserve-small");

        if (!currentStatus.equals("disponible")) {
            btnBook.setDisable(true);
        } else {
            btnBook.setOnAction(event -> handleBookPack(p));
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        actionBox.getChildren().addAll(statusBadge, spacer, btnDetails, btnBook);
        content.getChildren().addAll(category, name, price, actionBox);
        card.getChildren().addAll(imageHeader, content);

        return card;
    }

    // --- FIX: Location is not set solved here ---
    private void handleBookPack(Pack selectedPack) {
        try {
            // Utilise le path complet depuis les ressources
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/reservationpackform.fxml"));
            Parent bookingView = loader.load();

            PackBookingController controller = loader.getController();
            controller.setPackDataFromEntity(selectedPack);

            BorderPane mainLayout = (BorderPane) rootPane.getScene().lookup("#mainLayout");
            if (mainLayout != null) {
                mainLayout.setCenter(bookingView);
            } else {
                rootPane.getChildren().setAll(bookingView);
            }
        } catch (IOException e) {
            System.err.println("❌ Erreur: Fichier reservationpackform.fxml introuvable. Thabbet fel path!");
            e.printStackTrace();
        }
    }

    private void showPackDetails(Pack p) {
        try {
            // FIX: Toujours ajouter / devant le nom si le fichier est à la racine des ressources
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/PackDetails.fxml"));
            Parent detailsView = loader.load();
            PackDetailsController controller = loader.getController();
            controller.setPackData(p);

            BorderPane mainLayout = (BorderPane) rootPane.getScene().lookup("#mainLayout");
            if (mainLayout != null) {
                mainLayout.setCenter(detailsView);
            }
        } catch (IOException e) {
            System.err.println("❌ Erreur: Fichier PackDetails.fxml introuvable.");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleSearch() {
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

    @FXML
    private void handleShowPacks(ActionEvent event) {
        SceneManager.loadClientContent("/PacksContent.fxml");
    }

    @FXML
    private void handleShowDestinations() {
        SceneManager.loadClientContent("/ClientDestinationView.fxml");
    }

    @FXML
    private void openAIChat() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ChatAI.fxml"));
            Parent root = loader.load();
            Stage chatStage = new Stage();
            chatStage.setTitle("Assistant GoVacate");
            chatStage.setScene(new Scene(root));
            chatStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleShowMesReservations() {
        try {
            // FIX: Si le fichier est dans le package gui
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Mes Réservations.fxml"));
            Parent view = loader.load();

            BorderPane mainLayout = (BorderPane) rootPane.getScene().lookup("#mainLayout");
            if (mainLayout != null) {
                mainLayout.setCenter(view);
            }
        } catch (IOException e) {
            System.err.println("❌ Erreur chargement MesReservations: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML private void handleProfileClick() { SceneManager.loadClientContent("/Profile.fxml"); }
    @FXML private void handleLogout() { SessionManager.clearSession(); SceneManager.switchTo("/Auth.fxml"); }
    @FXML private void handleDashboard(ActionEvent event) { SceneManager.loadClientContent("/ClientDashboard.fxml"); }
}