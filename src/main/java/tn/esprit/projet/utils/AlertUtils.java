package tn.esprit.projet.utils;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.util.Optional;

/**
 * Utilitaire pour afficher des boîtes de dialogue Alert JavaFX
 */
public final class AlertUtils {

    private AlertUtils() {}

    public static void showError(String title, String message) {
        showAlert(Alert.AlertType.ERROR, title, message);
    }

    public static void showInfo(String title, String message) {
        showAlert(Alert.AlertType.INFORMATION, title, message);
    }

    public static void showWarning(String title, String message) {
        showAlert(Alert.AlertType.WARNING, title, message);
    }

    public static Optional<ButtonType> showConfirmation(String title, String message) {
        try {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle(title != null ? title : "Confirmation");
            alert.setHeaderText(null);
            alert.setContentText(message != null ? message : "");
            return alert.showAndWait();
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private static void showAlert(Alert.AlertType type, String title, String message) {
        try {
            Alert alert = new Alert(type);
            alert.setTitle(title != null ? title : "");
            alert.setHeaderText(null);
            alert.setContentText(message != null ? message : "");
            alert.showAndWait();
        } catch (Exception e) {
            System.err.println("Erreur affichage alerte: " + e.getMessage());
        }
    }
}
