package tn.esprit.projet.services;

import tn.esprit.projet.entities.Reservation;
import tn.esprit.projet.entities.ReservationRestaurant;
import tn.esprit.projet.entities.Restaurant;
import tn.esprit.projet.entities.StatutReservation;
import tn.esprit.projet.utils.MyDBConnexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReservationRestaurantServiceImpl implements CrudService<ReservationRestaurant, Long> {

    private final Connection cnx = MyDBConnexion.getInstance().getConnection();



    /**
     * Récupère la ligne enfant à partir de l'ID de la réservation parente
     */
    public ReservationRestaurant findByReservationId(Long resId) {
        String sql = "SELECT * FROM reservation_restaurant WHERE reservation_id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setLong(1, resId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Mise à jour atomique : Modifie la table 'reservation' ET 'reservation_restaurant'
     */
    public void updateFullReservation(Reservation res, ReservationRestaurant rr) {
        String sqlRes = "UPDATE reservation SET prix_total=?, date_debut=?, nombre_personnes=? WHERE id=?";
        String sqlResto = "UPDATE reservation_restaurant SET restaurant_id=?, date_reservation=?, heure_souhaitee=?, nombre_personnes=?, prix=? WHERE reservation_id=?";

        try {
            cnx.setAutoCommit(false);

            // 1. Update Parent
            PreparedStatement ps1 = cnx.prepareStatement(sqlRes);
            ps1.setDouble(1, res.getPrix_total());
            ps1.setDate(2, java.sql.Date.valueOf(res.getDate_debut()));
            ps1.setInt(3, res.getNombre_personnes());
            ps1.setLong(4, res.getId());
            ps1.executeUpdate();

            // 2. Update Enfant (Incluant l'heure)
            PreparedStatement ps2 = cnx.prepareStatement(sqlResto);
            ps2.setLong(1, rr.getRestaurant_id());
            ps2.setDate(2, java.sql.Date.valueOf(rr.getDate_reservation()));
            ps2.setString(3, rr.getHeure_souhaitee()); // Nouveau champ
            ps2.setInt(4, rr.getNombre_personnes());
            ps2.setDouble(5, rr.getPrix());
            ps2.setLong(6, res.getId());
            ps2.executeUpdate();

            cnx.commit();
            System.out.println("✅ Mise à jour complète réussie !");
        } catch (SQLException e) {
            try { cnx.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            e.printStackTrace();
        } finally {
            try { cnx.setAutoCommit(true); } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    /**
     * Création atomique : Insère dans 'reservation' puis récupère l'ID pour 'reservation_restaurant'
     */
    public void createFullReservation(Reservation res, ReservationRestaurant rr) {
        String sqlRes = "INSERT INTO reservation (prix_total, statut, type_res, date_debut, date_fin, nombre_personnes, user_id) VALUES (?, ?, ?, ?, ?, ?, ?)";
        String sqlResto = "INSERT INTO reservation_restaurant (reservation_id, restaurant_id, date_reservation, heure_souhaitee, nombre_personnes, prix) VALUES (?, ?, ?, ?, ?, ?)";

        try {
            cnx.setAutoCommit(false);

            // 1. Insertion Parent
            PreparedStatement ps1 = cnx.prepareStatement(sqlRes, Statement.RETURN_GENERATED_KEYS);
            ps1.setDouble(1, res.getPrix_total());
            ps1.setString(2, res.getStatut().name());
            ps1.setString(3, res.getType_res());
            ps1.setDate(4, java.sql.Date.valueOf(res.getDate_debut()));
            ps1.setDate(5, java.sql.Date.valueOf(res.getDate_debut())); // date_fin = date_debut pour resto
            ps1.setInt(6, res.getNombre_personnes());
            ps1.setLong(7, res.getUser_id());
            ps1.executeUpdate();

            ResultSet rs = ps1.getGeneratedKeys();
            if (rs.next()) {
                long lastId = rs.getLong(1);

                // 2. Insertion Enfant
                PreparedStatement ps2 = cnx.prepareStatement(sqlResto);
                ps2.setLong(1, lastId);
                ps2.setLong(2, rr.getRestaurant_id());
                ps2.setDate(3, java.sql.Date.valueOf(rr.getDate_reservation()));
                ps2.setString(4, rr.getHeure_souhaitee()); // Nouveau champ
                ps2.setInt(5, rr.getNombre_personnes());
                ps2.setDouble(6, rr.getPrix());
                ps2.executeUpdate();

                cnx.commit();
                System.out.println("✅ Création complète réussie ! ID Parent: " + lastId);
            }
        } catch (SQLException e) {
            try { cnx.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            e.printStackTrace();
        } finally {
            try { cnx.setAutoCommit(true); } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    /**
     * Récupère la liste des restaurants pour le ComboBox
     */
    public List<Restaurant> findAllRestaurants() {
        List<Restaurant> list = new ArrayList<>();
        String sql = "SELECT * FROM restaurant";
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Restaurant r = new Restaurant();
                r.setId(rs.getInt("id"));
                r.setName(rs.getString("name"));
                r.setCapacity(rs.getInt("capacity"));
                String s = rs.getString("status");
                r.setStatus(s != null ? s : "OPEN");
                list.add(r);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }


    @Override
    public ReservationRestaurant create(ReservationRestaurant rr) {
        String sql = "INSERT INTO reservation_restaurant (reservation_id, restaurant_id, date_reservation, heure_souhaitee, nombre_personnes, prix) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, rr.getReservation_id());
            ps.setLong(2, rr.getRestaurant_id());
            ps.setDate(3, Date.valueOf(rr.getDate_reservation()));
            ps.setString(4, rr.getHeure_souhaitee());
            ps.setInt(5, rr.getNombre_personnes());
            ps.setDouble(6, rr.getPrix());
            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) rr.setId(rs.getLong(1));
            return rr;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public ReservationRestaurant update(ReservationRestaurant rr) {
        String sql = "UPDATE reservation_restaurant SET date_reservation=?, heure_souhaitee=?, nombre_personnes=?, prix=? WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(rr.getDate_reservation()));
            ps.setString(2, rr.getHeure_souhaitee());
            ps.setInt(3, rr.getNombre_personnes());
            ps.setDouble(4, rr.getPrix());
            ps.setLong(5, rr.getId());
            ps.executeUpdate();
            return rr;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void delete(Long id) {
        String sql = "DELETE FROM reservation_restaurant WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public ReservationRestaurant findById(Long id) {
        String sql = "SELECT * FROM reservation_restaurant WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return map(rs);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<ReservationRestaurant> findAll() {
        List<ReservationRestaurant> list = new ArrayList<>();
        String sql = "SELECT * FROM reservation_restaurant";
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(map(rs));
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
        rr.setHeure_souhaitee(rs.getString("heure_souhaitee")); // Récupération de l'heure
        rr.setNombre_personnes(rs.getInt("nombre_personnes"));
        rr.setPrix(rs.getDouble("prix"));
        return rr;
    }
}