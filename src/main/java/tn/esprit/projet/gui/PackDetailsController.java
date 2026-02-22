package tn.esprit.projet.gui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import tn.esprit.projet.entities.Pack;
import tn.esprit.projet.services.CurrencyService;
import tn.esprit.projet.services.DestinationService;
import tn.esprit.projet.services.HotelService;
import tn.esprit.projet.services.ExcursionService;
import tn.esprit.projet.services.WeatherService;
import tn.esprit.projet.utils.MyDBConnexion;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class PackDetailsController implements Initializable {

    @FXML private Label nameLabel;
    @FXML private Label categoryLabel;
    @FXML private Label descriptionLabel;
    @FXML private Label priceLabel;
    @FXML private Label durationLabel;
    @FXML private Label statusLabel;
    @FXML private Label dateDebutLabel;
    @FXML private Label dateFinLabel;

    @FXML private Label destinationLabel;
    @FXML private Label hotelLabel;
    @FXML private Label excursionLabel;

    @FXML private ImageView packImageView;
    @FXML private Button btnBack;
    @FXML private Button btnReserver;

    // --- ÉLÉMENTS MÉTÉO ---
    @FXML private ImageView weatherIcon;
    @FXML private Label tempLabel;
    @FXML private Label descLabel;

    // --- ÉLÉMENTS DEVISE ---
    @FXML private Label convertedPriceLabel;
    @FXML private ComboBox<String> currencyCombo;

    private final Connection connection = MyDBConnexion.getInstance().getConnection();
    private final DestinationService destinationService = new DestinationService(connection);
    private final HotelService hotelService = new HotelService(connection);
    private final ExcursionService excursionService = new ExcursionService(connection);

    private double currentPackPrice; // Stockage du prix pour conversion

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Configuration du ComboBox de devises
        if (currencyCombo != null) {
            currencyCombo.getItems().addAll("EUR", "USD", "GBP");
            currencyCombo.getSelectionModel().select("EUR"); // Sélection par défaut

            currencyCombo.setOnAction(event -> {
                String selected = currencyCombo.getValue();
                if (selected != null) {
                    updateConvertedPrice(selected);
                }
            });
        }
    }

    public void setPackData(Pack pack) {
        // 1. Informations de base
        nameLabel.setText(pack.getName());
        categoryLabel.setText(pack.getCategorie());
        descriptionLabel.setText(pack.getDescription());

        // Stockage et affichage du prix
        this.currentPackPrice = pack.getPrix();
        priceLabel.setText(pack.getPrix() + " DT");

        durationLabel.setText(pack.getDuree() + " Jours");

        // Gestion du statut
        String status = (pack.getStatus() != null) ? pack.getStatus().trim() : "";
        statusLabel.setText(status.toUpperCase());
        statusLabel.getStyleClass().removeAll("status-available", "status-unavailable");

        if ("Disponible".equalsIgnoreCase(status)) {
            statusLabel.getStyleClass().add("status-available");
            btnReserver.setDisable(false);
            btnReserver.setText("Confirmer la Réservation");
            btnReserver.setOpacity(1.0);
        } else {
            statusLabel.getStyleClass().add("status-unavailable");
            btnReserver.setDisable(true);
            btnReserver.setText("Indisponible");
            btnReserver.setOpacity(0.5);
        }

        // Dates
        dateDebutLabel.setText(pack.getDateDepart() != null ? pack.getDateDepart().toString() : "Non définie");
        dateFinLabel.setText(pack.getDateArriver() != null ? pack.getDateArriver().toString() : "Non définie");

        // 2. Image
        try {
            String imagePath = "/images/" + pack.getImageName();
            if (getClass().getResource(imagePath) != null) {
                packImageView.setImage(new Image(getClass().getResource(imagePath).toExternalForm()));
            }
        } catch (Exception e) {
            System.err.println("Erreur image : " + e.getMessage());
        }

        // 3. MÉTÉO DYNAMIQUE (Utilise le nom récupéré par la jointure PackService)
        if (pack.getDestinationName() != null && !pack.getDestinationName().isEmpty()) {
            afficherMeteo(pack.getDestinationName()); //
        }

        // 4. CONVERSION AUTOMATIQUE AU CHARGEMENT
        if (currencyCombo != null && currencyCombo.getValue() != null) {
            updateConvertedPrice(currencyCombo.getValue());
        }

        // 5. Détails liés (Hôtel/Excursion)
        try {
            destinationLabel.setText(pack.getDestinationName() != null ? pack.getDestinationName() : "Non spécifiée");

            if (pack.getHotelId() > 0) {
                var hotel = hotelService.getById(pack.getHotelId());
                hotelLabel.setText(hotel != null ? hotel.getName() : "Aucun hôtel");
            }
            if (pack.getExcursionId() > 0) {
                var excur = excursionService.getById(pack.getExcursionId());
                excursionLabel.setText(excur != null ? excur.getName() : "Aucune excursion");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateConvertedPrice(String currency) {
        if (currentPackPrice <= 0) return;

        new Thread(() -> {
            try {
                double rate = CurrencyService.getExchangeRate(currency);
                double converted = currentPackPrice * rate;

                Platform.runLater(() -> {
                    if (rate > 0) {
                        convertedPriceLabel.setText(String.format("≈ %.2f %s", converted, currency));
                    } else {
                        convertedPriceLabel.setText("Erreur");
                    }
                });
            } catch (Exception e) {
                System.err.println("Erreur conversion : " + e.getMessage());
            }
        }).start();
    }

    private void afficherMeteo(String ville) {
        new Thread(() -> {
            // Correction de la syntaxe d'appel
            tn.esprit.projet.services.WeatherService.WeatherData data = WeatherService.getWeather(ville);

            Platform.runLater(() -> {
                if (data != null) {
                    // Ces méthodes doivent exister dans ta classe WeatherData
                    tempLabel.setText(String.format("%.1f°C", data.getTemp()));
                    descLabel.setText(data.getDescription());

                    // Chargement de l'icône via l'URL fournie par l'API
                    weatherIcon.setImage(new Image(data.getIconUrl()));
                } else {
                    descLabel.setText("Météo indisponible");
                }
            });
        }).start();
    }

    @FXML
    private void handleBack() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/adminView.fxml"));
            nameLabel.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}