package tn.esprit.projet.gui;

import tn.esprit.projet.entities.Pack;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import java.io.IOException;
import java.net.URL;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import tn.esprit.projet.services.PackService;


public class PackCardController {

    @FXML private Label packName;
    @FXML private Label packPrice;

    // RENOMMÉ ICI
    @FXML private Label packStatus;
    @FXML private Label packBadge; // Pour corriger l'erreur packBadge
    private PackService packService = new PackService();
    private Pack currentPack;

    public void setData(Pack pack) {
        this.currentPack = pack;
        int currentReservations = packService.getCurrentPackReservations(pack.getId());
        double dynamicPrice = packService.calculateDynamicPackPrice(pack, currentReservations);
        if (packName != null) packName.setText(pack.getName());
        if (packPrice != null) packPrice.setText(dynamicPrice + " DT");

        if (dynamicPrice < pack.getPrix()) {
            packBadge.setText("🔥 PROMO");
            packBadge.setVisible(true);
            packPrice.setStyle("-fx-text-fill: #2ecc71;"); // Vert pour la promo
        }
        else if (currentReservations >= 20) {
            packBadge.setText("⚡ POPULAIRE");
            packBadge.setVisible(true);
            packPrice.setStyle("-fx-text-fill: #e74c3c;"); // Rouge pour la forte demande
        }
        else {
            packBadge.setVisible(false);
            packPrice.setStyle("-fx-text-fill: #FF8210;"); // Orange par défaut
        }

        // Utilisation du nouveau nom packStatus
        if (packStatus != null) {
            // 1. Nettoyage des anciennes classes CSS
            packStatus.getStyleClass().removeAll("status-available", "status-unavailable");

            // 2. Application du style selon le statut (vert ou rouge)
            if ("Disponible".equalsIgnoreCase(pack.getStatus())) {
                packStatus.setText("Disponible");
                packStatus.getStyleClass().add("status-available");
            } else {
                packStatus.setText("Non Disponible");
                packStatus.getStyleClass().add("status-unavailable");
            }
        } else {
            // Message d'alerte si l'id n'est pas encore mis à jour dans le FXML
            System.err.println("⚠️ Attention : packStatus est null. Vérifiez l'fx:id dans PackCard.fxml");
        }
    }

    // ... handleDetails et handleClose restent inchangés ...
    @FXML
    private void handleDetails(Pack pack) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/tn/esprit/projet/gui/PackDetails.fxml"));
            Parent root = loader.load();

            // C'est ici que la classe manquante est utilisée
            PackDetailsController controller = loader.getController();
            controller.setPackData(pack);

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Détails du Voyage - " + pack.getName());
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleClose(ActionEvent event) {
        try {
            URL location = getClass().getResource("/MainLayout.fxml");
            if (location == null) return;

            Parent root = FXMLLoader.load(location);
            Scene scene = ((Node) event.getSource()).getScene();
            Pane contentArea = (Pane) scene.lookup("#contentArea");

            if (contentArea != null) {
                contentArea.getChildren().setAll(root);
            } else {
                scene.setRoot(root);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}