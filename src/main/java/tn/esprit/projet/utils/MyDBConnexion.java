package tn.esprit.projet.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDBConnexion {
    private final String URL = "jdbc:mysql://localhost:3306/GoVacate2"; // Thabbet mel esm mta el DB dima
    private final String USER = "root";
    private final String PASSWORD = "";
    private Connection connection;
    private static MyDBConnexion instance;

    private MyDBConnexion() {
        // Le constructeur peut rester vide ou appeler getConnection() une fois
    }

    public static MyDBConnexion getInstance() {
        if (instance == null) {
            instance = new MyDBConnexion();
        }
        return instance;
    }

    public Connection getConnection() {
        try {
            // 🔥 FIX: Ken el connection null walla tsakret (Closed), n-3awdou n-connectiw
            if (connection == null || connection.isClosed()) {
                Class.forName("com.mysql.cj.jdbc.Driver");
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
                System.out.println("✅ Connexion (re)établie avec succès !");
            }
        } catch (ClassNotFoundException e) {
            System.err.println("✗ Driver MySQL introuvable: " + e.getMessage());
        } catch (SQLException e) {
            System.err.println("✗ Erreur SQL lors de la connexion: " + e.getMessage());
        }
        return connection;
    }

    // Méthode de secours pour fermer proprement si besoin
    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("🔌 Connexion fermée proprement.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}