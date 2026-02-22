package tn.esprit.projet.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class govacate_connect {
    private final String URL = "jdbc:mysql://localhost:3306/govacate";
    private final String USER = "root";
    private final String PASSWORD = "";
    private Connection connection;

    private static govacate_connect instance;

    private govacate_connect() {
        try {
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("Connexion établie avec succès à govacate_db !");
        } catch (SQLException e) {
            System.err.println("Erreur de connexion : " + e.getMessage());
        }
    }

    // CORRECTION : Retourne MyDBConnexion
    public static govacate_connect getInstance() {
        if (instance == null) {
            instance = new govacate_connect();
        }
        return instance;
    }

    public Connection getConnection() {
        return connection;
    }
}
