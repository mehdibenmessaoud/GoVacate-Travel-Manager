package tn.esprit.projet.gui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import tn.esprit.projet.entities.Excursion;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import tn.esprit.projet.services.CurrencyService;

import javafx.fxml.Initializable;
import javafx.application.Platform; // Pour le thread de l'API

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.image.Image;
import javafx.application.Platform;
import org.json.JSONObject; // Assure-toi d'avoir la bibliothèque JSON dans ton projet
import tn.esprit.projet.services.ExcursionService;
import tn.esprit.projet.services.WeatherService;


public class ExcursionDetailsController implements Initializable {

    @FXML private Label nameLabel;
    @FXML private Label activiteLabel;
    @FXML private Label priceLabel;
    @FXML private Label durationLabel;
    @FXML private Label statusLabel;
    @FXML private Label datesLabel;
    @FXML private Label maxParticipantsLabel;
    @FXML private Text descriptionLabel;
    @FXML private HBox imageContainer;
    @FXML private Label locationLabel; // Pour afficher "Ville, Pays"

    @FXML
    private ImageView weatherIcon;

    @FXML
    private Label tempLabel;

    @FXML
    private Label descLabel;

    @FXML private ComboBox<String> currencyCombo;
    @FXML private Label convertedPriceLabel;
    @FXML private Label promoBadge;
    private double currentExcursionPrice; // Pour stocker le prix en DT
    private final ExcursionService excursionService = new ExcursionService();
    // AJOUTE CETTE LIGNE ICI :



