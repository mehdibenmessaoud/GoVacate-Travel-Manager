package tn.esprit.projet.utils;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.net.URL;
import java.util.function.Function;

/**
 * Unified dialog styling and notification utilities for both Admin and Client controllers.
 * Replaces the duplicated DialogHelper and DialogHelper.ClientDialogs classes.
 */
public final class DialogHelper {

    private DialogHelper() {}

    // ── Button style constants (deprecated — use CSS classes gv-btn-blue, gv-btn-orange, gv-btn-red) ──

    /** @deprecated Use CSS class "gv-btn-blue" instead */
    @Deprecated public static final String BLUE_BTN = "-fx-background-color: linear-gradient(to bottom, #679AC1, #5a8ab0); -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 8 16; -fx-cursor: hand; -fx-font-size: 12px; -fx-font-weight: bold; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 4, 0, 0, 1);";
    /** @deprecated Use CSS class "gv-btn-blue" instead */
    @Deprecated public static final String BLUE_BTN_HOVER = "-fx-background-color: linear-gradient(to bottom, #7aadd4, #679AC1); -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 8 16; -fx-cursor: hand; -fx-font-size: 12px; -fx-font-weight: bold; -fx-effect: dropshadow(gaussian, rgba(103,154,193,0.5), 8, 0, 0, 2);";
    /** @deprecated Use CSS class "gv-btn-orange" instead */
    @Deprecated public static final String ORANGE_BTN = "-fx-background-color: linear-gradient(to bottom, #FF8210, #e67400); -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 8 16; -fx-cursor: hand; -fx-font-size: 12px; -fx-font-weight: bold; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 4, 0, 0, 1);";
    /** @deprecated Use CSS class "gv-btn-orange" instead */
    @Deprecated public static final String ORANGE_BTN_HOVER = "-fx-background-color: linear-gradient(to bottom, #ff9933, #FF8210); -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 8 16; -fx-cursor: hand; -fx-font-size: 12px; -fx-font-weight: bold; -fx-effect: dropshadow(gaussian, rgba(255,130,16,0.5), 8, 0, 0, 2);";
    /** @deprecated Use CSS class "gv-btn-red" instead */
    @Deprecated public static final String RED_BTN = "-fx-background-color: linear-gradient(to bottom, #dc3545, #c82333); -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 8 16; -fx-cursor: hand; -fx-font-size: 12px; -fx-font-weight: bold; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 4, 0, 0, 1);";
    /** @deprecated Use CSS class "gv-btn-red" instead */
    @Deprecated public static final String RED_BTN_HOVER = "-fx-background-color: linear-gradient(to bottom, #e74c5c, #dc3545); -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 8 16; -fx-cursor: hand; -fx-font-size: 12px; -fx-font-weight: bold; -fx-effect: dropshadow(gaussian, rgba(220,53,69,0.5), 8, 0, 0, 2);";

    // ── Dialog form grid builder ──────────────────────────────────────────────

    public static GridPane createDialogFormGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(14);
        grid.setPadding(new Insets(24));
        grid.getStyleClass().add("gv-dialog-form");

        ColumnConstraints labelColumn = new ColumnConstraints();
        labelColumn.setMinWidth(130);
        labelColumn.setPrefWidth(130);

        ColumnConstraints fieldColumn = new ColumnConstraints();
        fieldColumn.setHgrow(Priority.ALWAYS);
        fieldColumn.setFillWidth(true);
        fieldColumn.setMinWidth(360);
        fieldColumn.setPrefWidth(520);

