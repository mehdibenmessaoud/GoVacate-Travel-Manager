package tn.esprit.projet.services;

import tn.esprit.projet.entities.ReservationHotel;
import tn.esprit.projet.utils.MyDBConnexion;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReservationHotelServiceImpl implements CrudService<ReservationHotel, Long> {

    private final Connection cnx = MyDBConnexion.getInstance().getConnection();

    @Override
    public ReservationHotel create(ReservationHotel rh) {
        try {
            String sql = """
                INSERT INTO reservation_hotel 
                (reservation_id, hotel_id, chambre_id, date_checkin, date_checkout, prix) 
                VALUES (?, ?, ?, ?, ?, ?)
            """;
            PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, rh.getReservation_id());
            ps.setLong(2, rh.getHotel_id());
            ps.setLong(3, rh.getChambre_id());
            ps.setDate(4, Date.valueOf(rh.getDate_checkin()));
            ps.setDate(5, Date.valueOf(rh.getDate_checkout()));
            ps.setDouble(6, rh.getPrix());

            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                rh.setId(rs.getLong(1));
            }
            return rh;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public ReservationHotel update(ReservationHotel rh) {
        try {
            // Updated SQL to include chambre_id and hotel_id in case they change
            String sql = """
                UPDATE reservation_hotel 
                SET hotel_id=?, chambre_id=?, date_checkin=?, date_checkout=?, prix=? 
                WHERE id=?
            """;
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setLong(1, rh.getHotel_id());
            ps.setLong(2, rh.getChambre_id());
            ps.setDate(3, Date.valueOf(rh.getDate_checkin()));
            ps.setDate(4, Date.valueOf(rh.getDate_checkout()));
            ps.setDouble(5, rh.getPrix());
            ps.setLong(6, rh.getId());

            ps.executeUpdate();
            return rh;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void delete(Long id) {
        try {
            PreparedStatement ps = cnx.prepareStatement("DELETE FROM reservation_hotel WHERE id=?");
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public ReservationHotel findById(Long id) {
        try {
            PreparedStatement ps = cnx.prepareStatement("SELECT * FROM reservation_hotel WHERE id=?");
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return map(rs);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<ReservationHotel> findAll() {
        List<ReservationHotel> list = new ArrayList<>();
        try {
            ResultSet rs = cnx.createStatement().executeQuery("SELECT * FROM reservation_hotel");
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    private ReservationHotel map(ResultSet rs) throws SQLException {
        ReservationHotel rh = new ReservationHotel();
        rh.setId(rs.getLong("id"));
        rh.setReservation_id(rs.getLong("reservation_id"));
        rh.setHotel_id(rs.getLong("hotel_id"));
        rh.setChambre_id(rs.getLong("chambre_id"));
        rh.setDate_checkin(rs.getDate("date_checkin").toLocalDate());
        rh.setDate_checkout(rs.getDate("date_checkout").toLocalDate());
        rh.setPrix(rs.getDouble("prix"));
        return rh;
    }
}