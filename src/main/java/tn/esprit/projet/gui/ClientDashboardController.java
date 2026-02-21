package tn.esprit.projet.gui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import java.io.IOException;

public class ClientDashboardController {

    // On utilise EXACTEMENT les IDs du FXML
    @FXML private ScrollPane explorerView;
    @FXML private VBox clientReservationView;
    @FXML private VBox clientFactureView;

    @FXML
    public void initialize() {
        // Sécurité : On vérifie que le FXML est bien chargé
        if (explorerView != null && clientReservationView != null && clientFactureView != null) {
            showExplorer();
        } else {
            System.err.println("Erreur : Les IDs FXML ne correspondent pas au Controller !");
        }
    }

    @FXML
    private void showExplorer() {
        explorerView.setVisible(true);
        clientReservationView.setVisible(false);
        clientFactureView.setVisible(false);
    }

    @FXML
    private void showReservations() {
        try {
            clientReservationView.getChildren().clear();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Mes Réservations.fxml"));
            Parent root = loader.load();
            clientReservationView.getChildren().add(root);

            explorerView.setVisible(false);
            clientReservationView.setVisible(true);
            clientFactureView.setVisible(false);
        } catch (IOException e) {
            System.err.println("Erreur chargement MesReservations.fxml: " + e.getMessage());
        }
    }

    @FXML
    private void showFactures() {
        try {
            clientFactureView.getChildren().clear();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/MesFactures.fxml"));
            Parent root = loader.load();
            clientFactureView.getChildren().add(root);

            explorerView.setVisible(false);
            clientReservationView.setVisible(false);
            clientFactureView.setVisible(true);
        } catch (IOException e) {
            System.err.println("Erreur chargement MesFactures.fxml: " + e.getMessage());
        }
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        System.out.println("Déconnexion client...");
        // Tu peux ajouter ici le retour à la page Login
    }
}