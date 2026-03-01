package tn.esprit.projet.gui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Builds the main image + thumbnails gallery UI used in both
 * hotel and room detail views — no duplication.
 *
 * @param <T> image entity type (HotelImage or RoomImage)
 */
public final class ImageGalleryBuilder {

    private ImageGalleryBuilder() {}

    /**
     * @param images          list of image entities
     * @param urlExtractor    function to get the URL/path from each entity
     * @param mainContainer   StackPane to put the main image + nav arrows
     * @param thumbnailsContainer HBox to put thumbnail StackPanes
     * @param hoverColor      highlight color for thumbnails on hover (hex string)
     * @param placeholderIcon letter icon for the placeholder (e.g. "H" or "R")
     * @param resourceBase    class for classpath resource resolution
     */
    public static <T> void build(List<T> images,
                                 Function<T, String> urlExtractor,
                                 StackPane mainContainer,
                                 HBox thumbnailsContainer,
                                 String hoverColor,
                                 String placeholderIcon,
                                 Class<?> resourceBase) {
        build(images, urlExtractor, mainContainer, thumbnailsContainer, hoverColor, placeholderIcon, resourceBase, null);
    }

    public static <T> void build(List<T> images,
                                 Function<T, String> urlExtractor,
                                 StackPane mainContainer,
                                 HBox thumbnailsContainer,
                                 String hoverColor,
                                 String placeholderIcon,
                                 Class<?> resourceBase,
                                 Consumer<String> onImagePreview) {
        mainContainer.getChildren().clear();
        thumbnailsContainer.getChildren().clear();

        if (images == null || images.isEmpty()) {
            showPlaceholder(placeholderIcon, mainContainer);
            return;
        }

        Image firstImg = ImageLoader.load(urlExtractor.apply(images.get(0)), resourceBase);
        if (firstImg == null) {
            showPlaceholder(placeholderIcon, mainContainer);
            return;
        }

        final int[] currentIndex = {0};

        ImageView mainView = new ImageView();
        applyCoverFit(mainView, firstImg, 520, 330);
        mainView.setOnMouseClicked(ev -> {
            if (onImagePreview != null && currentIndex[0] >= 0 && currentIndex[0] < images.size()) {
                onImagePreview.accept(urlExtractor.apply(images.get(currentIndex[0])));
            }
        });
        mainContainer.getChildren().add(mainView);

        if (images.size() > 1) {
            Button prevBtn = navButton("<");
            Button nextBtn = navButton(">");
            prevBtn.setOnAction(ev -> {
                currentIndex[0] = (currentIndex[0] - 1 + images.size()) % images.size();
                Image newImg = ImageLoader.load(urlExtractor.apply(images.get(currentIndex[0])), resourceBase);
                if (newImg != null) applyCoverFit(mainView, newImg, 520, 330);
            });
            nextBtn.setOnAction(ev -> {
                currentIndex[0] = (currentIndex[0] + 1) % images.size();
                Image newImg = ImageLoader.load(urlExtractor.apply(images.get(currentIndex[0])), resourceBase);
                if (newImg != null) applyCoverFit(mainView, newImg, 520, 330);
            });
            StackPane.setAlignment(prevBtn, Pos.CENTER_LEFT);
            StackPane.setAlignment(nextBtn, Pos.CENTER_RIGHT);
            StackPane.setMargin(prevBtn, new Insets(0, 10, 0, 10));
            StackPane.setMargin(nextBtn, new Insets(0, 10, 0, 10));
            mainContainer.getChildren().addAll(prevBtn, nextBtn);
        }

        for (int i = 0; i < images.size(); i++) {
            final int idx = i;
            Image thumbImg = ImageLoader.load(urlExtractor.apply(images.get(i)), resourceBase);
            if (thumbImg == null) continue;
            ImageView thumb = new ImageView(thumbImg);
            thumb.setFitWidth(104); thumb.setFitHeight(70); thumb.setPreserveRatio(false);
            StackPane thumbBox = new StackPane(thumb);
            thumbBox.getStyleClass().add("gallery-thumbnail");
            thumbBox.setOnMouseEntered(ev -> thumbBox.getStyleClass().add("gallery-thumbnail-hover"));
            thumbBox.setOnMouseExited(ev -> thumbBox.getStyleClass().remove("gallery-thumbnail-hover"));
            thumbBox.setOnMouseClicked(ev -> {
                currentIndex[0] = idx;
                Image newImg = ImageLoader.load(urlExtractor.apply(images.get(idx)), resourceBase);
                if (newImg != null) applyCoverFit(mainView, newImg, 520, 330);
            });
            thumbnailsContainer.getChildren().add(thumbBox);
        }
    }

    public static void showPlaceholder(String icon, StackPane mainContainer) {
        VBox placeholder = new VBox(15);
        placeholder.setAlignment(Pos.CENTER);
        Label iconLabel = new Label(icon);
        iconLabel.getStyleClass().add("gallery-placeholder-icon");
        Label textLabel = new Label("Aucune image disponible");
        textLabel.getStyleClass().add("gallery-placeholder-text");
        placeholder.getChildren().addAll(iconLabel, textLabel);
        mainContainer.getChildren().add(placeholder);
    }

    private static void applyCoverFit(ImageView view, Image image, double w, double h) {
        view.setFitWidth(w);
        view.setFitHeight(h);
        view.setPreserveRatio(false);
        view.setSmooth(true);
        view.setImage(image);
    }

    private static Button navButton(String label) {
        Button btn = new Button(label);
        btn.getStyleClass().add("gallery-arrow-button");
        return btn;
    }
}
