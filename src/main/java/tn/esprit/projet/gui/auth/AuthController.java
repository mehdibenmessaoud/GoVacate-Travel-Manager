package tn.esprit.projet.gui.auth;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.scene.text.Text;
import javafx.util.Duration;
import tn.esprit.projet.entities.User;
import tn.esprit.projet.services.UserService;
import tn.esprit.projet.utils.SceneManager;
import tn.esprit.projet.utils.SessionManager;
import tn.esprit.projet.utils.ValidationUtils;

import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;

import javafx.event.EventHandler;
import javafx.scene.input.KeyEvent;

public class AuthController implements Initializable {

    @FXML private Pane slidingPane;
    @FXML private Button switchBtn;
    @FXML private Text overlayTitle;
    @FXML private Text overlayText;

    @FXML private TextField loginEmail;
    @FXML private PasswordField loginPass;
    @FXML private TextField loginPassVisible;
    @FXML private Button toggleLoginPass;

    @FXML private TextField signName;
    @FXML private TextField signEmail;
    @FXML private PasswordField signPass;
    @FXML private TextField signPassVisible;
    @FXML private Button toggleSignPass;
    
    @FXML private PasswordField signPassConfirm;
    @FXML private TextField signPassConfirmVisible;
    @FXML private Button toggleSignPassConfirm;
    
    @FXML private TextField signTel;
    @FXML private DatePicker signDate;

    @FXML private Label loginErrorLabel;
    @FXML private Label registerErrorLabel;
    @FXML private Label passwordStrengthLabel;

    private boolean isLoginView = true;
    private UserService userService;

