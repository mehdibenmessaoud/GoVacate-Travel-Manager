package tn.esprit.projet.gui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.BorderPane;
import javafx.scene.Node;
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

        // Liaison des colonnes avec les attributs de l'entité Destination
        colNom.setCellValueFactory(new PropertyValueFactory<>("nameDestination"));
        colPays.setCellValueFactory(new PropertyValueFactory<>("pays"));
        colVille.setCellValueFactory(new PropertyValueFactory<>("ville"));

        setupActionsColumn();
        loadData();
    }

    private void loadData() {
        try {
            masterData.setAll(destService.getAll());
            destTable.setItems(masterData);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void setupActionsColumn() {
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnEdit = new Button("Modifier");
            private final Button btnDelete = new Button("Supprimer");
            private final HBox container = new HBox(10, btnEdit, btnDelete);

            {
                container.setAlignment(Pos.CENTER);
                btnEdit.getStyleClass().add("btn-table-edit");
                btnDelete.getStyleClass().add("btn-table-delete");

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

            // Si on a un objet destination, on l'envoie au contrôleur du formulaire
            if (d != null) {
                DestinationFormController controller = loader.getController();
                controller.setUpdateMode(d);
            }

            // On injecte le formulaire au centre du BorderPane principal
            // On récupère le rootPane (StackPane) pour trouver le BorderPane
            BorderPane mainLayout = (BorderPane) destTable.getScene().lookup("#mainLayout");
            // Note : Assurez-vous que votre BorderPane dans adminView.fxml a l'id "mainLayout"

            if (mainLayout != null) {
                mainLayout.setCenter(formView);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleDeleteDestination(Destination d) {
        if (d == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer la destination : " + d.getNameDestination());
        confirm.setContentText("Attention : Supprimer cette destination pourrait affecter les Packs associés.");

        if (confirm.showAndWait().get() == ButtonType.OK) {
            try {
                destService.delete(d.getId());
                masterData.remove(d); // Mise à jour visuelle immédiate
                System.out.println("✅ Destination supprimée");
            } catch (SQLException e) {
                showAlert("Erreur", "Impossible de supprimer la destination : " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleSearch() {
        String query = (searchField != null) ? searchField.getText().toLowerCase().trim() : "";

        if (query.isEmpty()) {
            destTable.setItems(masterData);
            return;
        }

        // Filtrage sur le nom, le pays ou la ville
        javafx.collections.transformation.FilteredList<Destination> filteredData = new javafx.collections.transformation.FilteredList<>(masterData, d ->
                d.getNameDestination().toLowerCase().contains(query) ||
                        d.getPays().toLowerCase().contains(query) ||
                        d.getVille().toLowerCase().contains(query)
        );

        destTable.setItems(filteredData);
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}