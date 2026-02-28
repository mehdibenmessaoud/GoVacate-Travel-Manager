/*
package tn.esprit.projet.utils;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SceneManager {
    private static final Logger LOG = Logger.getLogger(SceneManager.class.getName());
    private static Stage primaryStage;
    private static Image appIcon;
    private static Pane adminContentPane;
    private static Pane clientContentPane;

    public static void setPrimaryStage(Stage stage) {
        if (stage == null) {
            throw new IllegalArgumentException("Le Stage ne peut pas être null");
        }
        primaryStage = stage;
        applyAppIcons(primaryStage);
    }

    // ============================================
    // NAVIGATION PRINCIPALE
    // ============================================

    public static void switchTo(String fxmlPath) {
        if (fxmlPath == null || fxmlPath.isEmpty()) {
            LOG.severe("Chemin FXML null ou vide");
            showErrorDialog("Erreur de navigation", "Chemin de vue invalide");
            return;
        }
        
        try {
            System.out.println("🔄 Navigation vers: " + fxmlPath);

            // Vérifier que le ressource existe
            URL resourceUrl = SceneManager.class.getResource(fxmlPath);
            if (resourceUrl == null) {
                LOG.severe("FXML non trouvé: " + fxmlPath);
                showErrorDialog("Erreur", "Vue non trouvée: " + fxmlPath);
                return;
            }

            FXMLLoader loader = new FXMLLoader(resourceUrl);
            Parent root = loader.load();

            if (root == null) {
                LOG.severe("Root null après chargement FXML: " + fxmlPath);
                showErrorDialog("Erreur", "Erreur lors du chargement de la vue");
                return;
            }

            Scene scene = new Scene(root);

            // Charger le CSS global
            URL cssUrl = SceneManager.class.getResource("/css/style.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }
            
            // Style dashboard admin ou client selon le chemin
            if (fxmlPath.contains("/admin/") || fxmlPath.contains("/client/")) {
                URL commonDashboardCss = SceneManager.class.getResource("/css/common-dashboard.css");
                if (commonDashboardCss != null) {
                    scene.getStylesheets().add(commonDashboardCss.toExternalForm());
                } else {
                    LOG.warning("CSS commun non trouvé: /css/common-dashboard.css");
                }
            }

            if (fxmlPath.contains("/admin/")) {
                URL adminCss = SceneManager.class.getResource("/css/admin-dashboard-style.css");
                if (adminCss != null) {
                    scene.getStylesheets().add(adminCss.toExternalForm());
                }
            } else if (fxmlPath.contains("/client/")) {
                URL clientCss = SceneManager.class.getResource("/css/client-dashboard-style.css");
                if (clientCss != null) {
                    scene.getStylesheets().add(clientCss.toExternalForm());
                }
            }

            if (primaryStage == null) {
                LOG.severe("PrimaryStage est null");
                showErrorDialog("Erreur", "Erreur interne: Stage non initialisé");
                return;
            }
            
            primaryStage.setScene(scene);
            applyAppIcons(primaryStage);
            primaryStage.show();

        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Erreur navigation vers: " + fxmlPath, e);
            showErrorDialog("Erreur", "Erreur lors de la navigation: " + e.getMessage());
        }
    }

    // ============================================
    // REDIRECTION SELON RÔLE
    // ============================================

    public static void redirectBasedOnRole() {
        if (SessionManager.isAdmin()) {
            switchTo("/Admin_Dashboard.fxml");
        } else if (SessionManager.isClient()) {
            switchTo("/fxml/client/ClientDashboardLayout.fxml");
        } else {
            System.err.println("Rôle inconnu!");
            switchTo("/fxml/auth/Auth.fxml");
        }
    }

    */
/** Enregistre la zone de contenu du dashboard admin (appelé par AdminDashboardController). *//*

    public static void setAdminContentPane(Pane pane) {
        if (pane == null) {
            throw new IllegalArgumentException("Le pane ne peut pas être null");
        }
        adminContentPane = pane;
    }

    */
/** Charge une vue dans la zone de contenu admin (sans changer de scène). *//*

    public static void loadAdminContent(String fxmlPath) {
        if (adminContentPane != null) {
            loadInto(adminContentPane, fxmlPath);
        } else {
            LOG.warning("AdminContentPane est null, impossible de charger: " + fxmlPath);
        }
    }

    */
/** Enregistre la zone de contenu du dashboard client. *//*

    public static void setClientContentPane(Pane pane) {
        if (pane == null) {
            throw new IllegalArgumentException("Le pane ne peut pas être null");
        }
        clientContentPane = pane;
    }

    */
