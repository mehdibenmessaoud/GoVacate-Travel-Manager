package tn.esprit.projet.utils;

import java.util.regex.Pattern;

/**
 * Utilitaire de validation pour les formulaires
 */
public class ValidationUtils {

    // Patterns de validation
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );
    
    private static final Pattern TELEPHONE_PATTERN = Pattern.compile(
        "^[0-9]{8}$"
    );
    
    private static final Pattern NOM_PATTERN = Pattern.compile(
        "^[a-zA-ZÀ-ÿ\\s'-]{2,50}$"
    );

    /**
     * Valide une adresse email
     * @param email l'email à valider
     * @return true si valide, false sinon
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    /**
     * Valide un numéro de téléphone
     * @param telephone le téléphone à valider
     * @return true si valide, false sinon
     */
    public static boolean isValidTelephone(String telephone) {
        if (telephone == null || telephone.trim().isEmpty()) {
            return false;  // Téléphone peut être optionnel
        }
        return TELEPHONE_PATTERN.matcher(telephone.trim()).matches();
    }

    /**
     * Valide un nom (prénom, nom de famille)
     * @param nom le nom à valider
     * @return true si valide, false sinon
     */
    public static boolean isValidNom(String nom) {
        if (nom == null || nom.trim().isEmpty()) {
            return false;
        }
        return NOM_PATTERN.matcher(nom.trim()).matches();
    }

    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
        "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$"
    );

    /**
     * Valide la force d'un mot de passe
     * @param password le mot de passe à valider
     * @return true si valide
     */
    public static boolean isValidPassword(String password) {
        if (password == null) {
            return false;
        }
        return PASSWORD_PATTERN.matcher(password).matches();
    }

    /**
     * Valide qu'une chaîne n'est pas vide ou seulement des espaces
     * @param value la chaîne à valider
     * @return true si non vide
     */
    public static boolean isNotEmpty(String value) {
        return value != null && !value.trim().isEmpty();
    }

    /**
     * Valide qu'un mot de passe et sa confirmation correspondent
     * @param password mot de passe
     * @param confirmPassword confirmation
     * @return true si identiques
     */
    public static boolean matchesPassword(String password, String confirmPassword) {
        if (password == null || confirmPassword == null) {
            return false;
        }
        return password.equals(confirmPassword);
    }

    /**
     * Valide qu'une date de naissance n'est pas dans le futur
     * @param date la date à valider
     * @return true si valide
     */
    public static boolean isValidDateDeNaissance(java.time.LocalDate date) {
        if (date == null) {
            return true;  // Optionnel
        }
        return !date.isAfter(java.time.LocalDate.now());
    }

    /**
     * Retourne un message d'erreur pour email invalide
     */
    public static String getEmailErrorMessage() {
        return "Format d'email invalide (ex: nom@exemple.com)";
    }

    /**
     * Retourne un message d'erreur pour téléphone invalide
     */
    public static String getTelephoneErrorMessage() {
        return "Le numéro de téléphone doit contenir exactement 8 chiffres";
    }

    /**
     * Retourne un message d'erreur pour nom invalide
     */
    public static String getNomErrorMessage() {
        return "Le nom doit contenir 2 à 50 caractères (lettres uniquement)";
    }

    /**
     * Retourne un message d'erreur pour mot de passe invalide
     */
    public static String getPasswordErrorMessage() {
        return "Le mot de passe doit contenir au moins 8 caractères, incluant majuscule, minuscule, chiffre et caractère spécial.";
    }
}

