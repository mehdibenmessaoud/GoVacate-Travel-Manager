package tn.esprit.projet.gui;

import javafx.application.Platform;
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

    // Persist theme state
    private static boolean isDarkMode = true;

    private final ReservationPackServiceImpl service = new ReservationPackServiceImpl();
    private int selectedPackId;
    private double packPrix;
    private LocalDate dDepart, dArrivee;
    private String packName;

    @FXML
    public void initialize() {
        // This ensures that when the window opens, it's NOT white.
        // It applies your pack.css immediately.
        Platform.runLater(() -> {
            if (lblPackName.getScene() != null) {
                applyTheme(lblPackName.getScene());
            }
        });
    }

    @FXML
    void toggleTheme(ActionEvent event) {
        isDarkMode = !isDarkMode;
        applyTheme(((Node) event.getSource()).getScene());
    }

    private void applyTheme(Scene scene) {
        // 1. Clear everything to avoid the "ghosting" / white background issue
        scene.getStylesheets().clear();

        // 2. Add your original pack.css (The design you want to keep)
        scene.getStylesheets().add(getClass().getResource("/css/pack.css").toExternalForm());

        // 3. If it's light mode, add the overrides on top.
        // If dark, we do nothing extra because pack.css is already dark.
        if (!isDarkMode) {
            scene.getStylesheets().add(getClass().getResource("/css/light-mode.css").toExternalForm());
        }
    }

    // --- Data & Navigation Logic ---

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

            service.createFullPack(res, rp);
            new Alert(Alert.AlertType.INFORMATION, "Réservation effectuée avec succès !").showAndWait();
            redirectToMesReservations(event);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void redirectToMesReservations(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/Mes Réservations.fxml"));
            Scene scene = ((Node) event.getSource()).getScene();

            applyTheme(scene); // Keep the theme consistent after redirect

            VBox contentArea = (VBox) scene.lookup("#clientReservationView");
            if (contentArea != null) {
                contentArea.getChildren().clear();
                contentArea.getChildren().add(root);
            } else {
                Parent dashboard = FXMLLoader.load(getClass().getResource("/ClientDashboard.fxml"));
                scene.setRoot(dashboard);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void handleCancel(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }
}