package tn.esprit.projet.gui;

import javafx.scene.image.Image;
import java.io.File;
import java.net.URL;

/**
 * Utility for loading images from URL, absolute path, or classpath.
 */
public final class ImageLoader {

    private ImageLoader() {}

    public static Image load(String imagePath, Class<?> resourceBase) {
        if (imagePath == null || imagePath.isEmpty()) return null;
        try {
            if (imagePath.startsWith("http")) return new Image(imagePath, true);
            File absolute = new File(imagePath);
            if (absolute.exists()) return new Image(absolute.toURI().toString());
            URL url = resourceBase.getResource("/images/" + imagePath);
            if (url != null) return new Image(url.toExternalForm());
            File file = new File("src/main/resources/images/" + imagePath);
            if (file.exists()) return new Image(file.toURI().toString());
        } catch (Exception ignored) {}
        return null;
    }
}
