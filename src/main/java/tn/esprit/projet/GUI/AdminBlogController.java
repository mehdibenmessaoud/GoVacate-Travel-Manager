package tn.esprit.projet.GUI;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.util.Callback;
import tn.esprit.projet.models.Post;
import tn.esprit.projet.services.BlogueService;

import java.sql.SQLException;
import java.util.Optional;

public class AdminBlogController {

    @FXML private TableView<Post> tableBlogs;
    @FXML private TableColumn<Post, Integer> colId;
    @FXML private TableColumn<Post, String> colTitre;
    @FXML private TableColumn<Post, String> colContenu;
    @FXML private TableColumn<Post, String> colDate;
    @FXML private TableColumn<Post, String> colStatus;
    @FXML private TableColumn<Post, Void> colAction; // Colonne pour le bouton

    private final BlogueService service = new BlogueService();
    private ObservableList<Post> dataList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupTable();
        loadData();
    }

    private void setupTable() {
        // Liaison des colonnes avec les attributs de Post
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colTitre.setCellValueFactory(new PropertyValueFactory<>("title"));
        colContenu.setCellValueFactory(new PropertyValueFactory<>("content"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        // Pour la date, on la convertit en String pour l'affichage
        colDate.setCellValueFactory(cellData -> {
            if (cellData.getValue().getCreatedat() != null)
                return new SimpleStringProperty(cellData.getValue().getCreatedat().toString());
            return new SimpleStringProperty("");
        });

        // AJOUT DU BOUTON SUPPRIMER DANS LA COLONNE ACTION
        addButtonToTable();
    }

    private void loadData() {
        try {
            dataList.clear();
            // On utilise un Post vide car selectAll(Post t) le demande dans votre interface,
            // mais selectAll n'utilise pas forcément le paramètre.
            dataList.addAll(service.selectAll(new Post()));
            tableBlogs.setItems(dataList);
        } catch (SQLException e) {
            System.err.println("Erreur chargement : " + e.getMessage());
        }
    }

    @FXML
    private void refreshTable() {
        loadData();
    }

    private void addButtonToTable() {
        Callback<TableColumn<Post, Void>, TableCell<Post, Void>> cellFactory = new Callback<>() {
            @Override
            public TableCell<Post, Void> call(final TableColumn<Post, Void> param) {
                return new TableCell<>() {

                    private final Button btnDelete = new Button("Supprimer");

                    {
                        btnDelete.setStyle("-fx-background-color: #ff4d4d; -fx-text-fill: white;");

                        btnDelete.setOnAction((event) -> {
                            // 1. Récupérer le post de la ligne actuelle
                            Post data = getTableView().getItems().get(getIndex());

                            // 2. Demander confirmation
                            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                            alert.setTitle("Confirmation");
                            alert.setHeaderText("Supprimer le blog : " + data.getTitle() + " ?");
                            alert.setContentText("Cela supprimera aussi les images (Cascade).");

                            Optional<ButtonType> result = alert.showAndWait();
                            if (result.isPresent() && result.get() == ButtonType.OK) {
                                try {
                                    // 3. Appel du service DELETE (Votre méthode existante)
                                    service.delete(data);

                                    // 4. Mettre à jour le tableau sans recharger la BDD (plus fluide)
                                    tableBlogs.getItems().remove(data);

                                } catch (SQLException e) {
                                    Alert err = new Alert(Alert.AlertType.ERROR, "Erreur SQL : " + e.getMessage());
                                    err.show();
                                }
                            }
                        });
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            HBox container = new HBox(btnDelete);
                            container.setStyle("-fx-alignment:CENTER"); // Centrer le bouton
                            setGraphic(container);
                        }
                    }
                };
            }
        };

        colAction.setCellFactory(cellFactory);
    }
}