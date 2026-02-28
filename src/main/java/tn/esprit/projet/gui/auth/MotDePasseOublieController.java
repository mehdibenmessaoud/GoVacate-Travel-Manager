package tn.esprit.projet.gui.auth;

import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import tn.esprit.projet.entities.User;
import tn.esprit.projet.services.EmailVerificationService;
import tn.esprit.projet.services.UserService;
import tn.esprit.projet.utils.SceneManager;
import tn.esprit.projet.utils.ValidationUtils;

public class MotDePasseOublieController {

    @FXML private VBox step1Box;
    @FXML private VBox step2Box;
    @FXML private VBox step3Box;

    @FXML private TextField emailField;
    @FXML private Label emailDisplayLabel;
    @FXML private Label devCodeLabel;
    @FXML private TextField codeField;

    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;

    @FXML private Label errorLabel;
    @FXML private Button actionButton;
    @FXML private Button backButton;
    
    @FXML private Circle step1Circle;
    @FXML private Circle step2Circle;
    @FXML private Circle step3Circle;

    private UserService userService;
    private EmailVerificationService emailVerificationService;
    private User currentUser = null;
    private int currentStep = 1;

    @FXML
    public void initialize() {
        try {
            userService = new UserService();
            emailVerificationService = new EmailVerificationService();
        } catch (Exception e) {
            System.err.println("Erreur initialisation services: " + e.getMessage());
        }
        
        if (step1Box != null && step2Box != null && step3Box != null) {
            showStep(1);
        }
        
        if (errorLabel != null) {
            errorLabel.setText("");
            errorLabel.setVisible(false);
        }
    }

    @FXML
    private void handleAction() {
        switch (currentStep) {
            case 1: verifyEmail(); break;
            case 2: verifyCode(); break;
            case 3: resetPassword(); break;
            default:
                showError("Étape invalide");
        }
    }

    @FXML
    private void handleBack() {
        if (currentStep > 1) {
            showStep(currentStep - 1);
        } else {
            SceneManager.goToLogin();
        }
    }

    private void verifyEmail() {
        if (emailField == null) {
            showError("Erreur: Composant email non chargé");
            return;
        }
        
        String email = emailField.getText();
        
        if (!ValidationUtils.isNotEmpty(email)) {
            showError("Veuillez entrer votre email");
            return;
        }
        
        if (!ValidationUtils.isValidEmail(email)) {
            showError(ValidationUtils.getEmailErrorMessage());
            return;
        }

        try {
            if (userService == null) {
                userService = new UserService();
            }
            
            currentUser = userService.getByEmail(email.trim());
            
            if (currentUser == null) {
                showError("Aucun compte trouvé avec cet email");
                return;
            }
            
            if (emailDisplayLabel != null) {
                emailDisplayLabel.setText(email.trim());
            }
            
            hideError();
            
            if (emailVerificationService == null) {
                emailVerificationService = new EmailVerificationService();
            }
            
            String codeSent = emailVerificationService.sendVerificationCode(email.trim());
            
            if (codeSent == null) {
                showError("Impossible d'envoyer le code. Vérifiez l'API ou réessayez.");
                return;
            }
            
            if (codeField != null) {
                codeField.clear();
            }

            showStep(2);
            updateDevCodeLabel();
            
        } catch (Exception e) {
            showError("Erreur: " + e.getMessage());
            System.err.println("Erreur verifyEmail: " + e.getMessage());
        }
    }

    @FXML
    private void resendCode() {
        if (currentUser == null || currentUser.getEmail() == null) {
            showError("Session invalide. Retournez à l'étape 1.");
            return;
        }
        
        try {
            if (emailVerificationService == null) {
                emailVerificationService = new EmailVerificationService();
            }
            
            String email = currentUser.getEmail();
            boolean sent = emailVerificationService.resendCode(email);
            
            if (sent) {
                hideError();
                if (codeField != null) {
                    codeField.clear();
                }
                updateDevCodeLabel();
                if (emailVerificationService.isLastSendFallback()) {
                    showAlert("Code renvoyé (mode développement)", "Un nouveau code a été généré. Consultez l'affichage ci-dessous.", Alert.AlertType.INFORMATION);
                } else {
                    showAlert("Code renvoyé", "Un nouveau code a été envoyé à " + email, Alert.AlertType.INFORMATION);
                }
            } else {
                showError("Impossible de renvoyer le code. Réessayez.");
            }
        } catch (Exception e) {
            showError("Erreur lors du renvoi du code: " + e.getMessage());
            System.err.println("Erreur resendCode: " + e.getMessage());
        }
    }

