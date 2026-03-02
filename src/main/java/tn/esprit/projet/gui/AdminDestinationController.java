package tn.esprit.projet.gui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList; // Pour la recherche dynamique
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.BorderPane;
import tn.esprit.projet.entities.Destination;
import tn.esprit.projet.services.DestinationService;
import tn.esprit.projet.utils.MyDBConnexion;



import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class AdminDestinationController implements Initializable {

    @FXML private TableView<Destination> destTable;
    @FXML private TableColumn<Destination, String> colNom, colPays, colVille;
    @FXML private TableColumn<Destination, Void> colActions;
    @FXML private TextField searchField;

    private DestinationService destService;
    private ObservableList<Destination> masterData = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        destService = new DestinationService(MyDBConnexion.getInstance().getConnection());

        // 1. Liaison des colonnes
        colNom.setCellValueFactory(new PropertyValueFactory<>("nameDestination"));
        colPays.setCellValueFactory(new PropertyValueFactory<>("pays"));
        colVille.setCellValueFactory(new PropertyValueFactory<>("ville"));

        // 2. Configuration des boutons d'actions
        setupActionsColumn();

        // 3. Chargement initial des données
        loadData();

        // 4. Mise en place de la recherche dynamique (Temps réel)
        setupDynamicSearch();
    }

    private void loadData() {
        try {
            masterData.setAll(destService.getAll());
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger les données : " + e.getMessage());
        }
    }

    private void setupDynamicSearch() {
        // Créer une liste filtrée basée sur masterData
        FilteredList<Destination> filteredData = new FilteredList<>(masterData, p -> true);

        // Ajouter un écouteur sur le champ de recherche
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(destination -> {
                // Si le champ est vide, on affiche tout
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }

                String lowerCaseFilter = newValue.toLowerCase().trim();

                // Recherche multi-critères (Nom, Pays ou Ville)
                if (destination.getNameDestination().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                } else if (destination.getPays().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                } else if (destination.getVille().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                }

                return false; // Aucune correspondance
            });
        });

        // Lier la liste filtrée à la TableView
        destTable.setItems(filteredData);
    }

    private void setupActionsColumn() {
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnEdit = new Button("Modifier");
            private final Button btnDelete = new Button("Supprimer");
            private final HBox container = new HBox(10, btnEdit, btnDelete);

            {
                container.setAlignment(Pos.CENTER);

                // Style homogène avec admin.css
                btnEdit.getStyleClass().add("btn-table-edit");
                btnDelete.getStyleClass().add("btn-table-delete"); // Style orange/rouge corrigé

                btnEdit.setOnAction(event -> handleEditDestination(getTableView().getItems().get(getIndex())));
                btnDelete.setOnAction(event -> handleDeleteDestination(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : container);
            }
        });
    }

    @FXML
    private void onAddDestination() {
        loadForm(null); // Mode Ajout
    }

    private void handleEditDestination(Destination d) {
        loadForm(d); // Mode Modification
    }

    private void loadForm(Destination d) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/DestinationForm.fxml"));
            Parent formView = loader.load();

            if (d != null) {
                DestinationFormController controller = loader.getController();
                controller.setUpdateMode(d);
            }

            // Navigation sécurisée via lookup du mainLayout
            BorderPane mainLayout = (BorderPane) destTable.getScene().lookup("#mainLayout");

            if (mainLayout != null) {
                mainLayout.setCenter(formView);
            } else {
                System.err.println("Erreur: mainLayout non trouvé dans la scène.");
            }
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Erreur lors du chargement du formulaire.");
        }
    }

    private void handleDeleteDestination(Destination d) {
        if (d == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation de suppression");
        confirm.setHeaderText("Supprimer la destination : " + d.getNameDestination());
        confirm.setContentText("Voulez-vous vraiment supprimer cette destination ?");

        if (confirm.showAndWait().get() == ButtonType.OK) {
            try {
                destService.delete(d.getId());
                masterData.remove(d); // Mise à jour dynamique de la liste
                System.out.println("✅ Destination supprimée avec succès.");
            } catch (SQLException e) {
                showAlert("Erreur", "Échec de la suppression : " + e.getMessage());
            }
        }
    }



    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}