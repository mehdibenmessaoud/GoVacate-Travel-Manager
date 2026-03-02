package tn.esprit.projet.gui;
import javafx.scene.layout.BorderPane;

import javafx.scene.control.Button;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.application.Platform;
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
import javafx.scene.layout.BorderPane;
import javafx.geometry.Insets;
import javafx.scene.layout.Pane;
public class ExcursionBookingController {
    @FXML
    private DatePicker dateExc;
    @FXML
    private TextField txtPersonnes;
    @FXML
    private TextField txtPrixUnitaire;
    @FXML
    private ComboBox<String> comboHeure;
    @FXML
    private Button btnValider;
    @FXML
    private Button btnAnnuler;
    @FXML private Label priceLabel;
    @FXML private Label convertedPriceLabel;
    private Excursion selectedExcursion;
    private static boolean isDarkMode = true;
    private final ReservationExcursionServiceImpl service = new ReservationExcursionServiceImpl();
    private Reservation reservationModif = null;

    public void setExcursionData(Excursion exc) {
        this.selectedExcursion = exc;
        if (exc != null) {
            txtPrixUnitaire.setText(String.valueOf(exc.getPrice()));
        }
    }

    @FXML
    public void initialize() {
        Platform.runLater(() -> {
            if (btnValider.getScene() != null) applyTheme(btnValider.getScene());
        });

        ObservableList<String> hours = FXCollections.observableArrayList();
        for (int h = 8; h <= 19; h++) {
            hours.add(String.format("%02d:00", h));
            hours.add(String.format("%02d:30", h));
        }
        comboHeure.setItems(hours);

        dateExc.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isBefore(LocalDate.now()));
            }
        });
    }

    // 🔥 Added: To fix the FXML LoadException
    @FXML
    void toggleTheme(ActionEvent event) {
        isDarkMode = !isDarkMode;
        applyTheme(((Node) event.getSource()).getScene());
    }

    // 🔥 Added: To fix the FXML handleBack reference
    @FXML
    void handleBack(ActionEvent event) {
        redirectToMesReservations(event);
    }

    @FXML
    void handleReserverExcursion(ActionEvent event) {
        try {
            if (selectedExcursion == null) {
                showAlert("Erreur", "Aucune excursion sélectionnée.");
                return;
            }

            // 🔥 FIX: Jib el ID mel SessionManager bech ma yetplontach el SQL
            long currentUserId;
            if (tn.esprit.projet.utils.SessionManager.isLoggedIn()) {
                currentUserId = (long) tn.esprit.projet.utils.SessionManager.getCurrentUserId();
            } else {
                // Ken mafammach session (test), khaliha 6 khaterha mawjouda f-el base mte3ek
                currentUserId = 6L;
                System.out.println("⚠️ Session vide, utilisation de l'ID par défaut: 6L");
            }

            String heure = comboHeure.getValue();
            if (dateExc.getValue() == null || txtPersonnes.getText().isEmpty() || heure == null) {
                showAlert("Champs manquants", "Veuillez remplir tous les champs.");
                return;
            }

            int nb = Integer.parseInt(txtPersonnes.getText());
            LocalDate date = dateExc.getValue();
            double total = nb * selectedExcursion.getPrice();

            Reservation res = (reservationModif != null) ? reservationModif : new Reservation();
            res.setType_res("EXCURSION");
            res.setStatut(StatutReservation.EN_ATTENTE);
            res.setDate_debut(date);
            res.setPrix_total(total);
            res.setNombre_personnes(nb);
            res.setUser_id(currentUserId); // 🔥 ID Dynamique tawa

            ReservationExcursion re = new ReservationExcursion();
            re.setExcursion_id((long) selectedExcursion.getId());
            re.setDate_excursion(date);
            re.setHeure_souhaitee(heure);
            re.setNombre_personnes(nb);
            re.setPrix(total);

            if (reservationModif == null) {
                service.createFullExcursion(res, re);
                showAlert("Succès", "Réservation enregistrée !");
            } else {
                // Cas de modification
                re.setReservation_id(reservationModif.getId());
                service.updateFullExcursion(res, re);
                showAlert("Succès", "Mise à jour réussie !");
            }

            redirectToMesReservations(event);

        } catch (NumberFormatException e) {
            showAlert("Erreur", "Le nombre de personnes doit être un chiffre.");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur SQL", "Vérifiez la connexion ou l'ID utilisateur.");
        }
    }

    // 🚀 METTRE À JOUR CETTE MÉTHODE DANS ExcursionBookingController
    private void redirectToMesReservations(ActionEvent event) {
        try {
            // On charge la vue des réservations
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Mes Réservations.fxml"));
            Parent view = loader.load();

            // Lawaj 3al mainLayout mta el Dashboard
            Scene scene = ((Node) event.getSource()).getScene();
            BorderPane mainLayout = (BorderPane) scene.lookup("#mainLayout");

            if (mainLayout != null) {
                mainLayout.setCenter(view);
                System.out.println("✅ Redirection vers Mes Réservations réussie.");
            } else {
                // Fallback si on est en mode "Full Window" sans dashboard
                scene.setRoot(view);
            }
        } catch (IOException e) {
            System.err.println("❌ Erreur redirection : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void applyTheme(Scene scene) {
        if (scene.getStylesheets() == null) return;
        scene.getStylesheets().clear();
        String style = getClass().getResource("/css/booking_style.css").toExternalForm();
        scene.getStylesheets().add(style);
        if (!isDarkMode) {
            scene.getStylesheets().add(getClass().getResource("/css/light-mode.css").toExternalForm());
        }
    }

    private void showAlert(String t, String c) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(t);
        a.setHeaderText(null);
        a.setContentText(c);
        a.showAndWait();
    }

    public void setReservationData(Reservation res, ReservationExcursion re, Excursion exc) {
        this.reservationModif = res;
        this.selectedExcursion = exc;

        javafx.application.Platform.runLater(() -> {
            // 1. Remplissage des champs classiques
            if (dateExc != null) dateExc.setValue(re.getDate_excursion());
            if (txtPersonnes != null) txtPersonnes.setText(String.valueOf(re.getNombre_personnes()));
            if (comboHeure != null) comboHeure.setValue(re.getHeure_souhaitee());

            // 🚀 FIX: Testa3mel txtPrixUnitaire khater el priceLabel mouch mawjoud fel FXML
            if (txtPrixUnitaire != null && exc != null) {
                txtPrixUnitaire.setText(String.valueOf(exc.getPrice()));
            }

            // Optionnel: Ken t7eb tzid label lel Prix TOTAL dakhil el formulaire
            // lezem t-zidu sa3a fel FXML ba3d tasta3mlu hna.

            // 4. Changer le texte du bouton
            if (btnValider != null) btnValider.setText("Enregistrer les modifications 🔥");

            // Afficher le bouton annuler s'il existe
            if (btnAnnuler != null) {
                btnAnnuler.setManaged(true);
                btnAnnuler.setVisible(true);
            }
        });
    }
}