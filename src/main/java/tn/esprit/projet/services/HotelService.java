package tn.esprit.projet.services;

import tn.esprit.projet.entities.Hotel;
import tn.esprit.projet.utils.MyDBConnexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Service class for Hotel CRUD operations.
 */
public class HotelService implements CRUD<Hotel> {

    private Connection cnx;

    public HotelService() {
        cnx = MyDBConnexion.getInstance().getConnection();
    }

    @Override
    public void create(Hotel hotel) throws SQLException {
        String sql = "INSERT INTO hotel (name, description, stars, status, locationId) VALUES (?, ?, ?, ?, ?)";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, hotel.getName());
        ps.setString(2, hotel.getDescription());
        ps.setInt(3, hotel.getStars());
        ps.setString(4, hotel.getStatus());
        ps.setInt(5, hotel.getLocationId());
        ps.executeUpdate();
    }

    @Override
    public List<Hotel> getAll() throws SQLException {
        List<Hotel> hotels = new ArrayList<>();
        String sql = "SELECT * FROM hotel";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);
        
        while (rs.next()) {
            Hotel h = new Hotel(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getInt("stars"),
                rs.getString("status"),
                rs.getInt("locationId")
            );
            hotels.add(h);
        }
        return hotels;
    }

    @Override
    public Hotel getById(int id) throws SQLException {
        String sql = "SELECT * FROM hotel WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
        
        if (rs.next()) {
            return new Hotel(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getInt("stars"),
                rs.getString("status"),
                rs.getInt("locationId")
            );
        }
        return null;
    }

    @Override
    public void update(Hotel hotel) throws SQLException {
        String sql = "UPDATE hotel SET name=?, description=?, stars=?, status=?, locationId=? WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, hotel.getName());
        ps.setString(2, hotel.getDescription());
        ps.setInt(3, hotel.getStars());
        ps.setString(4, hotel.getStatus());
        ps.setInt(5, hotel.getLocationId());
        ps.setInt(6, hotel.getId());
        ps.executeUpdate();
    }

    @Override
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM hotel WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    /**
     * Get hotels by destination/location.
     */
    public List<Hotel> getByLocation(int locationId) throws SQLException {
        List<Hotel> hotels = new ArrayList<>();
        String sql = "SELECT * FROM hotel WHERE locationId = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, locationId);
        ResultSet rs = ps.executeQuery();
        
        while (rs.next()) {
            hotels.add(new Hotel(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getInt("stars"),
                rs.getString("status"),
                rs.getInt("locationId")
            ));
        }
        return hotels;
    }

    /**
     * Get hotels by status (AVAILABLE, OCCUPIED, MAINTENANCE).
     */
    public List<Hotel> getByStatus(String status) throws SQLException {
        List<Hotel> hotels = new ArrayList<>();
        String sql = "SELECT * FROM hotel WHERE status = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, status);
        ResultSet rs = ps.executeQuery();
        
        while (rs.next()) {
            hotels.add(new Hotel(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getInt("stars"),
                rs.getString("status"),
                rs.getInt("locationId")
            ));
        }
        return hotels;
    }

    /**
     * Get hotels with minimum star rating.
     */
    public List<Hotel> getByMinStars(int minStars) throws SQLException {
        List<Hotel> hotels = new ArrayList<>();
        String sql = "SELECT * FROM hotel WHERE stars >= ? ORDER BY stars DESC";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, minStars);
        ResultSet rs = ps.executeQuery();
        
        while (rs.next()) {
            hotels.add(new Hotel(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getInt("stars"),
                rs.getString("status"),
                rs.getInt("locationId")
            ));
        }
        return hotels;
    }

    /**
     * Search hotels by name.
     */
    public List<Hotel> searchByName(String keyword) throws SQLException {
        List<Hotel> hotels = new ArrayList<>();
        String sql = "SELECT * FROM hotel WHERE name LIKE ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, "%" + keyword + "%");
        ResultSet rs = ps.executeQuery();
        
        while (rs.next()) {
            hotels.add(new Hotel(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getInt("stars"),
                rs.getString("status"),
                rs.getInt("locationId")
            ));
        }
        return hotels;
    }
}
