package tn.esprit.projet.gui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList; // Import essentiel
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.BorderPane;
import org.json.JSONObject;
import tn.esprit.projet.entities.Excursion;
import tn.esprit.projet.services.ExcursionService;
import tn.esprit.projet.services.WeatherService;
import tn.esprit.projet.utils.MyDBConnexion;

import javafx.application.Platform;
import javafx.scene.image.Image;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ResourceBundle;

public class AdminExcursionController implements Initializable {

    @FXML private TableView<Excursion> excursionTable;
    @FXML private TableColumn<Excursion, String> colNom, colActivite, colStatus;
    @FXML private TableColumn<Excursion, Double> colPrix;
    @FXML private TableColumn<Excursion, Integer> colDuree;
    @FXML private TableColumn<Excursion, Void> colActions;
    @FXML private TextField searchField;

    @FXML private Label tempLabel;
    @FXML private Label descLabel;
    @FXML private ImageView weatherIcon;

    private ExcursionService excursionService;
    private ObservableList<Excursion> masterData = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        excursionService = new ExcursionService(MyDBConnexion.getInstance().getConnection());

        // 1. Configuration des colonnes
        colNom.setCellValueFactory(new PropertyValueFactory<>("name"));
        colActivite.setCellValueFactory(new PropertyValueFactory<>("activite"));
        colPrix.setCellValueFactory(new PropertyValueFactory<>("price"));
        colDuree.setCellValueFactory(new PropertyValueFactory<>("duration"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        // 2. Configuration des boutons d'actions (Modifier, Supprimer, Détails)
        setupActionsColumn();

        // 3. Chargement initial des données
        loadData();

        // 4. Mise en place de la recherche dynamique (Temps réel)
        setupDynamicSearch();
    }

    private void loadData() {
        try {
            masterData.setAll(excursionService.getAll());
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Erreur de chargement", "Impossible de récupérer les excursions.");
        }
    }

    private void setupDynamicSearch() {
        // On crée une FilteredList basée sur masterData
        FilteredList<Excursion> filteredData = new FilteredList<>(masterData, p -> true);

        // On écoute chaque changement de texte dans le searchField
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(excursion -> {
                // Si le texte est vide, on affiche tout
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }

                String lowerCaseFilter = newValue.toLowerCase().trim();

                // Filtrage sur le Nom ou l'Activité
                if (excursion.getName().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                } else if (excursion.getActivite().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                } else if (excursion.getStatus().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                }

                return false; // Aucune correspondance
            });
        });

        // Très important : On lie la liste filtrée à la TableView
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

                // Application des styles homogènes définis dans admin.css
                btnDetails.getStyleClass().add("btn-table-details");
                btnEdit.getStyleClass().add("btn-table-edit");
                btnDelete.getStyleClass().add("btn-table-delete");

                btnDetails.setOnAction(event -> handleViewDetails(getTableView().getItems().get(getIndex())));
                btnEdit.setOnAction(event -> handleEditExcursion(getTableView().getItems().get(getIndex())));
                btnDelete.setOnAction(event -> handleDeleteExcursion(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : container);
            }
        });
    }

    // --- Méthodes de Navigation ---

    @FXML
    private void onAddExcursion() {
        loadForm(null);
    }

    private void handleEditExcursion(Excursion e) {
        loadForm(e);
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
            showAlert("Erreur", "Impossible de charger la vue détaillée.");
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
            showAlert("Erreur", "Impossible de charger le formulaire.");
        }
    }

    private void handleDeleteExcursion(Excursion e) {
        if (e == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation de suppression");
        confirm.setHeaderText("Supprimer l'excursion : " + e.getName());
        confirm.setContentText("Voulez-vous vraiment supprimer cette activité ?");

        if (confirm.showAndWait().get() == ButtonType.OK) {
            try {
                excursionService.delete(e.getId());
                masterData.remove(e);
            } catch (SQLException ex) {
                ex.printStackTrace();
                showAlert("Erreur SQL", "Une erreur est survenue lors de la suppression.");
            }
        }
    }
    public void afficherMeteo(String nomVille) {
        if (nomVille == null || nomVille.isEmpty()) return;

        new Thread(() -> {
            try {
                // Appel au service (utilise ta clé : c4f8cae49ccd9bd1619c99287b054b9e)
                JSONObject data = WeatherService.getWeatherByCity(nomVille);

                if (data != null) {
                    double temp = data.getJSONObject("main").getDouble("temp");
                    String desc = data.getJSONArray("weather").getJSONObject(0).getString("description");
                    String iconCode = data.getJSONArray("weather").getJSONObject(0).getString("icon");
                    String iconUrl = "https://openweathermap.org/img/wn/" + iconCode + "@2x.png";

                    Platform.runLater(() -> {
                        if (tempLabel != null) tempLabel.setText(String.format("%.1f°C", temp));
                        if (descLabel != null) descLabel.setText(desc.substring(0, 1).toUpperCase() + desc.substring(1));
                        if (weatherIcon != null) weatherIcon.setImage(new Image(iconUrl));
                    });
                }
            } catch (Exception e) {
                System.err.println("Erreur lors de la récupération météo : " + e.getMessage());
            }
        }).start();
    }



    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}