/** Charge une vue dans la zone de contenu client. *//*

    public static void loadClientContent(String fxmlPath) {
        if (clientContentPane != null) {
            loadInto(clientContentPane, fxmlPath);
        } else {
            LOG.warning("ClientContentPane est null, impossible de charger: " + fxmlPath);
        }
    }

    // ============================================
    // CHARGEMENT DYNAMIQUE (pour dashboards)
    // ============================================

    public static void loadInto(Pane container, String fxmlPath) {
        if (container == null) {
            LOG.severe("Container null pour chargement: " + fxmlPath);
            return;
        }
        
        if (fxmlPath == null || fxmlPath.isEmpty()) {
            LOG.severe("Chemin FXML vide");
            showErrorInContainer(container, "Chemin de vue invalide");
            return;
        }
        
        try {
            URL resourceUrl = SceneManager.class.getResource(fxmlPath);
            if (resourceUrl == null) {
                LOG.severe("FXML non trouvé: " + fxmlPath);
                showErrorInContainer(container, "Vue non trouvée: " + fxmlPath);
                return;
            }
            
            FXMLLoader loader = new FXMLLoader(resourceUrl);
            Parent content = loader.load();

            container.getChildren().clear();
            if (content instanceof javafx.scene.layout.Region) {
                ((javafx.scene.layout.Region) content).setMaxWidth(Double.MAX_VALUE);
                ((javafx.scene.layout.Region) content).setMaxHeight(Double.MAX_VALUE);
            }
            container.getChildren().add(content);

        } catch (IOException e) {
            System.err.println("❌ Erreur chargement contenu: " + fxmlPath);
            LOG.log(Level.WARNING, "Erreur chargement: " + fxmlPath, e);
            showErrorInContainer(container, "Erreur de chargement: " + fxmlPath);
        } catch (Exception e) {
            System.err.println("❌ Erreur inattendue: " + e.getMessage());
            LOG.log(Level.SEVERE, "Erreur inattendue: " + fxmlPath, e);
            showErrorInContainer(container, "Erreur inattendue");
        }
    }
    
    private static void showErrorInContainer(Pane container, String message) {
        if (container != null) {
            container.getChildren().clear();
            Label errorLabel = new Label(message);
            errorLabel.setStyle("-fx-text-fill: #E74C3C; -fx-padding: 20px; -fx-font-size: 14px;");
            container.getChildren().add(errorLabel);
        }
    }

    // ============================================
    // NAVIGATION SPÉCIFIQUE
    // ============================================

    public static void goToLogin() {
        switchTo("/fxml/auth/Auth.fxml");
    }

    public static void goToRegister() {
        switchTo("/fxml/auth/Auth.fxml");
    }

    public static void goToForgotPassword() {
        switchTo("/fxml/auth/MotDePasseOublie.fxml");
    }

    public static void goToProfile() {
        if (SessionManager.isLoggedIn()) {
            switchTo("/fxml/profile/Profile.fxml");
        } else {
            goToLogin();
        }
    }
    
    // ============================================
    // ICONES D'APPLICATION
    // ============================================

    public static void applyAppIcons(Stage stage) {
        if (stage == null) return;
        try {
            if (appIcon == null) {
                appIcon = new Image(SceneManager.class.getResourceAsStream("/logo/logo.jpg"));
            }
            if (appIcon != null && !stage.getIcons().contains(appIcon)) {
                stage.getIcons().add(appIcon);
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Impossible de charger l'icone: /logo/logo.jpg", e);
        }
    }

    // ============================================
    // DIALOGS D'ERREUR
    // ============================================
    
    private static void showErrorDialog(String title, String message) {
        try {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        } catch (Exception e) {
            // Fallback si même l'alerte ne peut pas s'afficher
            System.err.println("ERREUR: " + title + " - " + message);
        }
    }
}
*/


