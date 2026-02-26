package tn.esprit.projet.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDBConnexion {
    private final String URL = "jdbc:mysql://localhost:3306/govacate1";
    private final String USER = "root";
    private final String PASSWORD = "";
    private Connection connection;

    private static MyDBConnexion instance;

    private MyDBConnexion() {
        connect(); // On utilise une méthode séparée pour la clarté
    }

    private void connect() {
        try {
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("Connexion établie avec succès à govacate1 !");
        } catch (SQLException e) {
            System.err.println("Erreur de connexion : " + e.getMessage());
        }
    }

    public static MyDBConnexion getInstance() {
        if (instance == null) {
            instance = new MyDBConnexion();
        }
        return instance;
    }

    public Connection getConnection() {
        try {
            // FIX CRITIQUE : Si la connexion est nulle ou a été fermée, on la réouvre
            if (connection == null || connection.isClosed()) {
                System.out.println("Réouverture de la connexion fermée...");
                connect();
            }
        } catch (SQLException e) {
            System.err.println("Impossible de vérifier l'état de la connexion : " + e.getMessage());
        }
        return connection;
    }
}