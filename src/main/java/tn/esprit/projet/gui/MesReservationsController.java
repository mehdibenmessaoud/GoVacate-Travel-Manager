package tn.esprit.projet.gui;

import javafx.application.Platform;
import javafx.animation.FadeTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.Duration;

// Entities & Services
import tn.esprit.projet.entities.*;
import tn.esprit.projet.services.*;

// Stripe imports
import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

public class MesReservationsController {

    @FXML private TableView<Reservation> tableMesReservations;
    @FXML private TableColumn<Reservation, String> colType;
    @FXML private TableColumn<Reservation, LocalDate> colDate;
    @FXML private TableColumn<Reservation, Double> colPrix;
    @FXML private TableColumn<Reservation, Object> colStatut;
    @FXML private TableColumn<Reservation, Void> colAction;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilterCombo;

    private final ReservationServiceImpl service = new ReservationServiceImpl();
    private final ReservationRestaurantServiceImpl serviceResto = new ReservationRestaurantServiceImpl();
    private final ReservationExcursionServiceImpl serviceExc = new ReservationExcursionServiceImpl();
    private final FactureServiceImpl factureService = new FactureServiceImpl(); // Ajout du service Facture

    private final ObservableList<Reservation> masterData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        if (tableMesReservations == null) return;

        setupColumns();

        // Initialisation des filtres
        statusFilterCombo.getItems().addAll("Tous les statuts", "CONFIRMEE", "EN_ATTENTE", "ANNULEE");
        statusFilterCombo.setValue("Tous les statuts");

        chargerDonnees();
        setupFilterLogic();

