package tn.esprit.projet.services;

import tn.esprit.projet.entities.Reservation;
import tn.esprit.projet.entities.StatutReservation;
import tn.esprit.projet.utils.MyDBConnexion;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReservationServiceImpl implements IService<Reservation> {

    // ============================================================
    //      MÉTHODES DE L'INTERFACE IService (CRUD)
    // ============================================================

    @Override
    public void create(Reservation r) throws SQLException {
        String sql = "INSERT INTO reservation (statut, date_debut, date_fin, nombre_personnes, prix_total, commentaire_client, user_id, type_res) VALUES (?,?,?,?,?,?,?,?)";
        try (Connection conn = MyDBConnexion.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, r.getStatut().name());
            ps.setDate(2, Date.valueOf(r.getDate_debut()));
            ps.setDate(3, Date.valueOf(r.getDate_fin()));
            ps.setInt(4, r.getNombre_personnes());
            ps.setDouble(5, r.getPrix_total());
            ps.setString(6, r.getCommentaire_client());
            ps.setLong(7, r.getUser_id());
            ps.setString(8, r.getType_res());
            ps.executeUpdate();
        }
    }

    @Override
    public List<Reservation> getAll() throws SQLException {
        List<Reservation> list = new ArrayList<>();
        try (Connection conn = MyDBConnexion.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM reservation")) {
            while (rs.next()) {
                list.add(mapResultSetToReservation(rs));
            }
        }
        return list;
    }

    // 🔥 NOUVELLE MÉTHODE: Pour afficher uniquement les réservations du client connecté
    public List<Reservation> getReservationsByUserId(int userId) {
        List<Reservation> list = new ArrayList<>();
        String sql = "SELECT * FROM reservation WHERE user_id = ?";
        try (Connection conn = MyDBConnexion.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToReservation(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur getReservationsByUserId: " + e.getMessage());
        }
        return list;
    }

    @Override
    public void update(Reservation r) throws SQLException {
        String sql = "UPDATE reservation SET statut=?, prix_total=?, commentaire_client=? WHERE id=?";
        try (Connection conn = MyDBConnexion.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, r.getStatut().name());
            ps.setDouble(2, r.getPrix_total());
            ps.setString(3, r.getCommentaire_client());
            ps.setLong(4, r.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(int id) throws SQLException {
        try (Connection conn = MyDBConnexion.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM reservation WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    @Override
    public Reservation getById(int id) throws SQLException {
        try (Connection conn = MyDBConnexion.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM reservation WHERE id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToReservation(rs);
                }
            }
        }
        return null;
    }

    // ============================================================
    //      MÉTHODES LOGIQUES & MAPPING
    // ============================================================

    private Reservation mapResultSetToReservation(ResultSet rs) throws SQLException {
        Reservation r = new Reservation();
        r.setId(rs.getLong("id"));
        r.setStatut(StatutReservation.valueOf(rs.getString("statut").toUpperCase()));
        r.setDate_debut(rs.getDate("date_debut").toLocalDate());
        r.setDate_fin(rs.getDate("date_fin").toLocalDate());
        r.setNombre_personnes(rs.getInt("nombre_personnes"));
        r.setPrix_total(rs.getDouble("prix_total"));
        r.setCommentaire_client(rs.getString("commentaire_client"));
        r.setUser_id(rs.getLong("user_id"));
        try { r.setType_res(rs.getString("type_res")); } catch (Exception e) {}
        return r;
    }

    public List<Reservation> getAllReservations() {
        List<Reservation> reservations = new ArrayList<>();
        // Note: l'alias 'utilisateur' correspond à ton erreur SQL précédente
        String sql = "SELECT r.*, u.nom FROM reservation r LEFT JOIN utilisateur u ON r.user_id = u.id";

        try (Connection conn = MyDBConnexion.getInstance().getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                Reservation r = mapResultSetToReservation(rs);
                // On utilise le nom de l'utilisateur s'il existe pour le commentaire (Utile pour Admin)
                if (rs.getString("nom") != null) {
                    r.setCommentaire_client(rs.getString("nom"));
                }
                reservations.add(r);
            }
        } catch (SQLException e) {
            System.err.println("❌ SQL Error in getAllReservations: " + e.getMessage());
        }
        return reservations;
    }

    public void updateStatus(long id, String newStatus) {
        String sql = "UPDATE reservation SET statut = ? WHERE id = ?";
        try (Connection conn = MyDBConnexion.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setLong(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}