    // Track password visibility state
    private boolean isLoginPassVisible = false;
    private boolean isSignPassVisible = false;
    private boolean isSignPassConfirmVisible = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            userService = new UserService();
        } catch (Exception e) {
            System.err.println("Erreur initialisation UserService: " + e.getMessage());
        }
        
        if (loginErrorLabel != null) loginErrorLabel.setText("");
        if (loginErrorLabel != null) loginErrorLabel.setVisible(false);
        if (registerErrorLabel != null) registerErrorLabel.setText("");
        if (registerErrorLabel != null) registerErrorLabel.setVisible(false);
        
        // Initialize password visibility toggle buttons
        if (toggleLoginPass != null) {
            toggleLoginPass.setOnAction(e -> togglePasswordVisibility("login"));
            toggleLoginPass.setText("👁");
        }
        
        if (toggleSignPass != null) {
            toggleSignPass.setOnAction(e -> togglePasswordVisibility("sign"));
            toggleSignPass.setText("👁");
        }
        
        if (toggleSignPassConfirm != null) {
            toggleSignPassConfirm.setOnAction(e -> togglePasswordVisibility("signConfirm"));
            toggleSignPassConfirm.setText("👁");
        }
        
        // Listen to password field
        if (signPass != null) {
            signPass.setOnKeyReleased(event -> updatePasswordStrength(signPass.getText()));
        }
        
        if (signPassVisible != null) {
            signPassVisible.setOnKeyReleased(event -> updatePasswordStrength(signPassVisible.getText()));
        }
    }
    
    @FXML
    private void togglePasswordVisibility(String field) {
        switch(field) {
            case "login":
                isLoginPassVisible = !isLoginPassVisible;
                if (isLoginPassVisible) {
                    if (loginPassVisible != null && loginPass != null) {
                        loginPassVisible.setText(loginPass.getText());
                        loginPass.setVisible(false);
                        loginPassVisible.setVisible(true);
                    }
                    if (toggleLoginPass != null) toggleLoginPass.setText("🔒");
                } else {
                    if (loginPass != null && loginPassVisible != null) {
                        loginPass.setText(loginPassVisible.getText());
                        loginPassVisible.setVisible(false);
                        loginPass.setVisible(true);
                    }
                    if (toggleLoginPass != null) toggleLoginPass.setText("👁");
                }
                break;
                
            case "sign":
                isSignPassVisible = !isSignPassVisible;
                if (isSignPassVisible) {
                    if (signPassVisible != null && signPass != null) {
                        signPassVisible.setText(signPass.getText());
                        signPass.setVisible(false);
                        signPassVisible.setVisible(true);
                        updatePasswordStrength(signPass.getText());
                    }
                    if (toggleSignPass != null) toggleSignPass.setText("🔒");
                } else {
                    if (signPass != null && signPassVisible != null) {
                        signPass.setText(signPassVisible.getText());
                        signPassVisible.setVisible(false);
                        signPass.setVisible(true);
                        updatePasswordStrength(signPass.getText());
                    }
                    if (toggleSignPass != null) toggleSignPass.setText("👁");
                }
                break;
                
            case "signConfirm":
                isSignPassConfirmVisible = !isSignPassConfirmVisible;
                if (isSignPassConfirmVisible) {
                    if (signPassConfirmVisible != null && signPassConfirm != null) {
                        signPassConfirmVisible.setText(signPassConfirm.getText());
                        signPassConfirm.setVisible(false);
                        signPassConfirmVisible.setVisible(true);
                    }
                    if (toggleSignPassConfirm != null) toggleSignPassConfirm.setText("🔒");
                } else {
                    if (signPassConfirm != null && signPassConfirmVisible != null) {
                        signPassConfirm.setText(signPassConfirmVisible.getText());
                        signPassConfirmVisible.setVisible(false);
                        signPassConfirm.setVisible(true);
                    }
                    if (toggleSignPassConfirm != null) toggleSignPassConfirm.setText("👁");
                }
                break;
        }
    }

    private void updatePasswordStrength(String password) {
        if (passwordStrengthLabel == null) return;
        
        if (password == null || password.isEmpty()) {
            passwordStrengthLabel.setText("");
            passwordStrengthLabel.setVisible(false);
            return;
        }
        
        int length = password.length();
        
        if (length < 6) {
            passwordStrengthLabel.setText("Faible");
            passwordStrengthLabel.setStyle("-fx-text-fill: #E74C3C; -fx-font-size: 12px; -fx-font-weight: bold;");
        } else if (length == 6) {
            passwordStrengthLabel.setText("Moyenne");
            passwordStrengthLabel.setStyle("-fx-text-fill: #F39C12; -fx-font-size: 12px; -fx-font-weight: bold;");
        } else {
            passwordStrengthLabel.setText("Fort");
            passwordStrengthLabel.setStyle("-fx-text-fill: #27AE60; -fx-font-size: 12px; -fx-font-weight: bold;");
        }
        
        passwordStrengthLabel.setVisible(true);
    }

    @FXML
    private void handleSwitch() {
        if (slidingPane == null || switchBtn == null || overlayTitle == null || overlayText == null) {
            System.err.println("Composants FXML non chargés");
            return;
        }
        
        TranslateTransition translate = new TranslateTransition(Duration.seconds(0.7), slidingPane);
        FadeTransition fadeOut = new FadeTransition(Duration.seconds(0.3), slidingPane);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.2);
        FadeTransition fadeIn = new FadeTransition(Duration.seconds(0.3), slidingPane);
        fadeIn.setFromValue(0.2);
        fadeIn.setToValue(1.0);

        // Determine target state based on current state
        boolean targetIsLogin = !isLoginView;

        fadeOut.setOnFinished(e -> {
            if (targetIsLogin) {
                // We are moving TO Login View (Overlay moves Right, covers Register)
                overlayTitle.setText("Nouveau Voyageur ?");
                overlayText.setText("Inscrivez-vous dès aujourd'hui et commencez à planifier vos prochaines vacances.");
                switchBtn.setText("CRÉER UN COMPTE");
            } else {
                // We are moving TO Register View (Overlay moves Left, covers Login)
                overlayTitle.setText("Déjà Inscrit ?");
                overlayText.setText("Connectez-vous pour accéder à vos réservations et offres personnalisées.");
                switchBtn.setText("SE CONNECTER");
            }
        });

        if (isLoginView) {
            // Currently Login View (Overlay at Right). Move to Left (-500).
            translate.setToX(-500);
            isLoginView = false;
        } else {
            // Currently Register View (Overlay at Left). Move to Right (0).
            translate.setToX(0);
            isLoginView = true;
        }

        ParallelTransition parallel = new ParallelTransition(translate, new SequentialTransition(fadeOut, fadeIn));
        parallel.play();
        
        if (loginErrorLabel != null) {
            loginErrorLabel.setText("");
            loginErrorLabel.setVisible(false);
        }
        if (registerErrorLabel != null) {
            registerErrorLabel.setText("");
            registerErrorLabel.setVisible(false);
        }
    }

    @FXML
    private void processLogin() {
        if (loginEmail == null || loginPass == null) {
            showLoginError("Erreur: Composants non chargés");
            return;
        }
        
        String email = loginEmail.getText();
        String password = isLoginPassVisible && loginPassVisible != null && loginPassVisible.isVisible() 
            ? loginPassVisible.getText() 
            : loginPass.getText();

        if (!ValidationUtils.isNotEmpty(email)) {
            showLoginError("Veuillez entrer votre email");
            return;
        }
        
        if (!ValidationUtils.isValidEmail(email)) {
            showLoginError(ValidationUtils.getEmailErrorMessage());
            return;
        }

        if (!ValidationUtils.isNotEmpty(password)) {
            showLoginError("Veuillez entrer votre mot de passe");
            return;
        }

        try {
            if (userService == null) {
                userService = new UserService();
            }
            
            User user = userService.authenticate(email.trim(), password);

            if (user != null) {
                if (!user.isActive()) {
                    showLoginError("Votre compte est désactivé");
                    return;
                }
                SessionManager.login(user);
                SceneManager.redirectBasedOnRole();
            } else {
                showLoginError("Email ou mot de passe incorrect");
            }
        } catch (Exception e) {
            showLoginError("Erreur de connexion: " + e.getMessage());
            System.err.println("Erreur authentification: " + e.getMessage());
        }
    }

    @FXML
    private void processRegister() {
        if (signName == null || signEmail == null || signPass == null) {
            showRegisterError("Erreur: Composants non chargés");
            return;
        }
        
        String nom = signName.getText();
        String email = signEmail.getText();
        String pass = isSignPassVisible && signPassVisible != null && signPassVisible.isVisible() 
            ? signPassVisible.getText() 
            : signPass.getText();
        String tel = (signTel != null) ? signTel.getText() : "";
        LocalDate dateN = (signDate != null) ? signDate.getValue() : null;

        if (!ValidationUtils.isNotEmpty(nom)) {
            showRegisterError("Le nom est obligatoire");
            return;
        }
        
        if (!ValidationUtils.isValidNom(nom)) {
            showRegisterError(ValidationUtils.getNomErrorMessage());
            return;
        }

        if (!ValidationUtils.isNotEmpty(email)) {
            showRegisterError("L'email est obligatoire");
            return;
        }
        
        if (!ValidationUtils.isValidEmail(email)) {
            showRegisterError(ValidationUtils.getEmailErrorMessage());
            return;
        }

        if (!ValidationUtils.isNotEmpty(pass)) {
            showRegisterError("Le mot de passe est obligatoire");
            return;
        }
        
        if (!ValidationUtils.isValidPassword(pass)) {
            showRegisterError(ValidationUtils.getPasswordErrorMessage());
            return;
        }

        String passConfirm = isSignPassConfirmVisible && signPassConfirmVisible != null && signPassConfirmVisible.isVisible() 
            ? signPassConfirmVisible.getText() 
            : (signPassConfirm != null ? signPassConfirm.getText() : "");
        
        if (!ValidationUtils.isNotEmpty(passConfirm)) {
            showRegisterError("Veuillez confirmer votre mot de passe");
            return;
        }
        
        if (!ValidationUtils.matchesPassword(pass, passConfirm)) {
            showRegisterError("Les mots de passe ne correspondent pas");
            return;
        }

        if (ValidationUtils.isNotEmpty(tel) && !ValidationUtils.isValidTelephone(tel)) {
            showRegisterError(ValidationUtils.getTelephoneErrorMessage());
            return;
        }

        if (!ValidationUtils.isValidDateDeNaissance(dateN)) {
            showRegisterError("La date de naissance ne peut pas être dans le futur");
            return;
        }

        try {
            if (userService == null) {
                userService = new UserService();
            }
            
            if (userService.emailExiste(email.trim())) {
                showRegisterError("Cet email est déjà utilisé");
                return;
            }

            User newUser = new User(nom.trim(), email.trim(), pass, tel.trim(), dateN, 2, "actif");
            userService.create(newUser);
            showAlert("Succès", "Compte créé! Vous pouvez maintenant vous connecter.", Alert.AlertType.INFORMATION);
            clearRegisterFields();
            handleSwitch();
        } catch (java.sql.SQLException e) {
            showRegisterError(e.getMessage() != null ? e.getMessage() : "Erreur lors de la création du compte");
            System.err.println("Erreur création compte: " + e.getMessage());
        } catch (Exception e) {
            showRegisterError("Erreur: " + e.getMessage());
            System.err.println("Erreur création compte: " + e.getMessage());
        }
    }

    @FXML
    private void goToMotDePasseOublie() {
        SceneManager.goToForgotPassword();
    }

    private void clearRegisterFields() {
        if (signName != null) signName.clear();
        if (signEmail != null) signEmail.clear();
        if (signPass != null) signPass.clear();
        if (signPassVisible != null) signPassVisible.clear();
        if (signPassConfirm != null) signPassConfirm.clear();
        if (signPassConfirmVisible != null) signPassConfirmVisible.clear();
        if (signTel != null) signTel.clear();
        if (signDate != null) signDate.setValue(null);
        if (registerErrorLabel != null) {
            registerErrorLabel.setText("");
            registerErrorLabel.setVisible(false);
        }
        if (passwordStrengthLabel != null) {
            passwordStrengthLabel.setText("");
            passwordStrengthLabel.setVisible(false);
        }
        
        // Reset visibility states
        isSignPassVisible = false;
        isSignPassConfirmVisible = false;
    }

    private void showLoginError(String message) {
        if (loginErrorLabel != null) {
            loginErrorLabel.setText(message);
            loginErrorLabel.setVisible(true);
        }
    }

    private void showRegisterError(String message) {
        if (registerErrorLabel != null) {
            registerErrorLabel.setText(message);
            registerErrorLabel.setVisible(true);
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
