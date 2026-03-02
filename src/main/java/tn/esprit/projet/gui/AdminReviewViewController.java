package tn.esprit.projet.gui;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import tn.esprit.projet.API.reviews.ReviewIntelligenceService;
import tn.esprit.projet.entities.HotelReview;
import tn.esprit.projet.services.HotelReviewService;
import tn.esprit.projet.utils.AdminSharedState;
import tn.esprit.projet.utils.DialogHelper;
import tn.esprit.projet.utils.GuiUtils;

import java.sql.SQLException;
import java.util.Locale;

/**
 * Handles all review-related UI logic:
 * – Reviews table setup
 * – Add / Edit / Delete / Moderate review dialogs
 * – User label resolution (delegates to AdminSharedState)
 */
public class AdminReviewViewController {

    private static final String MODERATION_PREFIX = "[Avis masque par moderation]";

    private final AdminSharedState state;
    private final AdminController3 admin;
    private final HotelReviewService reviewService;

    public AdminReviewViewController(AdminSharedState state, AdminController3 admin,
                                     HotelReviewService reviewService) {
        this.state = state;
        this.admin = admin;
        this.reviewService = reviewService;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // TABLE
    // ═══════════════════════════════════════════════════════════════════════

    public void setupReviewsTable(TableView<HotelReview> reviewsTable,
                                  TableColumn<HotelReview, String> colReviewRating,
                                  TableColumn<HotelReview, String> colReviewComment,
                                  TableColumn<HotelReview, String> colReviewUser,
                                  TableColumn<HotelReview, Void> colReviewActions) {

        colReviewRating.setCellValueFactory(data ->
                new SimpleStringProperty(GuiUtils.renderStars(data.getValue().getRating()) + " (" + data.getValue().getRating() + "/5)"));
        colReviewComment.setCellValueFactory(data ->
                new SimpleStringProperty(maskReviewCommentForDisplay(data.getValue().getComment())));
        colReviewUser.setCellValueFactory(data ->
                new SimpleStringProperty(state.resolveUserLabel(data.getValue().getUserId())));

        colReviewActions.setCellFactory(col -> new TableCell<>() {
            private final Button hideBtn = DialogHelper.createTableActionButton("Masquer", "gv-btn-blue", 90);
            private final Button deleteBtn = DialogHelper.createTableActionButton("Supprimer", "gv-btn-red", 96);
            private final HBox box = new HBox(8, hideBtn, deleteBtn);
            {
                hideBtn.setTooltip(new Tooltip("Masquer cet avis"));
                deleteBtn.setTooltip(new Tooltip("Supprimer cet avis"));
                box.setAlignment(Pos.CENTER);
                hideBtn.setOnAction(e -> moderateReview(getTableView().getItems().get(getIndex())));
                deleteBtn.setOnAction(e -> DialogHelper.confirmDelete("avis", "", () -> handleDeleteReview(getTableView().getItems().get(getIndex())), getClass()));
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) { setGraphic(null); return; }
                HotelReview review = getTableView().getItems().get(getIndex());
                boolean alreadyModerated = review.getComment() != null && review.getComment().startsWith(MODERATION_PREFIX);
                hideBtn.setDisable(alreadyModerated);
                setGraphic(box);
            }
        });
    }