        // Animation d'entrée
        FadeTransition ft = new FadeTransition(Duration.millis(800), tableMesReservations);
        ft.setFromValue(0); ft.setToValue(1); ft.play();
    }

    private void setupColumns() {
        colType.setCellValueFactory(new PropertyValueFactory<>("type_res"));
        colPrix.setCellValueFactory(new PropertyValueFactory<>("prix_total"));

        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
        colStatut.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    String status = item.toString().toUpperCase();
                    Label badge = new Label(status);

                    // Design Professionnel : Fond semi-transparent, bordure et taille fixe
                    badge.setMinWidth(110); // Empêche le texte d'être coupé
                    badge.setAlignment(Pos.CENTER);

                    String baseStyle = "-fx-padding: 6 12; -fx-background-radius: 12; -fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: white;";

                    if (status.contains("CONFIRME") || status.contains("DISPONIBLE")) {
                        badge.setStyle(baseStyle + "-fx-background-color: #27ae60;"); // Vert émeraude
                    } else if (status.contains("ATTENTE") || status.contains("OCCUPE")) {
                        badge.setStyle(baseStyle + "-fx-background-color: #e67e22;"); // Orange pro
                    } else {
                        badge.setStyle(baseStyle + "-fx-background-color: #7f8c8d;"); // Gris moderne
                    }

                    setGraphic(badge);
                    setAlignment(Pos.CENTER);
                }
            }
        });
        colDate.setCellValueFactory(new PropertyValueFactory<>("date_debut"));
        colDate.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (empty || date == null) setText(null);
                else {
                    Reservation res = getTableView().getItems().get(getIndex());
                    String heure = "";
                    if ("RESTAURANT".equalsIgnoreCase(res.getType_res())) {
                        ReservationRestaurant rr = serviceResto.findByReservationId(res.getId());
                        heure = (rr != null && rr.getHeure_souhaitee() != null) ? rr.getHeure_souhaitee() : "20:00";
                    } else if ("EXCURSION".equalsIgnoreCase(res.getType_res())) {
                        ReservationExcursion re = serviceExc.findByReservationId(res.getId());
                        heure = (re != null && re.getHeure_souhaitee() != null) ? re.getHeure_souhaitee() : "09:00";
                    }
                    setText(date + (heure.isEmpty() ? "" : " à " + heure));
                    setStyle("-fx-text-fill: #00FFCC; -fx-font-weight: bold;");
                }
            }
        });

        setupActionColumn();
    }

    private void setupActionColumn() {
        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button btnModifier = new Button("Modifier");
            private final Button btnAnnuler = new Button("Supprimer");
            private final HBox pane = new HBox(10, btnModifier, btnAnnuler);
            {
                btnModifier.setStyle("-fx-background-color: #FF8210; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 5;");
                btnAnnuler.setStyle("-fx-background-color: #FF4B5C; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 5;");
                pane.setAlignment(Pos.CENTER);
                btnModifier.setOnAction(e -> handleModifierAction(getTableView().getItems().get(getIndex())));
                btnAnnuler.setOnAction(e -> confirmerAnnulation(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setGraphic(null);
                else {
                    Reservation res = getTableView().getItems().get(getIndex());
                    pane.getChildren().clear();
                    if ("PACK".equalsIgnoreCase(res.getType_res())) pane.getChildren().add(btnAnnuler);
                    else pane.getChildren().addAll(btnModifier, btnAnnuler);
                    setGraphic(pane);
                }
            }
        });
    }

    private void chargerDonnees() {
        try {
            List<Reservation> reservations = service.getAllReservations();
            if (reservations != null) masterData.setAll(reservations);
        } catch (Exception e) { System.err.println("Error: " + e.getMessage()); }
    }

    private void setupFilterLogic() {
        FilteredList<Reservation> filteredData = new FilteredList<>(masterData, p -> true);
        searchField.textProperty().addListener((obs, old, nv) -> applyFilters(filteredData));
        statusFilterCombo.valueProperty().addListener((obs, old, nv) -> applyFilters(filteredData));

        SortedList<Reservation> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tableMesReservations.comparatorProperty());
        tableMesReservations.setItems(sortedData);
    }

    private void applyFilters(FilteredList<Reservation> filteredData) {
        filteredData.setPredicate(res -> {
            String searchText = searchField.getText() == null ? "" : searchField.getText().toLowerCase();
            String selectedStatus = statusFilterCombo.getValue();
            boolean matchesType = res.getType_res().toLowerCase().contains(searchText);
            boolean matchesStatus = (selectedStatus == null || selectedStatus.equals("Tous les statuts") || res.getStatut().toString().equals(selectedStatus));
            return matchesType && matchesStatus;
        });
    }

    private void handleModifierAction(Reservation res) {
        try {
            String fxmlPath = res.getType_res().equalsIgnoreCase("RESTAURANT") ? "/RestaurantBookingView.fxml" : "/ExcursionBooking.fxml";
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            if (res.getType_res().equalsIgnoreCase("RESTAURANT")) {
                RestaurantBookingController c = loader.getController();
                c.initModif(res);
            } else {
                ExcursionBookingController c = loader.getController();
                c.initModif(res);
            }

            // Changement du Root sur la même Scene pour garder le design du stage
            tableMesReservations.getScene().setRoot(root);
        } catch (Exception e) {
            showAlert("Erreur", "Impossible de modifier : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void confirmerAnnulation(Reservation res) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Annuler la réservation #" + res.getId() + " ?", ButtonType.YES, ButtonType.NO);
        alert.showAndWait().ifPresent(r -> {
            if (r == ButtonType.YES) {
                service.delete(res.getId());
                chargerDonnees();
            }
        });
    }

    @FXML
    void handlePayerSelection() {
        Reservation selectedRes = tableMesReservations.getSelectionModel().getSelectedItem();

        if (selectedRes == null) {
            showAlert("Attention", "Veuillez sélectionner une réservation dans la liste.");
            return;
        }

        if ("RESTAURANT".equalsIgnoreCase(selectedRes.getType_res())) {
            showAlert("Information", "Le paiement se fait sur place pour les restaurants.");
            return;
        }

        try {
            com.stripe.Stripe.apiKey = "sk_test_51T2VgCGPX5GP9df5VYefnZIoxll2P0o64MBEcOPBsuP6zkrpqeGW54VhELQ0sKnHaVFdJFfZqK4qeDBLwIcToT61000bqv7bk5";

            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl("https://www.google.com")
                    .setCancelUrl("https://www.google.com")
                    .addLineItem(SessionCreateParams.LineItem.builder()
                            .setQuantity(1L)
                            .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                    .setCurrency("eur")
                                    .setUnitAmount((long)(selectedRes.getPrix_total() * 100))
                                    .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                            .setName("Réservation #" + selectedRes.getId())
                                            .build())
                                    .build())
                            .build())
                    .build();

            Session session = Session.create(params);

            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop.getDesktop().browse(new java.net.URI(session.getUrl()));
            }

            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.initOwner(tableMesReservations.getScene().getWindow());
            alert.setTitle("Vérification du paiement");
            alert.setHeaderText("Paiement en cours...");
            alert.setContentText("Cliquez sur VÉRIFIER après avoir payé sur Stripe.");

            ButtonType btnVerifier = new ButtonType("VÉRIFIER MON PAIEMENT");
            ButtonType btnAnnuler = new ButtonType("ANNULER", ButtonBar.ButtonData.CANCEL_CLOSE);
            alert.getButtonTypes().setAll(btnVerifier, btnAnnuler);

            alert.showAndWait().ifPresent(response -> {
                if (response == btnVerifier) {
                    verifierStatusStripe(session.getId(), selectedRes);
                }
            });

        } catch (Exception e) {
            showAlert("Erreur", "Erreur Stripe : " + e.getMessage());
        }
    }

    private void verifierStatusStripe(String sessionId, Reservation res) {
        try {
            Session verifiedSession = Session.retrieve(sessionId);

            if ("paid".equals(verifiedSession.getPaymentStatus())) {
                // 1. Marquer comme payé en BDD et UI
                marquerCommePayee(res);

                // 2. Créer et Sauvegarder la Facture
                Facture f = new Facture();
                f.setMontant(res.getPrix_total());
                f.setReservation_id((long) res.getId());
                f.setMethode_paiement(MethodePaiement.VISA);
                f.setStatut(StatutFacture.PAYEE);

                factureService.save(f);

                Platform.runLater(() -> {
                    showAlert("Succès", "Paiement validé ! Facture #" + f.getId() + " enregistrée.");
                    // exporterFacturePDF(res, f); // Appeler votre méthode PDF ici
                });
            } else {
                showAlert("Paiement non détecté", "Le statut Stripe n'est pas encore 'paid'.");
            }
        } catch (Exception e) {
            showAlert("Erreur", "Erreur de vérification : " + e.getMessage());
        }
    }

    @FXML
    void handleBack(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/RestaurantBookingView.fxml"));
            tableMesReservations.getScene().setRoot(root);
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void showAlert(String t, String c) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(t);
        a.setContentText(c);
        a.showAndWait();
    }

    private void marquerCommePayee(Reservation res) {
        service.updateStatus(res.getId(), StatutReservation.CONFIRMEE.name());
        res.setStatut(StatutReservation.CONFIRMEE);
        Platform.runLater(() -> tableMesReservations.refresh());
    }
}