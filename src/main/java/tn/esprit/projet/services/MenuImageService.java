package tn.esprit.projet.services;

import tn.esprit.projet.entities.MenuImage;
import tn.esprit.projet.utils.MyDBConnexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MenuImageService implements IService<MenuImage> {

    private Connection cnx;

    public MenuImageService() {
        cnx = MyDBConnexion.getInstance().getConnection();
    }

    @Override
    public void create(MenuImage mi) throws SQLException {
        String sql = "INSERT INTO menu_image (image_url, menu_id) VALUES (?, ?)";
        PreparedStatement ps = cnx.prepareStatement(sql);

        ps.setString(1, mi.getImageUrl());
        ps.setInt(2, mi.getMenuId());

        ps.executeUpdate();
    }

    @Override
    public List<MenuImage> getAll() throws SQLException {
        List<MenuImage> images = new ArrayList<>();
        String sql = "SELECT * FROM menu_image";

        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);

        while (rs.next()) {
            MenuImage mi = new MenuImage();
            mi.setId(rs.getInt("id"));
            mi.setImageUrl(rs.getString("image_url"));
            mi.setMenuId(rs.getInt("menu_id"));

            images.add(mi);
        }
        return images;
    }

    @Override
    public void update(MenuImage mi) throws SQLException {
        String sql = "UPDATE menu_image SET image_url=?, menu_id=? WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);

        ps.setString(1, mi.getImageUrl());
        ps.setInt(2, mi.getMenuId());
        ps.setInt(3, mi.getId());

        ps.executeUpdate();
    }

    @Override
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM menu_image WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    @Override
    public MenuImage getById(int id) throws SQLException {
        String sql = "SELECT * FROM menu_image WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);

        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return new MenuImage(
                    rs.getInt("id"),
                    rs.getString("image_url"),
                    rs.getInt("menu_id")
            );
        }
        return null;
    }

    /**
     * Utile pour récupérer toutes les photos d'un plat spécifique
     */
    public List<MenuImage> getByMenuId(int menuId) throws SQLException {
        List<MenuImage> images = new ArrayList<>();
        String sql = "SELECT * FROM menu_image WHERE menu_id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, menuId);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            images.add(new MenuImage(
                    rs.getInt("id"),
                    rs.getString("image_url"),
                    rs.getInt("menu_id")
            ));
        }
        return images;
    }
}