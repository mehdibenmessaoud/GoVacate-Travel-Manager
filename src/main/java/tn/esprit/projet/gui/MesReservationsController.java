package tn.esprit.projet.gui;

import javafx.animation.FadeTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import tn.esprit.projet.entities.Reservation;
import tn.esprit.projet.entities.ReservationRestaurant;
import tn.esprit.projet.entities.ReservationExcursion;
import tn.esprit.projet.services.ReservationServiceImpl;
import tn.esprit.projet.services.ReservationRestaurantServiceImpl;
import tn.esprit.projet.services.ReservationExcursionServiceImpl;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

public class MesReservationsController {

    @FXML private TableView<Reservation> tableMesReservations;
    @FXML private TableColumn<Reservation, String> colType;
    @FXML private TableColumn<Reservation, LocalDate> colDate;
    @FXML private TableColumn<Reservation, Double> colPrix;
    @FXML private TableColumn<Reservation, Object> colStatut;
    @FXML private TableColumn<Reservation, Void> colAction;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilterCombo;

    private final ReservationServiceImpl service = new ReservationServiceImpl();
    private final ReservationRestaurantServiceImpl serviceResto = new ReservationRestaurantServiceImpl();
    private final ReservationExcursionServiceImpl serviceExc = new ReservationExcursionServiceImpl();
    private final ObservableList<Reservation> masterData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        if (tableMesReservations == null) return;

        setupColumns();

        // Initialisation des filtres
        statusFilterCombo.getItems().addAll("Tous les statuts", "CONFIRMEE", "EN_ATTENTE", "ANNULEE");
        statusFilterCombo.setValue("Tous les statuts");

        chargerDonnees();
        setupFilterLogic();

