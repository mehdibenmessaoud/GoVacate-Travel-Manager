package tn.esprit.projet.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDBConnexion {
    private final String URL = "jdbc:mysql://localhost:3306/GoVacate2";
    private final String USER = "root";
    private final String PASSWORD = "";
    private Connection connection;
    private static MyDBConnexion instance;
    private boolean isConnected = false;

    private MyDBConnexion() {
        try {
            // Try to establish connection
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
            isConnected = true;
            System.out.println("✓ Connexion établie avec succès à GoVacate!");
        } catch (ClassNotFoundException e) {
            System.err.println("✗ Erreur: Driver MySQL non trouvé - " + e.getMessage());
            isConnected = false;
        } catch (SQLException e) {
            System.err.println("✗ Erreur de connexion à la base de données: " + e.getMessage());
            System.err.println("  Assurez-vous que MySQL est démarré et que la base 'GoVacate' existe");
            isConnected = false;
        } catch (Exception e) {
            System.err.println("✗ Erreur d'initialisation: " + e.getMessage());
            isConnected = false;
        }
    }

    public static MyDBConnexion getInstance() {
        if (instance == null) {
            instance = new MyDBConnexion();
        }
        return instance;
    }

    public Connection getConnection() {
        return connection;
    }

    public boolean isConnected() {
        return isConnected && connection != null;
    }

    public String getConnectionStatus() {
        if (isConnected) {
            return "Connecté";
        } else {
            return "Déconnecté - Vérifiez la base de données";
        }
    }

    public void testConnection() {
        if (isConnected) {
            try {
                if (!connection.isClosed()) {
                    System.out.println("✓ Test de connexion réussi");
                } else {
                    System.out.println("✗ Connexion fermée");
                    isConnected = false;
                }
            } catch (SQLException e) {
                System.err.println("✗ Test de connexion échoué: " + e.getMessage());
                isConnected = false;
            }
        } else {
            System.out.println("✗ Pas de connexion active");
        }
    }
}
