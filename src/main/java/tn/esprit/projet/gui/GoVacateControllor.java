package tn.esprit.projet.gui;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.util.Duration;
import tn.esprit.projet.entities.Reservation;
import tn.esprit.projet.services.ReservationServiceImpl;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

public class GoVacateControllor implements Initializable {

    @FXML private Pane slidingPane;
    @FXML private Button btnExplorer, btnVoyages;
    @FXML private ScrollPane explorerView;
    @FXML private VBox reservationView;
    @FXML private TableView<Reservation> tableMesReservations;
    @FXML private TableColumn<Reservation, String> colUserId, colType, colDate, colStatut;
    @FXML private TableColumn<Reservation, Double> colPrix;
    @FXML private TableColumn<Reservation, Void> colAction;
    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> comboStatut;

    private Button currentActiveBtn = null;
    private final ReservationServiceImpl reservationService = new ReservationServiceImpl();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTable();
        setupFilters();
        slidingPane.setMouseTransparent(true);

        Platform.runLater(() -> {
            if (btnExplorer != null) {
                slidingPane.setTranslateY(btnExplorer.getLayoutY());
                currentActiveBtn = btnExplorer;
            }
        });
    }

    private void setupFilters() {
        if (comboStatut != null) {
            // Utilisation d'une liste simple pour charger le combo
            comboStatut.setItems(FXCollections.observableArrayList("Tous", "EN_ATTENTE", "CONFIRMEE", "ANNULEE"));
            comboStatut.setValue("Tous");
        }
    }

    private void setupTable() {
        colUserId.setCellValueFactory(new PropertyValueFactory<>("commentaire_client"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type_res"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("date_debut"));
        colPrix.setCellValueFactory(new PropertyValueFactory<>("prix_total"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));

        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button btn = new Button("Supprimer");
            {
                btn.getStyleClass().add("btn-orange-glow");
                btn.setPrefWidth(90);
                btn.setOnAction(event -> {
                    // Utilisation de Optional pour éviter les erreurs si la ligne est vide
                    Optional.ofNullable(getTableView().getItems().get(getIndex()))
                            .ifPresent(res -> handleDelete(res));
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });
    }

    private void handleDelete(Reservation res) {

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Annuler la réservation ID: " + res.getId() + " ?", ButtonType.YES, ButtonType.NO);
        alert.setHeaderText(null);
        alert.showAndWait()
                .filter(response -> response == ButtonType.YES)
                .ifPresent(response -> {
                    reservationService.delete(res.getId());
                    showReservations(); // On rafraîchit
                });
    }

    @FXML
    private void showExplorer() {
        explorerView.setVisible(true);
        reservationView.setVisible(false);
        moveBubble(btnExplorer);
    }

    @FXML
    public void showReservations() {
        explorerView.setVisible(false);
        reservationView.setVisible(true);
        moveBubble(btnVoyages);

        // S On récupère les données et on les transforme en ObservableList proprement
        ObservableList<Reservation> data = FXCollections.observableArrayList(
                reservationService.getAllReservations().stream().toList()
        );

        FilteredList<Reservation> filteredData = new FilteredList<>(data, b -> true);

        // Listeners pour déclencher le filtrage
        txtSearch.textProperty().addListener((obs, old, nv) -> applyFilter(filteredData));
        comboStatut.valueProperty().addListener((obs, old, nv) -> applyFilter(filteredData));

        SortedList<Reservation> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tableMesReservations.comparatorProperty());
        tableMesReservations.setItems(sortedData);
    }

    private void applyFilter(FilteredList<Reservation> filteredData) {
        filteredData.setPredicate(res -> {

            String text = Optional.ofNullable(txtSearch.getText()).orElse("").toLowerCase().trim();
            String status = Optional.ofNullable(comboStatut.getValue()).orElse("Tous");

            boolean matchesText = text.isEmpty() ||
                    (res.getType_res() != null && res.getType_res().toLowerCase().contains(text)) ||
                    (res.getCommentaire_client() != null && res.getCommentaire_client().toLowerCase().contains(text));

            boolean matchesStatus = status.equals("Tous") ||
                    (res.getStatut() != null && res.getStatut().name().equals(status));

            return matchesText && matchesStatus;
        });
    }

    private void moveBubble(Button target) {
        if (currentActiveBtn == target) return;
        TranslateTransition tt = new TranslateTransition(Duration.millis(300), slidingPane);
        tt.setToY(target.getLayoutY());
        tt.setInterpolator(Interpolator.EASE_BOTH);
        tt.play();
        currentActiveBtn = target;
    }
}