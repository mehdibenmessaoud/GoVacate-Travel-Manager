package tn.esprit.projet.utils;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.Node;
import javafx.scene.effect.GaussianBlur;
import javafx.util.Duration;

public class GlassEffectUtils {

    /**
     * Smoothly blurs or unblurs a background node.
     * @param target The node to blur (e.g., your background pane or scrollpane)
     * @param radius The intensity of the frost (0 to remove)
     */
    public static void transitionBlur(Node target, double radius) {
        GaussianBlur blur = (target.getEffect() instanceof GaussianBlur)
                ? (GaussianBlur) target.getEffect()
                : new GaussianBlur(0);

        target.setEffect(blur);

        Timeline timeline = new Timeline(
                new KeyFrame(Duration.millis(400),
                        new KeyValue(blur.radiusProperty(), radius)
                )
        );
        timeline.play();
    }
}