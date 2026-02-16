package tn.esprit.projet.services;

import tn.esprit.projet.entities.HotelImage;
import tn.esprit.projet.utils.MyDBConnexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class HotelImageService implements CRUD<HotelImage> {

    private Connection cnx;

    public HotelImageService() {
        cnx = MyDBConnexion.getInstance().getConnection();
    }

    @Override
    public void create(HotelImage image) throws SQLException {
        String sql = "INSERT INTO hotel_image (imageUrl, hotelId) VALUES (?, ?)";

        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, image.getImageUrl());
        ps.setInt(2, image.getHotelId());

        ps.executeUpdate();
    }

    @Override
    public List<HotelImage> getAll() throws SQLException {
        List<HotelImage> images = new ArrayList<>();
        String sql = "SELECT * FROM hotel_image";

        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);

        while (rs.next()) {
            images.add(new HotelImage(
                    rs.getInt("id"),
                    rs.getString("imageUrl"),
                    rs.getInt("hotelId")
            ));
        }

        return images;
    }

    @Override
    public void update(HotelImage image) throws SQLException {
        String sql = "UPDATE hotel_image SET imageUrl=?, hotelId=? WHERE id=?";

        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, image.getImageUrl());
        ps.setInt(2, image.getHotelId());
        ps.setInt(3, image.getId());

        ps.executeUpdate();
    }

    @Override
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM hotel_image WHERE id=?";

        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);

        ps.executeUpdate();
    }

    @Override
    public HotelImage getById(int id) throws SQLException {
        String sql = "SELECT * FROM hotel_image WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);

        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            return new HotelImage(
                    rs.getInt("id"),
                    rs.getString("imageUrl"),
                    rs.getInt("hotelId")
            );
        }

        return null;
    }

    // Get all images for a specific hotel
    public List<HotelImage> getByHotelId(int hotelId) throws SQLException {
        List<HotelImage> images = new ArrayList<>();
        String sql = "SELECT * FROM hotel_image WHERE hotelId = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, hotelId);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            images.add(new HotelImage(
                    rs.getInt("id"),
                    rs.getString("imageUrl"),
                    rs.getInt("hotelId")
            ));
        }
        return images;
    }
}