    private void verifyCode() {
        if (codeField == null) {
            showError("Erreur: Composant code non chargé");
            return;
        }
        
        String code = codeField.getText();
        
        if (!ValidationUtils.isNotEmpty(code)) {
            showError("Veuillez entrer le code reçu par email");
            return;
        }

        if (currentUser == null || currentUser.getEmail() == null) {
            showError("Session invalide. Retournez à l'étape 1.");
            showStep(1);
            return;
        }

        try {
            if (emailVerificationService == null) {
                emailVerificationService = new EmailVerificationService();
            }
            
            if (!emailVerificationService.verifyCode(currentUser.getEmail(), code.trim())) {
                showError("Code incorrect ou expiré. Demandez un nouveau code.");
                return;
            }
            
            hideError();
            showStep(3);
        } catch (Exception e) {
            showError("Erreur lors de la vérification: " + e.getMessage());
            System.err.println("Erreur verifyCode: " + e.getMessage());
        }
    }

    private void resetPassword() {
        if (newPasswordField == null || confirmPasswordField == null) {
            showError("Erreur: Composants mot de passe non chargés");
            return;
        }
        
        String newPass = newPasswordField.getText();
        String confirmPass = confirmPasswordField.getText();

        if (!ValidationUtils.isNotEmpty(newPass) || !ValidationUtils.isNotEmpty(confirmPass)) {
            showError("Veuillez remplir tous les champs");
            return;
        }
        
        if (!ValidationUtils.isValidPassword(newPass)) {
            showError(ValidationUtils.getPasswordErrorMessage());
            return;
        }
        
        if (!ValidationUtils.matchesPassword(newPass, confirmPass)) {
            showError("Les mots de passe ne correspondent pas");
            confirmPasswordField.clear();
            return;
        }

        if (currentUser == null) {
            showError("Session invalide. Retournez à l'étape 1.");
            showStep(1);
            return;
        }

        try {
            if (userService == null) {
                userService = new UserService();
            }
            
            if (userService.changerMotDePasse(currentUser.getId(), newPass)) {
                showAlert("Succès", "Votre mot de passe a été réinitialisé avec succès!", Alert.AlertType.INFORMATION);
                SceneManager.goToLogin();
            } else {
                showError("Erreur lors de la mise à jour du mot de passe");
            }
        } catch (Exception e) {
            showError("Erreur: " + e.getMessage());
            System.err.println("Erreur resetPassword: " + e.getMessage());
        }
    }

    private void showStep(int step) {
        if (step1Box == null || step2Box == null || step3Box == null || actionButton == null) {
            System.err.println("Composants FXML non chargés");
            return;
        }
        
        step1Box.setVisible(false);
        step1Box.setManaged(false);
        step2Box.setVisible(false);
        step2Box.setManaged(false);
        step3Box.setVisible(false);
        step3Box.setManaged(false);

        VBox targetBox;
        String buttonText;
        
        // Update progress indicators
        if (step1Circle != null) step1Circle.setFill(javafx.scene.paint.Color.web("#C0C0C0"));
        if (step2Circle != null) step2Circle.setFill(javafx.scene.paint.Color.web("#C0C0C0"));
        if (step3Circle != null) step3Circle.setFill(javafx.scene.paint.Color.web("#C0C0C0"));

        switch (step) {
            case 1: 
                targetBox = step1Box; 
                buttonText = "VÉRIFIER EMAIL"; 
                if (step1Circle != null) step1Circle.setFill(javafx.scene.paint.Color.web("#FF8210"));
                break;
            case 2: 
                targetBox = step2Box; 
                buttonText = "VÉRIFIER LE CODE"; 
                if (step1Circle != null) step1Circle.setFill(javafx.scene.paint.Color.web("#FF8210"));
                if (step2Circle != null) step2Circle.setFill(javafx.scene.paint.Color.web("#FF8210"));
                break;
            case 3: 
                targetBox = step3Box; 
                buttonText = "RÉINITIALISER LE MOT DE PASSE"; 
                if (step1Circle != null) step1Circle.setFill(javafx.scene.paint.Color.web("#FF8210"));
                if (step2Circle != null) step2Circle.setFill(javafx.scene.paint.Color.web("#FF8210"));
                if (step3Circle != null) step3Circle.setFill(javafx.scene.paint.Color.web("#FF8210"));
                break;
            default: 
                return;
        }
        
        targetBox.setVisible(true);
        targetBox.setManaged(true);
        actionButton.setText(buttonText);
        
        FadeTransition fade = new FadeTransition(Duration.millis(300), targetBox);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
        
        currentStep = step;
        hideError();
    }

    private void showError(String message) {
        if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setVisible(true);
        }
    }

    private void hideError() {
        if (errorLabel != null) {
            errorLabel.setText("");
            errorLabel.setVisible(false);
        }
    }

    private void updateDevCodeLabel() {
        if (devCodeLabel == null || emailVerificationService == null) return;
        if (emailVerificationService.isLastSendFallback()) {
            String code = emailVerificationService.getLastSentCodeForDev();
            devCodeLabel.setText("Mode développement – Votre code : " + (code != null ? code : ""));
            devCodeLabel.setVisible(true);
            devCodeLabel.setManaged(true);
        } else {
            devCodeLabel.setVisible(false);
            devCodeLabel.setManaged(false);
        }
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        try {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
        } catch (Exception e) {
            System.err.println("Erreur affichage alerte: " + e.getMessage());
        }
    }
}
