package tn.esprit.projet.gui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.application.Platform;

import java.net.URL;

/**
 * Reusable dialog construction and styling utilities.
 * All Admin sub-controllers share this helper so dialog chrome
 * stays consistent without code duplication.
 */
public final class AdminDialogHelper {

    private AdminDialogHelper() {}

    // ── Layout helpers ───────────────────────────────────────────────────────

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

    // ── Styling ──────────────────────────────────────────────────────────────

    public static void styleDialog(Dialog<?> dialog, boolean destructivePrimary, Class<?> cssOwner) {
        if (dialog == null || dialog.getDialogPane() == null) return;
        DialogPane pane = dialog.getDialogPane();
        URL cssUrl = cssOwner.getResource("/css/admin-style.css");
        if (cssUrl != null) {
            String css = cssUrl.toExternalForm();
            if (!pane.getStylesheets().contains(css)) {
                pane.getStylesheets().add(css);
            }
        }
        if (!pane.getStyleClass().contains("gv-dialog")) pane.getStyleClass().add("gv-dialog");
        if (!pane.getStyleClass().contains("gv-admin-dialog")) pane.getStyleClass().add("gv-admin-dialog");
        applyTransparentStage(dialog);
        Platform.runLater(() -> styleDialogButtons(pane, destructivePrimary));
    }

    /**
     * Makes the dialog's underlying Stage transparent so the rounded
     * CSS border-radius on the dialog pane actually shows through instead
     * of being clipped by the opaque OS window decoration.
     * Must be called before the dialog is shown — we hook into onShowing
     * which fires just before the window becomes visible.
     */
    private static void applyTransparentStage(Dialog<?> dialog) {
        dialog.setOnShowing(e -> {
            try {
                Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
                stage.initStyle(StageStyle.TRANSPARENT);
                dialog.getDialogPane().getScene().setFill(Color.TRANSPARENT);
            } catch (Exception ignored) {
                // initStyle throws if called after the stage was already shown once;
                // safe to ignore — rounded corners simply won't apply that call.
            }
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
            ButtonBar.setButtonUniformSize(button, false);
            button.setWrapText(false);
            button.setMinWidth(132);
            button.setPrefWidth(148);
            button.setMinHeight(40);
        }
    }

    public static void styleConfirmationAlert(Alert alert, boolean destructivePrimary,
                                              String primaryLabel, String cancelLabel,
                                              Class<?> cssOwner) {
        if (alert == null || alert.getDialogPane() == null) return;
        alert.setGraphic(null);
        styleDialog(alert, destructivePrimary, cssOwner);
        DialogPane pane = alert.getDialogPane();
        pane.getStyleClass().removeAll("gv-notification", "gv-confirmation");
        if (!pane.getStyleClass().contains("gv-confirmation")) pane.getStyleClass().add("gv-confirmation");
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

    public static void showNotification(String message, String type, Class<?> cssOwner) {
        Alert alert = new Alert(
                "error".equals(type) ? Alert.AlertType.ERROR
                        : "warning".equals(type) ? Alert.AlertType.WARNING
                        : Alert.AlertType.INFORMATION);
        String title = "error".equals(type) ? "Erreur" : "warning".equals(type) ? "Attention" : "Succes";
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        styleConfirmationAlert(alert, "error".equals(type), "OK", null, cssOwner);
        DialogPane pane = alert.getDialogPane();
        pane.getStyleClass().removeAll("gv-confirmation", "gv-notification");
        if (!pane.getStyleClass().contains("gv-notification")) pane.getStyleClass().add("gv-notification");
        pane.setMinWidth(560);
        pane.setPrefWidth(560);
        pane.setMinHeight(240);
        Node okNode = pane.lookupButton(ButtonType.OK);
        if (okNode instanceof Button okButton) okButton.setText("OK");
        alert.showAndWait();
    }

    public static void confirmDelete(String type, String name, Runnable onConfirm, Class<?> cssOwner) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmer la suppression");
        confirm.setHeaderText("Supprimer " + (name.isEmpty() ? "cet " + type : type + " \"" + name + "\"") + " ?");
        confirm.setContentText("Cette action est irreversible.");
        styleConfirmationAlert(confirm, true, "Supprimer", "Annuler", cssOwner);
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            onConfirm.run();
        }
    }

