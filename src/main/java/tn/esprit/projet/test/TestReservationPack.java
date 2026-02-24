package tn.esprit.projet.test;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import tn.esprit.projet.entities.Pack;
import tn.esprit.projet.gui.PackBookingController;
import tn.esprit.projet.services.ReservationPackServiceImpl;

public class TestReservationPack extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/ReservationPackForm.fxml"));
        Parent root = loader.load();

        PackBookingController controller = loader.getController();
        ReservationPackServiceImpl service = new ReservationPackServiceImpl();

        Pack p = service.getPackById(1);
        if (p != null) {
            controller.setPackDataFromEntity(p);
        }

        // --- FIX : Force la taille de la fenêtre pour permettre le centrage ---
        Scene scene = new Scene(root, 1200, 800);

        primaryStage.setTitle("GOvacate - Réservation");
        primaryStage.setScene(scene);

        // --- FIX : Centre la fenêtre sur l'écran ---
        primaryStage.centerOnScreen();
        primaryStage.setResizable(true);

        primaryStage.show();
    }
    public static void main(String[] args) { launch(args); }
}