        // Animation d'entrée
        FadeTransition ft = new FadeTransition(Duration.millis(800), tableMesReservations);
        ft.setFromValue(0); ft.setToValue(1); ft.play();
    }

    private void setupColumns() {
        colType.setCellValueFactory(new PropertyValueFactory<>("type_res"));
        colPrix.setCellValueFactory(new PropertyValueFactory<>("prix_total"));


        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
        colStatut.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setGraphic(null);
                else {
                    String status = item.toString().toUpperCase();
                    Label badge = new Label(status);
                    badge.getStyleClass().add("status-badge");

                    if (status.contains("CONFIRME") || status.contains("DISPONIBLE")) {
                        badge.setStyle("-fx-background-color: rgba(74, 222, 128, 0.2); -fx-text-fill: #4ADE80; -fx-padding: 5 12; -fx-background-radius: 10;");
                    } else if (status.contains("ATTENTE") || status.contains("OCCUPÉ")) {
                        badge.setStyle("-fx-background-color: rgba(251, 146, 60, 0.2); -fx-text-fill: #FB923C; -fx-padding: 5 12; -fx-background-radius: 10;");
                    } else {
                        badge.setStyle("-fx-background-color: rgba(148, 163, 184, 0.2); -fx-text-fill: #94A3B8; -fx-padding: 5 12; -fx-background-radius: 10;");
                    }
                    setGraphic(badge);
                    setAlignment(Pos.CENTER);
                }
            }
        });

        // --- DATE ET HEURE
        colDate.setCellValueFactory(new PropertyValueFactory<>("date_debut"));
        colDate.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (empty || date == null) setText(null);
                else {
                    Reservation res = getTableView().getItems().get(getIndex());
                    String heure = "";

                    if ("RESTAURANT".equalsIgnoreCase(res.getType_res())) {
                        ReservationRestaurant rr = serviceResto.findByReservationId(res.getId());
                        heure = (rr != null && rr.getHeure_souhaitee() != null) ? rr.getHeure_souhaitee() : "20:00";
                    } else if ("EXCURSION".equalsIgnoreCase(res.getType_res())) {
                        ReservationExcursion re = serviceExc.findByReservationId(res.getId());
                        heure = (re != null && re.getHeure_souhaitee() != null) ? re.getHeure_souhaitee() : "09:00";
                    }

                    setText(date + (heure.isEmpty() ? "" : " à " + heure));
                    setStyle("-fx-text-fill: #00FFCC; -fx-font-weight: bold;");
                }
            }
        });

        setupActionColumn();
    }

    private void setupActionColumn() {
        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button btnModifier = new Button("Modifier");
            private final Button btnAnnuler = new Button("Supprimer");
            private final HBox pane = new HBox(10, btnModifier, btnAnnuler);
            {
                btnModifier.setStyle("-fx-background-color: #FF8210; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 5;");
                btnAnnuler.setStyle("-fx-background-color: #FF4B5C; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 5;");
                pane.setAlignment(Pos.CENTER);
                btnModifier.setOnAction(e -> handleModifierAction(getTableView().getItems().get(getIndex())));
                btnAnnuler.setOnAction(e -> confirmerAnnulation(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setGraphic(null);
                else {
                    Reservation res = getTableView().getItems().get(getIndex());
                    pane.getChildren().clear();
                    if ("PACK".equalsIgnoreCase(res.getType_res())) pane.getChildren().add(btnAnnuler);
                    else pane.getChildren().addAll(btnModifier, btnAnnuler);
                    setGraphic(pane);
                }
            }
        });
    }

    private void chargerDonnees() {
        try {
            List<Reservation> reservations = service.getAllReservations();
            if (reservations != null) masterData.setAll(reservations);
        } catch (Exception e) { System.err.println("Error: " + e.getMessage()); }
    }

    private void setupFilterLogic() {
        FilteredList<Reservation> filteredData = new FilteredList<>(masterData, p -> true);
        searchField.textProperty().addListener((obs, old, nv) -> applyFilters(filteredData));
        statusFilterCombo.valueProperty().addListener((obs, old, nv) -> applyFilters(filteredData));

        SortedList<Reservation> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tableMesReservations.comparatorProperty());
        tableMesReservations.setItems(sortedData);
    }

    private void applyFilters(FilteredList<Reservation> filteredData) {
        filteredData.setPredicate(res -> {
            String searchText = searchField.getText() == null ? "" : searchField.getText().toLowerCase();
            String selectedStatus = statusFilterCombo.getValue();
            boolean matchesType = res.getType_res().toLowerCase().contains(searchText);
            boolean matchesStatus = (selectedStatus == null || selectedStatus.equals("Tous les statuts") || res.getStatut().toString().equals(selectedStatus));
            return matchesType && matchesStatus;
        });
    }

    private void handleModifierAction(Reservation res) {
        try {
            String fxmlPath = res.getType_res().equalsIgnoreCase("RESTAURANT") ? "/RestaurantBookingView.fxml" : "/ExcursionBooking.fxml";
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            if (res.getType_res().equalsIgnoreCase("RESTAURANT")) {
                RestaurantBookingController c = loader.getController();
                c.initModif(res);
            } else {
                ExcursionBookingController c = loader.getController();
                c.initModif(res);
            }

            Stage stage = (Stage) tableMesReservations.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (Exception e) { showAlert("Erreur", "Impossible de modifier : " + e.getMessage()); }
    }

    private void confirmerAnnulation(Reservation res) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Annuler la réservation #" + res.getId() + " ?", ButtonType.YES, ButtonType.NO);
        alert.showAndWait().ifPresent(r -> {
            if (r == ButtonType.YES) {
                service.delete(res.getId());
                chargerDonnees();
            }
        });
    }

    @FXML void handlePayerSelection() {
        if (tableMesReservations.getSelectionModel().getSelectedItem() == null) {
            showAlert("Attention", "Veuillez sélectionner une réservation pour payer.");
        } else {
            showAlert("Paiement", "Redirection vers la plateforme de paiement sécurisée...");
        }
    }

    @FXML void handleBack(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/RestaurantBookingView.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void showAlert(String t, String c) {
        Alert a = new Alert(Alert.AlertType.INFORMATION); a.setTitle(t); a.setContentText(c); a.showAndWait();
    }
}