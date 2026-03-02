package tn.esprit.projet.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDBConnexion {
    private final String URL      = "jdbc:mysql://localhost:3306/GoVacate2?autoReconnect=true&useSSL=false&allowPublicKeyRetrieval=true";
    private final String USER     = "root";
    private final String PASSWORD = "";

    private Connection connection;
    private static MyDBConnexion instance;

    private MyDBConnexion() {}

    public static MyDBConnexion getInstance() {
        if (instance == null) {
            instance = new MyDBConnexion();
        }
        return instance;
    }

    public Connection getConnection() {
        try {
            // Always check if connection is valid before returning
            if (connection == null || connection.isClosed()) {
                connection = createNewConnection();
            } else if (!isValid()) {
                // Connection exists but is invalid, reconnect
                try {
                    connection.close();
                } catch (Exception e) {
                    // Ignore errors from closing invalid connection
                }
                connection = createNewConnection();
            }
        } catch (SQLException e) {
            System.err.println("✗ Erreur SQL lors de la vérification de la connexion: " + e.getMessage());
            // Force reconnection on error
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (Exception ex) {
                // Ignore
            }
            try {
                connection = createNewConnection();
            } catch (SQLException ex) {
                System.err.println("✗ Impossible de rétablir la connexion: " + ex.getMessage());
            }
        }
        return connection;
    }

    /** Create a new database connection. */
    private Connection createNewConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("Driver MySQL introuvable: " + e.getMessage(), e);
        }
        Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
        conn.setAutoCommit(true);
        System.out.println("✅ Connexion (re)établie avec succès !");
        return conn;
    }

    /** Quick validity check — ping the server. */
    private boolean isValid() {
        try {
            return connection != null && connection.isValid(2); // 2 second timeout
        } catch (SQLException e) {
            return false;
        }
    }

    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("🔌 Connexion fermée proprement.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        // Clear the cached connection so a new one will be created
        connection = null;
    }

    /** Reconnect to the database (force a new connection). */
    public void reconnect() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (Exception e) {
            // Ignore
        }
        connection = null;
        getConnection(); // This will create a new connection
    }
}
