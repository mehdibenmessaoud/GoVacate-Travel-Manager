package tn.esprit.projet.services;

import tn.esprit.projet.entities.Reservation;
import tn.esprit.projet.entities.ReservationRestaurant;
import tn.esprit.projet.entities.Restaurant;
import tn.esprit.projet.utils.MyDBConnexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReservationRestaurantServiceImpl implements IService<ReservationRestaurant> {

    // On récupère la connexion via ton Singleton
    private Connection getConnection() {
        return MyDBConnexion.getInstance().getConnection();
    }

    // ============================================================
    //      MÉTHODES DE L'INTERFACE IService (MATCHING CRUD)
    // ============================================================

    @Override
    public void create(ReservationRestaurant rr) throws SQLException {
        String sql = "INSERT INTO reservation_restaurant (reservation_id, restaurant_id, date_reservation, heure_souhaitee, nombre_personnes, prix) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, rr.getReservation_id());
            ps.setLong(2, rr.getRestaurant_id());
            ps.setDate(3, Date.valueOf(rr.getDate_reservation()));
            ps.setString(4, rr.getHeure_souhaitee());
            ps.setInt(5, rr.getNombre_personnes());
            ps.setDouble(6, rr.getPrix());
            ps.executeUpdate();
        }
    }

    @Override
    public List<ReservationRestaurant> getAll() throws SQLException {
        List<ReservationRestaurant> list = new ArrayList<>();
        String sql = "SELECT * FROM reservation_restaurant";
        try (Connection conn = getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(map(rs));
            }
        }
        return list;
    }

    @Override
    public void update(ReservationRestaurant rr) throws SQLException {
        String sql = "UPDATE reservation_restaurant SET date_reservation=?, heure_souhaitee=?, nombre_personnes=?, prix=? WHERE id=?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(rr.getDate_reservation()));
            ps.setString(2, rr.getHeure_souhaitee());
            ps.setInt(3, rr.getNombre_personnes());
            ps.setDouble(4, rr.getPrix());
            ps.setLong(5, rr.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM reservation_restaurant WHERE id=?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    @Override
    public ReservationRestaurant getById(int id) throws SQLException {
        String sql = "SELECT * FROM reservation_restaurant WHERE id=?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        }
        return null;
    }

    // ============================================================
    //      TES MÉTHODES SPÉCIFIQUES (CONCEPT TRANSACTIONNEL)
    // ============================================================

    public void createFullReservation(Reservation res, ReservationRestaurant rr) {
        String sqlRes = "INSERT INTO reservation (prix_total, statut, type_res, date_debut, date_fin, nombre_personnes, user_id) VALUES (?, ?, ?, ?, ?, ?, ?)";
        String sqlResto = "INSERT INTO reservation_restaurant (reservation_id, restaurant_id, date_reservation, heure_souhaitee, nombre_personnes, prix) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps1 = conn.prepareStatement(sqlRes, Statement.RETURN_GENERATED_KEYS)) {
                ps1.setDouble(1, res.getPrix_total());
                ps1.setString(2, res.getStatut().name());
                ps1.setString(3, res.getType_res());
                ps1.setDate(4, Date.valueOf(res.getDate_debut()));
                ps1.setDate(5, Date.valueOf(res.getDate_debut()));
                ps1.setInt(6, res.getNombre_personnes());
                ps1.setLong(7, res.getUser_id());
                ps1.executeUpdate();

                try (ResultSet rs = ps1.getGeneratedKeys()) {
                    if (rs.next()) {
                        long lastId = rs.getLong(1);
                        try (PreparedStatement ps2 = conn.prepareStatement(sqlResto)) {
                            ps2.setLong(1, lastId);
                            ps2.setLong(2, rr.getRestaurant_id());
                            ps2.setDate(3, Date.valueOf(rr.getDate_reservation()));
                            ps2.setString(4, rr.getHeure_souhaitee());
                            ps2.setInt(5, rr.getNombre_personnes());
                            ps2.setDouble(6, rr.getPrix());
                            ps2.executeUpdate();
                        }
                    }
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updateFullReservation(Reservation res, ReservationRestaurant rr) {
        String sqlRes = "UPDATE reservation SET prix_total=?, date_debut=?, nombre_personnes=? WHERE id=?";
        String sqlResto = "UPDATE reservation_restaurant SET restaurant_id=?, date_reservation=?, heure_souhaitee=?, nombre_personnes=?, prix=? WHERE reservation_id=?";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                PreparedStatement ps1 = conn.prepareStatement(sqlRes);
                ps1.setDouble(1, res.getPrix_total());
                ps1.setDate(2, Date.valueOf(res.getDate_debut()));
                ps1.setInt(3, res.getNombre_personnes());
                ps1.setLong(4, res.getId());
                ps1.executeUpdate();

                PreparedStatement ps2 = conn.prepareStatement(sqlResto);
                ps2.setLong(1, rr.getRestaurant_id());
                ps2.setDate(2, Date.valueOf(rr.getDate_reservation()));
                ps2.setString(3, rr.getHeure_souhaitee());
                ps2.setInt(4, rr.getNombre_personnes());
                ps2.setDouble(5, rr.getPrix());
                ps2.setLong(6, res.getId());
                ps2.executeUpdate();

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public ReservationRestaurant findByReservationId(Long resId) {
        String sql = "SELECT * FROM reservation_restaurant WHERE reservation_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, resId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Restaurant> findAllRestaurants() {
        List<Restaurant> list = new ArrayList<>();
        String sql = "SELECT * FROM restaurant";
        try (Connection conn = getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Restaurant r = new Restaurant();
                r.setId(rs.getInt("id"));
                r.setName(rs.getString("name"));
                r.setCapacity(rs.getInt("capacity"));
                r.setStatus(rs.getString("status") != null ? rs.getString("status") : "OPEN");
                list.add(r);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    private ReservationRestaurant map(ResultSet rs) throws SQLException {
        ReservationRestaurant rr = new ReservationRestaurant();
        rr.setId(rs.getLong("id"));
        rr.setReservation_id(rs.getLong("reservation_id"));
        rr.setRestaurant_id(rs.getLong("restaurant_id"));
        rr.setDate_reservation(rs.getDate("date_reservation").toLocalDate());
        rr.setHeure_souhaitee(rs.getString("heure_souhaitee"));
        rr.setNombre_personnes(rs.getInt("nombre_personnes"));
        rr.setPrix(rs.getDouble("prix"));
        return rr;
    }
}