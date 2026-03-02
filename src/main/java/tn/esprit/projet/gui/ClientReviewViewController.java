package tn.esprit.projet.gui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import tn.esprit.projet.API.reviews.ReviewIntelligenceService;
import tn.esprit.projet.entities.*;
import tn.esprit.projet.utils.ClientSharedState;
import tn.esprit.projet.utils.DialogHelper;

import java.io.File;
import java.sql.SQLException;
import java.util.List;
import java.util.function.Function;

/**
 * Manages review rendering and the "Add Review" dialog for hotel detail views.
 */
public class ClientReviewViewController {

    private final ClientSharedState state;
    private final DialogHelper.ClientDialogs dialogs;
    private final Function<String, Image> imageLoader;

    public ClientReviewViewController(
            ClientSharedState  state,
            DialogHelper.ClientDialogs dialogs,
            Function<String, Image> imageLoader
    ) {
        this.state       = state;
        this.dialogs     = dialogs;
        this.imageLoader = imageLoader;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Reviews section builder
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Builds the full reviews section (header + list + add button) for the
     * given hotel. The section reloads itself reactively via reloadReviews.
     */
    public VBox buildReviewsSection(Hotel hotel) {
        VBox section = new VBox(15);
        section.getStyleClass().add("review-section");

        Label reviewsTitle = new Label("Avis des clients");
        reviewsTitle.getStyleClass().add("review-section-title");

        Button addReviewBtn = new Button("+ Ajouter un avis");
        addReviewBtn.getStyleClass().addAll("btn-orange-glow", "review-add-btn");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(10, reviewsTitle, spacer, addReviewBtn);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox reviewsBox = new VBox(12);
        Runnable reloadReviews = () -> {
            reviewsBox.getChildren().clear();
            try {
                List<HotelReview> reviews = state.hotelReviewService.getReviewsByHotel(hotel.getId());
                if (reviews.isEmpty()) {
                    Label empty = new Label("Aucun avis pour le moment - Soyez le premier a donner votre avis!");
                    empty.getStyleClass().add("review-empty-label");
                    reviewsBox.getChildren().add(empty);
                } else {
                    int visibleCount = 0;
                    for (HotelReview review : reviews) {
                        // Skip reviews that have been moderated/masked - don't show them to clients
                        String comment = review.getComment();
                        if (comment != null && comment.startsWith(ReviewIntelligenceService.MODERATION_PREFIX)) continue;
                        List<HotelReviewImage> images = loadReviewImagesSafe(review.getId());
                        reviewsBox.getChildren().add(buildReviewBox(
                                review.getRating(),
                                comment == null ? "" : comment.trim(),
                                images
                        ));
                        visibleCount++;
                    }
                    if (visibleCount == 0) {
                        Label empty = new Label("Aucun avis pour le moment - Soyez le premier a donner votre avis!");
                        empty.getStyleClass().add("review-empty-label");
                        reviewsBox.getChildren().add(empty);
                    }
                }
            } catch (SQLException e) {
                Label error = new Label("Erreur de chargement des avis");
                error.getStyleClass().add("review-error-label");
                reviewsBox.getChildren().add(error);
            }
        };
        reloadReviews.run();

        addReviewBtn.setOnAction(e -> showAddReviewDialog(hotel, reloadReviews));

        section.getChildren().addAll(header, reviewsBox);
        return section;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Add review dialog
    // ─────────────────────────────────────────────────────────────────────────

    public void showAddReviewDialog(Hotel hotel, Runnable onReviewSaved) {
        if (!state.databaseAvailable || state.hotelReviewService == null) {
            dialogs.showWarning("Service indisponible", "Impossible d'ajouter un avis pour le moment.");
            return;
        }

        Dialog<HotelReview> dialog = new Dialog<>();
        dialog.setTitle("Ajouter un avis");

        ButtonType saveBtn = new ButtonType("Publier", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        GridPane form = new GridPane();
        form.setHgap(14);
        form.setVgap(12);
        form.setPadding(new Insets(12));
        form.getStyleClass().add("gv-dialog-form");

        Label hotelLabel   = formLabel("Hotel");
        Label ratingLabel  = formLabel("Note");
        Label commentLabel = formLabel("Commentaire *");
        Label imageLabel   = formLabel("Image (optionnel)");

        Label hotelValue = new Label(hotel.getName());
        hotelValue.getStyleClass().add("review-hotel-value");

        // Star rating widget - click stars or read num/5 label
        final int[] selectedRating = {5};
        HBox starRatingBox = new HBox(4);
        starRatingBox.setAlignment(Pos.CENTER_LEFT);
        Button[] starBtns = new Button[5];
        Label ratingNumLabel = new Label("5/5");
        ratingNumLabel.getStyleClass().add("review-rating-num");
        // Create buttons first
        for (int i = 0; i < 5; i++) {
            starBtns[i] = new Button("\u2605");
            starBtns[i].getStyleClass().add("review-star-btn");
        }
        // Now define refresh (after array is populated)
        Runnable refreshStars = () -> {
            for (int i = 0; i < 5; i++) {
                boolean filled = i < selectedRating[0];
                starBtns[i].setText(filled ? "\u2605" : "\u2606");
                starBtns[i].getStyleClass().removeAll("review-star-btn", "review-star-btn-empty");
                starBtns[i].getStyleClass().add(filled ? "review-star-btn" : "review-star-btn-empty");
            }
            ratingNumLabel.setText(selectedRating[0] + "/5");
        };
        for (int i = 0; i < 5; i++) {
            final int val = i + 1;
            starBtns[i].setOnAction(ev -> { selectedRating[0] = val; refreshStars.run(); });
        }
        refreshStars.run();
        starRatingBox.getChildren().addAll(starBtns);
        starRatingBox.getChildren().add(ratingNumLabel);
        // Fake Spinner wrapper so form logic still works
        Spinner<Integer> ratingSpinner = new Spinner<>(1, 5, 5);
        ratingSpinner.setManaged(false);
        ratingSpinner.setVisible(false);

        TextArea commentArea = new TextArea();
        commentArea.setPromptText("Partagez votre experience dans cet hotel...");
        commentArea.setWrapText(true);
        commentArea.setPrefRowCount(4);
        commentArea.setPrefWidth(430);

        TextField imageField = new TextField();
        imageField.setPromptText("Aucune image selectionnee");
        imageField.setEditable(false);
        imageField.setPrefWidth(320);

        Button chooseImageBtn = new Button("Choisir image");
        chooseImageBtn.getStyleClass().add("btn-orange-glow");
        chooseImageBtn.setMinWidth(120);

        Button clearImageBtn = new Button("Effacer");
        clearImageBtn.getStyleClass().add("btn-orange-glow");
        clearImageBtn.setMinWidth(90);

        chooseImageBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Selectionner une image");
            fc.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.webp", "*.gif", "*.bmp"));
            String existing = imageField.getText() == null ? "" : imageField.getText().trim();
            if (!existing.isBlank()) {
                try {
                    File parent = new File(existing).getParentFile();
                    if (parent != null && parent.exists()) fc.setInitialDirectory(parent);
                } catch (Exception ignored) { /* keep default */ }
            }
            Window owner = dialog.getDialogPane().getScene() == null ? null : dialog.getDialogPane().getScene().getWindow();
            File selected = fc.showOpenDialog(owner);
            if (selected != null) imageField.setText(selected.getAbsolutePath());
        });
        clearImageBtn.setOnAction(e -> imageField.clear());

        HBox imageInputRow = new HBox(10, imageField, chooseImageBtn, clearImageBtn);
        imageInputRow.setAlignment(Pos.CENTER_LEFT);

        form.add(hotelLabel,   0, 0); form.add(hotelValue,    1, 0);
        form.add(ratingLabel,  0, 1); form.add(starRatingBox, 1, 1);
        form.add(commentLabel, 0, 2); form.add(commentArea,   1, 2);
        form.add(imageLabel,   0, 3); form.add(imageInputRow, 1, 3);

        dialog.getDialogPane().setContent(form);
        dialogs.styleDialog(dialog, false);

        Node saveNode = dialog.getDialogPane().lookupButton(saveBtn);
        saveNode.setDisable(true);
        commentArea.textProperty().addListener((obs, old, nv) ->
                saveNode.setDisable(nv == null || nv.trim().isEmpty()));

        dialog.setResultConverter(btn -> {
            if (btn != saveBtn) return null;
            String comment = commentArea.getText() == null ? "" : commentArea.getText().trim();
            if (comment.isEmpty()) return null;
            int userId = state.resolveClientReviewUserId();
            return new HotelReview(0, selectedRating[0], comment, userId, hotel.getId());
        });

        dialog.showAndWait().ifPresent(review -> {
            try {
                ReviewIntelligenceService.ReviewProcessingResult processing =
                        state.hotelReviewService.createWithAiProcessing(review);

                String imageInput = imageField.getText() == null ? "" : imageField.getText().trim();
                String imageNote  = "";
                if (!imageInput.isBlank()) {
                    if (state.hotelReviewImageService == null) {
                        imageNote = "\nImage non enregistree: service indisponible.";
                    } else if (review.getId() <= 0) {
                        imageNote = "\nImage non enregistree: identifiant avis indisponible.";
                    } else {
                        try {
                            state.hotelReviewImageService.createWithImagePipeline(imageInput, review.getId());
                            imageNote = "\nImage jointe avec succes.";
                        } catch (IllegalArgumentException ex) {
                            imageNote = "\nImage non enregistree: " + ex.getMessage();
                        } catch (SQLException ex) {
                            imageNote = "\nImage non enregistree pour le moment. Veuillez reessayer.";
                        }
                    }
                }

                String message;
                if (processing.moderated()) {
                    message = "Merci pour votre avis! Il sera examine par notre equipe avant publication.";
                } else {
                    message = "Votre avis a ete publie avec succes!";
                }
                if (!imageNote.isBlank() && imageNote.contains("succes")) {
                    message += "\nImage jointe avec succes.";
                } else if (!imageNote.isBlank() && !imageNote.contains("succes")) {
                    message += "\nImage non ajoutee: format ou contenu non supporte.";
                }

                dialogs.showInfo("Avis publie", message);
                if (onReviewSaved != null) onReviewSaved.run();

            } catch (SQLException ex) {
                dialogs.showError("Erreur", ex.getMessage());
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Review box builder
    // ─────────────────────────────────────────────────────────────────────────

    private HBox buildReviewBox(int rating, String comment, List<HotelReviewImage> reviewImages) {
        HBox box = new HBox(16);
        box.getStyleClass().add("review-card-box");
        box.setAlignment(Pos.TOP_LEFT);

        // Avatar circle with gradient
        StackPane avatar = new StackPane();
        avatar.setMinSize(44, 44);
        avatar.setPrefSize(44, 44);
        avatar.setMaxSize(44, 44);
        avatar.getStyleClass().add("review-avatar");
        Label avatarIcon = new Label("U");
        avatarIcon.getStyleClass().add("review-avatar-icon");
        avatar.getChildren().add(avatarIcon);

        VBox reviewContent = new VBox(8);
        HBox.setHgrow(reviewContent, Priority.ALWAYS);

        // Star rating row
        HBox ratingRow = new HBox(6);
        ratingRow.setAlignment(Pos.CENTER_LEFT);
        String stars = renderStars(rating);
        Label starsLabel = new Label(stars);
        starsLabel.getStyleClass().add("review-stars-label");
        Label ratingNum = new Label(rating + "/5");
        ratingNum.getStyleClass().add("review-rating-num-card");
        ratingRow.getChildren().addAll(starsLabel, ratingNum);

        Label commentLabel = new Label(comment);
        commentLabel.getStyleClass().add("card-sub");
        commentLabel.setWrapText(true);

        reviewContent.getChildren().addAll(ratingRow, commentLabel);

        if (reviewImages != null && !reviewImages.isEmpty()) {
            FlowPane imagesPane = new FlowPane();
            imagesPane.setHgap(8);
            imagesPane.setVgap(8);
            imagesPane.setPrefWrapLength(520);

            int limit = Math.min(4, reviewImages.size());
            for (int i = 0; i < limit; i++) {
                imagesPane.getChildren().add(buildReviewImageThumbnail(reviewImages.get(i).getImageUrl()));
            }
            if (reviewImages.size() > limit) {
                Label more = new Label("+" + (reviewImages.size() - limit));
                more.getStyleClass().add("review-more-label");
                imagesPane.getChildren().add(more);
            }
            reviewContent.getChildren().add(imagesPane);
        }

        box.getChildren().addAll(avatar, reviewContent);
        return box;
    }

    private StackPane buildReviewImageThumbnail(String imagePath) {
        StackPane thumb = new StackPane();
        thumb.setMinSize(90, 66);
        thumb.setPrefSize(90, 66);
        thumb.setMaxSize(90, 66);
        thumb.getStyleClass().add("review-thumb");

        Image img = imageLoader.apply(imagePath);
        if (img != null) {
            javafx.scene.image.ImageView view = new javafx.scene.image.ImageView(img);
            view.setFitWidth(88);
            view.setFitHeight(64);
            view.setPreserveRatio(true);
            view.setSmooth(true);
            thumb.getChildren().add(view);
            thumb.setOnMouseClicked(e -> dialogs.showImagePreview(imagePath, "Image de l'avis"));
        } else {
            Label ph = new Label("IMG");
            ph.getStyleClass().add("review-thumb-ph");
            thumb.getChildren().add(ph);
        }
        return thumb;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private List<HotelReviewImage> loadReviewImagesSafe(int reviewId) {
        if (state.hotelReviewImageService == null || reviewId <= 0) return List.of();
        try {
            return state.hotelReviewImageService.getByReviewId(reviewId);
        } catch (SQLException ignored) { return List.of(); }
    }

    // renderStars helper for review display
    private String renderStars(int rating) {
        int clamped = Math.max(1, Math.min(5, rating));
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) sb.append(i < clamped ? "\u2605" : "\u2606");
        return sb.toString();
    }

    private Label formLabel(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("gv-dialog-form-label");
        return l;
    }
}
