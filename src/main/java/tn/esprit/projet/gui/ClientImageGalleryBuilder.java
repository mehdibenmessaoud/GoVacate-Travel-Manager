package tn.esprit.projet.gui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Builds a reusable main-image + thumbnail gallery for any entity type.
 * Eliminates duplicate gallery code between hotel and room detail views.
 *
 * @param <T> image record type (e.g. HotelImage or RoomImage)
 */
public class ClientImageGalleryBuilder<T> {

    /** Called when user clicks the main image — passes image URL. */
    private Consumer<String> onImagePreview;
    /** Accent colour for thumbnail hover (#FF8210 for hotels, #679AC1 for rooms). */
    private final String accentColor;
    /** Placeholder letter shown when no image is available. */
    private final String placeholderLetter;
    /** Extracts the URL string from an image record. */
    private final Function<T, String> urlExtractor;
    /** Loads an Image from a URL; may return null. */
    private final Function<String, Image> imageLoader;

    public ClientImageGalleryBuilder(
            String accentColor,
            String placeholderLetter,
            Function<T, String> urlExtractor,
            Function<String, Image> imageLoader,
            Consumer<String> onImagePreview
    ) {
        this.accentColor        = accentColor;
        this.placeholderLetter  = placeholderLetter;
        this.urlExtractor       = urlExtractor;
        this.imageLoader        = imageLoader;
        this.onImagePreview     = onImagePreview;
    }

    /**
     * Builds and returns a VBox containing the main image container (with
     * prev/next arrows) and, when multiple images exist, a thumbnail strip.
     *
     * @param images list of image records
     * @return gallery VBox (prefWidth = 520)
     */
    public VBox build(List<T> images) {
        VBox gallery = new VBox(10);
        gallery.setPrefWidth(520);

        StackPane mainContainer = buildMainContainer();

        ImageView mainImageView  = buildMainImageView();
        Label     placeholder    = buildPlaceholder();

        if (images == null || images.isEmpty()) {
            mainContainer.getChildren().add(placeholder);
            gallery.getChildren().add(mainContainer);
            return gallery;
        }

        final int[] currentIndex = {0};
        Image firstImg = imageLoader.apply(urlExtractor.apply(images.get(0)));

        if (firstImg != null) {
            applyBestFit(mainImageView, firstImg, 520, 330);
            mainImageView.setStyle("-fx-cursor: hand;");
            mainImageView.setOnMouseClicked(ev -> {
                if (onImagePreview != null)
                    onImagePreview.accept(urlExtractor.apply(images.get(currentIndex[0])));
            });
            mainContainer.getChildren().add(mainImageView);
        } else {
            mainContainer.getChildren().add(placeholder);
        }

        if (images.size() > 1) {
            Button prevBtn = arrowButton("<");
            Button nextBtn = arrowButton(">");

            prevBtn.setOnAction(ev -> {
                currentIndex[0] = (currentIndex[0] - 1 + images.size()) % images.size();
                Image img = imageLoader.apply(urlExtractor.apply(images.get(currentIndex[0])));
                if (img != null) applyBestFit(mainImageView, img, 520, 330);
            });
            nextBtn.setOnAction(ev -> {
                currentIndex[0] = (currentIndex[0] + 1) % images.size();
                Image img = imageLoader.apply(urlExtractor.apply(images.get(currentIndex[0])));
                if (img != null) applyBestFit(mainImageView, img, 520, 330);
            });

            StackPane.setAlignment(prevBtn, Pos.CENTER_LEFT);
            StackPane.setAlignment(nextBtn, Pos.CENTER_RIGHT);
            StackPane.setMargin(prevBtn, new Insets(0, 10, 0, 10));
            StackPane.setMargin(nextBtn, new Insets(0, 10, 0, 10));
            mainContainer.getChildren().addAll(prevBtn, nextBtn);

            HBox thumbnails = buildThumbnailStrip(images, currentIndex, mainImageView);
            gallery.getChildren().addAll(mainContainer, thumbnails);
        } else {
            gallery.getChildren().add(mainContainer);
        }

        return gallery;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    private StackPane buildMainContainer() {
        StackPane c = new StackPane();
        c.setPrefSize(520, 330);
        c.setStyle("-fx-background-color: rgba(0,0,0,0.4); -fx-background-radius: 15;");
        Rectangle clip = new Rectangle();
        clip.setArcWidth(24); clip.setArcHeight(24);
        clip.widthProperty().bind(c.widthProperty());
        clip.heightProperty().bind(c.heightProperty());
        c.setClip(clip);
        return c;
    }

    private ImageView buildMainImageView() {
        ImageView v = new ImageView();
        v.setFitWidth(520);
        v.setFitHeight(330);
        v.setPreserveRatio(true);
        v.setSmooth(true);
        v.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 15, 0, 0, 5);");
        return v;
    }

    private Label buildPlaceholder() {
        Label l = new Label(placeholderLetter);
        l.setStyle("-fx-font-size: 80px; -fx-text-fill: rgba(255,255,255,0.5);");
        return l;
    }

    private Button arrowButton(String text) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color: rgba(0,0,0,0.55); -fx-text-fill: white; "
                + "-fx-font-size: 16px; -fx-font-weight: bold; -fx-background-radius: 20; "
                + "-fx-min-width: 36; -fx-min-height: 36; -fx-cursor: hand;");
        return b;
    }

    private HBox buildThumbnailStrip(List<T> images, int[] currentIndex, ImageView mainImageView) {
        HBox strip = new HBox(10);
        strip.setAlignment(Pos.CENTER_LEFT);

        for (int i = 0; i < images.size(); i++) {
            final int index = i;
            Image thumbImg = imageLoader.apply(urlExtractor.apply(images.get(i)));
            if (thumbImg == null) continue;

            ImageView thumb = new ImageView(thumbImg);
            thumb.setFitWidth(112);
            thumb.setFitHeight(76);
            thumb.setPreserveRatio(true);

            StackPane thumbContainer = new StackPane(thumb);
            String normalStyle   = "-fx-background-color: rgba(0,0,0,0.3); -fx-background-radius: 8; -fx-padding: 3; -fx-cursor: hand;";
            String hoverStyle    = "-fx-background-color: " + accentColor + "; -fx-background-radius: 8; -fx-padding: 3; -fx-cursor: hand;";
            thumbContainer.setStyle(normalStyle);
            thumbContainer.setOnMouseEntered(ev -> thumbContainer.setStyle(hoverStyle));
            thumbContainer.setOnMouseExited(ev  -> thumbContainer.setStyle(normalStyle));
            thumbContainer.setOnMouseClicked(ev -> {
                currentIndex[0] = index;
                Image img = imageLoader.apply(urlExtractor.apply(images.get(index)));
                if (img != null) applyBestFit(mainImageView, img, 520, 330);
            });

            strip.getChildren().add(thumbContainer);
        }
        return strip;
    }

    private void applyBestFit(ImageView view, Image image, double maxWidth, double maxHeight) {
        if (view == null || image == null) return;
        double iw = image.getWidth(), ih = image.getHeight();
        if (iw > 0 && ih > 0) {
            double scale = Math.min(Math.min(maxWidth / iw, maxHeight / ih), 2.0);
            view.setFitWidth(Math.max(1, iw * scale));
            view.setFitHeight(Math.max(1, ih * scale));
        } else {
            view.setFitWidth(maxWidth);
            view.setFitHeight(maxHeight);
        }
        view.setPreserveRatio(true);
        view.setSmooth(true);
        view.setImage(image);
    }
}
