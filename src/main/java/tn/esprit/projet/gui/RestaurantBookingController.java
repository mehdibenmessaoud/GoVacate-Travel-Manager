package tn.esprit.projet.gui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import tn.esprit.projet.entities.*;
import tn.esprit.projet.services.ReservationRestaurantServiceImpl;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import javafx.event.ActionEvent;
public class RestaurantBookingController {
    @FXML private Button btnAnnuler; // Ajoute ceci ici
    @FXML private ComboBox<Restaurant> comboResto;
    @FXML private DatePicker dateRes;
    @FXML private TextField txtPersonnes;
    @FXML private ComboBox<String> comboHeure;
    @FXML private Button btnValider;

    private final ReservationRestaurantServiceImpl serviceResto = new ReservationRestaurantServiceImpl();
    private Reservation reservationModif = null;

    @FXML
    public void initialize() {
        // 1. Charger les restaurants
        comboResto.setItems(FXCollections.observableArrayList(serviceResto.findAllRestaurants()));

        // 2. Configuration des contraintes de DATE
        setupDateConstraints();

        // 3. Configuration du TIME PICKER
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
                // Désactiver si la date est avant aujourd'hui
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

        // 1. Afficher le bouton Annuler
        if (btnAnnuler != null) {
            btnAnnuler.setVisible(true);
            btnAnnuler.setManaged(true);
        }

        // 2. Remplissage des champs du Parent (Reservation)
        dateRes.setValue(res.getDate_debut());
        // On récupère le nombre de personnes du parent
        txtPersonnes.setText(String.valueOf(res.getNombre_personnes()));

        // 3. Récupération des détails spécifiques au Restaurant
        ReservationRestaurant rr = serviceResto.findByReservationId(res.getId());
        if (rr != null) {
            // RECUPERATION CRUCIALE :
            if (rr.getNombre_personnes() > 0) {
                txtPersonnes.setText(String.valueOf(rr.getNombre_personnes()));
            }

            comboHeure.setValue(rr.getHeure_souhaitee());

            // Sélection du bon restaurant dans la ComboBox
            for (Restaurant r : comboResto.getItems()) {
                if (r.getId() == rr.getRestaurant_id().intValue()) {
                    comboResto.getSelectionModel().select(r);
                    break;
                }
            }
        }

        // 4. Changer le texte du bouton principal
        if (btnValider != null) btnValider.setText("METTRE À JOUR LA RÉSERVATION");
    }
    @FXML
    void handleCancel(ActionEvent event) {
        // Redirection directe vers la liste
        redirectToMesReservations();
    }

    @FXML
    void handleReserverRestaurant() {
        try {
            Restaurant selected = comboResto.getSelectionModel().getSelectedItem();
            String heure = comboHeure.getValue(); // On récupère la valeur du ComboBox

            // 1. Contrôle des champs
            if (selected == null || dateRes.getValue() == null || txtPersonnes.getText().isEmpty() || heure == null) {
                showAlert("Champs manquants", "Veuillez remplir tous les champs, y compris l'heure.");
                return;
            }

            if (dateRes.getValue().isBefore(LocalDate.now())) {
                showAlert("Date invalide", "Vous ne pouvez pas réserver dans le passé.");
                return;
            }

            String status = selected.getStatus().toUpperCase();
            if ("CLOSED".equals(status)) {
                showAlert("Restaurant Fermé", "Désolé, ce restaurant est fermé.");
                return;
            }

            int nbPersonnes = Integer.parseInt(txtPersonnes.getText());
            if (nbPersonnes > selected.getCapacity()) {
                showAlert("Capacité insuffisante", "Max " + selected.getCapacity() + " personnes.");
                return;
            }

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

            redirectToMesReservations();

        } catch (NumberFormatException e) {
            showAlert("Erreur", "Le nombre de personnes doit être un chiffre.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void redirectToMesReservations() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/Mes Réservations.fxml"));
            Stage stage = (Stage) dateRes.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}