package tn.esprit.projet.services;

import tn.esprit.projet.entities.Excursion;
import tn.esprit.projet.entities.Reservation;
import tn.esprit.projet.entities.ReservationExcursion;
import tn.esprit.projet.utils.MyDBConnexion;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReservationExcursionServiceImpl implements CRUD<ReservationExcursion, Long> {

    private final Connection cnx = MyDBConnexion.getInstance().getConnection();

    // --- HELPER FOR UI ---
    public Excursion findExcursionById(int id) {
        String sql = "SELECT * FROM excursion WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Excursion exc = new Excursion();
                    exc.setId(rs.getInt("id"));
                    exc.setName(rs.getString("name"));
                    exc.setPrice(rs.getDouble("price"));
                    // imagePath logic removed as requested
                    return exc;
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    // --- TRANSACTIONAL METHODS ---
    public void createFullExcursion(Reservation res, ReservationExcursion re) {
        String sqlRes = "INSERT INTO reservation (prix_total, statut, type_res, date_debut, date_fin, nombre_personnes, user_id) VALUES (?, ?, ?, ?, ?, ?, ?)";
        String sqlExc = "INSERT INTO reservation_excursion (reservation_id, excursion_id, date_excursion, nombre_personnes, prix, heure_souhaitee) VALUES (?, ?, ?, ?, ?, ?)";

        try {
            cnx.setAutoCommit(false);
            PreparedStatement ps1 = cnx.prepareStatement(sqlRes, Statement.RETURN_GENERATED_KEYS);
            ps1.setDouble(1, res.getPrix_total());
            ps1.setString(2, res.getStatut().name());
            ps1.setString(3, "EXCURSION");
            ps1.setDate(4, Date.valueOf(res.getDate_debut()));
            ps1.setDate(5, Date.valueOf(res.getDate_debut()));
            ps1.setInt(6, res.getNombre_personnes());
            ps1.setLong(7, res.getUser_id());
            ps1.executeUpdate();

            ResultSet rs = ps1.getGeneratedKeys();
            if (rs.next()) {
                long generatedId = rs.getLong(1);
                PreparedStatement ps2 = cnx.prepareStatement(sqlExc);
                ps2.setLong(1, generatedId);
                ps2.setLong(2, re.getExcursion_id());
                ps2.setDate(3, Date.valueOf(re.getDate_excursion()));
                ps2.setInt(4, re.getNombre_personnes());
                ps2.setDouble(5, re.getPrix());
                ps2.setString(6, re.getHeure_souhaitee());
                ps2.executeUpdate();
                cnx.commit();
            }
        } catch (SQLException e) {
            try { cnx.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            e.printStackTrace();
        } finally {
            try { cnx.setAutoCommit(true); } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    // --- CRUD OVERRIDES ---
    @Override
    public ReservationExcursion insert(ReservationExcursion re) throws SQLException {
        String sql = "INSERT INTO reservation_excursion (reservation_id, excursion_id, date_excursion, nombre_personnes, prix, heure_souhaitee) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, re.getReservation_id());
            ps.setLong(2, re.getExcursion_id());
            ps.setDate(3, Date.valueOf(re.getDate_excursion()));
            ps.setInt(4, re.getNombre_personnes());
            ps.setDouble(5, re.getPrix());
            ps.setString(6, re.getHeure_souhaitee());
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) re.setId(rs.getLong(1));
            return re;
        }
    }

    @Override
    public ReservationExcursion update(ReservationExcursion re) throws SQLException {
        String sql = "UPDATE reservation_excursion SET date_excursion=?, nombre_personnes=?, prix=?, heure_souhaitee=? WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(re.getDate_excursion()));
            ps.setInt(2, re.getNombre_personnes());
            ps.setDouble(3, re.getPrix());
            ps.setString(4, re.getHeure_souhaitee());
            ps.setLong(5, re.getId());
            ps.executeUpdate();
            return re;
        }
    }

    @Override
    public void delete(Long id) throws SQLException {
        String sql = "DELETE FROM reservation_excursion WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    @Override
    public List<ReservationExcursion> selectAll() throws SQLException {
        List<ReservationExcursion> list = new ArrayList<>();
        String sql = "SELECT * FROM reservation_excursion";
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapResultSetToEntity(rs));
        }
        return list;
    }

    @Override
    public ReservationExcursion getById(Long id) throws SQLException {
        String sql = "SELECT * FROM reservation_excursion WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapResultSetToEntity(rs);
            }
        }
        return null;
    }

    private ReservationExcursion mapResultSetToEntity(ResultSet rs) throws SQLException {
        ReservationExcursion re = new ReservationExcursion();
        re.setId(rs.getLong("id"));
        re.setReservation_id(rs.getLong("reservation_id"));
        re.setExcursion_id(rs.getLong("excursion_id"));
        re.setDate_excursion(rs.getDate("date_excursion").toLocalDate());
        re.setNombre_personnes(rs.getInt("nombre_personnes"));
        re.setPrix(rs.getDouble("prix"));
        re.setHeure_souhaitee(rs.getString("heure_souhaitee"));
        return re;
    }
}