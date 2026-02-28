package tn.esprit.projet.test;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import tn.esprit.projet.entities.Role;
import tn.esprit.projet.entities.User;
import tn.esprit.projet.utils.SessionManager;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;

public class HelloApplication extends Application {

    @Override
    public void start(Stage stage) {
        try {
            // 1. Initialiser la Session Admin (Indispensable pour démarrer le serveur)
            mockAdminSession();

            // 2. Chargement du FXML Admin
            // On essaie d'abord la racine, puis le dossier gui
            URL fxmlLocation = getClass().getResource("/GoVacate.fxml");
            if (fxmlLocation == null) {
                fxmlLocation = getClass().getResource("/GoVacate.fxml");
            }

            if (fxmlLocation == null) {
                System.err.println("❌ ERREUR : Fichier GoVacate.fxml introuvable !");
                return;
            }

            FXMLLoader fxmlLoader = new FXMLLoader(fxmlLocation);
            // Remplacez AnchorPane par le type de root de votre GoVacate.fxml si différent
            Scene scene = new Scene(fxmlLoader.load(), 1100, 750);

            // 3. Application du CSS
            URL cssLocation = getClass().getResource("/css/style.css");
            if (cssLocation != null) {
                scene.getStylesheets().add(cssLocation.toExternalForm());
            }

            stage.setTitle("GoVacate ADMIN - " + SessionManager.getCurrentUserName());
            stage.setScene(scene);
            stage.setResizable(false);
            stage.show();

            System.out.println("✅ Dashboard Admin lancé avec succès !");

        } catch (IOException e) {
            System.err.println("❌ Erreur de chargement Admin : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void mockAdminSession() {
        // Mock d'un utilisateur avec le rôle ADMIN
        Role roleAdmin = new Role(1, "ADMIN");
        User adminUser = new User(1, "Moez Admin", "admin@govacate.tn", "admin123",
                roleAdmin, "55667788", LocalDate.of(1990, 5, 15),
                "actif", null, null);

        SessionManager.login(adminUser);
        System.out.println("🚀 Session Admin active : " + SessionManager.getCurrentUserName());
    }

    public static void main(String[] args) {
        launch(args);
    }
}