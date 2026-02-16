package tn.esprit.projet.gui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class HotelBookingController {

    @FXML private ComboBox<?> comboHotel;
    @FXML private ComboBox<?> comboChambre;
    @FXML private DatePicker dateArrivee;
    @FXML private DatePicker dateDepart;
    @FXML private TextField txtVoyageurs;
    @FXML private Label lblPrixEstimé;

    @FXML
    void handleVerification(ActionEvent event) {
        // C'est ici qu'on mettra la logique plus tard
        System.out.println("Bouton cliqué !");
    }
}