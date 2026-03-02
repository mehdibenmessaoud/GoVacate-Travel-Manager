package tn.esprit.projet.gui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import tn.esprit.projet.entities.Destination;
import tn.esprit.projet.services.DestinationService;
import tn.esprit.projet.utils.MyDBConnexion;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class DestinationFormController implements Initializable {

    @FXML private TextField nameField;
    @FXML private TextField paysField;
    @FXML private TextField villeField;
    @FXML private TextField imageField;
    @FXML private Button btnEnregistrer;

    private DestinationService destinationService;
    private Destination destinationAModifier; // Stocke la destination en cas de modification

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        destinationService = new DestinationService(MyDBConnexion.getInstance().getConnection());
    }

    /**
     * Cette méthode est appelée par AdminDestinationController pour passer en mode "Modification"
     */
    public void setUpdateMode(Destination d) {
        this.destinationAModifier = d;

        // Pré-remplir les champs avec les données existantes
        nameField.setText(d.getNameDestination());
        paysField.setText(d.getPays());
        villeField.setText(d.getVille());
        imageField.setText(d.getImage());

        btnEnregistrer.setText("Mettre à jour"); // Changer le texte du bouton
    }

    @FXML
    private void handleSave() {
        // 1. Récupération et nettoyage des données (Trim)
        String nom = nameField.getText().trim();
        String pays = paysField.getText().trim();
        String ville = villeField.getText().trim();
        String image = imageField.getText().trim();

        // 2. Validation : Champs vides
        if (nom.isEmpty() || pays.isEmpty() || ville.isEmpty()) {
            showAlert("Champs obligatoires", "Veuillez remplir le nom, le pays et la ville.");
            return;
        }

        // 3. Validation : Format du texte (Pas de chiffres dans Pays/Ville)
        // Cette regex vérifie que le texte contient principalement des lettres, espaces ou tirets
        String nameRegex = "^[a-zA-Z\\s\\-À-ÿ]+$";

        if (!pays.matches(nameRegex)) {
            showAlert("Format invalide", "Le nom du pays ne doit contenir que des lettres.");
            return;
        }

        if (!ville.matches(nameRegex)) {
            showAlert("Format invalide", "Le nom de la ville ne doit contenir que des lettres.");
            return;
        }

        // 4. Validation : Longueur minimale
        if (nom.length() < 3) {
            showAlert("Saisie trop courte", "Le nom de la destination doit comporter au moins 3 caractères.");
            return;
        }

        try {
            // 5. Préparation de l'objet (nouveau ou existant)
            Destination d = (destinationAModifier != null) ? destinationAModifier : new Destination();

            d.setNameDestination(nom);
            d.setPays(pays);
            d.setVille(ville);
            d.setImage(image.isEmpty() ? "default_dest.jpg" : image);

            // 6. Exécution de l'action via le service
            if (destinationAModifier != null) {
                destinationService.update(d);
                System.out.println("✅ Destination modifiée !");
            } else {
                destinationService.create(d);
                System.out.println("✅ Destination créée !");
            }

            // 7. Retour au tableau des destinations
            returnToTable();

        } catch (SQLException e) {
            showAlert("Erreur SQL", "Impossible d'enregistrer la destination : " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCancel() {
        returnToTable();
    }

    private void returnToTable() {
        try {
            // On recharge le tableau des destinations au centre du BorderPane
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/DestinationTable.fxml"));
            Parent table = loader.load();

            // On cherche le BorderPane via l'ID que vous avez mis dans adminView.fxml
            BorderPane mainLayout = (BorderPane) nameField.getScene().lookup("#mainLayout");
            if (mainLayout != null) {
                mainLayout.setCenter(table);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}