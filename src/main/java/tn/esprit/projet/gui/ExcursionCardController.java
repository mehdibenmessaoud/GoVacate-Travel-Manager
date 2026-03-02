package tn.esprit.projet.gui;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import tn.esprit.projet.entities.Excursion;
import java.io.File;
import java.util.function.Consumer;

public class ExcursionCardController {
    @FXML private ImageView excursionImage;
    @FXML private Label nameLabel, priceBadge, activiteLabel, durationLabel, capacityLabel;

    private Excursion excursion;
    private Consumer<Excursion> listener;

    public void setData(Excursion e, Consumer<Excursion> listener) {
        this.excursion = e;
        this.listener = listener;

        nameLabel.setText(e.getName());
        priceBadge.setText(e.getPrice() + " DT");
        activiteLabel.setText(e.getActivite().toUpperCase());
        durationLabel.setText("⏱ " + e.getDuration() + "h");
        capacityLabel.setText("👥 " + e.getMaxParticipants());

        if (e.getImages() != null && !e.getImages().isEmpty()) {
            String firstImage = e.getImages().split(",")[0];
            File file = new File("src/main/resources/imageEx/" + firstImage.trim());
            if (file.exists()) excursionImage.setImage(new Image(file.toURI().toString()));
        }
    }

    @FXML private void handleClick() { if (listener != null) listener.accept(excursion); }
}