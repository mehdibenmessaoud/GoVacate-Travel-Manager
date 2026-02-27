package tn.esprit.projet.gui;

import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import java.io.InputStream;
import java.util.List;

public class ImageValidationDialog extends Dialog<String> {
    private int currentIndex = 0;
    private final List<String> urls;
    private final ImageView imageView = new ImageView();
    private final ProgressIndicator loader = new ProgressIndicator();

    public ImageValidationDialog(String dishName, List<String> urls) {
        this.urls = urls;
        setTitle("AI Image Finder");
        setHeaderText("Validation pour: " + dishName);

        imageView.setFitWidth(450);
        imageView.setFitHeight(300);
        imageView.setPreserveRatio(true);

        StackPane holder = new StackPane(imageView, loader);
        holder.setPrefSize(450, 300);
        holder.setStyle("-fx-background-color: #1a1e23; -fx-border-color: #333;");

        VBox root = new VBox(15, new Label("Veuillez choisir l'image la plus précise:"), holder);
        root.setAlignment(Pos.CENTER);
        getDialogPane().setContent(root);

        ButtonType btnAccept = new ButtonType("Accepter", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnPrev = new ButtonType("Précédente", ButtonBar.ButtonData.BACK_PREVIOUS);
        ButtonType btnNext = new ButtonType("Suivante", ButtonBar.ButtonData.NEXT_FORWARD);
        ButtonType btnReject = new ButtonType("Passer", ButtonBar.ButtonData.CANCEL_CLOSE);

        getDialogPane().getButtonTypes().addAll(btnPrev, btnNext, btnAccept, btnReject);

        // Circular Navigation Logic
        setupNavButton(btnNext, 1);
        setupNavButton(btnPrev, -1);

        setResultConverter(btn -> (btn == btnAccept) ? urls.get(currentIndex) : null);
        updateImage();
    }

    private void setupNavButton(ButtonType type, int direction) {
        Button btn = (Button) getDialogPane().lookupButton(type);
        btn.addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
            currentIndex = (currentIndex + direction + urls.size()) % urls.size();
            updateImage();
            e.consume();
        });
    }

    private void updateImage() {
        String urlStr = urls.get(currentIndex);
        loader.setVisible(true);
        imageView.setImage(null);

        Task<Image> loadTask = new Task<>() {
            @Override
            protected Image call() throws Exception {
                java.net.URL url = new java.net.URL(urlStr);
                java.net.URLConnection conn = url.openConnection();
                conn.setRequestProperty("User-Agent", "Mozilla/5.0");
                try (InputStream in = conn.getInputStream()) {
                    return new Image(in);
                }
            }
        };

        loadTask.setOnSucceeded(e -> {
            imageView.setImage(loadTask.getValue());
            loader.setVisible(false);
        });

        loadTask.setOnFailed(e -> loader.setVisible(false));
        new Thread(loadTask).start();
    }
}