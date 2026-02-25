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

public class ClientController implements Initializable {

    @FXML private Pane slidingPane;
    @FXML private VBox menuContainer;
    @FXML private BorderPane mainBorderPane;

    // Client-Specific Navigation Buttons
    @FXML private Button btnHome, btnMyBookings, btnFavorites, btnRestaurantClient, btnProfile, btnLogout;

    private Button currentActiveBtn = null;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Optimization
        slidingPane.setMouseTransparent(true);
        slidingPane.setCache(true);
        slidingPane.setCacheHint(CacheHint.SPEED);

        Button[] buttons = {btnHome, btnMyBookings, btnFavorites, btnRestaurantClient, btnProfile, btnLogout};

        for (Button btn : buttons) {
            btn.setOnMouseEntered(e -> handleHover(btn));
        }

        // Initialize position
        Platform.runLater(() -> {
            if (btnHome != null) {
                slidingPane.setTranslateY(btnHome.getLayoutY());
                currentActiveBtn = btnHome;
                animateText(btnHome, true);
                // Load default section on startup
                loadSection("/ClientRestaurantView.fxml");
            }
            slidingPane.toBack();
        });

        // Hover tracking
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

        // Client Action Handlers
        btnRestaurantClient.setOnAction(event -> loadSection("/ClientRestaurantView.fxml"));
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

            // Get the controller AFTER loading
            Object controller = loader.getController();

            if (controller instanceof ClientRestaurantController) {
                ((ClientRestaurantController) controller).setMainClientController(this);
            } else if (controller instanceof ClientRestaurantDetailController) {
                ((ClientRestaurantDetailController) controller).setMainClientController(this);
            }

            mainBorderPane.setCenter(root);
        } catch (IOException e) {
            System.err.println("Error loading FXML: " + fxmlPath);
            e.printStackTrace();
        }
    }

    public BorderPane getMainBorderPane() {
        return mainBorderPane;
    }
}