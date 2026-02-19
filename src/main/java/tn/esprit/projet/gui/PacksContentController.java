package tn.esprit.projet.gui;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import tn.esprit.projet.entities.Pack;
import tn.esprit.projet.services.PackService;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class PacksContentController implements Initializable {

    @FXML private FlowPane packsContainer;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> categoryFilter;
    @FXML private ScrollPane mainContentArea;

    private final PackService sp = new PackService();
    private List<Pack> allPacks;


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 1. Initialisation des filtres
        categoryFilter.setItems(FXCollections.observableArrayList("Toutes", "Individual", "Couple", "Familialle"));
        categoryFilter.setValue("Toutes");

        // 2. RENDRE LA RECHERCHE DYNAMIQUE
        // Écoute chaque lettre tapée
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            handleSearch();
        });

        // Écoute le changement de catégorie
        categoryFilter.valueProperty().addListener((observable, oldValue, newValue) -> {
            handleSearch();
        });

        // 3. Chargement initial des données
        loadPacks();
    }

    private void loadPacks() {
        try {
            allPacks = sp.getAll();
            displayPacks(allPacks);
        } catch (Exception e) {
            System.err.println("Erreur chargement packs: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void displayPacks(List<Pack> packs) {
        packsContainer.getChildren().clear();
        for (Pack p : packs) {
            packsContainer.getChildren().add(createPackCard(p));
        }
    }

    /**
     * Crée dynamiquement une carte visuelle pour chaque pack
     */
    private VBox createPackCard(Pack p) {
        VBox card = new VBox();
        card.getStyleClass().add("water-card");
        card.setPrefWidth(280);
        card.setSpacing(0);

        // --- Image du Pack ---
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

        // Coins arrondis pour l'image
        Rectangle clip = new Rectangle(280, 160);
        clip.setArcWidth(40);
        clip.setArcHeight(40);
        imageView.setClip(clip);

        imageHeader.getChildren().add(imageView);

        // --- Contenu texte ---
        VBox content = new VBox(10);
        content.getStyleClass().add("glass-card");
        content.setPadding(new Insets(15));

        Label category = new Label(p.getCategorie().toUpperCase());
        category.getStyleClass().add("card-sub");

        Label name = new Label(p.getName());
        name.getStyleClass().add("card-title");

        Label price = new Label(p.getPrix() + " DT");
        price.setStyle("-fx-text-fill: #FF8210; -fx-font-size: 18px; -fx-font-weight: bold;");

        // --- Ligne d'actions (Statut, Détails, Réserver) ---
        HBox actionBox = new HBox(10);
        actionBox.setAlignment(Pos.CENTER_LEFT);

        Label statusBadge = new Label();
        statusBadge.getStyleClass().add("status-badge");
        String currentStatus = p.getStatus() != null ? p.getStatus().toLowerCase() : "";

        if (currentStatus.contains("disponible") && !currentStatus.contains("non")) {
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
        if (statusBadge.getText().equals("Indisponible")) btnBook.setDisable(true);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        actionBox.getChildren().addAll(statusBadge, spacer, btnDetails, btnBook);
        content.getChildren().addAll(category, name, price, actionBox);
        card.getChildren().addAll(imageHeader, content);

        return card;
    }

    /**
     * Affiche l'interface de détails dans la zone centrale
     */
    private void showPackDetails(Pack p) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/PackDetails.fxml"));
            Parent detailsView = loader.load();

            PackDetailsController controller = loader.getController();
            controller.setPackData(p);

            // On récupère le BorderPane principal via la scène
            BorderPane mainLayout = (BorderPane) packsContainer.getScene().lookup("#mainLayout");

            if (mainLayout != null) {
                mainLayout.setCenter(detailsView);
            }
        } catch (IOException e) {
            System.err.println("Erreur chargement détails : " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Filtre la liste des packs selon la recherche et la catégorie
     */

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
}