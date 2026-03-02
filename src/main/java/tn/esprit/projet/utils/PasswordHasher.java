package tn.esprit.projet.utils;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordHasher {

    private static final int WORKLOAD = 12;  // Complexité du hash

    /**
     * Hash un mot de passe avec BCrypt
     */
    public static String hashPassword(String password) {
        String salt = BCrypt.gensalt(WORKLOAD);
        return BCrypt.hashpw(password, salt);
    }

    /**
     * Vérifie si le mot de passe correspond au hash
     */
    public static boolean verifyPassword(String password, String hashedPassword) {
        if (password == null || hashedPassword == null) {
            return false;
        }
        return BCrypt.checkpw(password, hashedPassword);
    }

    /**
     * Vérifie si un mot de passe est déjà hashé (pour éviter double hash)
     */
    public static boolean isHashed(String password) {
        return password != null && password.startsWith("$2a$");
    }
}
