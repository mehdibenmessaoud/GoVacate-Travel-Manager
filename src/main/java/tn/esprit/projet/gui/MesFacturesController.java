package tn.esprit.projet.gui;

import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.property.TextAlignment;
import com.itextpdf.layout.property.UnitValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import tn.esprit.projet.entities.Facture;
import tn.esprit.projet.services.FactureServiceImpl;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MesFacturesController {

    @FXML private TableView<Facture> tableFactures;
    @FXML private TableColumn<Facture, Long> colId;
    @FXML private TableColumn<Facture, Long> colResId;
    @FXML private TableColumn<Facture, Double> colMontant;
    @FXML private TableColumn<Facture, LocalDateTime> colDate;
    @FXML private TableColumn<Facture, Void> colAction;
    @FXML private TableColumn<Facture, String> colMethode;
    private final FactureServiceImpl factureService = new FactureServiceImpl();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colResId.setCellValueFactory(new PropertyValueFactory<>("reservation_id"));
        colMontant.setCellValueFactory(new PropertyValueFactory<>("montant"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("date_facture"));

        // CORRECTION 2 : Liaison avec l'attribut exact de Facture.java
        // Il cherche le getter getMethode_paiement() que tu as bien défini.
        if (colMethode != null) {
            colMethode.setCellValueFactory(new PropertyValueFactory<>("methode_paiement"));
        }

        setupActionColumn();
        chargerDonnees();
    }

    private void chargerDonnees() {
        // Charge les données depuis govacate_db
        ObservableList<Facture> data = FXCollections.observableArrayList(factureService.getAll());
        tableFactures.setItems(data);
    }

    private void setupActionColumn() {
        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button btnPdf = new Button("Exporter PDF");
            {
                // Style orange correspondant à ton interface GoVacate
                btnPdf.setStyle("-fx-background-color: #FF8210; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5; -fx-cursor: hand;");
                btnPdf.setOnAction(e -> {
                    Facture f = getTableView().getItems().get(getIndex());
                    genererFacturePDF(f);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnPdf);
            }
        });
    }

    private void genererFacturePDF(Facture f) {
        try {
            String dest = System.getProperty("user.home") + File.separator + "Desktop" + File.separator + "Facture_" + f.getId() + ".pdf";
            PdfWriter writer = new PdfWriter(dest);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            // Définition des couleurs (Orange GoVacate et Gris pro)
            DeviceRgb orangeTheme = new DeviceRgb(255, 130, 16);
            DeviceRgb darkGrey = new DeviceRgb(64, 64, 64);

            // --- EN-TÊTE ---
            document.add(new Paragraph("GOVACATE TRAVEL")
                    .setFontColor(orangeTheme)
                    .setBold().setFontSize(26)
                    .setTextAlignment(TextAlignment.RIGHT));

            document.add(new Paragraph("Facture Officielle de Réservation")
                    .setFontColor(darkGrey).setFontSize(10).setTextAlignment(TextAlignment.RIGHT));

            document.add(new Paragraph("\n"));

            // --- INFOS CLIENT & FACTURE ---
            float[] columnWidths = {1, 1};
            Table infoTable = new Table(UnitValue.createPercentArray(columnWidths)).useAllAvailableWidth();

            infoTable.addCell(new Cell().add(new Paragraph("Détails de la facture :\nN° #" + f.getId() + "\nDate: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))))
                    .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER));

            infoTable.addCell(new Cell().add(new Paragraph("ID Réservation : " + f.getReservation_id() + "\nStatut : " + f.getStatut())
                            .setTextAlignment(TextAlignment.RIGHT))
                    .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER));

            document.add(infoTable);
            document.add(new Paragraph("\n"));

            // --- TABLEAU DES PRIX ---
            Table priceTable = new Table(UnitValue.createPercentArray(new float[]{3, 1})).useAllAvailableWidth();

            // Header
            priceTable.addHeaderCell(new Cell().add(new Paragraph("Description")).setBackgroundColor(orangeTheme).setFontColor(DeviceRgb.WHITE));
            priceTable.addHeaderCell(new Cell().add(new Paragraph("Montant")).setBackgroundColor(orangeTheme).setFontColor(DeviceRgb.WHITE));

            // Ligne de données
            priceTable.addCell(new Cell().add(new Paragraph("Réservation de voyage  ")));
            priceTable.addCell(new Cell().add(new Paragraph(f.getMontant() + " DT")).setTextAlignment(TextAlignment.RIGHT));

            document.add(priceTable);

            // --- RÉSUMÉ FINAL ---
            document.add(new Paragraph("\n"));
            document.add(new Paragraph("Méthode de paiement : " + f.getMethode_paiement()).setFontSize(10).setItalic());

            document.add(new Paragraph("TOTAL PAYÉ : " + f.getMontant() + " DT")
                    .setBold().setFontSize(18)
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setFontColor(orangeTheme));

            // --- PIED DE PAGE ---
            document.add(new Paragraph("\n\n\nMerci d'avoir choisi GoVacate pour vos aventures !")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(9)
                    .setFontColor(darkGrey));

            document.close();
            showAlert("Export réussi", "La facture stylisée a été générée sur votre bureau : " + dest);

        } catch (Exception e) {
            showAlert("Erreur PDF", "Impossible de générer le fichier : " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    void handleBack(javafx.event.ActionEvent event) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/MesReservations.fxml"));
            javafx.scene.Parent root = loader.load();
            tableFactures.getScene().setRoot(root);
        } catch (java.io.IOException e) {
            showAlert("Erreur", "Impossible de retourner aux réservations.");
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}