      //Remplit l'interface avec les données de l'excursion sélectionnée.

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (currencyCombo != null) {
            currencyCombo.getItems().addAll("EUR", "USD", "GBP");

            // Sélectionner EUR par défaut pour que l'utilisateur voit tout de suite l'utilité
            currencyCombo.getSelectionModel().select("EUR");

            currencyCombo.setOnAction(event -> {
                String selected = currencyCombo.getValue();
                if (selected != null) {
                    updateConvertedPrice(selected);
                }
            });
        }
    }

    /*public void setExcursionData(Excursion e) {
        // 0. Stockage du prix pour la conversion monétaire
        this.currentExcursionPrice = e.getPrice();

        // 1. Remplissage des textes basiques
        if (nameLabel != null) nameLabel.setText(e.getName());
        if (activiteLabel != null) activiteLabel.setText(e.getActivite());
        if (priceLabel != null) priceLabel.setText(e.getPrice() + " DT");
        if (durationLabel != null) durationLabel.setText(e.getDuration() + " h");
        if (statusLabel != null) statusLabel.setText(e.getStatus());
        if (descriptionLabel != null) descriptionLabel.setText(e.getDescription());

        int currentReservations = excursionService.getCurrentReservations(e.getId());
        double dynamicPrice = excursionService.calculateDynamicPrice(e, currentReservations);
        this.currentExcursionPrice = dynamicPrice;

        if (dynamicPrice < e.getPrice()) {
            // Cas PROMO : on affiche le nouveau prix et on change de style
            priceLabel.setText(String.format("%.1f DT", dynamicPrice));
            priceLabel.setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold;"); // Vert

            // Optionnel : Ajouter un texte "Offre Spéciale"
            if (statusLabel != null) {
                statusLabel.setText("OFFRE FLASH");
                statusLabel.setStyle("-fx-background-color: #FF8210;");
            }
        } else if (dynamicPrice > e.getPrice()) {
            // Cas FORTE DEMANDE
            priceLabel.setText(String.format("%.1f DT", dynamicPrice));
            priceLabel.setStyle("-fx-text-fill: #e74c3c;"); // Rouge
        } else {
            priceLabel.setText(e.getPrice() + " DT");
        }

        // Mise à jour de la conversion monétaire avec le nouveau prix
        if (currencyCombo != null && currencyCombo.getValue() != null) {
            updateConvertedPrice(currencyCombo.getValue());
        }



        // 2. Gestion unifiée de la Localisation et de la Météo
        // On priorise 'fullLocation' (Ville, Pays) pour le texte et 'ville' pour l'API Météo
        String villePourMeteo = null;

        if (e.getFullLocation() != null && !e.getFullLocation().trim().isEmpty()) {
            locationLabel.setText(e.getFullLocation());
            villePourMeteo = e.getVille();
        } else if (e.getDestinationName() != null && !e.getDestinationName().trim().isEmpty()) {
            // Fallback si fullLocation est vide mais destinationName existe
            locationLabel.setText(e.getDestinationName());
            villePourMeteo = e.getDestinationName();
        } else {
            locationLabel.setText("Destination inconnue");
        }

        // Un SEUL appel à la météo pour éviter les conflits de threads
        if (villePourMeteo != null) {
            System.out.println("Chargement météo pour : " + villePourMeteo);
            afficherMeteo(villePourMeteo);
        } else if (descLabel != null) {
            descLabel.setText("Lieu non défini");
        }

        // 3. Conversion de prix (si la devise est déjà sélectionnée)
        if (currencyCombo != null && currencyCombo.getValue() != null) {
            updateConvertedPrice(currencyCombo.getValue());
        }

        // 4. Participants et Dates
        if (maxParticipantsLabel != null) {
            maxParticipantsLabel.setText(e.getMaxParticipants() + " personnes");
        }

        if (datesLabel != null) {
            if (e.getDateDebut() != null && e.getDateFin() != null) {
                datesLabel.setText("Du " + e.getDateDebut() + " au " + e.getDateFin());
            } else {
                datesLabel.setText("Période non spécifiée");
            }
        }

        // 5. Galerie d'images (Carousel)
        chargerGalerie(e.getImages());
    }*/


    public void setExcursionData(Excursion e) {
        // 0. Récupération des données dynamiques via le Service
        int currentReservations = excursionService.getCurrentReservations(e.getId());
        double dynamicPrice = excursionService.calculateDynamicPrice(e, currentReservations);
        this.currentExcursionPrice = dynamicPrice;

        // 1. Remplissage des textes basiques
        if (nameLabel != null) nameLabel.setText(e.getName());
        if (activiteLabel != null) activiteLabel.setText(e.getActivite());
        if (durationLabel != null) durationLabel.setText(e.getDuration() + " h");
        if (statusLabel != null) statusLabel.setText(e.getStatus());
        if (descriptionLabel != null) descriptionLabel.setText(e.getDescription());

        // --- LOGIQUE VISUELLE DU PRIX ET DU BADGE ---
        if (dynamicPrice < e.getPrice()) {
            // Cas PROMO
            priceLabel.setText(String.format("%.1f DT", dynamicPrice));
            priceLabel.setStyle("-fx-text-fill: #ff9800; -fx-font-weight: bold; -fx-font-size: 20;"); // Vert

            if (promoBadge != null) {
                promoBadge.setText("🔥 OFFRE DERNIÈRE MINUTE : -20%");
                promoBadge.setVisible(true);
            }
        } else if (dynamicPrice > e.getPrice()) {
            // Cas FORTE DEMANDE
            priceLabel.setText(String.format("%.1f DT", dynamicPrice));
            priceLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-font-size: 32;"); // Rouge

            if (promoBadge != null) {
                promoBadge.setText("⚡ TRÈS DEMANDÉ");
                promoBadge.setVisible(true);
            }
        } else {
            // Cas NORMAL
            priceLabel.setText(e.getPrice() + " DT");
            priceLabel.setStyle("-fx-text-fill: #FF8210;"); // Orange normal
            if (promoBadge != null) promoBadge.setVisible(false);
        }

        // 2. Gestion unifiée de la Localisation et de la Météo
        String villePourMeteo = null;
        if (e.getFullLocation() != null && !e.getFullLocation().trim().isEmpty()) {
            locationLabel.setText(e.getFullLocation());
            villePourMeteo = e.getVille();
        } else if (e.getDestinationName() != null && !e.getDestinationName().trim().isEmpty()) {
            locationLabel.setText(e.getDestinationName());
            villePourMeteo = e.getDestinationName();
        } else {
            locationLabel.setText("Destination inconnue");
        }

        if (villePourMeteo != null) {
            afficherMeteo(villePourMeteo);
        } else if (descLabel != null) {
            descLabel.setText("Lieu non défini");
        }

        // 3. Mise à jour de la conversion monétaire
        if (currencyCombo != null && currencyCombo.getValue() != null) {
            updateConvertedPrice(currencyCombo.getValue());
        }

        // 4. Participants et Dates
        if (maxParticipantsLabel != null) {
            maxParticipantsLabel.setText(e.getMaxParticipants() + " personnes");
        }

        if (datesLabel != null) {
            if (e.getDateDebut() != null && e.getDateFin() != null) {
                datesLabel.setText("Du " + e.getDateDebut() + " au " + e.getDateFin());
            } else {
                datesLabel.setText("Période non spécifiée");
            }
        }

        // 5. Galerie d'images
        chargerGalerie(e.getImages());
    }



    /**
     * Méthode extraite pour garder le code propre
     */
    private void chargerGalerie(String imagesStr) {
        if (imageContainer == null) return;

        imageContainer.getChildren().clear();
        if (imagesStr != null && !imagesStr.isEmpty()) {
            String[] imagePaths = imagesStr.split(",");
            for (String path : imagePaths) {
                try {
                    File file = new File("src/main/resources/imageEx/" + path.trim());
                    if (file.exists()) {
                        Image img = new Image(file.toURI().toString());
                        ImageView iv = new ImageView(img);
                        iv.setFitHeight(220);
                        iv.setPreserveRatio(true);
                        iv.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 10, 0, 0, 0);");
                        imageContainer.getChildren().add(iv);
                    }
                } catch (Exception ex) {
                    System.err.println("Erreur image : " + path);
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
        if (nomVille == null || nomVille.trim().isEmpty()) return;

        new Thread(() -> {
            try {
                JSONObject data = WeatherService.getWeatherByCity(nomVille);
                if (data != null && data.has("main")) {
                    double temp = data.getJSONObject("main").getDouble("temp");
                    String desc = data.getJSONArray("weather").getJSONObject(0).getString("description");
                    String iconCode = data.getJSONArray("weather").getJSONObject(0).getString("icon");
                    String iconUrl = "https://openweathermap.org/img/wn/" + iconCode + "@2x.png";

                    Platform.runLater(() -> {
                        if (tempLabel != null) tempLabel.setText(String.format("%.1f°C", temp));
                        if (descLabel != null) {
                            String formattedDesc = desc.substring(0, 1).toUpperCase() + desc.substring(1);
                            descLabel.setText(formattedDesc);
                        }
                        if (weatherIcon != null) weatherIcon.setImage(new Image(iconUrl));
                    });
                }
            } catch (Exception e) {
                System.err.println("Météo introuvable pour : " + nomVille);
                Platform.runLater(() -> {
                    if (descLabel != null) descLabel.setText("Météo indisponible");
                });
            }
        }).start();
    }

    private void updateConvertedPrice(String currency) {
        // On vérifie que le prix de l'excursion n'est pas nul
        if (currentExcursionPrice <= 0) {
            System.out.println("Le prix actuel est invalide pour la conversion.");
            return;
        }

        // On lance l'appel API dans un nouveau Thread pour ne pas bloquer l'interface (UI)
        new Thread(() -> {
            try {
                // 1. Appel au service de change pour récupérer le taux
                double rate = CurrencyService.getExchangeRate(currency);

                // 2. Calcul du prix converti
                double convertedValue = currentExcursionPrice * rate;

                // 3. Mise à jour de l'interface graphique sur le thread principal JavaFX
                Platform.runLater(() -> {
                    if (rate > 0) {
                        convertedPriceLabel.setText(String.format("≈ %.2f %s", convertedValue, currency));
                    } else {
                        convertedPriceLabel.setText("Erreur taux");
                    }
                });
            } catch (Exception ex) {
                System.err.println("Erreur lors de la conversion : " + ex.getMessage());
                Platform.runLater(() -> convertedPriceLabel.setText("Indisponible"));
            }
        }).start();
    }
}