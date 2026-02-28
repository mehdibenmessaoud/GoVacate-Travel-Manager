package tn.esprit.projet.services;

import tn.esprit.projet.entities.User;
import tn.esprit.projet.utils.MyDBConnexion; // Assuming this is your connection helper

import java.sql.*;

public class UserService {

    private Connection connection;

    public UserService() {
        connection = MyDBConnexion.getInstance().getConnection();
    }

    /**
     * Fetches a user by their ID from the database.
     * This will include the 'position' column (lat,lon).
     */
    public User getById(int id) {
        String query = "SELECT * FROM utilisateur WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                // Map the ResultSet to your User object
                User user = new User(
                        rs.getInt("id"),
                        rs.getString("nom"),
                        rs.getString("email"),
                        rs.getString("password"),
                        null, // Role will be handled by setRoleId below
                        rs.getString("telephone"),
                        rs.getDate("date_naissance") != null ? rs.getDate("date_naissance").toLocalDate() : null,
                        rs.getString("status"),
                        rs.getTimestamp("last_login") != null ? rs.getTimestamp("last_login").toLocalDateTime() : null,
                        rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null
                );

                // Set the role using your helper
                user.setRoleId(rs.getInt("role_id"));

                // Set the new position string (lat,lon)
                user.setPosition(rs.getString("position"));

                return user;
            }
        } catch (SQLException e) {
            System.err.println("Error fetching user: " + e.getMessage());
        }
        return null;
    }

    public void updatePosition(int userId, String newPosition) {
        String sql = "UPDATE utilisateur SET position = ? WHERE id = ?";

        // We use a try-with-resources to manage the connection/statement
        try (java.sql.PreparedStatement ps = tn.esprit.projet.utils.MyDBConnexion.getInstance().getConnection().prepareStatement(sql)) {
            ps.setString(1, newPosition);
            ps.setInt(2, userId);

            int rowsUpdated = ps.executeUpdate();
            if (rowsUpdated > 0) {
                System.out.println("✅ Position updated in Database for User ID: " + userId);
            }
        } catch (java.sql.SQLException e) {
            // Since you mentioned being Read-Only, this catch block is vital
            System.err.println("⚠️ Database Update Failed (Likely Read-Only): " + e.getMessage());
        }
    }

}