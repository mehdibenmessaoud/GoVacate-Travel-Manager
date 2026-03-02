package tn.esprit.projet.gui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.FlowPane;
import javafx.scene.control.Label;
import tn.esprit.projet.entities.Destination;
import tn.esprit.projet.entities.Excursion;
import tn.esprit.projet.services.ExcursionService;
import tn.esprit.projet.utils.MyDBConnexion;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;

public class ClientExcursionController {
    @FXML private FlowPane excursionContainer;
    @FXML private Label titleLabel;
    private ExcursionService excursionService = new ExcursionService(MyDBConnexion.getInstance().getConnection());

    public void loadExcursions(Destination d) {
        titleLabel.setText("Activites a " + d.getNameDestination());
        try {
            // Filtrage par ID de destination
            List<Excursion> list = excursionService.getByLocation(d.getId());
            excursionContainer.getChildren().clear();
            for (Excursion e : list) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/ExcursionCard.fxml"));
                Parent card = loader.load();
                ((ExcursionCardController)loader.getController()).setData(e, this::showDetails);
                excursionContainer.getChildren().add(card);
            }
        } catch (SQLException | IOException ex) { ex.printStackTrace(); }
    }

    private void showDetails(Excursion e) {
        try {
            // 1. Charger le fichier FXML des détails de l'excursion
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ExcursionDetails.fxml"));
            Parent detailsView = loader.load();

            // 2. Récupérer le contrôleur et lui passer les données de l'excursion sélectionnée
            ExcursionDetailsController controller = loader.getController();
            controller.setExcursionData(e); // Utilise la méthode qu'on a corrigée au début

            // 3. Accéder au BorderPane principal pour changer le centre
            // On récupère la scène à partir du conteneur actuel
            Scene scene = excursionContainer.getScene();
            StackPane st = (StackPane) scene.getRoot();
            BorderPane bp = (BorderPane) st.getChildren().get(1);

            // 4. Afficher la vue des détails au centre
            bp.setCenter(detailsView);

        } catch (IOException ex) {
            System.err.println("Erreur lors de l'ouverture des détails : " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ClientDestinationView.fxml"));
            Parent destinationView = loader.load();

            // On fait la même chose ici pour retrouver le BorderPane principal
            Scene scene = excursionContainer.getScene();
            StackPane root = (StackPane) scene.getRoot();
            BorderPane bp = (BorderPane) root.getChildren().get(1);

            bp.setCenter(destinationView);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}