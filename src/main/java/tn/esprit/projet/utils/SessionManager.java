package tn.esprit.projet.utils;

import tn.esprit.projet.entities.User;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

public class SessionManager {
    private static User currentUser;
    private static LocalDateTime loginTime;
    private static boolean isLoggedIn = false;

    // Set pour suivre les utilisateurs actifs (pour statistiques)
    private static Set<Integer> activeUsers = new HashSet<>();

    // ============================================
    // CONNEXION / DÉCONNEXION
    // ============================================

    public static void login(User user) {
        currentUser = user;
        loginTime = LocalDateTime.now();
        isLoggedIn = true;
        activeUsers.add(user.getId());

        System.out.println("✅ Session démarrée: " + user.getNom() +
                " [" + user.getRoleName() + "] à " + loginTime);
    }

    public static void logout() {
        if (currentUser != null) {
            activeUsers.remove(currentUser.getId());
            System.out.println("👋 Session terminée pour: " + currentUser.getNom());
        }
        currentUser = null;
        loginTime = null;
        isLoggedIn = false;
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
     * Vérifie si l'utilisateur a la permission d'accéder à une ressource
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
                return true;  // Tout le monde peut voir son profil
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
