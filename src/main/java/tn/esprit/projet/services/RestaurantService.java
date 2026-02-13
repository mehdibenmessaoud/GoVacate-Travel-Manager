package tn.esprit.projet.services;

import tn.esprit.projet.entities.Restaurant;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


import tn.esprit.projet.utils.MyDBConnexion;

public class RestaurantService implements IService<Restaurant> {

    // Variable de connexion
    private Connection cnx;

    public RestaurantService() {
        // Récupération de l'instance de connexion
        cnx = MyDBConnexion.getInstance().getConnection();

        // Note: Si tu n'as pas de classe Singleton, tu peux initialiser ta connexion ici
        // ou la passer dans le constructeur.
    }

    @Override
    public void create(Restaurant t) throws SQLException {
        // On suppose que 'id' est AUTO_INCREMENT dans la base, donc on ne l'insère pas.
        // On suppose que les colonnes en base sont en snake_case (ex: destination_id)
        String sql = "INSERT INTO restaurant (name, category, address, phone, email, capacity, status, destination_id, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        PreparedStatement ps = cnx.prepareStatement(sql);

        ps.setString(1, t.getName());
        ps.setString(2, t.getCategory());
        ps.setString(3, t.getAddress());
        ps.setString(4, t.getPhone());
        ps.setString(5, t.getEmail());
        ps.setInt(6, t.getCapacity());
        ps.setString(7, t.getStatus());
        ps.setInt(8, t.getDestinationId());

        // Gestion des dates (LocalDateTime -> Timestamp SQL)
        // Si createdAt est null, on met la date actuelle
        LocalDateTime created = (t.getCreatedAt() != null) ? t.getCreatedAt() : LocalDateTime.now();
        ps.setTimestamp(9, Timestamp.valueOf(created));

        // Pour updatedAt lors de la création, souvent idem creation ou null
        ps.setTimestamp(10, Timestamp.valueOf(created));

        ps.executeUpdate();
    }

    @Override
    public List<Restaurant> getAll() throws SQLException {
        List<Restaurant> restaurants = new ArrayList<>();
        String sql = "SELECT * FROM restaurant";

        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);

        while (rs.next()) {
            Restaurant r = new Restaurant();

            r.setId(rs.getInt("id"));
            r.setName(rs.getString("name"));
            r.setCategory(rs.getString("category"));
            r.setAddress(rs.getString("address"));
            r.setPhone(rs.getString("phone"));
            r.setEmail(rs.getString("email"));
            r.setCapacity(rs.getInt("capacity"));
            r.setStatus(rs.getString("status"));
            r.setDestinationId(rs.getInt("destination_id")); // Attention au nom de colonne en base

            // Conversion Timestamp SQL -> LocalDateTime Java
            Timestamp createdAt = rs.getTimestamp("created_at");
            if (createdAt != null) r.setCreatedAt(createdAt.toLocalDateTime());

            Timestamp updatedAt = rs.getTimestamp("updated_at");
            if (updatedAt != null) r.setUpdatedAt(updatedAt.toLocalDateTime());

            restaurants.add(r);
        }

        return restaurants;
    }

    @Override
    public void update(Restaurant t) throws SQLException {
        String sql = "UPDATE restaurant SET name=?, category=?, address=?, phone=?, email=?, capacity=?, status=?, destination_id=?, updated_at=? WHERE id=?";

        PreparedStatement ps = cnx.prepareStatement(sql);

        ps.setString(1, t.getName());
        ps.setString(2, t.getCategory());
        ps.setString(3, t.getAddress());
        ps.setString(4, t.getPhone());
        ps.setString(5, t.getEmail());
        ps.setInt(6, t.getCapacity());
        ps.setString(7, t.getStatus());
        ps.setInt(8, t.getDestinationId());

        // Mise à jour de la date de modification
        ps.setTimestamp(9, Timestamp.valueOf(LocalDateTime.now()));

        // Clause WHERE id = ...
        ps.setInt(10, t.getId());

        ps.executeUpdate();
    }

    @Override
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM restaurant WHERE id = ?";

        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);

        ps.executeUpdate();
    }

    @Override
    public Restaurant getById(int id) throws SQLException {
        String sql = "SELECT * FROM restaurant WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);

        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            Restaurant r = new Restaurant();
            r.setId(rs.getInt("id"));
            r.setName(rs.getString("name"));
            r.setCategory(rs.getString("category"));
            r.setAddress(rs.getString("address"));
            r.setPhone(rs.getString("phone"));
            r.setEmail(rs.getString("email"));
            r.setCapacity(rs.getInt("capacity"));
            r.setStatus(rs.getString("status"));
            r.setDestinationId(rs.getInt("destination_id"));

            Timestamp createdAt = rs.getTimestamp("created_at");
            if (createdAt != null) r.setCreatedAt(createdAt.toLocalDateTime());

            Timestamp updatedAt = rs.getTimestamp("updated_at");
            if (updatedAt != null) r.setUpdatedAt(updatedAt.toLocalDateTime());

            return r;
        }

        return null; // Ou lancer une exception si non trouvé
    }

}