package tn.esprit.projet.gui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.net.URL;
import java.util.function.Function;

/**
 * Centralises all dialog creation and styling for the Client module.
 * Eliminates the repeated styleClientDialog() calls scattered through the original file.
 */
public class ClientDialogHelper {

    private final Class<?> cssContext;
    private final Function<String, Image> imageLoader;

    /**
     * @param cssContext   any class in the same module — used to resolve /css/client-style.css
     * @param imageLoader  shared image-loading function
     */
    public ClientDialogHelper(Class<?> cssContext, Function<String, Image> imageLoader) {
        this.cssContext  = cssContext;
        this.imageLoader = imageLoader;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Simple dialogs
    // ─────────────────────────────────────────────────────────────────────────

    public void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.setGraphic(null);
        styleDialog(alert, false);
        alert.showAndWait();
    }

    public void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.setGraphic(null);
        styleDialog(alert, false);
        alert.showAndWait();
    }

    public void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setGraphic(null);

        // Build custom content
        VBox content = new VBox(16);
        content.setPadding(new Insets(8, 4, 8, 4));

        // Icon + heading row
        HBox headRow = new HBox(14);
        headRow.setAlignment(Pos.CENTER_LEFT);

        StackPane iconCircle = new StackPane();
        iconCircle.setPrefSize(44, 44);
        iconCircle.setMinSize(44, 44);
        iconCircle.setMaxSize(44, 44);
        iconCircle.setStyle(
                "-fx-background-color: rgba(255,130,16,0.15);" +
                        "-fx-background-radius: 22;"
        );
        Label checkLbl = new Label("✓");
        checkLbl.setStyle("-fx-font-size: 20px; -fx-font-weight: 900; -fx-text-fill: #FF8210;");
        iconCircle.getChildren().add(checkLbl);

        VBox headText = new VBox(3);
        Label headTitle = new Label(title);
        headTitle.setStyle("-fx-font-size: 17px; -fx-font-weight: 800; -fx-text-fill: white;");
        Label headSub = new Label("Votre réservation a bien été enregistrée");
        headSub.setStyle("-fx-font-size: 11px; -fx-text-fill: rgba(255,255,255,0.38);");
        headText.getChildren().addAll(headTitle, headSub);
        headRow.getChildren().addAll(iconCircle, headText);

        // Details card
        VBox card = new VBox(10);
        card.setStyle(
                "-fx-background-color: rgba(255,255,255,0.05);" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-color: rgba(255,255,255,0.08);" +
                        "-fx-border-radius: 10;" +
                        "-fx-border-width: 1;"
        );
        card.setPadding(new Insets(14, 18, 14, 18));

        for (String line : message.split("\n")) {
            String t = line.trim();
            if (t.isEmpty() || t.startsWith("Votre réservation")) continue;
            if (t.startsWith("Chambre ") && t.contains("–")) {
                Label l = new Label(t);
                l.setStyle("-fx-font-size: 14px; -fx-font-weight: 800; -fx-text-fill: white;");
                card.getChildren().add(l);
            } else if (t.contains("nuit") || t.contains("personne") || t.contains("Arrivée")) {
                HBox chips = new HBox(8);
                chips.setAlignment(Pos.CENTER_LEFT);
                for (String p : t.split("·")) {
                    String s = p.trim();
                    if (s.isEmpty()) continue;
                    Label chip = new Label(s);
                    chip.setStyle(
                            "-fx-background-color: rgba(255,130,16,0.12);" +
                                    "-fx-background-radius: 20;" +
                                    "-fx-border-color: rgba(255,130,16,0.28);" +
                                    "-fx-border-radius: 20;" +
                                    "-fx-border-width: 1;" +
                                    "-fx-text-fill: #FF8210;" +
                                    "-fx-font-size: 11px; -fx-font-weight: 700;" +
                                    "-fx-padding: 5 12;"
                    );
                    chips.getChildren().add(chip);
                }
                card.getChildren().add(chips);
            } else {
                Label l = new Label(t);
                l.setStyle("-fx-font-size: 12px; -fx-text-fill: rgba(255,255,255,0.48);");
                card.getChildren().add(l);
            }
        }

        content.getChildren().addAll(headRow, card);
        alert.getDialogPane().setContent(content);
        styleDialog(alert, false);
        alert.showAndWait();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Image preview dialog
    // ─────────────────────────────────────────────────────────────────────────

    public void showImagePreview(String imagePath, String title) {
        Image image = imageLoader.apply(imagePath);
        if (image == null) {
            showWarning("Image", "Impossible de charger cette image.");
            return;
        }

        Rectangle2D screen        = Screen.getPrimary().getVisualBounds();
        double      maxWidth      = Math.max(560, screen.getWidth()  * 0.88);
        double      maxHeight     = Math.max(420, screen.getHeight() * 0.84);
        double      imageWidth    = image.getWidth()  > 0 ? image.getWidth()  : maxWidth;
        double      imageHeight   = image.getHeight() > 0 ? image.getHeight() : maxHeight;
        double      contentWidth  = Math.max(420, Math.min(imageWidth,  maxWidth  - 40));
        double      contentHeight = Math.max(260, Math.min(imageHeight, maxHeight - 120));

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle((title == null || title.isBlank()) ? "Apercu image" : title);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.setResizable(true);

        StackPane container = new StackPane();
        container.setPadding(new Insets(8));
        container.setStyle("-fx-background-color: rgba(0,0,0,0.35); -fx-background-radius: 12;");

        ImageView preview = new ImageView(image);
        preview.setPreserveRatio(true);
        preview.setSmooth(true);
        preview.setFitWidth(contentWidth);
        preview.setFitHeight(contentHeight);

        ScrollPane scroll = new ScrollPane(preview);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        scroll.setPannable(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scroll.setPrefViewportWidth(contentWidth);
        scroll.setPrefViewportHeight(contentHeight);

        container.getChildren().add(scroll);
        dialog.getDialogPane().setContent(container);
        dialog.getDialogPane().setMinWidth(480);
        dialog.getDialogPane().setMinHeight(340);
        dialog.getDialogPane().setPrefWidth(Math.min(maxWidth,  contentWidth  + 28));
        dialog.getDialogPane().setPrefHeight(Math.min(maxHeight, contentHeight + 96));
        styleDialog(dialog, false);
        dialog.showAndWait();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Booking confirmation dialog
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Shows a confirmation then a success alert. Returns true if the user confirmed.
     */
    public boolean showBookingConfirmation(
            String roomNumber, String roomType, double pricePerNight,
            int capacity, String hotelName, int nights
    ) {
        double total = pricePerNight * Math.max(1, nights);
        int safeNights = Math.max(1, nights);

        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Confirmation de reservation");

        ButtonType confirmBtn = new ButtonType("Confirmer", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(confirmBtn, ButtonType.CANCEL);

        // Build content
        javafx.scene.layout.VBox root = new javafx.scene.layout.VBox(20);
        root.setPadding(new javafx.geometry.Insets(28, 32, 24, 32));
        root.setMinWidth(480);

        // Title
        javafx.scene.control.Label titleLabel = new javafx.scene.control.Label("Reserver la chambre " + roomNumber);
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: 800; -fx-text-fill: white;");

        // Details grid
        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(16); grid.setVgap(10);
        grid.setStyle("-fx-background-color: rgba(255,255,255,0.04); -fx-background-radius: 12; -fx-padding: 16;");
        addBookingRow(grid, 0, "Hotel", hotelName);
        addBookingRow(grid, 1, "Type de chambre", roomType);
        addBookingRow(grid, 2, "Prix / nuit", ClientUtils.formatPrice(pricePerNight) + " DT");
        addBookingRow(grid, 3, "Nombre de nuits", String.valueOf(safeNights));
        addBookingRow(grid, 4, "Capacite", capacity + " personne(s)");

        // Total row - highlighted
        javafx.scene.control.Label totalKey = new javafx.scene.control.Label("Total estime");
        totalKey.setStyle("-fx-font-size: 14px; -fx-text-fill: rgba(255,255,255,0.60); -fx-font-weight: 600;");
        javafx.scene.control.Label totalVal = new javafx.scene.control.Label(ClientUtils.formatPrice(total) + " DT");
        totalVal.setStyle("-fx-font-size: 18px; -fx-text-fill: #FF8210; -fx-font-weight: 800;");
        grid.add(totalKey, 0, 5);
        grid.add(totalVal, 1, 5);

        root.getChildren().addAll(titleLabel, grid);
        dialog.getDialogPane().setContent(root);
        styleDialog(dialog, false);

        // Style the confirm button as orange
        javafx.application.Platform.runLater(() -> {
            javafx.scene.Node confirmNode = dialog.getDialogPane().lookupButton(confirmBtn);
            if (confirmNode instanceof javafx.scene.control.Button b) {
                b.setStyle("-fx-background-color: #FF8210; -fx-text-fill: white; -fx-font-weight: 700; -fx-background-radius: 10; -fx-min-width: 140; -fx-min-height: 40;");
            }
        });

        dialog.setResultConverter(btn -> btn == confirmBtn);
        boolean confirmed = dialog.showAndWait().orElse(false);
        if (confirmed) {
            showInfo("Reservation confirmee",
                    "Votre chambre a ete reservee avec succes!\n\n"
                            + "Chambre " + roomNumber + " - " + roomType + "\n"
                            + hotelName + "\n"
                            + safeNights + " nuit(s) - Total: " + ClientUtils.formatPrice(total) + " DT");
        }
        return confirmed;
    }

    private void addBookingRow(javafx.scene.layout.GridPane grid, int row, String key, String value) {
        javafx.scene.control.Label k = new javafx.scene.control.Label(key);
        k.setStyle("-fx-font-size: 13px; -fx-text-fill: rgba(255,255,255,0.55); -fx-font-weight: 600;");
        javafx.scene.control.Label v = new javafx.scene.control.Label(value);
        v.setStyle("-fx-font-size: 13px; -fx-text-fill: rgba(255,255,255,0.88); -fx-font-weight: 600;");
        grid.add(k, 0, row);
        grid.add(v, 1, row);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Core styling
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Applies client-style.css and button classes to any dialog.
     *
     * @param dialog            the dialog to style
     * @param destructivePrimary true → primary button uses danger style
     */
    public void styleDialog(Dialog<?> dialog, boolean destructivePrimary) {
        if (dialog == null || dialog.getDialogPane() == null) return;

        DialogPane pane = dialog.getDialogPane();
        if (dialog instanceof Alert) {
            pane.setMinWidth(560);
            pane.setPrefWidth(560);
            pane.setMinHeight(220);
        }

        URL cssUrl = cssContext.getResource("/css/client-style.css");
        if (cssUrl != null) {
            String css = cssUrl.toExternalForm();
            if (!pane.getStylesheets().contains(css)) pane.getStylesheets().add(css);
        }

        if (!pane.getStyleClass().contains("gv-dialog"))        pane.getStyleClass().add("gv-dialog");
        if (!pane.getStyleClass().contains("gv-client-dialog")) pane.getStyleClass().add("gv-client-dialog");

        applyTransparentStage(dialog);
        Platform.runLater(() -> {
            for (ButtonType buttonType : pane.getButtonTypes()) {
                Node node = pane.lookupButton(buttonType);
                if (!(node instanceof Button button)) continue;

                button.getStyleClass().removeAll("gv-dialog-btn-primary", "gv-dialog-btn-neutral", "gv-dialog-btn-danger");
                if (!button.getStyleClass().contains("gv-dialog-btn")) button.getStyleClass().add("gv-dialog-btn");

                boolean isCancel  = buttonType.getButtonData().isCancelButton() || buttonType == ButtonType.CANCEL;
                boolean isPrimary = buttonType.getButtonData().isDefaultButton() || buttonType == ButtonType.OK;

                if (isCancel)                          button.getStyleClass().add("gv-dialog-btn-neutral");
                else if (destructivePrimary && isPrimary) button.getStyleClass().add("gv-dialog-btn-danger");
                else                                   button.getStyleClass().add("gv-dialog-btn-primary");
            }

            Node contentNode = pane.lookup(".content.label");
            if (contentNode instanceof Label contentLabel) {
                contentLabel.setWrapText(true);
                contentLabel.setMaxWidth(520);
                contentLabel.setMinHeight(Region.USE_PREF_SIZE);
            }
        });
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
}