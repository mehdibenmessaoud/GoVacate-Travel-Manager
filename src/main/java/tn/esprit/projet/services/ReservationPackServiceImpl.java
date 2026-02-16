package tn.esprit.projet.services;

import tn.esprit.projet.entities.Pack;
import tn.esprit.projet.entities.Reservation;
import tn.esprit.projet.entities.ReservationPack;
import tn.esprit.projet.utils.MyDBConnexion;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReservationPackServiceImpl implements CrudService<ReservationPack, Long> {

    private final Connection cnx = MyDBConnexion.getInstance().getConnection();



    public Pack getPackById(int id) {
        String sql = "SELECT * FROM pack WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Pack p = new Pack();
                p.setId(rs.getInt("id"));
                p.setName(rs.getString("name"));
                p.setCategorie(rs.getString("categorie"));
                p.setPrix(rs.getDouble("prix"));
                p.setDescription(rs.getString("description"));
                p.setDuree(rs.getInt("duree"));
                p.setStatus(rs.getString("status"));

                // Mapping des dates avec underscores (_) comme demandé
                if (rs.getDate("date_depart") != null)
                    p.setDateDepart(rs.getDate("date_depart").toLocalDate());
                if (rs.getDate("date_arriver") != null)
                    p.setDateArriver(rs.getDate("date_arriver").toLocalDate());

                return p;
            }
        } catch (SQLException e) {
            System.err.println("Erreur affichage Pack : " + e.getMessage());
        }
        return null;
    }


    public void createFullPack(Reservation res, ReservationPack rp) {
        String sqlRes = "INSERT INTO reservation (prix_total, statut, type_res, date_debut, date_fin, nombre_personnes, commentaire_client, user_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        String sqlPack = "INSERT INTO reservation_pack (reservation_id, pack_id, prix_pack) VALUES (?, ?, ?)";

        try {
            cnx.setAutoCommit(false); // Transaction sécurisée

            PreparedStatement ps1 = cnx.prepareStatement(sqlRes, Statement.RETURN_GENERATED_KEYS);
            ps1.setDouble(1, res.getPrix_total());
            ps1.setString(2, res.getStatut().name());
            ps1.setString(3, "PACK");
            ps1.setDate(4, Date.valueOf(res.getDate_debut()));
            ps1.setDate(5, Date.valueOf(res.getDate_fin()));
            ps1.setInt(6, res.getNombre_personnes());
            ps1.setString(7, res.getCommentaire_client());
            ps1.setLong(8, res.getUser_id());
            ps1.executeUpdate();

            ResultSet rs = ps1.getGeneratedKeys();
            if (rs.next()) {
                long generatedId = rs.getLong(1);
                PreparedStatement ps2 = cnx.prepareStatement(sqlPack);
                ps2.setLong(1, generatedId);
                ps2.setLong(2, rp.getPack_id());
                ps2.setDouble(3, rp.getPrix_pack());
                ps2.executeUpdate();

                cnx.commit();
                System.out.println("✅ Réservation concept 'Full Pack' réussie !");
            }
        } catch (SQLException e) {
            try { cnx.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            e.printStackTrace();
        } finally {
            try { cnx.setAutoCommit(true); } catch (SQLException e) { e.printStackTrace(); }
        }
    }



    @Override
    public ReservationPack create(ReservationPack rp) {
        String sql = "INSERT INTO reservation_pack (reservation_id, pack_id, prix_pack) VALUES (?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, rp.getReservation_id());
            ps.setLong(2, rp.getPack_id());
            ps.setDouble(3, rp.getPrix_pack());
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) rp.setId(rs.getLong(1));
            return rp;
        } catch (SQLException e) { e.printStackTrace(); return null; }
    }

    @Override
    public ReservationPack update(ReservationPack rp) {
        String sql = "UPDATE reservation_pack SET reservation_id=?, pack_id=?, prix_pack=? WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setLong(1, rp.getReservation_id());
            ps.setLong(2, rp.getPack_id());
            ps.setDouble(3, rp.getPrix_pack());
            ps.setLong(4, rp.getId());
            ps.executeUpdate();
            return rp;
        } catch (SQLException e) { e.printStackTrace(); return null; }
    }

    @Override
    public void delete(Long id) {
        String sql = "DELETE FROM reservation_pack WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @Override
    public List<ReservationPack> findAll() {
        List<ReservationPack> list = new ArrayList<>();
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery("SELECT * FROM reservation_pack")) {
            while (rs.next()) {
                ReservationPack rp = new ReservationPack();
                rp.setId(rs.getLong("id"));
                rp.setReservation_id(rs.getLong("reservation_id"));
                rp.setPack_id(rs.getLong("pack_id"));
                rp.setPrix_pack(rs.getDouble("prix_pack"));
                list.add(rp);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    @Override
    public ReservationPack findById(Long id) {
        String sql = "SELECT * FROM reservation_pack WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                ReservationPack rp = new ReservationPack();
                rp.setId(rs.getLong("id"));
                rp.setReservation_id(rs.getLong("reservation_id"));
                rp.setPack_id(rs.getLong("pack_id"));
                rp.setPrix_pack(rs.getDouble("prix_pack"));
                return rp;
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }
}