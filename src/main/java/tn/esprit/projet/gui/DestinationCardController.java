package tn.esprit.projet.gui;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import tn.esprit.projet.entities.Destination;

import java.io.File;
import java.util.function.Consumer;

public class DestinationCardController {
    @FXML private ImageView destinationImage;
    @FXML private Label nameLabel;
    @FXML private Label locationLabel;

    private Destination destination;
    private Consumer<Destination> onClickListener;

    public void setData(Destination d, Consumer<Destination> listener) {
        this.destination = d;
        this.onClickListener = listener;

        nameLabel.setText(d.getNameDestination());
        locationLabel.setText(d.getVille() + ", " + d.getPays());

        // Chargement de l'image (chemin à adapter selon ton stockage admin)
        try {
            File file = new File("src/main/resources/imageEx/" + d.getImage()); // On réutilise ton dossier
            if (file.exists()) {
                destinationImage.setImage(new Image(file.toURI().toString()));
            }
        } catch (Exception e) {
            System.out.println("Image destination introuvable : " + d.getImage());
        }
    }

    @FXML
    private void handleClick() {
        if (onClickListener != null) {
            onClickListener.accept(destination);
        }
    }
}