    // ── Table cell helpers ───────────────────────────────────────────────────

    public static void applyPlainTextCellFactory(TableColumn<Object, String> column) {
        column.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); }
                else { setText(item); setGraphic(null); }
                setAlignment(Pos.CENTER);
                setTextOverrun(OverrunStyle.CLIP);
                setWrapText(false);
            }
        });
    }

    // ── Detail action button helpers ─────────────────────────────────────────

    public static Button createDetailActionButton(String label, String backgroundColor) {
        Button button = new Button(label);
        button.setStyle("-fx-background-color: " + backgroundColor + "; -fx-text-fill: white; -fx-background-radius: 25; -fx-padding: 12 22; -fx-font-size: 14px; -fx-font-weight: 600; -fx-cursor: hand;");
        button.setWrapText(false);
        button.setTextOverrun(OverrunStyle.CLIP);
        button.setMinWidth(Region.USE_PREF_SIZE);
        button.setPrefWidth(Region.USE_COMPUTED_SIZE);
        button.setMaxWidth(Region.USE_PREF_SIZE);
        button.setMinHeight(44);
        return button;
    }

    public static Button createTableActionButton(String label, String normalStyle, String hoverStyle, double minWidth) {
        Button btn = new Button(label);
        btn.setStyle(normalStyle);
        btn.setOnMouseEntered(e -> btn.setStyle(hoverStyle));
        btn.setOnMouseExited(e -> btn.setStyle(normalStyle));
        btn.setWrapText(false);
        btn.setTextOverrun(OverrunStyle.CLIP);
        btn.setMinWidth(minWidth);
        btn.setPrefWidth(minWidth);
        btn.setMaxWidth(minWidth);
        return btn;
    }

    // ── Preset table button styles ───────────────────────────────────────────

    public static final String BLUE_BTN = "-fx-background-color: linear-gradient(to bottom, #679AC1, #5a8ab0); -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 8 16; -fx-cursor: hand; -fx-font-size: 12px; -fx-font-weight: bold; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 4, 0, 0, 1);";
    public static final String BLUE_BTN_HOVER = "-fx-background-color: linear-gradient(to bottom, #7aadd4, #679AC1); -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 8 16; -fx-cursor: hand; -fx-font-size: 12px; -fx-font-weight: bold; -fx-effect: dropshadow(gaussian, rgba(103,154,193,0.5), 8, 0, 0, 2);";
    public static final String ORANGE_BTN = "-fx-background-color: linear-gradient(to bottom, #FF8210, #e67400); -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 8 16; -fx-cursor: hand; -fx-font-size: 12px; -fx-font-weight: bold; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 4, 0, 0, 1);";
    public static final String ORANGE_BTN_HOVER = "-fx-background-color: linear-gradient(to bottom, #ff9933, #FF8210); -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 8 16; -fx-cursor: hand; -fx-font-size: 12px; -fx-font-weight: bold; -fx-effect: dropshadow(gaussian, rgba(255,130,16,0.5), 8, 0, 0, 2);";
    public static final String RED_BTN = "-fx-background-color: linear-gradient(to bottom, #dc3545, #c82333); -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 8 16; -fx-cursor: hand; -fx-font-size: 12px; -fx-font-weight: bold; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 4, 0, 0, 1);";
    public static final String RED_BTN_HOVER = "-fx-background-color: linear-gradient(to bottom, #e74c5c, #dc3545); -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 8 16; -fx-cursor: hand; -fx-font-size: 12px; -fx-font-weight: bold; -fx-effect: dropshadow(gaussian, rgba(220,53,69,0.5), 8, 0, 0, 2);";
}