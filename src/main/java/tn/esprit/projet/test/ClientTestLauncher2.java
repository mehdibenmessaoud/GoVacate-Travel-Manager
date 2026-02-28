package tn.esprit.projet.test;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import tn.esprit.projet.entities.Role;
import tn.esprit.projet.entities.User;
import tn.esprit.projet.utils.SessionManager;
import tn.esprit.projet.utils.MyDBConnexion;
import tn.esprit.projet.gui.ClientDashboardController;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class ClientTestLauncher2 extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        // 🎯 1. LOGIN DYNAMIQUE MEL MYSQL
        // Badal el email hna b-hasb el client el jdid elli zedtou fil base
        String emailToTest = "tes@gmail.com";
        boolean success = loginFromDatabase(emailToTest);

        if (!success) {
            System.err.println("❌ Erreur : Le client " + emailToTest + " n'existe pas dans MySQL !");
            return;
        }

        // 2. Load the FXML
        URL fxmlLocation = getClass().getResource("/ClientDashboard.fxml");
        if (fxmlLocation == null) fxmlLocation = getClass().getResource("/tn/esprit/projet/gui/ClientDashboard.fxml");

        FXMLLoader loader = new FXMLLoader(fxmlLocation);
        StackPane root = loader.load();

        // 3. Setup Controller
        ClientDashboardController controller = loader.getController();
        controller.showChat();

        // 4. Show the stage
        stage.setTitle("GoVacate - Session de : " + SessionManager.getCurrentUserName());
        stage.setScene(new Scene(root, 1100, 750));
        stage.show();

        System.out.println("✅ Fenêtre de [" + SessionManager.getCurrentUserName() + "] lancée.");
    }

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

                // Mapping mel base lel entité User mte3ek
                Role role = new Role(roleId, (roleId == 1 ? "ADMIN" : "CLIENT"));
                User dbUser = new User(id, nom, email, "pass_hidden", role,
                        null, null, status, null, null);

                SessionManager.login(dbUser);
                System.out.println("✅ [MySQL Session] User: " + nom + " (ID: " + id + ") connecté.");
                return true;
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur DB: " + e.getMessage());
        }
        return false;
    }

    public static void main(String[] args) { launch(args); }
}