package tn.esprit.projet.utils;

import tn.esprit.projet.entities.User;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Gestionnaire de session unifié pour GoVacate.
 * Résout les erreurs de symboles non trouvés et gère la déconnexion.
 */
public class SessionManager {

    // 1. Instance unique pour le pattern Singleton (Indispensable pour getInstance())
    private static SessionManager instance;

    // 2. Variables de session
    private static User currentUser;
    private static LocalDateTime loginTime;
    private static boolean isLoggedIn = false;

    // Set pour suivre les utilisateurs actifs (pour statistiques)
    private static Set<Integer> activeUsers = new HashSet<>();

    // Constructeur privé pour empêcher l'instanciation externe
    private SessionManager() {}

    /**
     * Retourne l'instance unique du SessionManager.
     * @return SessionManager instance
     */
    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    // ============================================
    // CONNEXION / DÉCONNEXION
    // ============================================

    /**
     * Initialise la session lors de la connexion.
     */
    public static void login(User user) {
        currentUser = user;
        loginTime = LocalDateTime.now();
        isLoggedIn = true;
        activeUsers.add(user.getId());

        System.out.println("✅ Session démarrée: " + user.getNom() +
                " [" + user.getRoleName() + "] à " + loginTime);
    }

    /**
     * Vide les données de l'utilisateur pour la déconnexion.
     * Cette méthode est appelée par vos contrôleurs.
     */
    public static void clearSession() {
        if (currentUser != null) {
            activeUsers.remove(currentUser.getId());
            System.out.println("👋 Session terminée pour: " + currentUser.getNom());
        }
        currentUser = null;
        loginTime = null;
        isLoggedIn = false;
        // Optionnel : On peut aussi réinitialiser l'instance si nécessaire
        // instance = null;
    }

    /**
     * Alias pour clearSession afin de maintenir la compatibilité.
     */
    public static void logout() {
        clearSession();
    }

    // ============================================
    // VÉRIFICATIONS DE RÔLE ET PERMISSIONS
    // ============================================

    public static boolean isLoggedIn() {
        return isLoggedIn && currentUser != null;
    }

    public static boolean isAdmin() {
        return isLoggedIn() && currentUser.isAdmin();
    }

    public static boolean isClient() {
        return isLoggedIn() && currentUser.isClient();
    }

    public static boolean hasRole(String roleName) {
        return isLoggedIn() && currentUser.getRoleName().equalsIgnoreCase(roleName);
    }

    /**
     * Vérifie si l'utilisateur a la permission d'accéder à une ressource.
     */
    public static boolean hasPermission(String permission) {
        if (!isLoggedIn()) return false;

        switch (permission) {
            case "USER_CREATE":
            case "USER_UPDATE":
            case "USER_DELETE":
            case "VIEW_STATISTICS":
                return isAdmin();
            case "VIEW_PROFILE":
            case "EDIT_PROFILE":
                return true;
            default:
                return false;
        }
    }

    // ============================================
    // ACCÈS AUX DONNÉES
    // ============================================

    public static User getCurrentUser() {
        return currentUser;
    }

    public static int getCurrentUserId() {
        return currentUser != null ? currentUser.getId() : -1;
    }

    public static String getCurrentUserName() {
        return currentUser != null ? currentUser.getNom() : "Invité";
    }

    public static String getCurrentUserRole() {
        if (!isLoggedIn()) return "GUEST";
        return currentUser.getRoleName();
    }

    public static LocalDateTime getLoginTime() {
        return loginTime;
    }

    // ============================================
    // STATISTIQUES
    // ============================================

    public static int getActiveUsersCount() {
        return activeUsers.size();
    }

    public static Set<Integer> getActiveUserIds() {
        return new HashSet<>(activeUsers);
    }

    // ============================================
    // SÉCURITÉ: CONTRÔLE D'ACCÈS
    // ============================================

    public static void requireAuth() throws SecurityException {
        if (!isLoggedIn()) {
            throw new SecurityException("Authentification requise");
        }
    }

    public static void requireAdmin() throws SecurityException {
        if (!isAdmin()) {
            throw new SecurityException("Accès réservé aux administrateurs");
        }
    }
}