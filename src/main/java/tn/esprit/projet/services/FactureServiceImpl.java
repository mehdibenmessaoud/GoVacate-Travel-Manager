package tn.esprit.projet.services;

import tn.esprit.projet.entities.Facture;
import tn.esprit.projet.entities.MethodePaiement;
import tn.esprit.projet.entities.StatutFacture;
import tn.esprit.projet.utils.MyDBConnexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FactureServiceImpl implements CrudService<Facture, Long> {

    private final Connection cnx = MyDBConnexion.getInstance().getConnection();

    @Override
    public Facture create(Facture f) {
        try {
            // date_facture is set to NOW() by the database, or handled via Timestamp
            String sql = """
                INSERT INTO facture 
                (montant, methode_paiement, statut, date_facture, reservation_id) 
                VALUES (?, ?, ?, ?, ?)
            """;
            PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setDouble(1, f.getMontant());
            ps.setString(2, f.getMethode_paiement().name());
            ps.setString(3, f.getStatut().name());
            // Setting current time if date_facture is null
            ps.setTimestamp(4, f.getDate_facture() != null ?
                    Timestamp.valueOf(f.getDate_facture()) : new Timestamp(System.currentTimeMillis()));
            ps.setLong(5, f.getReservation_id());

            ps.executeUpdate();

            // Getting the auto-generated ID from MySQL
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                f.setId(rs.getLong(1));
            }
            return f;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public Facture update(Facture f) {
        try {
            String sql = """
                UPDATE facture 
                SET montant=?, methode_paiement=?, statut=?, date_paiement=? 
                WHERE id=?
            """;
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setDouble(1, f.getMontant());
            ps.setString(2, f.getMethode_paiement().name());
            ps.setString(3, f.getStatut().name());
            ps.setTimestamp(4, f.getDate_paiement() != null ? Timestamp.valueOf(f.getDate_paiement()) : null);
            ps.setLong(5, f.getId());

            ps.executeUpdate();
            return f;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void delete(Long id) {
        try {
            PreparedStatement ps = cnx.prepareStatement("DELETE FROM facture WHERE id=?");
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Facture findById(Long id) {
        try {
            PreparedStatement ps = cnx.prepareStatement("SELECT * FROM facture WHERE id=?");
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapResultSetToFacture(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<Facture> findAll() {
        List<Facture> list = new ArrayList<>();
        try {
            ResultSet rs = cnx.createStatement().executeQuery("SELECT * FROM facture");
            while (rs.next()) {
                list.add(mapResultSetToFacture(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // --- Helper Method to map SQL data to Facture Object ---
    private Facture mapResultSetToFacture(ResultSet rs) throws SQLException {
        Facture f = new Facture();
        f.setId(rs.getLong("id"));
        f.setMontant(rs.getDouble("montant"));
        f.setMethode_paiement(MethodePaiement.valueOf(rs.getString("methode_paiement")));
        f.setStatut(StatutFacture.valueOf(rs.getString("statut")));
        f.setReservation_id(rs.getLong("reservation_id"));

        // Convert SQL Timestamps to LocalDateTime
        Timestamp tsFacture = rs.getTimestamp("date_facture");
        if (tsFacture != null) f.setDate_facture(tsFacture.toLocalDateTime());

        Timestamp tsPaiement = rs.getTimestamp("date_paiement");
        if (tsPaiement != null) f.setDate_paiement(tsPaiement.toLocalDateTime());

        return f;
    }
}