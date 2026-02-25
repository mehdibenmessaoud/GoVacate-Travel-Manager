package tn.esprit.projet.gui;

import javafx.application.Platform;
import javafx.animation.FadeTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.scene.Node;

import tn.esprit.projet.entities.*;
import tn.esprit.projet.services.*;

import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;

public class MesReservationsController {

    private List<Node> originalUIElements = new ArrayList<>();

    @FXML private TableView<Reservation> tableMesReservations;
    @FXML private TableColumn<Reservation, String> colType;
    @FXML private TableColumn<Reservation, LocalDate> colDate;
    @FXML private TableColumn<Reservation, Double> colPrix;
    @FXML private TableColumn<Reservation, Object> colStatut;
    @FXML private TableColumn<Reservation, Void> colAction;
    @FXML private TableColumn<Reservation, Void> colTicket;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilterCombo;

    private final ReservationServiceImpl service = new ReservationServiceImpl();
    private final FactureServiceImpl factureService = new FactureServiceImpl();

    private final ObservableList<Reservation> masterData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        if (tableMesReservations == null) return;
        setupColumns();
        if (statusFilterCombo != null) {
            statusFilterCombo.getItems().setAll("Tous les statuts", "CONFIRMEE", "EN_ATTENTE", "ANNULEE");
            statusFilterCombo.setValue("Tous les statuts");
        }
        chargerDonnees();
        setupFilterLogic();

