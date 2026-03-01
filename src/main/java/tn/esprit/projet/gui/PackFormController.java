package tn.esprit.projet.gui;

import java.io.File;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import tn.esprit.projet.entities.*;
import tn.esprit.projet.services.*;
import tn.esprit.projet.utils.MyDBConnexion;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class PackFormController implements Initializable {

    @FXML private TextField nameField, priceField, durationField, imageField;
    @FXML private TextArea descriptionArea;
    @FXML private DatePicker dateDepPicker, dateArrPicker;
    @FXML private ComboBox<Destination> destinationCombo;
    @FXML private ComboBox<Hotel> hotelCombo;
    @FXML private ComboBox<Excursion> excursionCombo;
    @FXML private ComboBox<String> categoryCombo, statusCombo;
    @FXML private TextField imagePathField;
    private File selectedImageFile;

    private DestinationService destinationService;
    private HotelService hotelService;
    private ExcursionService excursionService;
    private PackService packService;
    private Pack packAModifier;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        var conn = MyDBConnexion.getInstance().getConnection();
        destinationService = new DestinationService(conn);
        hotelService = new HotelService(conn);
        excursionService = new ExcursionService(conn);
        packService = new PackService(conn);

        // 1. Configuration de l'affichage (Noms uniquement dans les listes)
        setupComboBoxDisplay();

        // 2. Remplir les catégories et statuts
        categoryCombo.setItems(FXCollections.observableArrayList("individuel", "couple", "familial"));
        statusCombo.setItems(FXCollections.observableArrayList("Disponible", "Complet", "Bientôt","non disponible"));

        // 3. Charger les destinations initiales
        try {
            destinationCombo.setItems(FXCollections.observableArrayList(destinationService.getAll()));
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // 4. Filtrage dynamique Hôtels et Excursions selon la Destination
        destinationCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                try {
                    int destId = newVal.getId();

                    // Filtrer les hôtels DISPONIBLES
                    List<Hotel> filteredHotels = hotelService.getByLocation(destId);
                    hotelCombo.setItems(FXCollections.observableArrayList(filteredHotels));

                    // Filtrer les excursions DISPONIBLES
                    List<Excursion> filteredExcursions = excursionService.getByLocation(destId);
                    excursionCombo.setItems(FXCollections.observableArrayList(filteredExcursions));

                    hotelCombo.setPromptText("Hôtels à " + newVal.getNameDestination());
                    excursionCombo.setPromptText("Excursions à " + newVal.getNameDestination());

                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @FXML
    private void handleSave() {
        try {
            // 1. Validation des champs textuels (Nom et Destination)
            if (nameField.getText().trim().isEmpty()) {
                showAlert("Erreur de saisie", "Le nom du pack ne peut pas être vide.");
                return;
            }

            if (destinationCombo.getValue() == null) {
                showAlert("Erreur de saisie", "Veuillez sélectionner une destination.");
                return;
            }

            // 2. Validation du Prix (Doit être un nombre positif)
            double prix = 0;
            try {
                prix = Double.parseDouble(priceField.getText());
                if (prix <= 0) {
                    showAlert("Erreur de saisie", "Le prix doit être supérieur à 0.");
                    return;
                }
            } catch (NumberFormatException e) {
                showAlert("Erreur de saisie", "Le prix doit être un nombre valide (ex: 500.0).");
                return;
            }

            // 3. Validation de la Durée (Doit être un entier positif)
            int duree = 0;
            try {
                duree = Integer.parseInt(durationField.getText());
                if (duree <= 0) {
                    showAlert("Erreur de saisie", "La durée doit être d'au moins 1 jour.");
                    return;
                }
            } catch (NumberFormatException e) {
                showAlert("Erreur de saisie", "La durée doit être un nombre entier.");
                return;
            }

            // 4. Validation des Dates (Départ et Arrivée)
            var debut = dateDepPicker.getValue();
            var fin = dateArrPicker.getValue();

            if (debut == null || fin == null) {
                showAlert("Erreur de saisie", "Les dates de départ et d'arrivée sont obligatoires.");
                return;
            }

            if (!debut.isBefore(fin)) {
                showAlert("Incohérence des dates", "La date de départ doit être strictement avant la date d'arrivée.");
                return;
            }

            if (debut.isBefore(java.time.LocalDate.now()) && packAModifier == null) {
                showAlert("Date invalide", "Pour un nouveau pack, la date ne peut pas être dans le passé.");
                return;
            }

            // 5. Si toutes les validations passent, on prépare l'objet
            Pack p = (packAModifier != null) ? packAModifier : new Pack();
            if (selectedImageFile != null) {
                // Définir le dossier de destination
                File destDir = new File("src/main/resources/images/");
                if (!destDir.exists()) destDir.mkdirs();

                // Créer le fichier de destination
                File destFile = new File(destDir, selectedImageFile.getName());

                // Copier le fichier physiquement (Nécessite import java.nio.file.Files et StandardCopyOption)
                java.nio.file.Files.copy(
                        selectedImageFile.toPath(),
                        destFile.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING
                );

                p.setImageName(selectedImageFile.getName());
            } else if (packAModifier == null) {
                p.setImageName("default.jpg");
            }


            p.setName(nameField.getText().trim());
            p.setDescription(descriptionArea.getText());
            p.setCategorie(categoryCombo.getValue());
            p.setPrix(prix);
            p.setDuree(duree);
            p.setStatus(statusCombo.getValue());
            p.setDateDepart(debut);
            p.setDateArriver(fin);
            p.setImageName(imageField.getText().isEmpty() ? "default.jpg" : imageField.getText());

            p.setDestinationId(destinationCombo.getValue().getId());
            p.setHotelId(hotelCombo.getValue() != null ? (int) hotelCombo.getValue().getId() : -1);
            p.setExcursionId(excursionCombo.getValue() != null ? excursionCombo.getValue().getId() : -1);

            // 6. Enregistrement
            if (packAModifier != null) {
                packService.update(p);
                System.out.println("✅ Pack mis à jour !");
            } else {
                packService.create(p);
                System.out.println("✅ Pack créé !");
            }

            returnToTable();

        } catch (Exception e) {
            showAlert("Erreur", "Erreur lors de l'enregistrement : " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCancel() {
        returnToTable();
    }

    private void setupComboBoxDisplay() {
        // Affichage Hôtel
        hotelCombo.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Hotel item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
            }
        });
        hotelCombo.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Hotel item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
            }
        });

        // Affichage Excursion
        excursionCombo.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Excursion item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
            }
        });
        excursionCombo.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Excursion item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
            }
        });
    }

    private void returnToTable() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/adminView.fxml"));
            nameField.getScene().setRoot(root);
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public void setUpdateMode(Pack p) {
        this.packAModifier = p;

        // Remplir les champs textuels
        nameField.setText(p.getName());
        descriptionArea.setText(p.getDescription());
        priceField.setText(String.valueOf(p.getPrix()));
        durationField.setText(String.valueOf(p.getDuree()));
        imageField.setText(p.getImageName());
        dateDepPicker.setValue(p.getDateDepart());
        dateArrPicker.setValue(p.getDateArriver());

        // Sélectionner les bonnes valeurs dans les ComboBox
        categoryCombo.setValue(p.getCategorie());
        statusCombo.setValue(p.getStatus());

        // Pour les destinations/hôtels, il faut retrouver l'objet dans la liste
        // (Ceci suppose que les combos sont déjà chargés dans initialize)
        destinationCombo.getItems().stream()
                .filter(d -> d.getId() == p.getDestinationId())
                .findFirst()
                .ifPresent(d -> destinationCombo.setValue(d));

        // Note: Le filtrage dynamique chargera les hôtels/excursions automatiquement
        // grâce au listener que nous avons déjà mis sur destinationCombo
    }

    @FXML
    private void handleUploadImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir l'image du pack");

        // Filtrer pour ne voir que les images
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg")
        );

        // Ouvrir la fenêtre de dialogue
        File file = fileChooser.showOpenDialog(nameField.getScene().getWindow());

        if (file != null) {
            this.selectedImageFile = file;
            // On affiche le nom du fichier dans le champ pour que l'admin voie son choix
            imageField.setText(file.getName());
        }
    }

}