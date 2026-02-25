package tn.esprit.projet.services;

import tn.esprit.projet.entities.Menu;
import tn.esprit.projet.utils.govacate_connect;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MenuService implements CRUD<Menu> {

    private Connection cnx;

    public MenuService() {
        cnx = govacate_connect.getInstance().getConnection();
    }

    // Renamed from create to insert
    @Override
    public void insert(Menu menu) throws SQLException {
        String sql = "INSERT INTO menu (name, description, price, status, restaurant_id) VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, menu.getName());
            ps.setString(2, menu.getDescription());
            ps.setBigDecimal(3, menu.getPrice());
            ps.setString(4, menu.getStatus());
            ps.setInt(5, menu.getRestaurantId());
            ps.executeUpdate();
        }
    }

    // Renamed from getAll to selectAll and added parameter
    @Override
    public List<Menu> selectAll(Menu unused) throws SQLException {
        List<Menu> menus = new ArrayList<>();
        String sql = "SELECT * FROM menu";

        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Menu m = new Menu();
                m.setId(rs.getInt("id"));
                m.setName(rs.getString("name"));
                m.setDescription(rs.getString("description"));
                m.setPrice(rs.getBigDecimal("price"));
                m.setStatus(rs.getString("status"));
                m.setRestaurantId(rs.getInt("restaurant_id"));
                menus.add(m);
            }
        }
        return menus;
    }

    @Override
    public void update(Menu menu) throws SQLException {
        String sql = "UPDATE menu SET name=?, description=?, price=?, status=?, restaurant_id=? WHERE id=?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, menu.getName());
            ps.setString(2, menu.getDescription());
            ps.setBigDecimal(3, menu.getPrice());
            ps.setString(4, menu.getStatus());
            ps.setInt(5, menu.getRestaurantId());
            ps.setInt(6, menu.getId());
            ps.executeUpdate();
        }
    }

    // Changed parameter from int id to Menu object
    @Override
    public void delete(Menu menu) throws SQLException {
        String sql = "DELETE FROM menu WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, menu.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public Menu getById(int id) throws SQLException {
        String sql = "SELECT * FROM menu WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
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
        }
        return null;
    }
}