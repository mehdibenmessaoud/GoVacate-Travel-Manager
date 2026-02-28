package tn.esprit.projet.gui;

import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import tn.esprit.projet.entities.Reservation;
import java.awt.Desktop;
import java.net.URI;

public class PaymentController {

    @FXML private Label lblTotal;
    private Reservation currentReservation;

    public void setReservationData(Reservation res) {
        this.currentReservation = res;
        System.out.println("Prêt pour Stripe. Montant : " + res.getPrix_total() + " DT");

        if (lblTotal != null) {
            lblTotal.setText(res.getPrix_total() + " DT");
        }
    }

    @FXML
    public void procederAuPaiement() {
        // Use your Secret Key from Stripe Dashboard
        Stripe.apiKey = "sk_test_YOUR_SECRET_KEY";

        try {
            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl("https://success.com") // Replace with your URL
                    .setCancelUrl("https://cancel.com")
                    .addLineItem(SessionCreateParams.LineItem.builder()
                            .setQuantity(1L)
                            .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                    .setCurrency("eur")
                                    .setUnitAmount((long)(currentReservation.getPrix_total() * 100)) // Convert to cents
                                    .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                            .setName("Réservation #" + currentReservation.getId())
                                            .build())
                                    .build())
                            .build())
                    .build();

            Session session = Session.create(params);

            // Open the Stripe Checkout page in the browser
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(session.getUrl()));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}