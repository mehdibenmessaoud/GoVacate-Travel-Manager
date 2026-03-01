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
import tn.esprit.projet.services.*;
import tn.esprit.projet.utils.MyDBConnexion;
import tn.esprit.projet.utils.SceneManager;
import tn.esprit.projet.utils.SessionManager;
import javafx.event.ActionEvent; // 👈 Import important
import javafx.scene.Node;        // 👈 Import important
import javafx.scene.Scene;      // 👈 Import important
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
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
    @FXML private Label promoBadge;

    // --- ÉLÉMENTS MÉTÉO ---
    @FXML private ImageView weatherIcon;
    @FXML private Label tempLabel;
    @FXML private Label descLabel;
    private Pack currentPack;
    // --- ÉLÉMENTS DEVISE ---
    @FXML private Label convertedPriceLabel;
    @FXML private ComboBox<String> currencyCombo;

    private PackService packService = new PackService();
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
        // --- MODIFICATION : LOGIQUE DE PRIX DYNAMIQUE ---
        // Récupération des données depuis le service
        int currentReservations = packService.getCurrentPackReservations(pack.getId());
        double dynamicPrice = packService.calculateDynamicPackPrice(pack, currentReservations);
        this.currentPackPrice = dynamicPrice; // On stocke le prix calculé (promo ou hausse)

        // Gestion visuelle du Badge et de la couleur du prix
        if (dynamicPrice < pack.getPrix()) {
            // Cas PROMO
            priceLabel.setText(String.format("%.1f DT", dynamicPrice));
            priceLabel.setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold;"); // Vert
            if (promoBadge != null) {
                promoBadge.setText("🔥 OFFRE FLASH : -20%");
                promoBadge.setVisible(true);
            }
        } else if (currentReservations >= 20) {
            // Cas FORTE DEMANDE (Seuil de 20 comme demandé)
            priceLabel.setText(String.format("%.1f DT", dynamicPrice));
            priceLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;"); // Rouge
            if (promoBadge != null) {
                promoBadge.setText("⚡ PACK TRÈS DEMANDÉ");
                promoBadge.setVisible(true);
            }
        } else {
            // Cas NORMAL
            priceLabel.setText(pack.getPrix() + " DT");
            priceLabel.setStyle("-fx-text-fill: #FF8210;");
            if (promoBadge != null) promoBadge.setVisible(false);
        }
        // ------------------------------------------------

        // 1. Informations de base
        nameLabel.setText(pack.getName());
        categoryLabel.setText(pack.getCategorie());
        descriptionLabel.setText(pack.getDescription());

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

        // 3. MÉTÉO DYNAMIQUE
        if (pack.getDestinationName() != null && !pack.getDestinationName().isEmpty()) {
            afficherMeteo(pack.getDestinationName());
        }

        // 4. CONVERSION AUTOMATIQUE AU CHARGEMENT (Utilise maintenant currentPackPrice mis à jour)
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
        this.currentPack = pack;
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
            // 1. On vérifie qui est connecté via le SessionManager
            if (SessionManager.isAdmin()) {
                // Si c'est un Admin, on le renvoie vers la table de gestion des excursions
                // Note : Adaptez le chemin si nécessaire
                SceneManager.loadClientContent("/PackTable.fxml");
                System.out.println("Retour vers l'interface Admin.");

            } else if (SessionManager.isClient()) {
                // Si c'est un Client, on le renvoie vers son Dashboard ou sa liste simplifiée
                SceneManager.loadClientContent("/ClientPackView.fxml");
                System.out.println("Retour vers le Dashboard Client.");

            } else {
                // Sécurité au cas où
                SceneManager.switchTo("Auth.fxml");
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du retour dynamique : " + e.getMessage());
            e.printStackTrace();
        }
    }
    @FXML
    private void handleGoToBooking(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/reservationpackform.fxml"));
            Parent bookingView = loader.load();

            // Passi el data mta el pack lel controller el jdid
            PackBookingController controller = loader.getController();
            controller.setPackDataFromEntity(this.currentPack);

            // N-affichiw el form fel mainLayout (el wast mta el app)
            Scene scene = ((Node) event.getSource()).getScene();
            // Nlawjou 3al mainLayout mta el dashboard
            javafx.scene.layout.BorderPane mainLayout = (javafx.scene.layout.BorderPane) scene.lookup("#mainLayout");

            if (mainLayout != null) {
                mainLayout.setCenter(bookingView);
            } else {
                // Plan B: ken ma l9ach mainLayout, y7elha fi Stage jdid (Popup)
                Stage stage = new Stage();
                stage.setScene(new Scene(bookingView));
                stage.setTitle("Réserver Pack: " + currentPack.getName());
                stage.show();
            }
        } catch (IOException e) {
            System.err.println("❌ Erreur chargement booking form: " + e.getMessage());
            e.printStackTrace();
        }
    }
}