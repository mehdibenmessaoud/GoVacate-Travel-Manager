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
import javafx.scene.layout.BorderPane;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import java.io.IOException;

import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.fxml.FXMLLoader;
import java.io.IOException;


import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
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

    @FXML private Button btnExplorer; // Bouton "Explorer les Packs"
    @FXML private Button btnExplorerDest; // Bouton "Explorer les Destinations"

    @FXML private VBox packContainer;
    private final PackService sp = new PackService();
    private List<Pack> allPacks;

    @Override

    public void initialize(URL location, ResourceBundle resources) {
        // 1. Initialisation des filtres
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



}