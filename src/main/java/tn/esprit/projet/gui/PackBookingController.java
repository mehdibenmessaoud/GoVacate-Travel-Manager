package tn.esprit.projet.gui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import tn.esprit.projet.entities.Pack;
import tn.esprit.projet.entities.Reservation;
import tn.esprit.projet.entities.ReservationPack;
import tn.esprit.projet.entities.StatutReservation;
import tn.esprit.projet.services.ReservationPackServiceImpl;
import java.io.IOException;
import java.time.LocalDate;
import javafx.scene.layout.VBox;
public class PackBookingController {

    @FXML private Label lblPackName, lblDateDepart, lblDateArrivee, lblPrix, lblCategorie;

    private final ReservationPackServiceImpl service = new ReservationPackServiceImpl();
    private int selectedPackId;
    private double packPrix;
    private LocalDate dDepart, dArrivee;
    private String packName;

    public void setPackDataFromEntity(Pack pack) {
        this.selectedPackId = pack.getId();
        this.packName = pack.getName();
        this.dDepart = pack.getDateDepart();
        this.dArrivee = pack.getDateArriver();
        this.packPrix = pack.getPrix();

        if (lblPackName != null) lblPackName.setText(pack.getName());
        if (lblCategorie != null) lblCategorie.setText(pack.getCategorie());
        if (lblDateDepart != null) lblDateDepart.setText(pack.getDateDepart().toString());
        if (lblDateArrivee != null) lblDateArrivee.setText(pack.getDateArriver().toString());
        if (lblPrix != null) lblPrix.setText(pack.getPrix() + " DT");
    }

    @FXML
    void handleReserve(ActionEvent event) {
        try {
            Reservation res = new Reservation();
            res.setType_res("PACK");
            res.setStatut(StatutReservation.EN_ATTENTE);
            res.setDate_debut(dDepart);
            res.setDate_fin(dArrivee);
            res.setPrix_total(packPrix);
            res.setNombre_personnes(1);
            res.setCommentaire_client("Pack: " + packName);
            res.setUser_id(1L);

            ReservationPack rp = new ReservationPack();
            rp.setPack_id((long) selectedPackId);
            rp.setPrix_pack(packPrix);

            // Insertion réelle en BDD
            service.createFullPack(res, rp);

            new Alert(Alert.AlertType.INFORMATION, "Réservation effectuée avec succès !").showAndWait();

            redirectToMesReservations(event);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void redirectToMesReservations(ActionEvent event) {
        try {
            // 1. Charger la vue de la liste
            Parent root = FXMLLoader.load(getClass().getResource("/Mes Réservations.fxml"));

            // 2. Récupérer la scène actuelle
            Scene scene = ((Node) event.getSource()).getScene();

            // 3. Trouver le conteneur central (VBox) du Dashboard
            // Le cast (VBox) fonctionne uniquement si l'import est présent
            VBox contentArea = (VBox) scene.lookup("#clientReservationView");

            if (contentArea != null) {
                // Nettoyer la zone centrale et injecter la liste
                contentArea.getChildren().clear();
                contentArea.getChildren().add(root);
            } else {
                // Si on ne trouve pas le conteneur, on recharge tout le dashboard
                Parent dashboard = FXMLLoader.load(getClass().getResource("/ClientDashboard.fxml"));
                scene.setRoot(dashboard);
            }
        } catch (IOException e) {
            System.err.println("Erreur de navigation : " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    void handleCancel(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }
}