        FadeTransition ft = new FadeTransition(Duration.millis(800), tableMesReservations);
        ft.setFromValue(0); ft.setToValue(1); ft.play();
    }

    private void setupColumns() {
        colType.setCellValueFactory(new PropertyValueFactory<>("type_res"));
        colPrix.setCellValueFactory(new PropertyValueFactory<>("prix_total"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("date_debut"));

        colStatut.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    String status = item.toString().toUpperCase();
                    Label badge = new Label(status);
                    badge.setMinWidth(110);
                    badge.setAlignment(Pos.CENTER);
                    String baseStyle = "-fx-padding: 6 12; -fx-background-radius: 12; -fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: white;";
                    String color = status.contains("CONFIRME") ? "#27ae60" : (status.contains("ATTENTE") ? "#e67e22" : "#7f8c8d");
                    badge.setStyle(baseStyle + "-fx-background-color: " + color + ";");
                    setGraphic(badge);
                }
            }
        });

        colDate.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (empty || date == null) {
                    setText(null);
                } else {
                    setText(date.toString());
                    setStyle("-fx-text-fill: #00FFCC; -fx-font-weight: bold;");
                }
            }
        });

        setupActionColumn();
        setupTicketColumn();
    }

    @FXML
    void handlePayerSelection() {
        Reservation selectedRes = tableMesReservations.getSelectionModel().getSelectedItem();
        if (selectedRes == null) {
            showAlert("Attention", "Veuillez sélectionner une réservation.");
            return;
        }

        // BLOCK: Stop if already confirmed
        if (StatutReservation.CONFIRMEE.equals(selectedRes.getStatut())) {
            showAlert("Info", "Cette réservation est déjà payée.");
            return;
        }

        try {
            Stripe.apiKey = "sk_test_51T2VgCGPX5GP9df5VYefnZIoxll2P0o64MBEcOPBsuP6zkrpqeGW54VhELQ0sKnHaVFdJFfZqK4qeDBLwIcToT61000bqv7bk5";

            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl("https://www.google.com/success")
                    .setCancelUrl("https://www.google.com/cancel")
                    .addLineItem(SessionCreateParams.LineItem.builder()
                            .setQuantity(1L)
                            .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                    .setCurrency("eur")
                                    .setUnitAmount((long)(selectedRes.getPrix_total() * 100))
                                    .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                            .setName("Réservation #" + selectedRes.getId()).build())
                                    .build())
                            .build())
                    .build();

            Session session = Session.create(params);
            switchToPaymentView(session.getUrl(), session.getId(), selectedRes);

        } catch (Exception e) {
            showAlert("Erreur", "Lancement Stripe échoué.");
        }
    }

    private void switchToPaymentView(String url, String sessionId, Reservation res) {
        VBox contentArea = (VBox) tableMesReservations.getScene().lookup("#clientReservationView");
        if (contentArea == null) return;

        originalUIElements.clear();
        originalUIElements.addAll(contentArea.getChildren());

        WebView webView = new WebView();
        WebEngine engine = webView.getEngine();
        webView.setPrefHeight(750);

        Button btnCancel = new Button("← Annuler et revenir");
        btnCancel.setStyle("-fx-background-color: #1a1a1a; -fx-text-fill: white; -fx-padding: 10 20; -fx-cursor: hand; -fx-font-weight: bold;");
        btnCancel.setOnAction(e -> restoreOriginalView(contentArea));

        engine.locationProperty().addListener((obs, oldUrl, newUrl) -> {
            if (newUrl.contains("/success")) {
                Platform.runLater(() -> {
                    restoreOriginalView(contentArea);
                    verifierStatusStripe(sessionId, res);
                });
            } else if (newUrl.contains("/cancel")) {
                Platform.runLater(() -> restoreOriginalView(contentArea));
            }
        });

        contentArea.getChildren().setAll(btnCancel, webView);
        engine.load(url);
    }

    private void verifierStatusStripe(String sessionId, Reservation res) {
        new Thread(() -> {
            try {
                Session s = Session.retrieve(sessionId);
                if ("paid".equals(s.getPaymentStatus())) {
                    Platform.runLater(() -> {
                        marquerCommePayee(res);
                        Facture f = new Facture();
                        f.setMontant(res.getPrix_total());
                        f.setReservation_id((long) res.getId());
                        f.setStatut(StatutFacture.PAYEE);
                        f.setMethode_paiement(MethodePaiement.VISA);
                        factureService.save(f);
                        showAlert("Succès", "Paiement validé !");
                    });
                }
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    private void setupActionColumn() {
        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button btnModifier = new Button("Modifier");
            private final Button btnAnnuler = new Button("Supprimer");
            private final Label lblLocked = new Label("Payé ✓");
            private final HBox pane = new HBox(12);
            {
                String btnStyle = "-fx-text-fill: white; -fx-cursor: hand; -fx-font-weight: bold; -fx-background-radius: 8; -fx-min-width: 85;";
                btnModifier.setStyle("-fx-background-color: #FF8210; " + btnStyle);
                btnAnnuler.setStyle("-fx-background-color: #FF4B5C; " + btnStyle);
                lblLocked.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
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

                    // LOCK LOGIC: If confirmed, hide buttons
                    if (StatutReservation.CONFIRMEE.equals(res.getStatut())) {
                        pane.getChildren().add(lblLocked);
                    } else {
                        if (!"PACK".equalsIgnoreCase(res.getType_res())) pane.getChildren().add(btnModifier);
                        pane.getChildren().add(btnAnnuler);
                    }
                    setGraphic(pane);
                }
            }
        });
    }

    private void showQRCodePopup(Reservation res) {
        Stage popupStage = new Stage();
        popupStage.initModality(Modality.APPLICATION_MODAL);
        popupStage.setTitle("E-Ticket #" + res.getId());

        String id = String.valueOf(res.getId());
        String type = res.getType_res().toUpperCase();
        String date = res.getDate_debut().toString();
        String prix = res.getPrix_total() + "€";
        String statut = res.getStatut().toString();

        try {
            String ticketContent = "https://image-charts.com/chart?chco=333333,00FFCC&chd=t:1,1,1,1,1&chf=bg,s,FFFFFF&chs=400x300&cht=gv&chl="
                    + URLEncoder.encode("digraph { node [shape=record, fontname=Arial, fontsize=14]; "
                    + "ticket [label=\"{GOVACATE TICKET|ID: #" + id + "|TYPE: " + type + "|DATE: " + date + "|PRIX: " + prix + "|STATUT: " + statut + "}\"];"
                    + "}", StandardCharsets.UTF_8);

            String qrApiURL = "https://quickchart.io/qr?text=" + URLEncoder.encode(ticketContent, StandardCharsets.UTF_8) +
                    "&size=250&margin=2";

            ImageView qrImageView = new ImageView(new Image(qrApiURL, true));
            qrImageView.setFitWidth(250);
            qrImageView.setFitHeight(250);

            VBox root = new VBox(15);
            root.setAlignment(Pos.CENTER);
            root.setStyle("-fx-background-color: white; -fx-padding: 30; -fx-border-color: #00FFCC; -fx-border-width: 3; -fx-background-radius: 15; -fx-border-radius: 15;");

            Label title = new Label("VOTRE BILLET ÉLECTRONIQUE");
            title.setStyle("-fx-text-fill: #1a1a1a; -fx-font-size: 16px; -fx-font-weight: bold;");

            Button btnFermer = new Button("Fermer");
            btnFermer.setOnAction(e -> popupStage.close());
            btnFermer.setStyle("-fx-background-color: #1a1a1a; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 8 20; -fx-background-radius: 5;");

            root.getChildren().addAll(title, qrImageView, btnFermer);
            popupStage.setScene(new Scene(root));
            popupStage.show();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void chargerDonnees() {
        new Thread(() -> {
            List<Reservation> list = service.getAllReservations();
            Platform.runLater(() -> { if (list != null) masterData.setAll(list); });
        }).start();
    }

    private void setupTicketColumn() {
        if (colTicket == null) return;
        colTicket.setCellFactory(param -> new TableCell<>() {
            private final Button btnQR = new Button("Voir e-Ticket");
            {
                btnQR.setStyle("-fx-background-color: #20B2AA; -fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 8; -fx-min-width: 90;");
                btnQR.setOnAction(e -> showQRCodePopup(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setGraphic(null);
                else {
                    Reservation res = getTableView().getItems().get(getIndex());
                    setGraphic(StatutReservation.CONFIRMEE.equals(res.getStatut()) ? btnQR : null);
                    setAlignment(Pos.CENTER);
                }
            }
        });
    }

    private void setupFilterLogic() {
        FilteredList<Reservation> filteredData = new FilteredList<>(masterData, p -> true);
        searchField.textProperty().addListener((o, old, nv) -> applyFilters(filteredData));
        statusFilterCombo.valueProperty().addListener((o, old, nv) -> applyFilters(filteredData));
        tableMesReservations.setItems(filteredData);
    }

    private void applyFilters(FilteredList<Reservation> filteredData) {
        filteredData.setPredicate(res -> {
            String search = searchField.getText().toLowerCase();
            String status = statusFilterCombo.getValue();
            boolean matchesType = res.getType_res().toLowerCase().contains(search);
            boolean matchesStatus = (status.equals("Tous les statuts") || res.getStatut().toString().equals(status));
            return matchesType && matchesStatus;
        });
    }

    private void handleModifierAction(Reservation res) {
        try {
            String fxml = res.getType_res().equalsIgnoreCase("RESTAURANT") ? "/RestaurantBookingView.fxml" : "/ExcursionBooking.fxml";
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Parent root = loader.load();
            VBox contentArea = (VBox) tableMesReservations.getScene().lookup("#clientReservationView");
            if (contentArea != null) contentArea.getChildren().setAll(root);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void confirmerAnnulation(Reservation res) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer ?", ButtonType.YES, ButtonType.NO);
        a.showAndWait().ifPresent(r -> {
            if (r == ButtonType.YES) { service.delete(res.getId()); chargerDonnees(); }
        });
    }

    private void showAlert(String t, String c) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(t); a.setContentText(c); a.showAndWait();
    }

    private void marquerCommePayee(Reservation res) {
        service.updateStatus(res.getId(), "CONFIRMEE");
        res.setStatut(StatutReservation.CONFIRMEE);
        tableMesReservations.refresh();
    }

    private void restoreOriginalView(VBox contentArea) {
        if (!originalUIElements.isEmpty()) {
            contentArea.getChildren().setAll(originalUIElements);
            chargerDonnees();
        }
    }
}