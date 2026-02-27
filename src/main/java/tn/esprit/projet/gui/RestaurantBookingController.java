package tn.esprit.projet.gui;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import tn.esprit.projet.entities.*;
import tn.esprit.projet.services.ReservationRestaurantServiceImpl;
import java.io.IOException;
import java.time.LocalDate;

public class RestaurantBookingController {
    @FXML private Button btnAnnuler, btnValider;
    @FXML private ComboBox<Restaurant> comboResto;
    @FXML private DatePicker dateRes;
    @FXML private TextField txtPersonnes;
    @FXML private ComboBox<String> comboHeure;

    // Persist theme state across the session
    private static boolean isDarkMode = true;

    private final ReservationRestaurantServiceImpl serviceResto = new ReservationRestaurantServiceImpl();
    private Reservation reservationModif = null;

    @FXML
    public void initialize() {
        // Fix: Apply theme immediately when the view is ready to prevent white screen
        Platform.runLater(() -> {
            if (btnValider.getScene() != null) {
                applyTheme(btnValider.getScene());
            }
        });

        comboResto.setItems(FXCollections.observableArrayList(serviceResto.findAllRestaurants()));
        setupDateConstraints();
        setupTimePicker();

        // Custom cell factory for status colors
        comboResto.setCellFactory(lv -> new ListCell<Restaurant>() {
            @Override protected void updateItem(Restaurant r, boolean empty) {
                super.updateItem(r, empty);
                if (empty || r == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(r.getName() + " (" + r.getStatus() + ")");
                    if ("CLOSED".equalsIgnoreCase(r.getStatus())) setStyle("-fx-text-fill: #FF4B5C;");
                    else if ("SUSPENDED".equalsIgnoreCase(r.getStatus())) setStyle("-fx-text-fill: #FF8210;");
                    else setStyle("-fx-text-fill: #00FFCC; -fx-font-weight: bold;");
                }
            }
        });
        comboResto.setButtonCell(comboResto.getCellFactory().call(null));
    }

    @FXML
    void toggleTheme(ActionEvent event) {
        isDarkMode = !isDarkMode;
        applyTheme(((Node) event.getSource()).getScene());
    }

    private void applyTheme(Scene scene) {
        scene.getStylesheets().clear();
        // 1. Always load your base design first
        scene.getStylesheets().add(getClass().getResource("/css/booking_style.css").toExternalForm());

        // 2. Add overrides ONLY if in Light Mode
        if (!isDarkMode) {
            scene.getStylesheets().add(getClass().getResource("/css/light-mode.css").toExternalForm());
        }
    }

    private void setupDateConstraints() {
        dateRes.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isBefore(LocalDate.now()));
            }
        });
        dateRes.setEditable(false);
    }

    private void setupTimePicker() {
        ObservableList<String> hours = FXCollections.observableArrayList();
        for (int h = 12; h <= 23; h++) {
            hours.add(String.format("%02d:00", h));
            hours.add(String.format("%02d:30", h));
        }
        comboHeure.setItems(hours);
    }

    @FXML
    void handleReserverRestaurant(ActionEvent event) {
        try {
            Restaurant selected = comboResto.getSelectionModel().getSelectedItem();
            String heure = comboHeure.getValue();

            if (selected == null || dateRes.getValue() == null || txtPersonnes.getText().isEmpty() || heure == null) {
                showAlert("Champs manquants", "Veuillez remplir tous les champs.");
                return;
            }

            Reservation resParent = (reservationModif != null) ? reservationModif : new Reservation();
            resParent.setStatut(StatutReservation.CONFIRMEE);
            resParent.setType_res("RESTAURANT");
            resParent.setDate_debut(dateRes.getValue());
            resParent.setNombre_personnes(Integer.parseInt(txtPersonnes.getText()));
            resParent.setUser_id(1L);

            ReservationRestaurant rr = new ReservationRestaurant();
            rr.setRestaurant_id((long) selected.getId());
            rr.setDate_reservation(dateRes.getValue());
            rr.setHeure_souhaitee(heure);
            rr.setNombre_personnes(resParent.getNombre_personnes());
            rr.setPrix(0.0);

            if (reservationModif == null) {
                serviceResto.createFullReservation(resParent, rr);
            } else {
                serviceResto.updateFullReservation(resParent, rr);
            }

            navigateToReservationList(event);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void navigateToReservationList(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/Mes Réservations.fxml"));
            Scene scene = ((Node) event.getSource()).getScene();
            applyTheme(scene); // Ensure theme persists

            VBox contentArea = (VBox) scene.lookup("#clientReservationView");
            if (contentArea != null) {
                contentArea.getChildren().clear();
                contentArea.getChildren().add(root);
            } else {
                Parent dashboard = FXMLLoader.load(getClass().getResource("/ClientDashboard.fxml"));
                scene.setRoot(dashboard);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML void handleCancel(ActionEvent event) { navigateToReservationList(event); }
}