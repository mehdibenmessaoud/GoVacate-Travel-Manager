package tn.esprit.projet.gui.auth;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.scene.text.Text;
import javafx.util.Duration;
import tn.esprit.projet.entities.User;
import tn.esprit.projet.services.SocialAuthService;
import tn.esprit.projet.services.UserService;
import tn.esprit.projet.utils.SceneManager;
import tn.esprit.projet.utils.SessionManager;
import tn.esprit.projet.utils.ValidationUtils;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ResourceBundle;

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
    private SocialAuthService socialAuthService;

    private boolean isLoginPassVisible = false;
    private boolean isSignPassVisible = false;
    private boolean isSignPassConfirmVisible = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            userService = new UserService();
            socialAuthService = new SocialAuthService();
        } catch (Exception e) {
            System.err.println("Erreur initialisation services: " + e.getMessage());
        }
        setupPasswordToggles();
    }

    private void setupPasswordToggles() {
        if (toggleLoginPass != null) toggleLoginPass.setOnAction(e -> togglePasswordVisibility("login"));
        if (toggleSignPass != null) toggleSignPass.setOnAction(e -> togglePasswordVisibility("sign"));
        if (toggleSignPassConfirm != null) toggleSignPassConfirm.setOnAction(e -> togglePasswordVisibility("signConfirm"));
    }

    @FXML
    private void processLogin() {
        String email = loginEmail.getText();
        String password = isLoginPassVisible ? loginPassVisible.getText() : loginPass.getText();

        if (email.isEmpty() || password.isEmpty()) {
            showLoginError("Veuillez remplir tous les champs");
            return;
        }

        try {
            User user = userService.authenticate(email.trim(), password);
            if (user != null) {
                if (!user.isActive()) {
                    showLoginError("Compte désactivé.");
                    return;
                }
                SessionManager.login(user);
                SceneManager.redirectBasedOnRole();
            } else {
                showLoginError("Email ou mot de passe incorrect");
            }
        } catch (Exception e) {
            showLoginError("Erreur: " + e.getMessage());
        }
    }

    @FXML
    private void processRegister() {
        String nom = signName.getText();
        String email = signEmail.getText();
        String pass = isSignPassVisible ? signPassVisible.getText() : signPass.getText();

        try {
            if (userService.emailExiste(email)) {
                showRegisterError("Cet email est déjà utilisé");
                return;
            }
            User newUser = new User(nom, email, pass, signTel.getText(), signDate.getValue(), 2, "actif");
            userService.create(newUser);
            showAlert("Succès", "Compte créé !", Alert.AlertType.INFORMATION);
            handleSwitch();
        } catch (Exception e) {
            showRegisterError("Erreur: " + e.getMessage());
        }
    }

    // --- LES DEUX MÉTHODES POUR ÉVITER L'ERREUR FXML ---

    @FXML
    private void handleForgotPassword() {
        SceneManager.switchTo("/MotDePasseOublie.fxml");
    }

    @FXML
    private void goToMotDePasseOublie() {
        handleForgotPassword();
    }

    // -------------------------------------------------

    @FXML
    private void handleSwitch() {
        TranslateTransition translate = new TranslateTransition(Duration.seconds(0.7), slidingPane);
        if (isLoginView) {
            translate.setToX(-500);
            overlayTitle.setText("Déjà Inscrit ?");
            overlayText.setText("Connectez-vous pour accéder à vos offres.");
            switchBtn.setText("SE CONNECTER");
            isLoginView = false;
        } else {
            translate.setToX(0);
            overlayTitle.setText("Nouveau Voyageur ?");
            overlayText.setText("Inscrivez-vous et commencez votre aventure.");
            switchBtn.setText("CRÉER UN COMPTE");
            isLoginView = true;
        }
        translate.play();
    }

    private void togglePasswordVisibility(String field) {
        if (field.equals("login")) {
            isLoginPassVisible = !isLoginPassVisible;
            toggleFields(isLoginPassVisible, loginPass, loginPassVisible, toggleLoginPass);
        } else if (field.equals("sign")) {
            isSignPassVisible = !isSignPassVisible;
            toggleFields(isSignPassVisible, signPass, signPassVisible, toggleSignPass);
        }
    }

    private void toggleFields(boolean show, PasswordField pf, TextField tf, Button btn) {
        if (show) {
            tf.setText(pf.getText());
            pf.setVisible(false); tf.setVisible(true);
            btn.setText("🔒");
        } else {
            pf.setText(tf.getText());
            tf.setVisible(false); pf.setVisible(true);
            btn.setText("👁");
        }
    }

    private void showLoginError(String message) {
        loginErrorLabel.setText(message);
        loginErrorLabel.setVisible(true);
    }

    private void showRegisterError(String message) {
        registerErrorLabel.setText(message);
        registerErrorLabel.setVisible(true);
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML private void handleGoogleLogin() {}
    @FXML private void handleFacebookLogin() {}
}