package tn.esprit.projet.services;

import tn.esprit.projet.entities.ReviewImage;
import tn.esprit.projet.utils.MyDBConnexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReviewImageService implements IService<ReviewImage> {

    private Connection cnx;

    public ReviewImageService() {
        cnx = MyDBConnexion.getInstance().getConnection();
    }

    @Override
    public void create(ReviewImage ri) throws SQLException {
        // Updated table name to restaurant_review_image
        String sql = "INSERT INTO restaurant_review_image (image_url, review_id) VALUES (?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, ri.getImageUrl());
            ps.setInt(2, ri.getReviewId());
            ps.executeUpdate();
        }
    }

    @Override
    public List<ReviewImage> getAll() throws SQLException {
        List<ReviewImage> images = new ArrayList<>();
        // Updated table name to restaurant_review_image
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
            System.err.println("Critical Error in ReviewImageService.getAll: " + e.getMessage());
            // We throw the exception so the controller knows the load failed,
            // but the try-with-resources ensures we don't leak memory.
            throw e;
        }
        return images;
    }

    @Override
    public void update(ReviewImage ri) throws SQLException {
        // Updated table name to restaurant_review_image
        String sql = "UPDATE restaurant_review_image SET image_url=?, review_id=? WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, ri.getImageUrl());
            ps.setInt(2, ri.getReviewId());
            ps.setInt(3, ri.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(int id) throws SQLException {
        // Updated table name to restaurant_review_image
        String sql = "DELETE FROM restaurant_review_image WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    @Override
    public ReviewImage getById(int id) throws SQLException {
        // Updated table name to restaurant_review_image
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