package tn.esprit.projet.test;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.image.Image;
import tn.esprit.projet.utils.MyDBConnexion;
import tn.esprit.projet.utils.SceneManager;

public class MainGovacate extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        SceneManager.setPrimaryStage(primaryStage);

        primaryStage.setTitle("GoVacate - Gestion de Voyages");
        primaryStage.setMinWidth(1000);
        primaryStage.setMinHeight(700);

        Image appIcon = new Image(getClass().getResourceAsStream("/logo/logo.jpg"));
        primaryStage.getIcons().add(appIcon);

        primaryStage.setOnCloseRequest(event -> {
            try {
                MyDBConnexion.getInstance().close();
            } catch (Exception e) {
                System.err.println("Erreur fermeture connexion: " + e.getMessage());
            }
        });

        SceneManager.goToLogin();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
