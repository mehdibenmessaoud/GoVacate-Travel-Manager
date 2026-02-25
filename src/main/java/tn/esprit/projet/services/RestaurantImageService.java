package tn.esprit.projet.services;

import tn.esprit.projet.entities.RestaurantImage;
import tn.esprit.projet.utils.govacate_connect;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RestaurantImageService implements CRUD<RestaurantImage> {

    private Connection cnx;

    public RestaurantImageService() {
        cnx = govacate_connect.getInstance().getConnection();
    }

    // Renamed from create to insert to match CRUD interface
    @Override
    public void insert(RestaurantImage ri) throws SQLException {
        String sql = "INSERT INTO restaurant_image (image_url, restaurant_id) VALUES (?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, ri.getImageUrl());
            ps.setInt(2, ri.getRestaurantId());
            ps.executeUpdate();
        }
    }

    // Renamed from getAll to selectAll to match CRUD interface
    @Override
    public List<RestaurantImage> selectAll(RestaurantImage unused) throws SQLException {
        List<RestaurantImage> images = new ArrayList<>();
        String sql = "SELECT * FROM restaurant_image";

        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                images.add(new RestaurantImage(
                        rs.getInt("id"),
                        rs.getString("image_url"),
                        rs.getInt("restaurant_id")
                ));
            }
        }
        return images;
    }

    @Override
    public void update(RestaurantImage ri) throws SQLException {
        String sql = "UPDATE restaurant_image SET image_url=?, restaurant_id=? WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, ri.getImageUrl());
            ps.setInt(2, ri.getRestaurantId());
            ps.setInt(3, ri.getId());
            ps.executeUpdate();
        }
    }

    // Matches CRUD interface signature delete(T t)
    @Override
    public void delete(RestaurantImage ri) throws SQLException {
        String sql = "DELETE FROM restaurant_image WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, ri.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public RestaurantImage getById(int id) throws SQLException {
        String sql = "SELECT * FROM restaurant_image WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new RestaurantImage(
                            rs.getInt("id"),
                            rs.getString("image_url"),
                            rs.getInt("restaurant_id")
                    );
                }
            }
        }
        return null;
    }

    /**
     * Specialized method (not in CRUD interface) for fetching images by restaurant
     */
    public List<RestaurantImage> getByRestaurantId(int restaurantId) throws SQLException {
        List<RestaurantImage> images = new ArrayList<>();
        String sql = "SELECT * FROM restaurant_image WHERE restaurant_id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, restaurantId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    images.add(new RestaurantImage(
                            rs.getInt("id"),
                            rs.getString("image_url"),
                            rs.getInt("restaurant_id")
                    ));
                }
            }
        }
        return images;
    }
}