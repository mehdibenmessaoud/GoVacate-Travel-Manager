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

        // On affiche le pack 1 de MySQL
        Pack p = service.getPackById(1);
        if (p != null) {
            controller.setPackDataFromEntity(p);
        }

        primaryStage.setTitle("GOvacate - Réservation");
        primaryStage.setScene(new Scene(root));
        primaryStage.show();
    }
    public static void main(String[] args) { launch(args); }
}