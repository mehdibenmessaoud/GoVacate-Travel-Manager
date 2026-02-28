package tn.esprit.projet.test;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import tn.esprit.projet.entities.Role;
import tn.esprit.projet.entities.User;
import tn.esprit.projet.utils.MyDBConnexion;
import tn.esprit.projet.utils.SessionManager;
import tn.esprit.projet.utils.MyDBConnexion;
import tn.esprit.projet.gui.ClientDashboardController;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;

public class ClientTestLauncher extends Application {

    @Override
    public void start(Stage stage) {
        try {
            // 1. Connect to MySQL and login the REAL test user from your DB
            // On va tester avec l'utilisateur "test@gmail.com" (ID 2 dans ta capture)
            boolean loggedIn = loginFromDatabase("test@gmail.com");

            if (!loggedIn) {
                System.err.println("❌ Erreur : Impossible de trouver l'utilisateur dans MySQL.");
                return;
            }

            // 2. Load FXML
            URL fxmlLocation = getClass().getResource("/ClientDashboard.fxml");
            if (fxmlLocation == null) {
                System.err.println("ERREUR : Fichier FXML non trouvé.");
                return;
            }

            FXMLLoader fxmlLoader = new FXMLLoader(fxmlLocation);
            StackPane root = fxmlLoader.load();

            // 3. Initialize Controller
            ClientDashboardController controller = fxmlLoader.getController();
            controller.showChat();

            Scene scene = new Scene(root, 1100, 750);
            stage.setTitle("GoVacate - Test Client : " + SessionManager.getCurrentUserName());
            stage.setScene(scene);
            stage.show();

            System.out.println("🚀 Client [" + SessionManager.getCurrentUserName() + "] lancé avec succès !");

        } catch (IOException e) {
            System.err.println("Erreur chargement UI: " + e.getMessage());
        }
    }

    /**
     * 🎯 Cette méthode remplace le Mock.
     * Elle va chercher les vraies infos dans ta table 'user'.
     */
    private boolean loginFromDatabase(String email) {
        String query = "SELECT * FROM user WHERE email = ?";
        try (Connection conn = MyDBConnexion.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                int id = rs.getInt("id");
                String nom = rs.getString("nom");
                int roleId = rs.getInt("role_id");
                String status = rs.getString("status");

                // Création de l'objet User avec les infos de MySQL
                Role role = new Role(roleId, (roleId == 1 ? "ADMIN" : "CLIENT"));
                User realUser = new User(id, nom, email, "pass_hidden", role,
                        null, null, status, null, null);

                // Simulation du Login dans le SessionManager
                SessionManager.login(realUser);
                System.out.println("✅ [Session] Connecté en tant que : " + nom + " (ID: " + id + ")");
                return true;
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur de connexion DB : " + e.getMessage());
        }
        return false;
    }

    public static void main(String[] args) {
        launch(args);
    }
}