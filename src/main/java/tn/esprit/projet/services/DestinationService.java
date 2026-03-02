package tn.esprit.projet.services;

import tn.esprit.projet.entities.Destination;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DestinationService {
    private Connection connection;

    public DestinationService(Connection connection) {
        this.connection = connection;
    }

    /**
     * Récupère une destination par son ID
     */
    public Destination getById(int id) throws SQLException {
        String query = "SELECT * FROM destination WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Destination(
                            rs.getInt("id"),
                            rs.getString("name_destination"),
                            rs.getString("pays"),
                            rs.getString("ville"),
                            rs.getString("imagedest") // Corrigé selon votre DB
                    );
                }
            }
        }
        return null;
    }

    /**
     * Ajoute une nouvelle destination
     */
    public void create(Destination d) throws SQLException {
        String query = "INSERT INTO destination (name_destination, pays, ville, imagedest) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, d.getNameDestination());
            ps.setString(2, d.getPays());
            ps.setString(3, d.getVille());
            ps.setString(4, d.getImage());
            ps.executeUpdate();
        }
    }

    /**
     * Récupère toutes les destinations
     */
    public List<Destination> getAll() throws SQLException {
        List<Destination> list = new ArrayList<>();
        String query = "SELECT * FROM destination";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                list.add(new Destination(
                        rs.getInt("id"),
                        rs.getString("name_destination"),
                        rs.getString("pays"),
                        rs.getString("ville"),
                        rs.getString("imagedest") // Corrigé selon votre DB
                ));
            }
        }
        return list;
    }

    /**
     * Met à jour une destination existante
     */
    public void update(Destination d) throws SQLException {
        // Correction de la requête : ajout de imagedest et vérification des paramètres
        String query = "UPDATE destination SET name_destination=?, pays=?, ville=?, imagedest=? WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, d.getNameDestination());
            ps.setString(2, d.getPays());
            ps.setString(3, d.getVille());
            ps.setString(4, d.getImage());
            ps.setInt(5, d.getId());

            int rowsUpdated = ps.executeUpdate();
            if (rowsUpdated > 0) {
                System.out.println("✅ Destination mise à jour avec succès !");
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la mise à jour de la destination : " + e.getMessage());
            throw e;
        }
    }

    /**
     * Supprime une destination via son ID
     */
    public void delete(int id) throws SQLException {
        String query = "DELETE FROM destination WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, id);

            int rowsDeleted = ps.executeUpdate();
            if (rowsDeleted > 0) {
                System.out.println("✅ Destination supprimée avec succès !");
            } else {
                System.out.println("⚠️ Aucune destination trouvée avec l'ID : " + id);
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la suppression de la destination : " + e.getMessage());
            throw e;
        }
    }
}