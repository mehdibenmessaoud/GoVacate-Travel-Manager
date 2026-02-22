package tn.esprit.projet.gui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import tn.esprit.projet.entities.Excursion;
import java.io.File;
import java.io.IOException;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.image.Image;
import javafx.application.Platform;
import org.json.JSONObject; // Assure-toi d'avoir la bibliothèque JSON dans ton projet
import tn.esprit.projet.services.WeatherService;

public class ExcursionDetailsController {

    @FXML private Label nameLabel;
    @FXML private Label activiteLabel;
    @FXML private Label priceLabel;
    @FXML private Label durationLabel;
    @FXML private Label statusLabel;
    @FXML private Label datesLabel;
    @FXML private Label maxParticipantsLabel;
    @FXML private Text descriptionLabel;
    @FXML private HBox imageContainer;

    @FXML
    private ImageView weatherIcon;

    @FXML
    private Label tempLabel;

    @FXML
    private Label descLabel;


      //Remplit l'interface avec les données de l'excursion sélectionnée.

    public void setExcursionData(Excursion e) {
        // 1. Remplissage des textes basiques
        if (nameLabel != null) nameLabel.setText(e.getName());
        if (activiteLabel != null) activiteLabel.setText(e.getActivite());
        if (priceLabel != null) priceLabel.setText(e.getPrice() + " DT");
        if (durationLabel != null) durationLabel.setText(e.getDuration() + " h");
        if (statusLabel != null) statusLabel.setText(e.getStatus());
        if (descriptionLabel != null) descriptionLabel.setText(e.getDescription());

        // 2. GESTION DE LA MÉTÉO (Dynamique grâce à la jointure SQL)
        // On vérifie que le nom de la destination est bien arrivé depuis le Service
        if (e.getDestinationName() != null && !e.getDestinationName().isEmpty()) {
            System.out.println("Chargement météo pour : " + e.getDestinationName());
            afficherMeteo(e.getDestinationName());
        } else {
            System.out.println("Avertissement : Nom de destination vide pour l'excursion " + e.getName());
            if (descLabel != null) descLabel.setText("Lieu non défini");
        }

        // 3. Affichage du nombre maximum de participants
        if (maxParticipantsLabel != null) {
            maxParticipantsLabel.setText(e.getMaxParticipants() + " personnes");
        }

        // 4. Gestion de l'affichage des dates (Période)
        if (datesLabel != null) {
            if (e.getDateDebut() != null && e.getDateFin() != null) {
                datesLabel.setText("Du " + e.getDateDebut() + " au " + e.getDateFin());
            } else {
                datesLabel.setText("Période non spécifiée");
            }
        }

        // 5. Gestion de la galerie d'images (Carousel)
        if (imageContainer != null) {
            imageContainer.getChildren().clear();

            if (e.getImages() != null && !e.getImages().isEmpty()) {
                String[] imagePaths = e.getImages().split(",");

                for (String path : imagePaths) {
                    try {
                        File file = new File("src/main/resources/imageEx/" + path.trim());

                        if (file.exists()) {
                            Image img = new Image(file.toURI().toString());
                            ImageView iv = new ImageView(img);

                            iv.setFitHeight(220);
                            iv.setPreserveRatio(true);
                            // Effet visuel pour le style Glassmorphism
                            iv.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 10, 0, 0, 0);");

                            imageContainer.getChildren().add(iv);
                        }
                    } catch (Exception ex) {
                        System.err.println("Erreur chargement image : " + path);
                    }
                }
            }
        }
    }
    /*public void setExcursionData(Excursion e) {
        // Remplissage des textes basiques
        if (nameLabel != null) nameLabel.setText(e.getName());
        if (activiteLabel != null) activiteLabel.setText(e.getActivite());
        if (priceLabel != null) priceLabel.setText(e.getPrice() + " DT");
        if (durationLabel != null) durationLabel.setText(e.getDuration() + " h");
        if (statusLabel != null) statusLabel.setText(e.getStatus());
        if (descriptionLabel != null) descriptionLabel.setText(e.getDescription());
        // On récupère le nom de la destination liée à l'excursion

        // Affichage du nombre maximum de participants
        if (maxParticipantsLabel != null) {
            maxParticipantsLabel.setText(e.getMaxParticipants() + " personnes");
        }

        // Gestion de l'affichage des dates (Période)
        if (datesLabel != null) {
            // On vérifie si les deux dates existent dans l'objet Excursion
            if (e.getDateDebut() != null && e.getDateFin() != null) {
                datesLabel.setText("Du " + e.getDateDebut() + " au " + e.getDateFin());
            } else {
                // Message si l'une des dates (ou les deux) est manquante
                datesLabel.setText("Période non spécifiée");
            }
        }

        // Gestion de la galerie d'images roulante (Carousel)
        if (imageContainer != null) {
            imageContainer.getChildren().clear(); // On vide les images précédentes

            // On vérifie si l'entité possède des images enregistrées
            if (e.getImages() != null && !e.getImages().isEmpty()) {
                // On sépare la chaîne de caractères si vous avez plusieurs images (séparées par des virgules)
                String[] imagePaths = e.getImages().split(",");

                for (String path : imagePaths) {
                    try {
                        // Utilisation du dossier cible configuré dans votre formulaire : imageEx
                        File file = new File("src/main/resources/imageEx/" + path.trim());

                        if (file.exists()) {
                            Image img = new Image(file.toURI().toString());
                            ImageView iv = new ImageView(img);

                            // Paramètres d'affichage pour la galerie
                            iv.setFitHeight(220);
                            iv.setPreserveRatio(true);

                            // Ajout d'un petit style pour arrondir les images si vous le souhaitez
                            iv.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 10, 0, 0, 0);");

                            imageContainer.getChildren().add(iv);
                        } else {
                            // Log si le fichier est présent en BDD mais absent physiquement du dossier imageEx
                            System.out.println("Fichier introuvable sur le disque : " + file.getAbsolutePath());
                        }
                    } catch (Exception ex) {
                        System.out.println("Erreur de chargement de l'image : " + path);
                    }
                }
            } else {
                // Log si le champ image est vide en base de données
                System.out.println("Aucune image trouvée pour cette excursion (Champ vide).");
            }
        }
    }*/
    /**
     * Retourne à la table de gestion des excursions.
     */
    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ExcursionTable.fxml"));
            Parent table = loader.load();

            // On recherche le conteneur principal (mainLayout) pour changer de vue
            if (nameLabel.getScene() != null) {
                BorderPane mainLayout = (BorderPane) nameLabel.getScene().lookup("#mainLayout");
                if (mainLayout != null) {
                    mainLayout.setCenter(table);
                }
            }
        } catch (IOException e) {
            System.err.println("Erreur lors du retour à la table : " + e.getMessage());
        }
    }

    public void afficherMeteo(String nomVille) {
        // 1. On vérifie si le nom de la ville est valide
        if (nomVille == null || nomVille.isEmpty()) {
            descLabel.setText("Ville non spécifiée");
            return;
        }

        // 2. On lance la requête dans un nouveau Thread pour ne pas bloquer l'interface (UI)
        new Thread(() -> {
            try {
                // Appel à ton service avec ta clé API
                JSONObject data = WeatherService.getWeatherByCity(nomVille);

                if (data != null) {
                    // Extraction des données du JSON
                    double temp = data.getJSONObject("main").getDouble("temp");
                    String desc = data.getJSONArray("weather").getJSONObject(0).getString("description");
                    String iconCode = data.getJSONArray("weather").getJSONObject(0).getString("icon");
                    String iconUrl = "https://openweathermap.org/img/wn/" + iconCode + "@2x.png";

                    // 3. Mise à jour de l'interface graphique sur le Thread principal de JavaFX
                    Platform.runLater(() -> {
                        tempLabel.setText(String.format("%.1f°C", temp));
                        descLabel.setText(desc.substring(0, 1).toUpperCase() + desc.substring(1));
                        weatherIcon.setImage(new Image(iconUrl));
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> descLabel.setText("Erreur météo"));
            }
        }).start();
    }
}