package tn.esprit.projet.gui;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import tn.esprit.projet.entities.Reservation; // Assure-toi que le chemin est correct

public class PaymentController {

    @FXML
    private Label lblTotal; // Optionnel : si tu as un label pour afficher le prix

    public void setReservationData(Reservation res) {
        // Test console pour vérifier que l'objet est bien passé
        System.out.println("Données reçues dans le PaymentController !");
        System.out.println("ID Réservation : " + res.getId());
        System.out.println("Prêt pour Stripe. Montant : " + res.getPrix_total() + " DT");

        // Si tu veux l'afficher sur l'interface plus tard :
        // if (lblTotal != null) lblTotal.setText(res.getPrix_total() + " DT");
    }
}