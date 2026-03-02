package tn.esprit.projet.gui;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import tn.esprit.projet.entities.User;
import tn.esprit.projet.entities.Role;
import tn.esprit.projet.services.UserService;

import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class UserFormController implements Initializable {

    @FXML private TextField nomField, emailField, telField;
    @FXML private ComboBox<String> roleCombo, statusCombo;
    @FXML private Label titleLabel;

    private User user;
    private boolean saveClicked = false;
    private final UserService userService = new UserService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        roleCombo.setItems(FXCollections.observableArrayList("ADMIN", "CLIENT"));
        statusCombo.setItems(FXCollections.observableArrayList("actif", "inactif"));
    }

    public void setUser(User user) {
        this.user = user;
        if (user != null) {
            titleLabel.setText("Modifier l'utilisateur");
            nomField.setText(user.getNom());
            emailField.setText(user.getEmail());
            telField.setText(user.getTelephone());
            roleCombo.setValue(user.getRoleName());
            statusCombo.setValue(user.getStatus());
        } else {
            titleLabel.setText("Ajouter un utilisateur");
        }
    }

    public boolean isSaveClicked() {
        return saveClicked;
    }

    @FXML
    private void handleSave() {
        if (isInputValid()) {
            user.setNom(nomField.getText());
            user.setEmail(emailField.getText());
            user.setTelephone(telField.getText());
            user.setStatus(statusCombo.getValue());

            // Gestion du rôle (1 pour Admin, 2 pour Client selon ton UserService)
            int roleId = roleCombo.getValue().equals("ADMIN") ? 1 : 2;
            user.setRole(new Role(roleId, roleCombo.getValue()));

            try {
                userService.update(user); // Appelle ta méthode update
                saveClicked = true;
                ((Stage) nomField.getScene().getWindow()).close();
            } catch (SQLException e) {
                showAlert("Erreur", "Impossible de mettre à jour l'utilisateur : " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleCancel() {
        ((Stage) nomField.getScene().getWindow()).close();
    }

    private boolean isInputValid() {
        if (nomField.getText().isEmpty() || emailField.getText().isEmpty()) {
            showAlert("Champs vides", "Le nom et l'email sont obligatoires.");
            return false;
        }
        return true;
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}