package tn.esprit.projet.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDBConnexion {

    private static final String URL      = "jdbc:mysql://localhost:3306/govacate";
    private static final String USER     = "root";
    private static final String PASSWORD = "";

    private Connection connection;
    private static volatile MyDBConnexion instance;

    private MyDBConnexion() {
        openConnection();
    }

    private void openConnection() {
        try {
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("Connexion etablie avec succes!");
        } catch (SQLException e) {
            throw new IllegalStateException("Erreur de connexion a la base: " + e.getMessage(), e);
        }
    }

    public static MyDBConnexion getInstance() {
        if (instance == null) {
            synchronized (MyDBConnexion.class) {
                if (instance == null) {
                    instance = new MyDBConnexion();
                }
            }
        }
        return instance;
    }

    public synchronized Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                openConnection();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Connexion BD invalide: " + e.getMessage(), e);
        }
        return connection;
    }
}