package tn.esprit.projet.gui;

import javafx.animation.TranslateTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import tn.esprit.projet.entities.Pack;
import tn.esprit.projet.services.PackService;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;

public class Admin2Controller implements Initializable {
    @FXML private BorderPane mainLayout;
    @FXML private StackPane rootPane;
    @FXML private Pane slidingPane;
    @FXML private TextField searchField;

    @FXML private TableView<Pack> packTable;
    @FXML private TableColumn<Pack, String> colNom;
    @FXML private TableColumn<Pack, String> colDesc;
    @FXML private TableColumn<Pack, String> colCategorie;
    @FXML private TableColumn<Pack, Double> colPrix;
    @FXML private TableColumn<Pack, String> colStatut;
    @FXML private TableColumn<Pack, Void> colActions;

    // Suppression de colDuree, colDateDep, colDateArr car elles ne sont pas dans votre FXML

    @FXML private Button btnExplorer, btnVoyages, btnFavoris, btnMessages, btnParametres, btnLogout;

    private final PackService packService = new PackService();
    private ObservableList<Pack> masterData = FXCollections.observableArrayList();
    // À ajouter au début de la classe avec les autres attributs @FXML
    private ObservableList<Pack> packList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Liaison des colonnes (Uniquement celles présentes dans le FXML)
        colNom.setCellValueFactory(new PropertyValueFactory<>("name"));
        colDesc.setCellValueFactory(new PropertyValueFactory<>("description"));
        colCategorie.setCellValueFactory(new PropertyValueFactory<>("categorie"));
        colPrix.setCellValueFactory(new PropertyValueFactory<>("prix"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("status"));

        if (searchField != null) {
            searchField.textProperty().addListener((observable, oldValue, newValue) -> {
                handleSearch(); // Appelle la recherche à chaque changement de texte
            });
        }
        setupActionsColumn();
        loadPackData();
        setupSidebarAnimations();

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
            // On lie le tableau à masterData
            packTable.setItems(masterData);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }*/


    private void setupActionsColumn() {
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnDetails = new Button("Détails");
            private final Button btnEdit = new Button("Modifier");
            private final Button btnDelete = new Button("Supprimer");
            private final HBox container = new HBox(12, btnDetails, btnEdit, btnDelete);

            {
                container.setAlignment(Pos.CENTER);

                // Application des styles définis dans votre admin.css
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
                System.out.println("✅ Retour à la gestion des packs.");
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

        // Si le champ est vide, on réaffiche tout
        if (query.isEmpty()) {
            packTable.setItems(masterData);
            return;
        }

        // Filtrage dynamique
        FilteredList<Pack> filteredData = new FilteredList<>(masterData, p -> {
            // On vérifie le nom ou la catégorie (tu peux ajouter la description ici aussi)
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
            // On utilise la table (packTable) pour remonter jusqu'à la scène actuelle
            BorderPane layout = (BorderPane) packTable.getScene().lookup("#mainLayout");

            if (layout != null) {
                // 3. Remplacer la table des packs par le formulaire au centre
                layout.setCenter(form);
                System.out.println("✅ Formulaire chargé au centre du layout.");
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
                // Sécurité : si mainLayout est null, on tente de le trouver via la scène
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

            // Récupérer le contrôleur du formulaire et lui passer le pack
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
            showAlert("Avertissement", "Aucun pack sélectionné.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation de suppression");
        confirm.setHeaderText("Supprimer le pack : " + p.getName());
        confirm.setContentText("Voulez-vous vraiment supprimer ce pack ?");

        applyCustomStyle(confirm);

        if (confirm.showAndWait().get() == ButtonType.OK) {
            try {
                // 1. Suppression base de données
                packService.delete(p.getId());

                // 2. CORRECTION : On supprime de masterData car c'est elle qui est liée au TableView
                masterData.remove(p);

                System.out.println("✅ Pack supprimé de la base et de l'interface.");

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

            // 2. Accéder au BorderPane qui est à l'intérieur du rootPane
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
            mainLayout.setCenter(view); // Utilise le BorderPane déjà configuré
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger la vue des excursions.");
        }
    }

    private void setupSidebarAnimations() {
        if (slidingPane != null) slidingPane.setMouseTransparent(true);
        Button[] buttons = {btnExplorer, btnVoyages, btnFavoris, btnMessages, btnParametres, btnLogout};
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

    private void applyCustomStyle(Alert alert) {
        DialogPane dialogPane = alert.getDialogPane();
        // On récupère le CSS que tu utilises déjà pour ton interface admin
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

            // L'insère au centre du layout principal
            if (mainLayout != null) {
                mainLayout.setCenter(dashboardView);
            }
        } catch (IOException e) {
            System.err.println("Erreur chargement Dashboard: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleShowUsers() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/UserManagement.fxml"));
            Parent view = loader.load();
            mainLayout.setCenter(view); // mainLayout est ton BorderPane principal
        } catch (IOException e) {
            e.printStackTrace();
        }
    }






}