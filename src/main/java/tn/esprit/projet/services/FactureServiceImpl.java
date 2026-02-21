package tn.esprit.projet.services;

import tn.esprit.projet.entities.Facture;
import tn.esprit.projet.entities.MethodePaiement;
import tn.esprit.projet.entities.StatutFacture;
import tn.esprit.projet.utils.MyDBConnexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FactureServiceImpl {

    public void save(Facture f) {
        String sql = "INSERT INTO facture (montant, methode_paiement, statut, reservation_id) VALUES (?, ?, ?, ?)";

        try (Connection conn = MyDBConnexion.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setDouble(1, f.getMontant());
            pstmt.setString(2, f.getMethode_paiement().name());
            pstmt.setString(3, f.getStatut().name());
            pstmt.setLong(4, f.getReservation_id());

            pstmt.executeUpdate();

            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                f.setId(rs.getLong(1));
            }
            System.out.println("Facture enregistrée avec succès !");

        } catch (SQLException e) {
            System.err.println("Erreur SQL Facture: " + e.getMessage());
        }
    }

    public List<Facture> getAll() {
        List<Facture> factures = new ArrayList<>();
        String sql = "SELECT * FROM facture";

        try (Connection conn = MyDBConnexion.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Facture f = new Facture();
                f.setId(rs.getLong("id"));
                f.setMontant(rs.getDouble("montant"));
                f.setReservation_id(rs.getLong("reservation_id"));

                // Mapping des Enums pour le design du PDF et de la TableView
                f.setMethode_paiement(MethodePaiement.valueOf(rs.getString("methode_paiement")));
                f.setStatut(StatutFacture.valueOf(rs.getString("statut")));

                if (rs.getTimestamp("date_facture") != null) {
                    f.setDate_facture(rs.getTimestamp("date_facture").toLocalDateTime());
                }

                factures.add(f);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return factures;
    }
}