package tn.esprit.projet.services;

import tn.esprit.projet.entities.ReviewImage;
import tn.esprit.projet.utils.govacate_connect; // Updated to match your singleton

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReviewImageService implements CRUD<ReviewImage> {

    private Connection cnx;

    public ReviewImageService() {
        // Corrected to use the singleton class you imported
        cnx = govacate_connect.getInstance().getConnection();
    }

    @Override
    public void insert(ReviewImage ri) throws SQLException {
        String sql = "INSERT INTO restaurant_review_image (image_url, review_id) VALUES (?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, ri.getImageUrl());
            ps.setInt(2, ri.getReviewId());
            ps.executeUpdate();
        }
    }

    @Override
    public List<ReviewImage> selectAll(ReviewImage unused) throws SQLException {
        List<ReviewImage> images = new ArrayList<>();
        String sql = "SELECT * FROM restaurant_review_image";

        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                images.add(new ReviewImage(
                        rs.getInt("id"),
                        rs.getString("image_url"),
                        rs.getInt("review_id")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Critical Error in ReviewImageService.selectAll: " + e.getMessage());
            throw e;
        }
        return images;
    }

    @Override
    public void update(ReviewImage ri) throws SQLException {
        String sql = "UPDATE restaurant_review_image SET image_url=?, review_id=? WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, ri.getImageUrl());
            ps.setInt(2, ri.getReviewId());
            ps.setInt(3, ri.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(ReviewImage ri) throws SQLException {
        String sql = "DELETE FROM restaurant_review_image WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, ri.getId()); // Using the ID from the object to match interface
            ps.executeUpdate();
        }
    }

    @Override
    public ReviewImage getById(int id) throws SQLException {
        String sql = "SELECT * FROM restaurant_review_image WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new ReviewImage(
                            rs.getInt("id"),
                            rs.getString("image_url"),
                            rs.getInt("review_id")
                    );
                }
            }
        }
        return null;
    }
}