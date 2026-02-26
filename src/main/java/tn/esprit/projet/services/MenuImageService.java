package tn.esprit.projet.services;

import tn.esprit.projet.entities.MenuImage;
import tn.esprit.projet.utils.govacate_connect;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

// Fixed: Implements CRUD with Entity and ID types to solve "required: 2" error
public class MenuImageService implements CRUD<MenuImage, Integer> {

    private final Connection cnx;

    public MenuImageService() {
        cnx = govacate_connect.getInstance().getConnection();
    }

    // Fixed: Return type changed to MenuImage
    @Override
    public MenuImage insert(MenuImage mi) throws SQLException {
        String sql = "INSERT INTO menu_image (image_url, menu_id) VALUES (?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, mi.getImageUrl());
            ps.setInt(2, mi.getMenuId());
            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) mi.setId(rs.getInt(1));
            return mi;
        }
    }

    // Fixed: Return type changed to MenuImage
    @Override
    public MenuImage update(MenuImage mi) throws SQLException {
        String sql = "UPDATE menu_image SET image_url=?, menu_id=? WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, mi.getImageUrl());
            ps.setInt(2, mi.getMenuId());
            ps.setInt(3, mi.getId());
            ps.executeUpdate();
            return mi;
        }
    }

    // Fixed: Parameter changed to Integer to match CRUD<T, ID>
    @Override
    public void delete(Integer id) throws SQLException {
        String sql = "DELETE FROM menu_image WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // Fixed: Removed unused parameter to match interface
    @Override
    public List<MenuImage> selectAll() throws SQLException {
        List<MenuImage> images = new ArrayList<>();
        String sql = "SELECT * FROM menu_image";
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                images.add(mapResultSetToEntity(rs));
            }
        }
        return images;
    }

    @Override
    public MenuImage getById(Integer id) throws SQLException {
        String sql = "SELECT * FROM menu_image WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapResultSetToEntity(rs);
            }
        }
        return null;
    }

    public List<MenuImage> getByMenuId(int menuId) throws SQLException {
        List<MenuImage> images = new ArrayList<>();
        String sql = "SELECT * FROM menu_image WHERE menu_id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, menuId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    images.add(mapResultSetToEntity(rs));
                }
            }
        }
        return images;
    }

    private MenuImage mapResultSetToEntity(ResultSet rs) throws SQLException {
        MenuImage mi = new MenuImage();
        mi.setId(rs.getInt("id"));
        mi.setImageUrl(rs.getString("image_url"));
        mi.setMenuId(rs.getInt("id"));
        return mi;
    }
}