    public void loadHotelReviews(int hotelId, TableView<HotelReview> reviewsTable) {
        try {
            reviewsTable.setItems(FXCollections.observableArrayList(reviewService.getReviewsByHotel(hotelId)));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ADD REVIEW DIALOG
    // ═══════════════════════════════════════════════════════════════════════

    public void showAddReviewDialog(int hotelId, TableView<HotelReview> reviewsTable) {
        if (hotelId <= 0) {
            DialogHelper.showNotification("Selectionnez d'abord un hotel.", "warning", getClass()); return;
        }
        Dialog<HotelReview> dialog = createReviewDialog(null);
        dialog.showAndWait().ifPresent(r -> {
            try {
                r.setHotelId(hotelId);
                ReviewIntelligenceService.ReviewProcessingResult processing = reviewService.createWithAiProcessing(r);
                loadHotelReviews(hotelId, reviewsTable);
                String sentimentSuffix = processing.sentimentAnalyzed()
                        ? String.format(Locale.ROOT, " (sentiment %.2f)", processing.sentimentScore()) : "";
                String message = processing.moderated()
                        ? "Avis ajoute et masque automatiquement par moderation." + sentimentSuffix
                        : "Avis ajoute!" + sentimentSuffix;
                DialogHelper.showNotification(message, "success", getClass());
            } catch (SQLException e) {
                DialogHelper.showNotification("Erreur: " + e.getMessage(), "error", getClass());
            } catch (RuntimeException e) {
                DialogHelper.showNotification("Erreur inattendue: " + e.getMessage(), "error", getClass());
            }
        });
    }

    private Dialog<HotelReview> createReviewDialog(HotelReview review) {
        Dialog<HotelReview> dialog = new Dialog<>();
        dialog.setTitle(review == null ? "Nouvel Avis" : "Modifier l'Avis");
        ButtonType saveBtn = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        GridPane grid = DialogHelper.createDialogFormGrid();

        // Star rating selector
        HBox starsBox = new HBox(8);
        starsBox.setAlignment(Pos.CENTER_LEFT);
        final int[] rating = {review != null ? review.getRating() : 4};
        Label[] stars = new Label[5];
        for (int i = 0; i < 5; i++) {
            final int r = i + 1;
            stars[i] = new Label(r <= rating[0] ? "\u2605" : "\u2606");
            stars[i].getStyleClass().add("star-rating-selector");
            stars[i].setOnMouseClicked(e -> {
                rating[0] = r;
                for (int j = 0; j < 5; j++) stars[j].setText(j < r ? "\u2605" : "\u2606");
            });
            starsBox.getChildren().add(stars[i]);
        }

        TextArea commentField = new TextArea(review != null ? review.getComment() : "");
        commentField.setPromptText("Commentaire de l'utilisateur");
        commentField.setPrefRowCount(4); commentField.setWrapText(true); commentField.setPrefHeight(108);
        DialogHelper.applyDialogFieldSizing(commentField);

        var userOptions = state.loadReviewUserOptions();
        if (review != null && review.getUserId() > 0
                && userOptions.stream().noneMatch(o -> o.id() == review.getUserId())) {
            userOptions.add(0, new AdminSharedState.ReviewUserOption(review.getUserId(), state.resolveUserLabel(review.getUserId())));
        }
        userOptions.sort(java.util.Comparator.comparingInt(AdminSharedState.ReviewUserOption::id));

        ComboBox<AdminSharedState.ReviewUserOption> userCombo = new ComboBox<>(FXCollections.observableArrayList(userOptions));
        DialogHelper.applyDialogFieldSizing(userCombo);
        userCombo.setPromptText("Selectionner un utilisateur");
        userCombo.setVisibleRowCount(Math.min(10, Math.max(3, userOptions.size())));

        if (review != null && review.getUserId() > 0) {
            userCombo.getItems().stream().filter(o -> o.id() == review.getUserId()).findFirst().ifPresent(userCombo::setValue);
        } else if (!userOptions.isEmpty()) {
            userCombo.setValue(userOptions.get(0));
        }

        grid.add(new Label("Note"), 0, 0); grid.add(starsBox, 1, 0);
        grid.add(new Label("Commentaire"), 0, 1); grid.add(commentField, 1, 1);
        grid.add(new Label("Utilisateur"), 0, 2); grid.add(userCombo, 1, 2);

        if (userOptions.isEmpty()) {
            Label warning = new Label("Aucun utilisateur detecte. Creez d'abord un utilisateur.");
            warning.getStyleClass().add("gv-warning-text"); warning.setWrapText(true);
            grid.add(warning, 1, 3);
        }

        dialog.getDialogPane().setContent(grid);
        DialogHelper.applyDialogPaneSizing(dialog.getDialogPane(), 700, 400);
        DialogHelper.styleDialog(dialog, false, getClass());

        Node saveButtonNode = dialog.getDialogPane().lookupButton(saveBtn);
        Runnable validate = () -> saveButtonNode.setDisable(GuiUtils.isBlank(commentField.getText()) || userCombo.getValue() == null);
        validate.run();
        commentField.textProperty().addListener((obs, oldValue, newValue) -> validate.run());
        userCombo.valueProperty().addListener((obs, oldValue, newValue) -> validate.run());

        dialog.setResultConverter(btn -> {
            if (btn != saveBtn) return null;
            var selectedUser = userCombo.getValue();
            return selectedUser == null ? null
                    : new HotelReview(0, rating[0], GuiUtils.trimToEmpty(commentField.getText()), selectedUser.id(), 0);
        });
        return dialog;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // MODERATION / DELETE
    // ═══════════════════════════════════════════════════════════════════════

    private void moderateReview(HotelReview review) {
        if (review == null) return;
        TextInputDialog dialog = new TextInputDialog("Contenu inapproprie");
        dialog.setTitle("Masquer l'avis");
        dialog.setHeaderText("Masquer cet avis client ?");
        dialog.setContentText("Motif (optionnel):");
        DialogHelper.styleDialog(dialog, false, getClass());
        dialog.showAndWait().ifPresent(reason -> {
            try {
                String trimmedReason = reason == null ? "" : reason.trim();
                String moderatedComment = trimmedReason.isEmpty()
                        ? MODERATION_PREFIX
                        : MODERATION_PREFIX + " Motif: " + trimmedReason;
                HotelReview moderated = new HotelReview(review.getId(), review.getRating(), moderatedComment, review.getUserId(), review.getHotelId());
                moderated.setCreatedAt(review.getCreatedAt());
                reviewService.update(moderated);
                int hotelId = state.getSelectedHotel() != null ? state.getSelectedHotel().getId() : review.getHotelId();
                // Trigger a refresh in the controller via a simple callback
                admin.reloadReviews(hotelId);
                DialogHelper.showNotification("Avis masque avec succes!", "success", getClass());
            } catch (SQLException e) {
                DialogHelper.showNotification("Erreur: " + e.getMessage(), "error", getClass());
            }
        });
    }

    private void handleDeleteReview(HotelReview review) {
        try {
            reviewService.delete(review.getId());
            int hotelId = state.getSelectedHotel() != null ? state.getSelectedHotel().getId() : review.getHotelId();
            admin.reloadReviews(hotelId);
            DialogHelper.showNotification("Avis supprime!", "success", getClass());
        } catch (SQLException e) {
            DialogHelper.showNotification("Erreur: " + e.getMessage(), "error", getClass());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════════════

    private String maskReviewCommentForDisplay(String comment) {
        String safeComment = comment == null ? "" : comment.trim();
        if (safeComment.isEmpty()) return "";
        if (!safeComment.startsWith(MODERATION_PREFIX)) return safeComment;
        int reasonIndex = safeComment.indexOf("Motif:");
        if (reasonIndex >= 0) return MODERATION_PREFIX + " " + safeComment.substring(reasonIndex).trim();
        return MODERATION_PREFIX;
    }
}
