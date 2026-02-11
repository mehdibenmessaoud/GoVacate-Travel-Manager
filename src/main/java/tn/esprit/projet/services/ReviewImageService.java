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
        String sql = "INSERT INTO review_image (image_url, review_id) VALUES (?, ?)";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, ri.getImageUrl());
        ps.setInt(2, ri.getReviewId());
        ps.executeUpdate();
    }

    @Override
    public List<ReviewImage> getAll() throws SQLException {
        List<ReviewImage> images = new ArrayList<>();
        String sql = "SELECT * FROM review_image";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);

        while (rs.next()) {
            images.add(new ReviewImage(
                    rs.getInt("id"),
                    rs.getString("image_url"),
                    rs.getInt("review_id")
            ));
        }
        return images;
    }

    @Override
    public void update(ReviewImage ri) throws SQLException {
        String sql = "UPDATE review_image SET image_url=?, review_id=? WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, ri.getImageUrl());
        ps.setInt(2, ri.getReviewId());
        ps.setInt(3, ri.getId());
        ps.executeUpdate();
    }

    @Override
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM review_image WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    @Override
    public ReviewImage getById(int id) throws SQLException {
        String sql = "SELECT * FROM review_image WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            return new ReviewImage(
                    rs.getInt("id"),
                    rs.getString("image_url"),
                    rs.getInt("review_id")
            );
        }
        return null;
    }
}