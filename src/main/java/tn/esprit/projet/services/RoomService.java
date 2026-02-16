package tn.esprit.projet.services;

import tn.esprit.projet.entities.Room;
import tn.esprit.projet.utils.MyDBConnexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Service class for Room CRUD operations.
 */
public class RoomService implements CRUD<Room> {

    private final Connection cnx;

    public RoomService() {
        cnx = MyDBConnexion.getInstance().getConnection();
    }

    @Override
    public void create(Room room) throws SQLException {
        if (room == null) {
            throw new IllegalArgumentException("room is null");
        }
        String sql = "INSERT INTO room (roomNumber, roomType, capacity, pricePerNight, status, hotelId) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, room.getRoomNumber());
            ps.setString(2, room.getRoomType());
            ps.setInt(3, room.getCapacity());
            ps.setDouble(4, room.getPricePerNight());
            ps.setString(5, room.getStatus());
            ps.setInt(6, room.getHotelId());
            ps.executeUpdate();
        }
    }

    @Override
    public List<Room> getAll() throws SQLException {
        List<Room> rooms = new ArrayList<>();
        String sql = "SELECT * FROM room";
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                rooms.add(extractRoom(rs));
            }
        }
        return rooms;
    }

    @Override
    public Room getById(int id) throws SQLException {
        String sql = "SELECT * FROM room WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return extractRoom(rs);
                }
            }
        }
        return null;
    }

    @Override
    public void update(Room room) throws SQLException {
        if (room == null) {
            throw new IllegalArgumentException("room is null");
        }
        String sql = "UPDATE room SET roomNumber=?, roomType=?, capacity=?, pricePerNight=?, status=?, hotelId=? " +
                     "WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, room.getRoomNumber());
            ps.setString(2, room.getRoomType());
            ps.setInt(3, room.getCapacity());
            ps.setDouble(4, room.getPricePerNight());
            ps.setString(5, room.getStatus());
            ps.setInt(6, room.getHotelId());
            ps.setInt(7, room.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM room WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    /**
     * Get all rooms for a specific hotel.
     */
    public List<Room> getRoomsByHotel(int hotelId) throws SQLException {
        List<Room> rooms = new ArrayList<>();
        String sql = "SELECT * FROM room WHERE hotelId = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, hotelId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rooms.add(extractRoom(rs));
                }
            }
        }
        return rooms;
    }

    /**
     * Get available rooms for a hotel.
     */
    public List<Room> getAvailableRoomsByHotel(int hotelId) throws SQLException {
        List<Room> rooms = new ArrayList<>();
        String sql = "SELECT * FROM room WHERE hotelId = ? AND status = 'AVAILABLE'";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, hotelId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rooms.add(extractRoom(rs));
                }
            }
        }
        return rooms;
    }

    /**
     * Get rooms by type (SINGLE, DOUBLE, SUITE, etc.).
     */
    public List<Room> getByType(String roomType) throws SQLException {
        List<Room> rooms = new ArrayList<>();
        String sql = "SELECT * FROM room WHERE roomType = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, roomType);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rooms.add(extractRoom(rs));
                }
            }
        }
        return rooms;
    }

    /**
     * Get rooms within a price range.
     */
    public List<Room> getByPriceRange(double minPrice, double maxPrice) throws SQLException {
        List<Room> rooms = new ArrayList<>();
        String sql = "SELECT * FROM room WHERE pricePerNight BETWEEN ? AND ? ORDER BY pricePerNight";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setDouble(1, minPrice);
            ps.setDouble(2, maxPrice);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rooms.add(extractRoom(rs));
                }
            }
        }
        return rooms;
    }

    // Helper method to extract Room from ResultSet
    private Room extractRoom(ResultSet rs) throws SQLException {
        return new Room(
            rs.getInt("id"),
            rs.getString("roomNumber"),
            rs.getString("roomType"),
            rs.getInt("capacity"),
            rs.getDouble("pricePerNight"),
            rs.getString("status"),
            rs.getInt("hotelId")
        );
    }
}