package tn.esprit.projet.utils;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SceneManager {
    private static final Logger LOG = Logger.getLogger(SceneManager.class.getName());
    private static Stage primaryStage;
    private static Image appIcon;
    private static Pane adminContentPane;
    private static Pane clientContentPane;

    public static void setPrimaryStage(Stage stage) {
        if (stage == null) {
            throw new IllegalArgumentException("Le Stage ne peut pas être null");
        }
        primaryStage = stage;
        // On n'appelle pas applyAppIcons ici car l'icône peut faire planter le démarrage
    }

    // ============================================
    // NAVIGATION PRINCIPALE
    // ============================================

    public static void switchTo(String fxmlPath) {
        if (fxmlPath == null || fxmlPath.isEmpty()) {
            LOG.severe("Chemin FXML null ou vide");
            return;
        }

        try {
            System.out.println("🔄 Navigation vers: " + fxmlPath);

            URL resourceUrl = SceneManager.class.getResource(fxmlPath);
            if (resourceUrl == null) {
                LOG.severe("FXML non trouvé: " + fxmlPath);
                showErrorDialog("Erreur", "Vue non trouvée: " + fxmlPath);
                return;
            }

            FXMLLoader loader = new FXMLLoader(resourceUrl);
            Parent root = loader.load();

            Scene scene = new Scene(root);

            // --- GESTION DU STYLE CSS ---
            // On charge le style global systématiquement
            URL cssUrl = SceneManager.class.getResource("/css/style.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            } else {
                System.out.println("⚠️ Attention: /css/style.css non trouvé");
            }

            // Si c'est l'admin dashboard (même à la racine), on charge le style admin
            if (fxmlPath.toLowerCase().contains("admin")) {
                URL adminCss = SceneManager.class.getResource("/css/admin-dashboard-style.css");
                if (adminCss != null) {
                    scene.getStylesheets().add(adminCss.toExternalForm());
                }
            }

            if (primaryStage == null) {
                LOG.severe("PrimaryStage est null. Vérifiez votre App.java");
                return;
            }

            primaryStage.setScene(scene);
            applyAppIcons(primaryStage); // On tente de charger l'icône sans risquer de crash
            primaryStage.show();

        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Erreur navigation vers: " + fxmlPath, e);
            e.printStackTrace();
        }
    }

    // ============================================
    // REDIRECTION SELON RÔLE
    // ============================================

    public static void redirectBasedOnRole() {
        if (SessionManager.isAdmin()) {
            // Correspondance avec ton fichier à la racine
            switchTo("/AdminView.fxml");
        } else if (SessionManager.isClient()) {
            // À ajuster si ton fichier client est aussi à la racine
            switchTo("/ClientDashboard2.fxml");
        } else {
            switchTo("/Auth.fxml");
        }
    }

    // ============================================
    // CHARGEMENT DYNAMIQUE DANS LES PANES
    // ============================================

    public static void loadInto(Pane container, String fxmlPath) {
        if (container == null) return;

        try {
            URL resourceUrl = SceneManager.class.getResource(fxmlPath);
            if (resourceUrl == null) {
                showErrorInContainer(container, "Vue non trouvée: " + fxmlPath);
                return;
            }

            FXMLLoader loader = new FXMLLoader(resourceUrl);
            Parent content = loader.load();

            container.getChildren().clear();
            container.getChildren().add(content);

        } catch (IOException e) {
            LOG.log(Level.WARNING, "Erreur chargement contenu: " + fxmlPath, e);
            showErrorInContainer(container, "Erreur de chargement");
        }
    }

    private static void showErrorInContainer(Pane container, String message) {
        container.getChildren().clear();
        container.getChildren().add(new Label(message));
    }

    // ============================================
    // ICONES (SÉCURISÉ CONTRE LE NULL)
    // ============================================

    public static void applyAppIcons(Stage stage) {
        if (stage == null) return;
        try {
            if (appIcon == null) {
                InputStream is = SceneManager.class.getResourceAsStream("/logo/logo.jpg");
                if (is != null) {
                    appIcon = new Image(is);
                    stage.getIcons().add(appIcon);
                } else {
                    // Si pas d'image, on ne fait rien (pas de crash)
                    System.out.println("ℹ️ Logo non trouvé à /logo/logo.jpg");
                }
            }
        } catch (Exception e) {
            // On ignore l'erreur d'icône pour laisser l'app démarrer
        }
    }

    private static void showErrorDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.show();
    }
    // ============================================
// MÉTHODES DE NAVIGATION RAPIDE
// ============================================

    public static void goToLogin() {
        switchTo("/Auth.fxml");
    }

    public static void goToRegister() {
        // Si tu as un switch dans Auth.fxml, on retourne simplement à Auth
        switchTo("/Auth.fxml");
    }
    /**
     * Charge une vue FXML dans le conteneur central du dashboard client.
     * @param fxmlPath Le chemin vers le fichier FXML (ex: "/fxml/profile/Profile.fxml")
     */
    public static void loadClientContent(String fxmlPath) {
        if (clientContentPane != null) {
            // On utilise la méthode générique loadInto déjà présente dans votre classe
            loadInto(clientContentPane, fxmlPath);
        } else {
            LOG.warning("Impossible de charger le contenu : clientContentPane n'est pas initialisé.");
            // Optionnel : si le pane est null, on peut tenter un switchTo classique
            // switchTo(fxmlPath);
        }
    }

    public static void setClientContentPane(Pane pane) {
        if (pane == null) {
            throw new IllegalArgumentException("Le pane ne peut pas être null");
        }
        clientContentPane = pane;
    }
}