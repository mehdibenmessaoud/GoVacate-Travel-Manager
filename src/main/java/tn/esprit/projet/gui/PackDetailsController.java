package tn.esprit.projet.gui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import tn.esprit.projet.entities.Pack;
import tn.esprit.projet.services.DestinationService;
import tn.esprit.projet.services.HotelService;
import tn.esprit.projet.services.ExcursionService;
import tn.esprit.projet.utils.MyDBConnexion;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

public class PackDetailsController {

    @FXML private Label nameLabel;
    @FXML private Label categoryLabel;
    @FXML private Label descriptionLabel;
    @FXML private Label priceLabel;
    @FXML private Label durationLabel;
    @FXML private Label statusLabel;

    // Nouveaux labels liés à la destination, l'hôtel et l'excursion
    @FXML private Label destinationLabel;
    @FXML private Label hotelLabel;
    @FXML private Label excursionLabel;

    @FXML private ImageView packImageView;
    @FXML private Button btnBack;
    @FXML private Button btnReserver;

    // À ajouter vers la ligne 34
    @FXML private Label dateDebutLabel;
    @FXML private Label dateFinLabel;

    // Initialisation des services pour récupérer les noms à partir des IDs
    private final Connection connection = MyDBConnexion.getInstance().getConnection();
    private final DestinationService destinationService = new DestinationService(connection);
    private final HotelService hotelService = new HotelService(connection);
    private final ExcursionService excursionService = new ExcursionService(connection);

    /**
     * Remplit l'interface avec les données du pack choisi
     */
    public void setPackData(Pack pack) {
        // 1. Informations de base du pack
        nameLabel.setText(pack.getName());
        categoryLabel.setText(pack.getCategorie());
        descriptionLabel.setText(pack.getDescription());
        priceLabel.setText(pack.getPrix() + " DT");
        durationLabel.setText(pack.getDuree() + " Jours");
        statusLabel.setText(pack.getStatus());


        String status = (pack.getStatus() != null) ? pack.getStatus().trim() : "";
        statusLabel.setText(status.toUpperCase());

        statusLabel.getStyleClass().removeAll("status-available", "status-unavailable");

        if ("Disponible".equalsIgnoreCase(status)) {
            statusLabel.getStyleClass().add("status-available");

            // Activer le bouton
            btnReserver.setDisable(false);
            btnReserver.setText("Confirmer la Réservation");
            btnReserver.setOpacity(1.0);
        } else {
            statusLabel.getStyleClass().add("status-unavailable");

            // DÉSACTIVER LE BOUTON
            btnReserver.setDisable(true);
            btnReserver.setText("Indisponible");

            // Optionnel : Réduire l'opacité pour renforcer l'aspect "grisé"
            btnReserver.setOpacity(0.5);
        }

        // À ajouter après durationLabel.setText(...)
        if (pack.getDateDepart() != null) {
            dateDebutLabel.setText(pack.getDateDepart().toString());
        } else {
            dateDebutLabel.setText("Non définie");
        }

        if (pack.getDateArriver() != null) {
            dateFinLabel.setText(pack.getDateArriver().toString());
        } else {
            dateFinLabel.setText("Non définie");
        }
        // 2. Chargement de l'image
        try {
            String imagePath = "/images/" + pack.getImageName();
            if (getClass().getResource(imagePath) != null) {
                packImageView.setImage(new Image(getClass().getResource(imagePath).toExternalForm()));
            }
        } catch (Exception e) {
            System.err.println("Erreur image : " + e.getMessage());
        }

        // 3. Récupération des détails liés (Destination, Hôtel, Excursion)
        try {
            // Destination
            if (pack.getDestinationId() > 0) {
                var dest = destinationService.getById(pack.getDestinationId());
                destinationLabel.setText(dest != null ? dest.getNameDestination() : "Non spécifiée");
            }

            // Hôtel
            if (pack.getHotelId() > 0) {
                var hotel = hotelService.getById(pack.getHotelId());
                hotelLabel.setText(hotel != null ? hotel.getName() : "Aucun hôtel");
            }

            // Excursion
            if (pack.getExcursionId() > 0) {
                var excur = excursionService.getById(pack.getExcursionId());
                excursionLabel.setText(excur != null ? excur.getName() : "Aucune excursion");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            destinationLabel.setText("Donnée indisponible");
        }
    }

    /**
     * Méthode pour revenir à la liste des packs dans le même BorderPane
     */


    @FXML
    private void handleBack() {
        try {
            // On récupère le chemin de la vue actuelle pour savoir si on est chez l'admin ou le client
            String currentView = nameLabel.getScene().getRoot().getId();

            // Solution simple : si on est dans l'interface admin (vérifiez l'ID de votre StackPane dans adminView.fxml)
            // On va plutôt recharger la vue admin si le bouton a été cliqué depuis là

            // Pour faire simple, on peut vérifier si on est dans "rootPane" (admin) ou "rootPane" (client)
            // Mais le plus efficace est de recharger adminView.fxml si on veut retourner à la table

            Parent root = FXMLLoader.load(getClass().getResource("/adminView.fxml"));
            nameLabel.getScene().setRoot(root);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}