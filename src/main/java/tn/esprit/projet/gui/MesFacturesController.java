package tn.esprit.projet.gui;
// --- Imports SQL (Pour l'image_f59678.png) ---
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import tn.esprit.projet.utils.MyDBConnexion;

// --- Imports JavaFX (Pour l'image_f53882.png) ---
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

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
import tn.esprit.projet.entities.MethodePaiement;
import tn.esprit.projet.entities.StatutFacture;
import tn.esprit.projet.utils.MyDBConnexion;
import java.io.File;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MesFacturesController {

    @FXML private TableView<Facture> tableFactures;
    @FXML private TableColumn<Facture, Long> colId;
    @FXML private TableColumn<Facture, Long> colResId;
    @FXML private TableColumn<Facture, Double> colMontant;
    @FXML private TableColumn<Facture, LocalDateTime> colDate;
    @FXML private TableColumn<Facture, String> colMethode;
    @FXML private TableColumn<Facture, Void> colAction;

    private final ObservableList<Facture> masterData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Mapping des colonnes avec l'entité Facture
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colResId.setCellValueFactory(new PropertyValueFactory<>("reservation_id"));
        colMontant.setCellValueFactory(new PropertyValueFactory<>("montant"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("date_facture"));

        if (colMethode != null) {
            colMethode.setCellValueFactory(new PropertyValueFactory<>("methode_paiement"));
        }

        setupActionColumn();
        // NOTE: On n'appelle plus chargerDonnees() ici pour éviter de charger toutes les factures
    }

    /**
     * Charge uniquement les factures liées à l'utilisateur connecté via un JOIN SQL
     */
    public void loadFacturesForUser(int currentUserId) {
        masterData.clear();
        // Query SQL filtrant par user_id via la table reservation
        String query = "SELECT f.* FROM facture f " +
                "JOIN reservation r ON f.reservation_id = r.id " +
                "WHERE r.user_id = ?";

        try (Connection conn = MyDBConnexion.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, currentUserId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Facture f = new Facture();
                f.setId(rs.getLong("id"));
                f.setReservation_id(rs.getLong("reservation_id"));
                f.setMontant(rs.getDouble("montant"));

                // Gestion de la date SQL vers LocalDateTime
                Timestamp ts = rs.getTimestamp("date_facture");
                if (ts != null) {
                    f.setDate_facture(ts.toLocalDateTime());
                }

                // Mapping du statut et de la méthode de paiement (String to Enum)
                f.setStatut(StatutFacture.valueOf(rs.getString("statut")));
                f.setMethode_paiement(MethodePaiement.valueOf(rs.getString("methode_paiement")));

                masterData.add(f);
            }

            tableFactures.setItems(masterData);
            System.out.println("✅ " + masterData.size() + " factures chargées pour l'utilisateur.");

        } catch (SQLException e) {
            System.err.println("❌ Erreur SQL Factures: " + e.getMessage());
        }
    }

    private void setupActionColumn() {
        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button btnPdf = new Button("Exporter PDF");
            {
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

            DeviceRgb orangeTheme = new DeviceRgb(255, 130, 16);
            DeviceRgb darkGrey = new DeviceRgb(64, 64, 64);

            document.add(new Paragraph("GOVACATE TRAVEL").setFontColor(orangeTheme).setBold().setFontSize(26).setTextAlignment(TextAlignment.RIGHT));
            document.add(new Paragraph("Facture Officielle de Réservation").setFontColor(darkGrey).setFontSize(10).setTextAlignment(TextAlignment.RIGHT));
            document.add(new Paragraph("\n"));

            Table infoTable = new Table(UnitValue.createPercentArray(new float[]{1, 1})).useAllAvailableWidth();
            infoTable.addCell(new Cell().add(new Paragraph("Détails de la facture :\nN° #" + f.getId() + "\nDate: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))).setBorder(com.itextpdf.layout.borders.Border.NO_BORDER));
            infoTable.addCell(new Cell().add(new Paragraph("ID Réservation : " + f.getReservation_id() + "\nStatut : " + f.getStatut()).setTextAlignment(TextAlignment.RIGHT)).setBorder(com.itextpdf.layout.borders.Border.NO_BORDER));
            document.add(infoTable);

            Table priceTable = new Table(UnitValue.createPercentArray(new float[]{3, 1})).useAllAvailableWidth();
            priceTable.addHeaderCell(new Cell().add(new Paragraph("Description")).setBackgroundColor(orangeTheme).setFontColor(DeviceRgb.WHITE));
            priceTable.addHeaderCell(new Cell().add(new Paragraph("Montant")).setBackgroundColor(orangeTheme).setFontColor(DeviceRgb.WHITE));
            priceTable.addCell(new Cell().add(new Paragraph("Réservation de voyage GoVacate")));
            priceTable.addCell(new Cell().add(new Paragraph(f.getMontant() + " DT")).setTextAlignment(TextAlignment.RIGHT));
            document.add(priceTable);

            document.add(new Paragraph("\nTOTAL PAYÉ : " + f.getMontant() + " DT").setBold().setFontSize(18).setTextAlignment(TextAlignment.RIGHT).setFontColor(orangeTheme));
            document.close();

            showAlert("Export réussi", "Facture générée sur le bureau.");
        } catch (Exception e) {
            showAlert("Erreur PDF", e.getMessage());
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
    @FXML
    void handleBack(javafx.event.ActionEvent event) {
        try {
            // Hedhi bech tarja3 lel view mta3 el Dashboard wala el Reservations
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/MesReservations.fxml"));
            Parent root = loader.load();

            // Nasta3mlou el Scene root bech n-badlou el view
            tableFactures.getScene().setRoot(root);
        } catch (java.io.IOException e) {
            System.err.println("❌ Erreur retour: " + e.getMessage());
        }
    }
}