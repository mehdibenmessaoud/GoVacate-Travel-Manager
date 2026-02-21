package tn.esprit.projet.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.io.InputStream;
import java.io.IOException;

public class MyDBConnexion {
    private static final String CONFIG_FILE = "/database.properties";
    private static String URL;
    private static String USER;
    private static String PASSWORD;
    
    private Connection connection;
    private static MyDBConnexion instance;
    private static boolean configLoaded = false;

    // Bloc statique pour charger la configuration
    static {
        loadConfig();
    }

    private static void loadConfig() {
        try (InputStream input = MyDBConnexion.class.getResourceAsStream(CONFIG_FILE)) {
            if (input != null) {
                Properties props = new Properties();
                props.load(input);
                URL = props.getProperty("db.url", "jdbc:mysql://localhost:3306/pi_dev");
                USER = props.getProperty("db.user", "root");
                PASSWORD = props.getProperty("db.password", "");
                configLoaded = true;
                System.out.println("✅ Configuration chargée depuis database.properties");
            } else {
                // Valeurs par défaut
                URL = "jdbc:mysql://localhost:3306/pi_dev?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
                USER = "root";
                PASSWORD = "";
                System.out.println("⚠️ Fichier config non trouvé, utilisation des valeurs par défaut");
            }
        } catch (IOException e) {
            URL = "jdbc:mysql://localhost:3306/pi_dev?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
            USER = "root";
            PASSWORD = "";
            System.err.println("Erreur chargement config: " + e.getMessage());
        }
    }

    private MyDBConnexion() {
        try {
            // Vérifier si le driver est chargé
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
            
            if (connection != null) {
                System.out.println("✅ Connexion établie avec succès à: " + URL);
            }
        } catch (ClassNotFoundException e) {
            System.err.println("❌ Driver JDBC MySQL non trouvé: " + e.getMessage());
            throw new RuntimeException("Driver JDBC MySQL non trouvé", e);
        } catch (SQLException e) {
            System.err.println("❌ Erreur de connexion: " + e.getMessage());
            // Ne pas thrower ici pour permettre à l'app de démarrer en mode dégradé
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

    /**
     * Retourne la connexion. Lance une exception si la connexion n'a pas pu être établie,
     * pour éviter des NullPointerException dans les services.
     */
    public Connection getConnection() {
        if (connection == null) {
            // Tenter de reconnecter
            try {
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
            } catch (SQLException e) {
                throw new IllegalStateException(
                    "La connexion à la base de données n'est pas disponible. " +
                    "Vérifiez les paramètres. Erreur: " + e.getMessage(), e);
            }
        }
        
        // Vérifier si la connexion est toujours valide
        try {
            if (!connection.isValid(2)) {
                // Reconnecter si invalide
                connection.close();
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
            }
        } catch (SQLException e) {
            throw new IllegalStateException(
                "La connexion à la base de données n'est pas disponible. Erreur: " + e.getMessage(), e);
        }
        
        return connection;
    }

    /** Vérifie si la connexion est disponible. */
    public boolean isConnected() {
        try {
            return connection != null && connection.isValid(2);
        } catch (SQLException e) {
            return false;
        }
    }
    
    /** Ferme la connexion. À appeler à la fermeture de l'application. */
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("🔌 Connexion fermée");
            }
        } catch (SQLException e) {
            System.err.println("Erreur fermeture connexion: " + e.getMessage());
        }
    }
    
    /** Recrée la connexion (pour recovery après erreur). */
    public void reconnect() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
        connection = DriverManager.getConnection(URL, USER, PASSWORD);
    }
}