        grid.getColumnConstraints().setAll(labelColumn, fieldColumn);
        return grid;
    }

    // ── Field sizing helpers ──────────────────────────────────────────────────

    public static void applyDialogFieldSizing(Region field) {
        field.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(field, Priority.ALWAYS);
    }

    public static void configureDialogSpinner(Spinner<?> spinner, double prefWidth) {
        if (prefWidth > 0) {
            spinner.setMinWidth(prefWidth);
            spinner.setPrefWidth(prefWidth);
            spinner.setMaxWidth(prefWidth);
            GridPane.setFillWidth(spinner, false);
            GridPane.setHgrow(spinner, Priority.NEVER);
            return;
        }
        spinner.setMinWidth(Region.USE_COMPUTED_SIZE);
        spinner.setPrefWidth(Region.USE_COMPUTED_SIZE);
        spinner.setMaxWidth(Double.MAX_VALUE);
        GridPane.setFillWidth(spinner, true);
        GridPane.setHgrow(spinner, Priority.ALWAYS);
    }

    public static void applyDialogPaneSizing(DialogPane pane, double minWidth, double minHeight) {
        pane.setMinWidth(minWidth);
        pane.setPrefWidth(minWidth);
        pane.setMinHeight(minHeight);
    }

    // ── Button builders ───────────────────────────────────────────────────────

    public static Button createDetailActionButton(String label, String backgroundColor) {
        Button button = new Button(label);
        button.getStyleClass().add("gv-detail-action-btn");
        button.setStyle("-fx-background-color: " + backgroundColor + ";");
        button.setWrapText(false);
        button.setMinWidth(Region.USE_PREF_SIZE);
        button.setMinHeight(44);
        return button;
    }

    public static Button createTableActionButton(String label, String cssClass, double minWidth) {
        Button btn = new Button(label);
        btn.getStyleClass().add(cssClass);
        btn.setWrapText(false);
        btn.setMinWidth(minWidth);
        btn.setPrefWidth(minWidth);
        btn.setMaxWidth(minWidth);
        return btn;
    }

    /** @deprecated Use CSS class version instead */
    @Deprecated
    public static Button createTableActionButton(String label, String normalStyle, String hoverStyle, double minWidth) {
        Button btn = new Button(label);
        btn.setStyle(normalStyle);
        btn.setOnMouseEntered(e -> btn.setStyle(hoverStyle));
        btn.setOnMouseExited(e -> btn.setStyle(normalStyle));
        btn.setWrapText(false);
        btn.setMinWidth(minWidth);
        btn.setPrefWidth(minWidth);
        btn.setMaxWidth(minWidth);
        return btn;
    }

    // ── Dialog styling (works for both admin and client CSS) ───────────────────

    /**
     * Style a dialog with the given CSS file.
     *
     * @param dialog             the dialog to style
     * @param destructivePrimary if true, primary button gets danger style
     * @param cssFile            CSS resource path, e.g. "/css/admin-hotel-style.css"
     * @param cssOwner           class used to resolve the CSS resource
     */
    public static void styleDialog(Dialog<?> dialog, boolean destructivePrimary, String cssFile, Class<?> cssOwner) {
        if (dialog == null || dialog.getDialogPane() == null) return;

        DialogPane pane = dialog.getDialogPane();
        if (dialog instanceof Alert) {
            pane.setMinWidth(560);
            pane.setPrefWidth(560);
            pane.setMinHeight(220);
        }

        URL cssUrl = cssOwner.getResource(cssFile);
        if (cssUrl != null && !pane.getStylesheets().contains(cssUrl.toExternalForm())) {
            pane.getStylesheets().add(cssUrl.toExternalForm());
        }

        if (!pane.getStyleClass().contains("gv-dialog")) pane.getStyleClass().add("gv-dialog");

        applyTransparentStage(dialog);
        Platform.runLater(() -> styleDialogButtons(pane, destructivePrimary));
    }

    /** Convenience: admin-style dialog */
    public static void styleAdminDialog(Dialog<?> dialog, boolean destructivePrimary, Class<?> cssOwner) {
        styleDialog(dialog, destructivePrimary, "/css/admin-hotel-style.css", cssOwner);
    }

    /**
     * Backward-compatible overload used by previous AdminDialogHelper call sites.
     */
    public static void styleDialog(Dialog<?> dialog, boolean destructivePrimary, Class<?> cssOwner) {
        styleAdminDialog(dialog, destructivePrimary, cssOwner);
    }

    /** Convenience: client-style dialog */
    public static void styleClientDialog(Dialog<?> dialog, boolean destructivePrimary, Class<?> cssOwner) {
        styleDialog(dialog, destructivePrimary, "/css/client-hotel-style.css", cssOwner);
    }

    private static void applyTransparentStage(Dialog<?> dialog) {
        dialog.setOnShowing(e -> {
            try {
                Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
                stage.initStyle(StageStyle.TRANSPARENT);
                dialog.getDialogPane().getScene().setFill(Color.TRANSPARENT);
            } catch (Exception ignored) {}
        });
    }

    private static void styleDialogButtons(DialogPane pane, boolean destructivePrimary) {
        for (ButtonType buttonType : pane.getButtonTypes()) {
            Node node = pane.lookupButton(buttonType);
            if (!(node instanceof Button button)) continue;

            button.getStyleClass().removeAll("gv-dialog-btn-primary", "gv-dialog-btn-neutral", "gv-dialog-btn-danger");
            if (!button.getStyleClass().contains("gv-dialog-btn")) button.getStyleClass().add("gv-dialog-btn");

            boolean isCancel = buttonType.getButtonData().isCancelButton() || buttonType == ButtonType.CANCEL;
            boolean isPrimary = buttonType.getButtonData().isDefaultButton() || buttonType == ButtonType.OK;

            if (isCancel) {
                button.getStyleClass().add("gv-dialog-btn-neutral");
            } else if (destructivePrimary && isPrimary) {
                button.getStyleClass().add("gv-dialog-btn-danger");
            } else {
                button.getStyleClass().add("gv-dialog-btn-primary");
            }
            button.setMinWidth(132);
            button.setPrefWidth(148);
            button.setMinHeight(40);
        }
    }

    // ── Confirmation alert ────────────────────────────────────────────────────

    public static void styleConfirmationAlert(Alert alert, boolean destructivePrimary,
                                              String primaryLabel, String cancelLabel,
                                              String cssFile, Class<?> cssOwner) {
        if (alert == null || alert.getDialogPane() == null) return;
        alert.setGraphic(null);
        styleDialog(alert, destructivePrimary, cssFile, cssOwner);
        DialogPane pane = alert.getDialogPane();
        pane.getStyleClass().add("gv-confirmation");
        pane.setMinWidth(560);
        pane.setPrefWidth(560);
        pane.setMinHeight(240);
        if (primaryLabel != null && !primaryLabel.isBlank()) {
            Node okNode = pane.lookupButton(ButtonType.OK);
            if (okNode instanceof Button okButton) okButton.setText(primaryLabel);
        }
        if (cancelLabel != null && !cancelLabel.isBlank()) {
            Node cancelNode = pane.lookupButton(ButtonType.CANCEL);
            if (cancelNode instanceof Button cancelButton) cancelButton.setText(cancelLabel);
        }
    }

    // ── Notifications (admin-style) ───────────────────────────────────────────

    public static void showNotification(String message, String type, Class<?> cssOwner) {
        showNotification(message, type, "/css/admin-hotel-style.css", cssOwner);
    }

    public static void showNotification(String message, String type, String cssFile, Class<?> cssOwner) {
        Alert alert = new Alert(
                "error".equals(type) ? Alert.AlertType.ERROR
                        : "warning".equals(type) ? Alert.AlertType.WARNING
                        : Alert.AlertType.INFORMATION);
        String title = "error".equals(type) ? "Erreur" : "warning".equals(type) ? "Attention" : "Succes";
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        styleConfirmationAlert(alert, "error".equals(type), "OK", null, cssFile, cssOwner);
        alert.showAndWait();
    }

    // ── Confirm delete ────────────────────────────────────────────────────────

    public static void confirmDelete(String type, String name, Runnable onConfirm, Class<?> cssOwner) {
        confirmDelete(type, name, onConfirm, "/css/admin-hotel-style.css", cssOwner);
    }

    public static void confirmDelete(String type, String name, Runnable onConfirm, String cssFile, Class<?> cssOwner) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmer la suppression");
        confirm.setHeaderText("Supprimer " + (name.isEmpty() ? "cet " + type : type + " \"" + name + "\"") + " ?");
        confirm.setContentText("Cette action est irreversible.");
        styleConfirmationAlert(confirm, true, "Supprimer", "Annuler", cssFile, cssOwner);
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            onConfirm.run();
        }
    }

    // ── Table cell factory ────────────────────────────────────────────────────

    public static void applyPlainTextCellFactory(TableColumn<Object, String> column) {
        column.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
                setGraphic(null);
                setAlignment(Pos.CENTER);
            }
        });
    }

    // ── Image preview dialog (from DialogHelper.ClientDialogs) ────────────────────────

    public static void showImagePreview(String imagePath, String title,
                                        Function<String, Image> imageLoader,
                                        String cssFile, Class<?> cssOwner) {
        Image image = imageLoader.apply(imagePath);
        if (image == null) {
            showNotification("Impossible de charger cette image.", "warning", cssFile, cssOwner);
            return;
        }

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(title == null || title.isBlank() ? "Apercu" : title);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.setResizable(true);

        StackPane container = new StackPane();
        container.setPadding(new Insets(8));
        container.getStyleClass().add("gv-image-preview-container");

        ImageView preview = new ImageView(image);
        preview.setPreserveRatio(true);
        preview.setSmooth(true);
        preview.setFitWidth(400);
        preview.setFitHeight(300);

        container.getChildren().add(preview);
        dialog.getDialogPane().setContent(container);
        dialog.getDialogPane().setMinWidth(480);
        dialog.getDialogPane().setMinHeight(340);
        styleDialog(dialog, false, cssFile, cssOwner);
        dialog.showAndWait();
    }

    // ── Simple alert helpers (from DialogHelper.ClientDialogs) ────────────────────────

    public static void showWarning(String title, String message, String cssFile, Class<?> cssOwner) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title); alert.setHeaderText(null); alert.setContentText(message); alert.setGraphic(null);
        styleDialog(alert, false, cssFile, cssOwner);
        alert.showAndWait();
    }

    public static void showError(String title, String message, String cssFile, Class<?> cssOwner) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title); alert.setHeaderText(null); alert.setContentText(message); alert.setGraphic(null);
        styleDialog(alert, false, cssFile, cssOwner);
        alert.showAndWait();
    }

    public static void showSuccess(String title, String message, String cssFile, Class<?> cssOwner) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title); alert.setHeaderText(null); alert.setContentText(message); alert.setGraphic(null);
        styleDialog(alert, false, cssFile, cssOwner);
        alert.showAndWait();
    }

    public static void showInfo(String title, String message, String cssFile, Class<?> cssOwner) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title); alert.setHeaderText(null); alert.setContentText(message); alert.setGraphic(null);
        styleDialog(alert, false, cssFile, cssOwner);
        alert.showAndWait();
    }

    /**
     * Lightweight client-side dialog context.
     * Replaces the old standalone DialogHelper.ClientDialogs file while keeping the same call style.
     */
    public static final class ClientDialogs {
        private final Class<?> cssContext;
        private final Function<String, Image> imageLoader;

        public ClientDialogs(Class<?> cssContext, Function<String, Image> imageLoader) {
            this.cssContext = cssContext;
            this.imageLoader = imageLoader;
        }

        public void showWarning(String title, String message) {
            DialogHelper.showWarning(title, message, "/css/client-hotel-style.css", cssContext);
        }

        public void showError(String title, String message) {
            DialogHelper.showError(title, message, "/css/client-hotel-style.css", cssContext);
        }

        public void showSuccess(String title, String message) {
            DialogHelper.showSuccess(title, message, "/css/client-hotel-style.css", cssContext);
        }

        public void showInfo(String title, String message) {
            DialogHelper.showInfo(title, message, "/css/client-hotel-style.css", cssContext);
        }

        public void showImagePreview(String imagePath, String title) {
            DialogHelper.showImagePreview(imagePath, title, imageLoader, "/css/client-hotel-style.css", cssContext);
        }

        public void styleDialog(Dialog<?> dialog, boolean destructivePrimary) {
            DialogHelper.styleClientDialog(dialog, destructivePrimary, cssContext);
        }
    }
}
