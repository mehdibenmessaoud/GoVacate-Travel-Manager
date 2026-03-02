package tn.esprit.projet.services;

import tn.esprit.projet.entities.RestaurantImage;
import tn.esprit.projet.utils.MyDBConnexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RestaurantImageService implements IService<RestaurantImage> {

    private Connection cnx;

    public RestaurantImageService() {
        cnx = MyDBConnexion.getInstance().getConnection();
    }

    @Override
    public void create(RestaurantImage ri) throws SQLException {
        String sql = "INSERT INTO restaurant_image (image_url, restaurant_id) VALUES (?, ?)";
        PreparedStatement ps = cnx.prepareStatement(sql);

        ps.setString(1, ri.getImageUrl());
        ps.setInt(2, ri.getRestaurantId());

        ps.executeUpdate();
    }

    @Override
    public List<RestaurantImage> getAll() throws SQLException {
        List<RestaurantImage> images = new ArrayList<>();
        String sql = "SELECT * FROM restaurant_image";

        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);

        while (rs.next()) {
            RestaurantImage ri = new RestaurantImage();
            ri.setId(rs.getInt("id"));
            ri.setImageUrl(rs.getString("image_url"));
            ri.setRestaurantId(rs.getInt("restaurant_id"));

            images.add(ri);
        }
        return images;
    }

    @Override
    public void update(RestaurantImage ri) throws SQLException {
        String sql = "UPDATE restaurant_image SET image_url=?, restaurant_id=? WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);

        ps.setString(1, ri.getImageUrl());
        ps.setInt(2, ri.getRestaurantId());
        ps.setInt(3, ri.getId());

        ps.executeUpdate();
    }

    @Override
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM restaurant_image WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    @Override
    public RestaurantImage getById(int id) throws SQLException {
        String sql = "SELECT * FROM restaurant_image WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);

        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return new RestaurantImage(
                    rs.getInt("id"),
                    rs.getString("image_url"),
                    rs.getInt("restaurant_id")
            );
        }
        return null;
    }

    /**
     * Méthode utilitaire pour récupérer toutes les images d'un restaurant spécifique
     */
    public List<RestaurantImage> getByRestaurantId(int restaurantId) throws SQLException {
        List<RestaurantImage> images = new ArrayList<>();
        String sql = "SELECT * FROM restaurant_image WHERE restaurant_id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, restaurantId);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            images.add(new RestaurantImage(
                    rs.getInt("id"),
                    rs.getString("image_url"),
                    rs.getInt("restaurant_id")
            ));
        }
        return images;
    }
}