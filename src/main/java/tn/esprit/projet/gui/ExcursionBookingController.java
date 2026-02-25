package tn.esprit.projet.gui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import tn.esprit.projet.entities.Excursion;
import tn.esprit.projet.entities.Reservation;
import tn.esprit.projet.entities.ReservationExcursion;
import tn.esprit.projet.entities.StatutReservation;
import tn.esprit.projet.services.ReservationExcursionServiceImpl;

import java.io.IOException;
import java.time.LocalDate;
import javafx.scene.layout.VBox;
public class ExcursionBookingController {

    @FXML private DatePicker dateExc;
    @FXML private TextField txtPersonnes;
    @FXML private TextField txtPrixUnitaire;
    @FXML private ComboBox<String> comboHeure;
    @FXML private Button btnValider;
    @FXML private Button btnAnnuler;

    private final ReservationExcursionServiceImpl service = new ReservationExcursionServiceImpl();
    private Reservation reservationModif = null;

    @FXML
    public void initialize() {
        // Remplir le combo des heures
        ObservableList<String> hours = FXCollections.observableArrayList();
        for (int h = 8; h <= 19; h++) {
            hours.add(String.format("%02d:00", h));
            hours.add(String.format("%02d:30", h));
        }
        comboHeure.setItems(hours);

        // Bloquer les dates passées
        dateExc.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isBefore(LocalDate.now()));
                if (date.isBefore(LocalDate.now())) {
                    setStyle("-fx-background-color: #2a2a2a; -fx-text-fill: #555555;");
                }
            }
        });


        try {
            Excursion exc = service.findExcursionById(1);
            if (exc != null) {
                txtPrixUnitaire.setText(String.valueOf(exc.getPrice()));
            }
        } catch (Exception e) {
            System.err.println("Erreur chargement prix: " + e.getMessage());
        }
    }

    public void initModif(Reservation res) {
        this.reservationModif = res;

        // Afficher le bouton annuler
        if (btnAnnuler != null) {
            btnAnnuler.setVisible(true);
            btnAnnuler.setManaged(true);
        }

        if (res.getDate_debut() != null) {
            dateExc.setValue(res.getDate_debut());
        }

        int nb = res.getNombre_personnes();

        try {
            ReservationExcursion re = service.findByReservationId(res.getId());
            if (re != null) {
                if (nb <= 0) nb = re.getNombre_personnes();

                // Sélectionner l'heure enregistrée
                comboHeure.setValue(re.getHeure_souhaitee());

                if (nb > 0) {
                    txtPrixUnitaire.setText(String.format("%.2f", re.getPrix() / nb));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        txtPersonnes.setText(String.valueOf(nb));
        if (btnValider != null) btnValider.setText("METTRE À JOUR");
    }

    @FXML
    void handleReserverExcursion(ActionEvent event) {
        try {
            Excursion exc = service.findExcursionById(1);
            String heure = comboHeure.getValue();

            if (dateExc.getValue() == null || txtPersonnes.getText().isEmpty() || heure == null) {
                showAlert("Champs manquants", "Veuillez remplir la date, l'heure et le nombre de personnes.");
                return;
            }

            int nb = Integer.parseInt(txtPersonnes.getText());
            LocalDate date = dateExc.getValue();

            double total = nb * exc.getPrice();

            Reservation res = (reservationModif != null) ? reservationModif : new Reservation();
            res.setType_res("EXCURSION");
            res.setStatut(StatutReservation.EN_ATTENTE);
            res.setDate_debut(date);
            res.setPrix_total(total);
            res.setNombre_personnes(nb);
            res.setUser_id(1L);

            ReservationExcursion re = new ReservationExcursion();
            re.setExcursion_id((long) exc.getId());
            re.setDate_excursion(date);
            re.setHeure_souhaitee(heure);
            re.setNombre_personnes(nb);
            re.setPrix(total);

            if (reservationModif == null) {
                service.createFullExcursion(res, re);
                showAlert("Succès", "Réservation enregistrée !");
            } else {
                re.setId(reservationModif.getId());
                service.updateFullExcursion(res, re);
                showAlert("Succès", "Mise à jour réussie !");
            }

            redirectToMesReservations(event);

        } catch (NumberFormatException e) {
            showAlert("Erreur", "Le nombre de personnes doit être un chiffre.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    void handleBack(ActionEvent event) {
        redirectToMesReservations(event);
    }

    private void redirectToMesReservations(ActionEvent event) {
        try {
            // 1. Charger uniquement le fragment de la liste des réservations
            // Assure-toi que le nom du fichier est exactement "Mes Réservations.fxml"
            Parent root = FXMLLoader.load(getClass().getResource("/Mes Réservations.fxml"));

            // 2. Accéder à la Scène actuelle
            Scene scene = ((Node) event.getSource()).getScene();

            // 3. Chercher la zone de contenu centrale du Dashboard par son ID
            // Note: L'ID doit correspondre à celui défini dans ton ClientDashboard.fxml (ex: clientReservationView)
            VBox contentArea = (VBox) scene.lookup("#clientReservationView");

            if (contentArea != null) {
                // Vider la zone centrale (qui contient le formulaire de modif actuel)
                contentArea.getChildren().clear();
                // Ajouter la liste des réservations
                contentArea.getChildren().add(root);
            } else {
                // Fallback : Si on ne trouve pas le conteneur, on recharge tout le dashboard
                // pour éviter de rester bloqué sur une page vide.
                Parent dashboard = FXMLLoader.load(getClass().getResource("/ClientDashboard.fxml"));
                scene.setRoot(dashboard);
            }
        } catch (IOException e) {
            System.err.println("Erreur de navigation : " + e.getMessage());
            e.printStackTrace();
        }
    }
    private void showAlert(String t, String c) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(t); a.setHeaderText(null); a.setContentText(c); a.showAndWait();
    }
}