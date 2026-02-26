package tn.esprit.projet.services;

import tn.esprit.projet.entities.ReviewImage;
import tn.esprit.projet.utils.govacate_connect;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

// Fixed: Implements CRUD with both ReviewImage and Integer
public class ReviewImageService implements CRUD<ReviewImage, Integer> {

    private final Connection cnx;

    public ReviewImageService() {
        cnx = govacate_connect.getInstance().getConnection();
    }

    // Fixed: Changed return type from void to ReviewImage
    @Override
    public ReviewImage insert(ReviewImage ri) throws SQLException {
        String sql = "INSERT INTO restaurant_review_image (image_url, review_id) VALUES (?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, ri.getImageUrl());
            ps.setInt(2, ri.getReviewId());
            ps.executeUpdate();

            // Retrieve generated ID to keep the object complete
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) ri.setId(rs.getInt(1));
            return ri;
        }
    }

    // Fixed: Changed return type from void to ReviewImage
    @Override
    public ReviewImage update(ReviewImage ri) throws SQLException {
        String sql = "UPDATE restaurant_review_image SET image_url=?, review_id=? WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, ri.getImageUrl());
            ps.setInt(2, ri.getReviewId());
            ps.setInt(3, ri.getId());
            ps.executeUpdate();
            return ri;
        }
    }

    // Fixed: Changed parameter from ReviewImage object to Integer ID to match CRUD<T, ID>
    @Override
    public void delete(Integer id) throws SQLException {
        String sql = "DELETE FROM restaurant_review_image WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // Fixed: Removed the parameter to match the interface selectAll() signature
    @Override
    public List<ReviewImage> selectAll() throws SQLException {
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
        }
        return images;
    }

    @Override
    public ReviewImage getById(Integer id) throws SQLException {
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