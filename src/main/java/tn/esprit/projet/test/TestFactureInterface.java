package tn.esprit.projet.test;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class TestFactureInterface extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Charge le FXML - Vérifie bien que le nom correspond au fichier dans resources
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/MesFactures.fxml"));
        Parent root = loader.load();

        primaryStage.setTitle("GoVacate - Historique des Factures");
        primaryStage.setScene(new Scene(root, 950, 650));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args); // C'est cette ligne qui bloque le processus pour garder la fenêtre ouverte
    }
}