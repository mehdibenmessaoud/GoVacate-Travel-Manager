package tn.esprit.projet.services;

import tn.esprit.projet.entities.Menu;
import tn.esprit.projet.utils.MyDBConnexion; // Assure-toi que le chemin est correct

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MenuService implements IService<Menu> {

    private Connection cnx;

    public MenuService() {
        // Récupération de la connexion via le Singleton
        cnx = MyDBConnexion.getInstance().getConnection();
    }

    @Override
    public void create(Menu menu) throws SQLException {
        // Supposition : la table s'appelle "menu" et les colonnes sont en snake_case
        String sql = "INSERT INTO menu (name, description, price, status, restaurant_id) VALUES (?, ?, ?, ?, ?)";

        PreparedStatement ps = cnx.prepareStatement(sql);

        ps.setString(1, menu.getName());
        ps.setString(2, menu.getDescription());
        ps.setBigDecimal(3, menu.getPrice()); // JDBC gère très bien BigDecimal
        ps.setString(4, menu.getStatus());
        ps.setInt(5, menu.getRestaurantId());

        ps.executeUpdate();
    }

    @Override
    public List<Menu> getAll() throws SQLException {
        List<Menu> menus = new ArrayList<>();
        String sql = "SELECT * FROM menu";

        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);

        while (rs.next()) {
            Menu m = new Menu();
            m.setId(rs.getInt("id"));
            m.setName(rs.getString("name"));
            m.setDescription(rs.getString("description"));
            m.setPrice(rs.getBigDecimal("price")); // Récupération du BigDecimal
            m.setStatus(rs.getString("status"));
            m.setRestaurantId(rs.getInt("restaurant_id")); // Attention : nom de colonne SQL

            menus.add(m);
        }

        return menus;
    }

    @Override
    public void update(Menu menu) throws SQLException {
        String sql = "UPDATE menu SET name=?, description=?, price=?, status=?, restaurant_id=? WHERE id=?";

        PreparedStatement ps = cnx.prepareStatement(sql);

        ps.setString(1, menu.getName());
        ps.setString(2, menu.getDescription());
        ps.setBigDecimal(3, menu.getPrice());
        ps.setString(4, menu.getStatus());
        ps.setInt(5, menu.getRestaurantId());

        // Paramètre pour le WHERE
        ps.setInt(6, menu.getId());

        ps.executeUpdate();
    }

    @Override
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM menu WHERE id=?";

        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);

        ps.executeUpdate();
    }

    @Override
    public Menu getById(int id) throws SQLException {
        String sql = "SELECT * FROM menu WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);

        ResultSet rs = ps.executeQuery();

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

        return null;
    }


}