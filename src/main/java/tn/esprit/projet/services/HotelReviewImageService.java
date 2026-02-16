package tn.esprit.projet.services;

import tn.esprit.projet.entities.HotelReviewImage;
import tn.esprit.projet.utils.MyDBConnexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class HotelReviewImageService implements CRUD<HotelReviewImage> {

    private Connection cnx;

    public HotelReviewImageService() {
        cnx = MyDBConnexion.getInstance().getConnection();
    }

    @Override
    public void create(HotelReviewImage image) throws SQLException {
        String sql = "INSERT INTO hotel_review_image (imageUrl, hotelReviewId) VALUES (?, ?)";

        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, image.getImageUrl());
        ps.setInt(2, image.getHotelReviewId());

        ps.executeUpdate();
    }

    @Override
    public List<HotelReviewImage> getAll() throws SQLException {
        List<HotelReviewImage> images = new ArrayList<>();
        String sql = "SELECT * FROM hotel_review_image";

        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);

        while (rs.next()) {
            images.add(new HotelReviewImage(
                    rs.getInt("id"),
                    rs.getString("imageUrl"),
                    rs.getInt("hotelReviewId")
            ));
        }

        return images;
    }

    @Override
    public void update(HotelReviewImage image) throws SQLException {
        String sql = "UPDATE hotel_review_image SET imageUrl=?, hotelReviewId=? WHERE id=?";

        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, image.getImageUrl());
        ps.setInt(2, image.getHotelReviewId());
        ps.setInt(3, image.getId());

        ps.executeUpdate();
    }

    @Override
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM hotel_review_image WHERE id=?";

        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);

        ps.executeUpdate();
    }

    @Override
    public HotelReviewImage getById(int id) throws SQLException {
        String sql = "SELECT * FROM hotel_review_image WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);

        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            return new HotelReviewImage(
                    rs.getInt("id"),
                    rs.getString("imageUrl"),
                    rs.getInt("hotelReviewId")
            );
        }

        return null;
    }

    // Get all images for a specific review
    public List<HotelReviewImage> getByReviewId(int hotelReviewId) throws SQLException {
        List<HotelReviewImage> images = new ArrayList<>();
        String sql = "SELECT * FROM hotel_review_image WHERE hotelReviewId = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, hotelReviewId);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            images.add(new HotelReviewImage(
                    rs.getInt("id"),
                    rs.getString("imageUrl"),
                    rs.getInt("hotelReviewId")
            ));
        }
        return images;
    }
}
