package tn.esprit.projet.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Database connection singleton.
 * Manages a single connection to the MySQL database.
 */
public class MyDBConnexion {

    private static final String DEFAULT_URL = "jdbc:mysql://localhost:3306/govacate";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASSWORD = "";

    private static final String URL = resolveConfig("govacate.db.url", "GOVACATE_DB_URL", DEFAULT_URL);
    private static final String USER = resolveConfig("govacate.db.user", "GOVACATE_DB_USER", DEFAULT_USER);
    private static final String PASSWORD = resolveConfig("govacate.db.password", "GOVACATE_DB_PASSWORD", DEFAULT_PASSWORD);
    private static final String CONNECT_TIMEOUT_MS = resolveConfig("govacate.db.connectTimeoutMs", "GOVACATE_DB_CONNECT_TIMEOUT_MS", "5000");
    private static final String SOCKET_TIMEOUT_MS = resolveConfig("govacate.db.socketTimeoutMs", "GOVACATE_DB_SOCKET_TIMEOUT_MS", "10000");
    
    private Connection connection;
    private static volatile MyDBConnexion instance;

    private MyDBConnexion() {
        openConnection();
    }

    private void openConnection() {
        try {
            Properties properties = new Properties();
            properties.setProperty("user", USER);
            properties.setProperty("password", PASSWORD);
            properties.setProperty("connectTimeout", CONNECT_TIMEOUT_MS);
            properties.setProperty("socketTimeout", SOCKET_TIMEOUT_MS);
            connection = DriverManager.getConnection(URL, properties);
            System.out.println("Connexion etablie avec succes: " + URL);
        } catch (SQLException e) {
            throw new IllegalStateException(buildConnectionErrorMessage(e), e);
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

    private static String resolveConfig(String systemPropertyKey, String envKey, String defaultValue) {
        String systemValue = System.getProperty(systemPropertyKey);
        if (systemValue != null && !systemValue.isBlank()) {
            return systemValue;
        }
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }
        return defaultValue;
    }

    private static String buildConnectionErrorMessage(SQLException error) {
        String rootMessage = findRootCauseMessage(error);
        return "Erreur de connexion a la base (url=" + URL + ", user=" + USER + "): " + rootMessage
                + ". Verifiez que MySQL est demarre et que les identifiants sont corrects. "
                + "Vous pouvez definir -Dgovacate.db.url, -Dgovacate.db.user et -Dgovacate.db.password.";
    }

    private static String findRootCauseMessage(Throwable error) {
        Throwable root = error;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        String message = root.getMessage();
        if (message == null || message.isBlank()) {
            return root.getClass().getSimpleName();
        }
        return message;
    }
}
