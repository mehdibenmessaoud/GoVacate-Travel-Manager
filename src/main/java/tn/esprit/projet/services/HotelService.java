package tn.esprit.projet.services;

import tn.esprit.projet.entities.Hotel;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class HotelService implements IService<Hotel> {
    private Connection connection;

    public HotelService(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void create(Hotel h) throws SQLException {
        String query = "INSERT INTO Hotel (name, description, stars, status, locationId) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, h.getName());
            ps.setString(2, h.getDescription());
            ps.setInt(3, h.getStars());
            ps.setString(4, h.getStatus());
            ps.setInt(5, h.getLocationId());
            ps.executeUpdate();
        }
    }

    @Override
    public List<Hotel> getAll() throws SQLException {
        List<Hotel> hotels = new ArrayList<>();
        String query = "SELECT * FROM Hotel";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                hotels.add(mapResultSetToHotel(rs));
            }
        }
        return hotels;
    }

    /**
     * NOUVELLE MÉTHODE : Permet de récupérer uniquement les hôtels
     * liés à une destination spécifique.
     */
    public List<Hotel> getByDestination(int locationId) throws SQLException {
        List<Hotel> hotels = new ArrayList<>();
        String query = "SELECT * FROM Hotel WHERE locationId = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, locationId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    hotels.add(mapResultSetToHotel(rs));
                }
            }
        }
        return hotels;
    }

    @Override
    public void update(Hotel h) throws SQLException {
        String query = "UPDATE Hotel SET name=?, description=?, stars=?, status=?, locationId=? WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, h.getName());
            ps.setString(2, h.getDescription());
            ps.setInt(3, h.getStars());
            ps.setString(4, h.getStatus());
            ps.setInt(5, h.getLocationId());
            ps.setLong(6, h.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(int id) throws SQLException {
        String query = "DELETE FROM Hotel WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    @Override
    public Hotel getById(int id) throws SQLException {
        String query = "SELECT * FROM Hotel WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToHotel(rs);
                }
            }
        }
        return null;
    }

    public List<Hotel> getByLocation(int locationId) throws SQLException {
        List<Hotel> hotels = new ArrayList<>();
        // Filtre sur la destination ET le statut disponible
        String query = "SELECT * FROM Hotel WHERE locationId = ? AND status = 'Disponible'";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, locationId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    hotels.add(mapResultSetToHotel(rs));
                }
            }
        }
        return hotels;
    }

    // Méthode utilitaire pour mapper le ResultSet vers l'objet Hotel
    private Hotel mapResultSetToHotel(ResultSet rs) throws SQLException {
        return new Hotel(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getInt("stars"),
                rs.getString("status"),
                rs.getInt("locationId")
        );
    }
}