package tn.esprit.projet.GUI;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.CacheHint;
import javafx.scene.control.Button;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.StackPane;
import tn.esprit.projet.utils.GlassEffectUtils;

import java.net.URL;
import java.security.cert.PolicyNode;
import java.util.ResourceBundle;

public class GoVacateControllor implements Initializable {

    @FXML private Pane slidingPane;
    @FXML private VBox menuContainer;
    @FXML private StackPane glassLayer;
    @FXML private GaussianBlur glassBlur;

    @FXML private Button btnExplorer, btnVoyages, btnFavoris, btnMessages, btnParametres, btnLogout,btnPost;

    private Button currentActiveBtn = null;
    @FXML private StackPane contentArea;
    @FXML private StackPane rootPane;
    private void handleNavigation(Button btn) {
        System.out.println("Clicked: " + btn.getText());
        if (btn == btnExplorer) {
            loadView("/HomeView.fxml");
        }
        else if (btn == btnPost) {
            loadView("/PostView.fxml");
        }
        // add others here later
    }


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        slidingPane.setMouseTransparent(true);
        slidingPane.setCache(true);
        slidingPane.setCacheHint(CacheHint.SPEED);

        Button[] buttons = {btnExplorer, btnVoyages, btnFavoris, btnMessages, btnParametres, btnLogout, btnPost};

        for (Button btn : buttons) {
            btn.setOnMouseEntered(e -> handleHover(btn));

            btn.setOnAction(e -> {
                handleHover(btn);   // keep animation
                handleNavigation(btn);
            });
        }


        Platform.runLater(() -> {
            if (btnExplorer != null) {
                slidingPane.setTranslateY(btnExplorer.getLayoutY());
                currentActiveBtn = btnExplorer;
                animateText(btnExplorer, true);
            }
            slidingPane.toBack();
        });

        menuContainer.setOnMouseMoved(event -> {
            double mouseY = event.getY();
            for (Button btn : buttons) {
                double startY = btn.getLayoutY();
                double endY = startY + btn.getHeight();
                if (mouseY >= startY && mouseY <= endY) {
                    handleHover(btn);
                    break;
                }
            }
        });
        if (glassLayer != null) {
            // 1. Optimize performance
            glassLayer.setCache(true);
            glassLayer.setCacheHint(CacheHint.SPEED);

            // 2. Create the blur effect
            glassBlur = new GaussianBlur(25); // Increased to 25 for visible 'frost'

        /* CRITICAL FIX:
           Apply the blur to the glassLayer itself.
           In your FXML, glassLayer should be a Pane that sits BEHIND your
           buttons/content but IN FRONT of the background.
        */
            glassLayer.setEffect(glassBlur);

            // 3. Dynamic responsiveness
            glassLayer.setOnMouseEntered(e -> {
                Timeline tint = new Timeline(
                        new KeyFrame(Duration.millis(300),
                                new KeyValue(glassBlur.radiusProperty(), 40))
                );
                tint.play();
            });

            glassLayer.setOnMouseExited(e -> {
                Timeline tint = new Timeline(
                        new KeyFrame(Duration.millis(300),
                                new KeyValue(glassBlur.radiusProperty(), 25))
                );
                tint.play();
            });
        }
    }

    private void handleHover(Button targetBtn) {
        if (currentActiveBtn == targetBtn) return;

        if (currentActiveBtn != null) {
            animateText(currentActiveBtn, false);
            currentActiveBtn.getStyleClass().remove("active");
        }

        applyLiquidDirection(targetBtn);
        moveBubble(targetBtn);

        animateText(targetBtn, true);
        if (!targetBtn.getStyleClass().contains("active")) {
            targetBtn.getStyleClass().add("active");
        }

        currentActiveBtn = targetBtn;
    }

    private void moveBubble(Button target) {
        double targetY = target.getLayoutY();
        TranslateTransition tt = new TranslateTransition(Duration.millis(400), slidingPane);
        tt.setToY(targetY);
        tt.setInterpolator(Interpolator.SPLINE(0.25, 0.1, 0.25, 1));
        tt.play();
    }

    private void animateText(Button btn, boolean activate) {
        ScaleTransition st = new ScaleTransition(Duration.millis(300), btn);
        st.setToX(activate ? 1.06 : 1.0);
        st.setToY(activate ? 1.12 : 1.0);
        st.setInterpolator(Interpolator.EASE_OUT);
        st.play();
    }

    private void applyLiquidDirection(Button target) {
        target.getStyleClass().removeAll("liquid-from-top", "liquid-from-bottom");
        if (currentActiveBtn == null) return;

        double oldY = currentActiveBtn.getLayoutY();
        double newY = target.getLayoutY();

        target.getStyleClass().add(newY > oldY ? "liquid-from-top" : "liquid-from-bottom");
    }
    // Dans GoVacateControllor.java

    private void loadView(String path) {
        try {
            java.net.URL fileUrl = getClass().getResource(path);
            if (fileUrl == null) return;

            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(fileUrl);
            javafx.scene.Parent view = loader.load();

            // FIX: S'assurer que le flou s'applique sur le premier enfant (le fond avec les cercles)
            if (rootPane != null && !rootPane.getChildren().isEmpty()) {
                // On floute le fond à 15px pour donner de la profondeur aux nouveaux contenus
                GlassEffectUtils.transitionBlur(rootPane.getChildren().get(0), 15);
            }

            // Configuration du contenu
            view.setOpacity(0);
            if (view instanceof Region) {
                ((Region) view).prefWidthProperty().bind(contentArea.widthProperty());
                ((Region) view).prefHeightProperty().bind(contentArea.heightProperty());
            }

            contentArea.getChildren().setAll(view);

            // Animation de fondu fluide
            FadeTransition ft = new FadeTransition(Duration.millis(450), view);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.play();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
