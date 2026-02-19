package tn.esprit.projet.gui;

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
import javafx.scene.layout.HBox;
import javafx.scene.layout.BorderPane;
import tn.esprit.projet.entities.Excursion;
import tn.esprit.projet.services.ExcursionService;
import tn.esprit.projet.utils.MyDBConnexion;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class AdminExcursionController implements Initializable {

    @FXML private TableView<Excursion> excursionTable;
    @FXML private TableColumn<Excursion, String> colNom, colActivite, colStatus;
    @FXML private TableColumn<Excursion, Double> colPrix;
    @FXML private TableColumn<Excursion, Integer> colDuree;
    @FXML private TableColumn<Excursion, Void> colActions;
    @FXML private TextField searchField;

    private ExcursionService excursionService;
    private ObservableList<Excursion> masterData = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        excursionService = new ExcursionService(MyDBConnexion.getInstance().getConnection());

        // Configuration des colonnes
        colNom.setCellValueFactory(new PropertyValueFactory<>("name"));
        colActivite.setCellValueFactory(new PropertyValueFactory<>("activite"));
        colPrix.setCellValueFactory(new PropertyValueFactory<>("price"));
        colDuree.setCellValueFactory(new PropertyValueFactory<>("duration"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        setupActionsColumn();
        loadData();
    }

    private void loadData() {
        try {
            masterData.setAll(excursionService.getAll());
            excursionTable.setItems(masterData);
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Erreur de chargement", "Impossible de récupérer les excursions depuis la base de données.");
        }
    }

    @FXML
    private void handleSearch() {
        String query = searchField.getText().toLowerCase().trim();
        if (query.isEmpty()) {
            excursionTable.setItems(masterData);
            return;
        }

        FilteredList<Excursion> filteredData = new FilteredList<>(masterData, e ->
                e.getName().toLowerCase().contains(query) ||
                        e.getActivite().toLowerCase().contains(query)
        );
        excursionTable.setItems(filteredData);
    }

    private void setupActionsColumn() {
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnDetails = new Button("Détails");
            private final Button btnEdit = new Button("Modifier");
            private final Button btnDelete = new Button("Supprimer");
            private final HBox container = new HBox(10, btnDetails, btnEdit, btnDelete);

            {
                container.setAlignment(Pos.CENTER);

                btnDetails.getStyleClass().add("btn-table-details"); // Style bleu translucide
                btnEdit.getStyleClass().add("btn-table-edit");
                btnDelete.getStyleClass().add("btn-table-delete"); // Style orange translucide

                // Style des boutons
                /*btnDetails.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-cursor: hand;");
                btnEdit.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-cursor: hand;");
                btnDelete.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-cursor: hand;");*/

                // Actions des boutons
                btnDetails.setOnAction(event -> handleViewDetails(getTableView().getItems().get(getIndex())));
                btnEdit.setOnAction(event -> handleEditExcursion(getTableView().getItems().get(getIndex())));
                btnDelete.setOnAction(event -> handleDeleteExcursion(getTableView().getItems().get(getIndex())));
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
    private void onAddExcursion() {
        loadForm(null); // Mode Ajout
    }

    private void handleEditExcursion(Excursion e) {
        loadForm(e); // Mode Modification
    }

    private void handleViewDetails(Excursion e) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ExcursionDetails.fxml"));
            Parent detailsView = loader.load();

            ExcursionDetailsController controller = loader.getController();
            controller.setExcursionData(e);

            BorderPane mainLayout = (BorderPane) excursionTable.getScene().lookup("#mainLayout");
            if (mainLayout != null) {
                mainLayout.setCenter(detailsView);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            showAlert("Erreur", "Impossible de charger la vue détaillée (ExcursionDetails.fxml).");
        }
    }

    private void loadForm(Excursion e) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ExcursionForm.fxml"));
            Parent formView = loader.load();

            if (e != null) {
                ExcursionFormController controller = loader.getController();
                controller.setUpdateMode(e);
            }

            BorderPane mainLayout = (BorderPane) excursionTable.getScene().lookup("#mainLayout");
            if (mainLayout != null) {
                mainLayout.setCenter(formView);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            showAlert("Erreur", "Impossible de charger le formulaire (ExcursionForm.fxml).");
        }
    }

    private void handleDeleteExcursion(Excursion e) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation de suppression");
        confirm.setHeaderText("Supprimer l'excursion : " + e.getName());
        confirm.setContentText("Voulez-vous vraiment supprimer cette activité ?");

        if (confirm.showAndWait().get() == ButtonType.OK) {
            try {
                excursionService.delete(e.getId());
                masterData.remove(e); // Mise à jour dynamique de la liste
            } catch (SQLException ex) {
                ex.printStackTrace();
                showAlert("Erreur SQL", "Une erreur est survenue lors de la suppression.");
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