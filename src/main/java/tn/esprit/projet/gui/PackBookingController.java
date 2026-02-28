package tn.esprit.projet.gui;
import tn.esprit.projet.utils.SessionManager;
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
import tn.esprit.projet.utils.SceneManager;
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
            // 1. Verifi ennou el user m-logui
            if (!SessionManager.isLoggedIn()) {
                new Alert(Alert.AlertType.ERROR, "Erreur: Vous devez être connecté !").show();
                return;
            }

            Reservation res = new Reservation();
            res.setType_res("PACK");
            res.setStatut(StatutReservation.EN_ATTENTE);
            res.setDate_debut(dDepart);
            res.setDate_fin(dArrivee);
            res.setPrix_total(packPrix);
            res.setNombre_personnes(1);
            res.setCommentaire_client("Pack: " + packName);

            // 🔥 FIX DYNAMIQUE: Nakhou el ID melli m-logui tawa
            res.setUser_id((long) SessionManager.getCurrentUserId());

            ReservationPack rp = new ReservationPack();
            rp.setPack_id((long) selectedPackId);
            rp.setPrix_pack(packPrix);

            service.createFullPack(res, rp);
            new Alert(Alert.AlertType.INFORMATION, "Réservation effectuée avec succès !").showAndWait();
            redirectToMesReservations(event);

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Erreur SQL: Vérifiez que votre ID existe en base!").show();
        }
    }
    private void redirectToMesReservations(ActionEvent event) {
        try {
            // 1. Nakhou el Scene el 7alia
            Scene scene = ((Node) event.getSource()).getScene();

            // 2. Nlawjou 3al "contentArea" mta el ClientDashboard2
            VBox contentArea = (VBox) scene.lookup("#contentArea");

            if (contentArea != null) {
                // ✅ Ken a7na déjà fi wast el Dashboard (Success case)
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/Mes Réservations.fxml"));
                Parent root = loader.load();

                contentArea.getChildren().clear();
                contentArea.getChildren().add(root);
                contentArea.setVisible(true);

                System.out.println("✅ Redirection vers Mes Réservations (Inline) réussie.");
            } else {
                // 🔄 Plan B: Ken el Scene mahich el Dashboard (ex: Popup walla scene okhra)
                // N-badlou el Scene kemla lel Dashboard w houwa taw i-hezna lel Reservations
                SceneManager.switchTo("/ClientDashboard2.fxml");
                // Ba3d el switch, tnejjem t-3ayet l-SceneManager.loadClientContent("/Mes Réservation.fxml")
            }
        } catch (IOException e) {
            System.err.println("❌ Erreur lors de la redirection: " + e.getMessage());
            e.printStackTrace();
        }
    }
    @FXML
    void handleCancel(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }
}