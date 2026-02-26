package tn.esprit.projet.services;

import tn.esprit.projet.entities.RestaurantImage;
import tn.esprit.projet.utils.govacate_connect;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

// Fixed: Implements CRUD with both RestaurantImage and Integer to satisfy generic requirements
public class RestaurantImageService implements CRUD<RestaurantImage, Integer> {

    private final Connection cnx;

    public RestaurantImageService() {
        this.cnx = govacate_connect.getInstance().getConnection();
    }

    @Override
    public RestaurantImage insert(RestaurantImage ri) throws SQLException {
        // Changed return type from void to RestaurantImage to match interface
        String sql = "INSERT INTO restaurant_image (image_url, restaurant_id) VALUES (?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, ri.getImageUrl());
            ps.setInt(2, ri.getRestaurantId());
            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) ri.setId(rs.getInt(1));
            return ri;
        }
    }

    @Override
    public RestaurantImage update(RestaurantImage ri) throws SQLException {
        // Changed return type from void to RestaurantImage
        String sql = "UPDATE restaurant_image SET image_url=?, restaurant_id=? WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, ri.getImageUrl());
            ps.setInt(2, ri.getRestaurantId());
            ps.setInt(3, ri.getId());
            ps.executeUpdate();
            return ri;
        }
    }

    @Override
    public void delete(Integer id) throws SQLException {
        // Fixed: Parameter changed from RestaurantImage object to Integer ID
        String sql = "DELETE FROM restaurant_image WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    @Override
    public List<RestaurantImage> selectAll() throws SQLException {
        // Fixed: Removed the 'unused' parameter to match selectAll() signature
        List<RestaurantImage> images = new ArrayList<>();
        String sql = "SELECT * FROM restaurant_image";
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                images.add(mapResultSetToEntity(rs));
            }
        }
        return images;
    }

    @Override
    public RestaurantImage getById(Integer id) throws SQLException {
        String sql = "SELECT * FROM restaurant_image WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapResultSetToEntity(rs);
            }
        }
        return null;
    }

    public List<RestaurantImage> getByRestaurantId(int restaurantId) throws SQLException {
        List<RestaurantImage> images = new ArrayList<>();
        String sql = "SELECT * FROM restaurant_image WHERE restaurant_id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, restaurantId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    images.add(mapResultSetToEntity(rs));
                }
            }
        }
        return images;
    }

    private RestaurantImage mapResultSetToEntity(ResultSet rs) throws SQLException {
        return new RestaurantImage(
                rs.getInt("id"),
                rs.getString("image_url"),
                rs.getInt("restaurant_id")
        );
    }
}