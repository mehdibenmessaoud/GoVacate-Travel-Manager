package tn.esprit.projet.GUI;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import tn.esprit.projet.entities.*;
import tn.esprit.projet.services.ReservationRestaurantServiceImpl;
import java.io.IOException;
import java.time.LocalDate;

public class RestaurantBookingController {
    @FXML private Button btnAnnuler;
    @FXML private ComboBox<Restaurant> comboResto;
    @FXML private DatePicker dateRes;
    @FXML private TextField txtPersonnes;
    @FXML private ComboBox<String> comboHeure;
    @FXML private Button btnValider;

    private final ReservationRestaurantServiceImpl serviceResto = new ReservationRestaurantServiceImpl();
    private Reservation reservationModif = null;

    @FXML
    public void initialize() {
        comboResto.setItems(FXCollections.observableArrayList(serviceResto.findAllRestaurants()));
        setupDateConstraints();
        setupTimePicker();

        comboResto.setCellFactory(lv -> new ListCell<Restaurant>() {
            @Override protected void updateItem(Restaurant r, boolean empty) {
                super.updateItem(r, empty);
                if (empty || r == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(r.getName() + " (" + r.getStatus() + ")");
                    if ("CLOSED".equalsIgnoreCase(r.getStatus())) setStyle("-fx-text-fill: #FF4B5C;");
                    else if ("SUSPENDED".equalsIgnoreCase(r.getStatus())) setStyle("-fx-text-fill: #FF8210;");
                    else setStyle("-fx-text-fill: #00FFCC; -fx-font-weight: bold;");
                }
            }
        });
        comboResto.setButtonCell(comboResto.getCellFactory().call(null));
    }

    private void setupDateConstraints() {
        dateRes.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isBefore(LocalDate.now()));
                if (date.isBefore(LocalDate.now())) {
                    setStyle("-fx-background-color: #2a2a2a; -fx-text-fill: #555555;");
                }
            }
        });
        dateRes.setEditable(false);
    }

    private void setupTimePicker() {
        ObservableList<String> hours = FXCollections.observableArrayList();
        for (int h = 12; h <= 23; h++) {
            hours.add(String.format("%02d:00", h));
            hours.add(String.format("%02d:30", h));
        }
        comboHeure.setItems(hours);
    }

    public void initModif(Reservation res) {
        this.reservationModif = res;
        if (btnAnnuler != null) {
            btnAnnuler.setVisible(true);
            btnAnnuler.setManaged(true);
        }
        dateRes.setValue(res.getDate_debut());
        txtPersonnes.setText(String.valueOf(res.getNombre_personnes()));

        ReservationRestaurant rr = serviceResto.findByReservationId(res.getId());
        if (rr != null) {
            if (rr.getNombre_personnes() > 0) {
                txtPersonnes.setText(String.valueOf(rr.getNombre_personnes()));
            }
            comboHeure.setValue(rr.getHeure_souhaitee());
            for (Restaurant r : comboResto.getItems()) {
                if (r.getId() == rr.getRestaurant_id().intValue()) {
                    comboResto.getSelectionModel().select(r);
                    break;
                }
            }
        }
        if (btnValider != null) btnValider.setText("METTRE À JOUR LA RÉSERVATION");
    }

    @FXML
    void handleCancel(ActionEvent event) {
        // Retourne à la liste des réservations
        navigateToReservationList(event);
    }

    @FXML
    void handleReserverRestaurant(ActionEvent event) { // Ajout de ActionEvent ici
        try {
            Restaurant selected = comboResto.getSelectionModel().getSelectedItem();
            String heure = comboHeure.getValue();

            if (selected == null || dateRes.getValue() == null || txtPersonnes.getText().isEmpty() || heure == null) {
                showAlert("Champs manquants", "Veuillez remplir tous les champs.");
                return;
            }

            int nbPersonnes = Integer.parseInt(txtPersonnes.getText());

            Reservation resParent = (reservationModif != null) ? reservationModif : new Reservation();
            resParent.setStatut(StatutReservation.CONFIRMEE);
            resParent.setType_res("RESTAURANT");
            resParent.setDate_debut(dateRes.getValue());
            resParent.setNombre_personnes(nbPersonnes);
            resParent.setUser_id(1L);

            ReservationRestaurant rr = new ReservationRestaurant();
            rr.setRestaurant_id((long) selected.getId());
            rr.setDate_reservation(dateRes.getValue());
            rr.setHeure_souhaitee(heure);
            rr.setNombre_personnes(nbPersonnes);
            rr.setPrix(0.0);

            if (reservationModif == null) {
                serviceResto.createFullReservation(resParent, rr);
                showAlert("Succès", "Table réservée !");
            } else {
                serviceResto.updateFullReservation(resParent, rr);
                showAlert("Succès", "Mise à jour effectuée !");
            }

            // Retourne à la liste après succès
            navigateToReservationList(event);

        } catch (NumberFormatException e) {
            showAlert("Erreur", "Le nombre de personnes doit être un chiffre.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Méthode centrale pour naviguer vers "Mes Réservations" à l'intérieur du Dashboard
     */
    private void navigateToReservationList(ActionEvent event) {
        try {
            // 1. Charger la vue de la liste (Mes Réservations)
            Parent root = FXMLLoader.load(getClass().getResource("/Mes Réservations.fxml"));

            // 2. Récupérer la scène à partir du bouton qui a déclenché l'événement
            Scene scene = ((Node) event.getSource()).getScene();

            // 3. Chercher la zone centrale du Dashboard par son ID fx:id
            VBox contentArea = (VBox) scene.lookup("#clientReservationView");

            if (contentArea != null) {
                // Vider l'interface actuelle et injecter la liste
                contentArea.getChildren().clear();
                contentArea.getChildren().add(root);
            } else {
                // Si on ne trouve pas le conteneur (test ou structure différente)
                // On recharge le Dashboard complet comme solution de secours
                Parent dashboard = FXMLLoader.load(getClass().getResource("/ClientDashboard.fxml"));
                scene.setRoot(dashboard);
            }
        } catch (IOException e) {
            System.err.println("Erreur de navigation : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}