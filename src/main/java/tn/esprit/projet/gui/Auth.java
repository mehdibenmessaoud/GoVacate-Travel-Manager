package tn.esprit.projet.gui;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.Pane;
import javafx.scene.text.Text;
import javafx.util.Duration;

public class Auth {

    @FXML private Pane slidingPane;
    @FXML private Button switchBtn;
    @FXML private Text overlayTitle;
    @FXML private Text overlayText;

    private boolean isLoginView = true;

    @FXML
    private void handleSwitch() {
        // Animation de déplacement
        TranslateTransition translate = new TranslateTransition(Duration.seconds(0.7), slidingPane);
        // Animation de fondu pour le texte
        FadeTransition fade = new FadeTransition(Duration.seconds(0.3), overlayTitle);
        FadeTransition fade2 = new FadeTransition(Duration.seconds(0.3), overlayText);

        if (isLoginView) {
            translate.setToX(425);
            overlayTitle.setText("Déjà Inscrit ?");
            overlayText.setText("Connectez-vous pour accéder à vos réservations et offres personnalisées.");
            switchBtn.setText("SE CONNECTER");
            isLoginView = false;
        } else {
            translate.setToX(0);
            overlayTitle.setText("Nouveau Voyageur ?");
            overlayText.setText("Inscrivez-vous dès aujourd'hui et commencez à planifier vos prochaines vacances.");
            switchBtn.setText("CRÉER UN COMPTE");
            isLoginView = true;
        }

        ParallelTransition parallel = new ParallelTransition(translate, fade, fade2);
        parallel.play();
    }

    // ... Gardez vos méthodes processLogin et processRegister

    @FXML
    private void processLogin() {
        System.out.println("Tentative de connexion...");
    }

    @FXML
    private void processRegister() {
        System.out.println("Création de compte...");
    }
}
