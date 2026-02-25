package tn.esprit.projet.services;

import tn.esprit.projet.entities.Restaurant;
import tn.esprit.projet.utils.govacate_connect;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class RestaurantService implements CRUD<Restaurant> {
    private Connection cnx;

    public RestaurantService() {
        cnx = govacate_connect.getInstance().getConnection();
    }

    // Changed from create() to insert() to match interface
    @Override
    public void insert(Restaurant t) throws SQLException {
        String sql = "INSERT INTO restaurant (name, category, address, phone, email, capacity, status, destination_id, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, t.getName());
            ps.setString(2, t.getCategory());
            ps.setString(3, t.getAddress());
            ps.setString(4, t.getPhone());
            ps.setString(5, t.getEmail());
            ps.setInt(6, t.getCapacity());
            ps.setString(7, t.getStatus());
            ps.setInt(8, t.getDestinationId());
            LocalDateTime now = LocalDateTime.now();
            ps.setTimestamp(9, Timestamp.valueOf(t.getCreatedAt() != null ? t.getCreatedAt() : now));
            ps.setTimestamp(10, Timestamp.valueOf(now));
            ps.executeUpdate();
        }
    }

    @Override
    public void update(Restaurant t) throws SQLException {
        String sql = "UPDATE restaurant SET name=?, category=?, address=?, phone=?, email=?, capacity=?, status=?, destination_id=?, updated_at=? WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, t.getName());
            ps.setString(2, t.getCategory());
            ps.setString(3, t.getAddress());
            ps.setString(4, t.getPhone());
            ps.setString(5, t.getEmail());
            ps.setInt(6, t.getCapacity());
            ps.setString(7, t.getStatus());
            ps.setInt(8, t.getDestinationId());
            ps.setTimestamp(9, Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(10, t.getId());
            ps.executeUpdate();
        }
    }

    // Changed to accept the Object to match interface
    @Override
    public void delete(Restaurant t) throws SQLException {
        String sql = "DELETE FROM restaurant WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, t.getId());
            ps.executeUpdate();
        }
    }

    // Changed from getAll() to selectAll(Restaurant t) to match interface
    @Override
    public List<Restaurant> selectAll(Restaurant t) throws SQLException {
        List<Restaurant> list = new ArrayList<>();
        String query = "SELECT r.*, d.name_destination FROM restaurant r INNER JOIN destination d ON r.destination_id = d.id";
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                list.add(mapResultSetToRestaurant(rs));
            }
        }
        return list;
    }

    @Override
    public Restaurant getById(int id) throws SQLException {
        String sql = "SELECT r.*, d.name_destination FROM restaurant r INNER JOIN destination d ON r.destination_id = d.id WHERE r.id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapResultSetToRestaurant(rs);
            }
        }
        return null;
    }

    private Restaurant mapResultSetToRestaurant(ResultSet rs) throws SQLException {
        Restaurant r = new Restaurant();
        r.setId(rs.getInt("id"));
        r.setName(rs.getString("name"));
        r.setCategory(rs.getString("category"));
        r.setAddress(rs.getString("address"));
        r.setPhone(rs.getString("phone"));
        r.setEmail(rs.getString("email"));
        r.setCapacity(rs.getInt("capacity"));
        r.setStatus(rs.getString("status"));
        r.setDestinationId(rs.getInt("destination_id"));

        try { r.setDestinationName(rs.getString("name_destination")); } catch (SQLException ignored) {}

        Timestamp ct = rs.getTimestamp("created_at");
        if (ct != null) r.setCreatedAt(ct.toLocalDateTime());
        Timestamp ut = rs.getTimestamp("updated_at");
        if (ut != null) r.setUpdatedAt(ut.toLocalDateTime());
        return r;
    }
}