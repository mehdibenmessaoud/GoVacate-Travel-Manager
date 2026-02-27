package tn.esprit.projet.GUI;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.CacheHint;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class AdminController implements Initializable {

    @FXML private Pane slidingPane;
    @FXML private VBox menuContainer;
    @FXML private StackPane glassLayer;
    @FXML private GaussianBlur glassBlur;
    @FXML private BorderPane mainBorderPane;

    // Navigation Buttons
    @FXML private Button btnExplorer, btnVoyages, btnFavoris, btnMessages, btnRestaurant, btnClientHome, btnParametres, btnLogout;

    private Button currentActiveBtn = null;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Optimization
        slidingPane.setMouseTransparent(true);
        slidingPane.setCache(true);
        slidingPane.setCacheHint(CacheHint.SPEED);

        // Include btnClientHome in the interaction array
        Button[] buttons = {btnExplorer, btnVoyages, btnFavoris, btnMessages, btnClientHome, btnRestaurant, btnParametres, btnLogout};

        for (Button btn : buttons) {
            btn.setOnMouseEntered(e -> handleHover(btn));
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
            glassLayer.setCache(true);
            glassLayer.setCacheHint(CacheHint.SPEED);
            glassBlur = new GaussianBlur(14);
            glassLayer.setEffect(glassBlur);
        }

        // Action Handlers
        btnRestaurant.setOnAction(event -> loadSection("/RestaurantAdminView.fxml"));
        btnClientHome.setOnAction(event -> loadSection("/ClientRestaurantView.fxml"));
    }

    public BorderPane getMainBorderPane() {
        return mainBorderPane;
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

    public void loadSection(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            // Setup controllers if needed
            Object controller = loader.getController();
            if (controller instanceof RestaurantAdminController) {
                ((RestaurantAdminController) controller).setMainAdminController(this);
            }

            mainBorderPane.setCenter(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}