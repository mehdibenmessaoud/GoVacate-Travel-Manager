package tn.esprit.projet.services;

import tn.esprit.projet.entities.Menu;
import tn.esprit.projet.utils.govacate_connect;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

// Fixed: Now providing both Menu and Integer to match CRUD<T, ID>
public class MenuService implements CRUD<Menu, Integer> {

    private final Connection cnx;

    public MenuService() {
        cnx = govacate_connect.getInstance().getConnection();
    }

    @Override
    public Menu insert(Menu menu) throws SQLException {
        String sql = "INSERT INTO menu (name, description, price, status, restaurant_id) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, menu.getName());
            ps.setString(2, menu.getDescription());
            ps.setBigDecimal(3, menu.getPrice());
            ps.setString(4, menu.getStatus());
            ps.setInt(5, menu.getRestaurantId());
            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) menu.setId(rs.getInt(1));
            return menu;
        }
    }

    @Override
    public Menu update(Menu menu) throws SQLException {
        String sql = "UPDATE menu SET name=?, description=?, price=?, status=?, restaurant_id=? WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, menu.getName());
            ps.setString(2, menu.getDescription());
            ps.setBigDecimal(3, menu.getPrice());
            ps.setString(4, menu.getStatus());
            ps.setInt(5, menu.getRestaurantId());
            ps.setInt(6, menu.getId());
            ps.executeUpdate();
            return menu;
        }
    }

    // Fixed: Matches the delete(ID id) signature from the interface
    @Override
    public void delete(Integer id) throws SQLException {
        String sql = "DELETE FROM menu WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    @Override
    public List<Menu> selectAll() throws SQLException {
        List<Menu> menus = new ArrayList<>();
        String sql = "SELECT * FROM menu";
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                menus.add(mapResultSetToMenu(rs));
            }
        }
        return menus;
    }

    @Override
    public Menu getById(Integer id) throws SQLException {
        String sql = "SELECT * FROM menu WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapResultSetToMenu(rs);
            }
        }
        return null;
    }

    // Helper to keep code clean
    private Menu mapResultSetToMenu(ResultSet rs) throws SQLException {
        Menu m = new Menu();
        m.setId(rs.getInt("id"));
        m.setName(rs.getString("name"));
        m.setDescription(rs.getString("description"));
        m.setPrice(rs.getBigDecimal("price"));
        m.setStatus(rs.getString("status"));
        m.setRestaurantId(rs.getInt("restaurant_id"));
        return m;
    }
}