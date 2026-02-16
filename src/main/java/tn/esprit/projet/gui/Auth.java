package tn.esprit.projet.gui;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;

public class Auth {

    @FXML private Pane slidingPane;
    @FXML private Button switchBtn;
    @FXML private Text overlayTitle;
    @FXML private Text overlayText;
    @FXML private TextField loginEmail;
    @FXML private PasswordField loginPass;
    @FXML private TextField signName;
    @FXML private TextField signEmail;
    @FXML private PasswordField signPass;

    private boolean isLoginView = true;

    @FXML
    private void handleSwitch() {
        TranslateTransition translate = new TranslateTransition(Duration.seconds(0.7), slidingPane);
        FadeTransition fadeTitle = new FadeTransition(Duration.seconds(0.3), overlayTitle);
        FadeTransition fadeText = new FadeTransition(Duration.seconds(0.3), overlayText);

        if (isLoginView) {
            translate.setToX(425);
            overlayTitle.setText("Deja inscrit ?");
            overlayText.setText("Connectez-vous pour acceder a vos reservations et offres personnalisees.");
            switchBtn.setText("SE CONNECTER");
            isLoginView = false;
        } else {
            translate.setToX(0);
            overlayTitle.setText("Nouveau Voyageur ?");
            overlayText.setText("Inscrivez-vous des aujourd'hui et commencez a planifier vos prochaines vacances.");
            switchBtn.setText("CREER UN COMPTE");
            isLoginView = true;
        }

        ParallelTransition parallel = new ParallelTransition(translate, fadeTitle, fadeText);
        parallel.play();
    }

    @FXML
    private void processLogin() {
        String email = loginEmail.getText();
        String password = loginPass.getText();

        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            showAlert(Alert.AlertType.WARNING, "Champs requis", "Veuillez remplir tous les champs.");
            return;
        }

        if (!isValidEmail(email)) {
            showAlert(Alert.AlertType.WARNING, "Email invalide", "Veuillez saisir un email valide.");
            return;
        }

        navigateToHotelManagement();
    }

    private void navigateToHotelManagement() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/Admin.fxml"));
            Stage stage = (Stage) loginEmail.getScene().getWindow();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            stage.setTitle("GoVacate - Gestion des Hotels");
            stage.setScene(scene);
            stage.setResizable(true);
            stage.setWidth(1100);
            stage.setHeight(700);
            stage.centerOnScreen();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir la page administration.");
        }
    }

    @FXML
    private void processRegister() {
        String name = signName.getText();
        String email = signEmail.getText();
        String password = signPass.getText();

        if (name == null || name.isBlank() || email == null || email.isBlank() || password == null || password.isBlank()) {
            showAlert(Alert.AlertType.WARNING, "Champs requis", "Veuillez remplir tous les champs.");
            return;
        }

        if (!isValidEmail(email)) {
            showAlert(Alert.AlertType.WARNING, "Email invalide", "Veuillez saisir un email valide.");
            return;
        }

        showAlert(Alert.AlertType.INFORMATION, "Inscription", "Creation de compte pour: " + name);
    }

    private boolean isValidEmail(String email) {
        return email != null && email.contains("@") && email.indexOf('.', email.indexOf('@')) > email.indexOf('@') + 1;
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
