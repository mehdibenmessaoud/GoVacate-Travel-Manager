package tn.esprit.projet.gui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.layout.FlowPane;
import tn.esprit.projet.entities.Destination;
import tn.esprit.projet.services.DestinationService;
import tn.esprit.projet.utils.MyDBConnexion;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;

import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.Parent;
import javafx.fxml.FXMLLoader;

public class ClientDestinationController implements Initializable {

    @FXML private FlowPane destinationContainer;

    private DestinationService destinationService;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        destinationService = new DestinationService(MyDBConnexion.getInstance().getConnection());
        loadDestinations();
    }

    private void loadDestinations() {
        try {
            // Récupération de toutes les destinations via le service
            List<Destination> list = destinationService.getAll();
            destinationContainer.getChildren().clear();

            for (Destination d : list) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/DestinationCard.fxml"));
                Parent card = loader.load();

                // On configure les données de la carte
                DestinationCardController controller = loader.getController();
                controller.setData(d, this::onDestinationSelected);

                destinationContainer.getChildren().add(card);
            }
        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Méthode appelée quand on clique sur une destination
     * C'est ici qu'on fera le lien vers l'étape 4 (affichage des excursions filtrées)
     */
    private void onDestinationSelected(Destination d) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ClientExcursionView.fxml"));
            Parent excursionView = loader.load();

            ClientExcursionController controller = loader.getController();
            controller.loadExcursions(d);

            // Cette ligne remplace le besoin de 'rootPane'
            // Elle remonte à la racine de la fenêtre et cherche le BorderPane
            Scene scene = destinationContainer.getScene();
            StackPane root = (StackPane) scene.getRoot();

            // On récupère le BorderPane qui est le deuxième enfant (index 1)
            BorderPane bp = (BorderPane) root.getChildren().get(1);

            bp.setCenter(excursionView);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}