package tn.esprit.projet.gui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import tn.esprit.projet.entities.Excursion;
import tn.esprit.projet.services.ExcursionService;
import tn.esprit.projet.utils.MyDBConnexion;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.collections.FXCollections;
import javafx.util.StringConverter;
import tn.esprit.projet.entities.Destination;
import tn.esprit.projet.services.DestinationService;
import java.util.List;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class ExcursionFormController implements Initializable {
    @FXML private Text formTitle;
    @FXML private TextField nameField;
    @FXML private TextArea descriptionArea;
    @FXML private TextField durationField;
    @FXML private TextField priceField;
    @FXML private TextField participantsField;
    @FXML private TextField activiteField;
    @FXML private ComboBox<String> statusCombo;
    @FXML private Button btnEnregistrer;
    @FXML private DatePicker dateDebutPicker, dateFinPicker;
    @FXML private TextField imagePathField;
    @FXML private ComboBox<Destination> destinationCombo;

    private ExcursionService excursionService;
    private Excursion excursionAModifier;
    private File selectedImageFile; // Stocke le fichier choisi avant la sauvegarde

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        excursionService = new ExcursionService(MyDBConnexion.getInstance().getConnection());
        statusCombo.getItems().addAll("Disponible", "Complet", "Annulé");
        statusCombo.setValue("Disponible");

        loadDestinations();
    }

    public void setUpdateMode(Excursion e) {
        this.excursionAModifier = e;
        formTitle.setText("Modifier l'Excursion");
        btnEnregistrer.setText("Mettre à jour");

        nameField.setText(e.getName());

        durationField.setText(String.valueOf(e.getDuration()));
        priceField.setText(String.valueOf(e.getPrice()));
        participantsField.setText(String.valueOf(e.getMaxParticipants()));
        activiteField.setText(e.getActivite());
        statusCombo.setValue(e.getStatus());
        dateDebutPicker.setValue(e.getDateDebut());
        dateFinPicker.setValue(e.getDateFin());
        descriptionArea.setText(e.getDescription());
        imagePathField.setText(e.getImages()); // Affiche le nom de l'image existante
        for (Destination d : destinationCombo.getItems()) {
            if (d.getId() == e.getLocationId()) {
                destinationCombo.setValue(d);
                break;
            }
        }
    }


    @FXML
    private void handleSave() {
        if (!validateInput()) return;

        try {
            // Traitement de l'image : Copie physique vers le dossier resources
            String fileName = imagePathField.getText();

            if (selectedImageFile != null) {
                // Définir le chemin de destination
                File destinationDir = new File("src/main/resources/imageEx/");
                if (!destinationDir.exists()) {
                    destinationDir.mkdirs(); // Créer le dossier s'il n'existe pas
                }

                File destinationFile = new File(destinationDir, selectedImageFile.getName());

                // Copier le fichier (écrase si existe déjà)
                Files.copy(selectedImageFile.toPath(), destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                fileName = selectedImageFile.getName(); // On enregistre le nom exact en BDD
            }

            Excursion e = (excursionAModifier != null) ? excursionAModifier : new Excursion();

            e.setName(nameField.getText());
            e.setDescription(descriptionArea.getText());
            e.setDuration(Integer.parseInt(durationField.getText()));
            e.setPrice(Double.parseDouble(priceField.getText()));
            e.setMaxParticipants(Integer.parseInt(participantsField.getText()));
            e.setActivite(activiteField.getText());
            e.setStatus(statusCombo.getValue());
            if (destinationCombo.getValue() != null) {
                e.setLocationId(destinationCombo.getValue().getId());
            }
            e.setDateDebut(dateDebutPicker.getValue());
            e.setDateFin(dateFinPicker.getValue());
            e.setImages(fileName); // Enregistre le nom du fichier dans l'objet

            if (excursionAModifier != null) {
                excursionService.update(e);
            } else {
                excursionService.create(e);
            }

            returnToTable();

        } catch (SQLException | NumberFormatException | IOException ex) {
            showAlert("Erreur", "Erreur lors de l'enregistrement ou de la copie d'image : " + ex.getMessage());
        }
    }

    @FXML
    private void handleUploadImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );

        File file = fileChooser.showOpenDialog(imagePathField.getScene().getWindow());

        if (file != null) {
            this.selectedImageFile = file; // On garde le fichier en mémoire
            imagePathField.setText(file.getName()); // On affiche le nom dans le champ texte
        }
    }

    @FXML
    private void handleCancel() {
        returnToTable();
    }

    private void returnToTable() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ExcursionTable.fxml"));
            Parent table = loader.load();

            // On cherche le BorderPane via l'ID défini dans votre AdminView.fxml
            BorderPane mainLayout = (BorderPane) nameField.getScene().lookup("#mainLayout");

            if (mainLayout != null) {
                mainLayout.setCenter(table);
                // Optionnel : Forcer le rafraîchissement du layout pour éviter les bugs visuels
                mainLayout.requestLayout();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean validateInput() {
        // 1. Vérification des champs textuels obligatoires
        if (nameField.getText().trim().isEmpty() ||
                priceField.getText().trim().isEmpty() ||
                activiteField.getText().trim().isEmpty()) {
            showAlert("Champs vides", "Veuillez remplir les champs obligatoires (Nom, Prix, Activité).");
            return false;
        }

        // 2. Vérification des Dates (Condition demandée)
        var debut = dateDebutPicker.getValue();
        var fin = dateFinPicker.getValue();

        if (debut == null || fin == null) {
            showAlert("Dates manquantes", "Veuillez renseigner les dates de début et de fin.");
            return false;
        }

        if (!debut.isBefore(fin)) {
            showAlert("Incohérence des dates", "La date de début doit être strictement antérieure (avant) à la date de fin.");
            return false;
        }

        // 3. Validation des formats numériques (pour éviter les NumberFormatException)
        try {
            if (Double.parseDouble(priceField.getText()) < 0) {
                showAlert("Valeur invalide", "Le prix ne peut pas être négatif.");
                return false;
            }
            if (Integer.parseInt(durationField.getText()) <= 0) {
                showAlert("Valeur invalide", "La durée doit être d'au moins 1 jour.");
                return false;
            }
            if (Integer.parseInt(participantsField.getText()) <= 0) {
                showAlert("Valeur invalide", "Le nombre de participants doit être supérieur à 0.");
                return false;
            }
        } catch (NumberFormatException e) {
            showAlert("Erreur de format", "Les champs Prix, Durée et Participants doivent contenir des nombres valides.");
            return false;
        }

        return true;
    }

    private void loadDestinations() {
        try {
            // 1. Appel au service pour récupérer la liste des destinations
            // Note : Assurez-vous d'avoir initialisé votre DestinationService dans initialize()
            DestinationService destinationService = new DestinationService(MyDBConnexion.getInstance().getConnection());
            List<Destination> destinations = destinationService.getAll();

            // 2. Peupler le ComboBox avec la liste
            destinationCombo.setItems(FXCollections.observableArrayList(destinations));

            // 3. Configurer l'affichage pour montrer "Pays, Ville"
            destinationCombo.setConverter(new StringConverter<Destination>() {
                @Override
                public String toString(Destination d) {
                    return (d == null) ? "" : d.getPays() + ", " + d.getVille();
                }

                @Override
                public Destination fromString(String string) {
                    return null; // Pas nécessaire pour un ComboBox non éditable
                }
            });

            // 4. Personnaliser l'affichage des cellules de la liste déroulante (Optionnel pour le style)
            destinationCombo.setCellFactory(lv -> new ListCell<Destination>() {
                @Override
                protected void updateItem(Destination item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(item.getPays() + ", " + item.getVille());
                    }
                }
            });

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Erreur de chargement", "Impossible de charger les destinations depuis la base de données.");
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}