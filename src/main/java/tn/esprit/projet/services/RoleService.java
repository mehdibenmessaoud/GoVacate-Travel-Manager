package tn.esprit.projet.services;

import tn.esprit.projet.entities.Role;
import tn.esprit.projet.utils.MyDBConnexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RoleService implements IService<Role> {

    private static final Logger LOG = Logger.getLogger(RoleService.class.getName());
    private Connection conn;

    public RoleService() {
        conn = MyDBConnexion.getInstance().getConnection();
    }

    @Override
    public void create(Role role) throws SQLException {
        String sql = "INSERT INTO role (nom_role) VALUES (?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, role.getNomRole());
            int affectedRows = ps.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        role.setId(rs.getInt(1));
                    }
                }
            } else {
                throw new SQLException("Creating role failed, no ID obtained.");
            }
        }
    }

    @Override
    public List<Role> getAll() throws SQLException {
        List<Role> roles = new ArrayList<>();
        String sql = "SELECT * FROM role";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                roles.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Erreur lors de la récupération des rôles, utilisation des rôles par défaut.", e);
            // Fallback if role table doesn't exist or is empty
            roles.add(new Role(1, "ADMIN"));
            roles.add(new Role(2, "CLIENT"));
        }
        return roles;
    }

    @Override
    public void update(Role role) throws SQLException {
        String sql = "UPDATE role SET nom_role = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, role.getNomRole());
            ps.setInt(2, role.getId());
            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Updating role failed, no rows affected.");
            }
        }
    }

    @Override
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM role WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Deleting role failed, no rows affected.");
            }
        }
    }

    @Override
    public Role getById(int id) throws SQLException {
        String sql = "SELECT * FROM role WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapResultSet(rs);
            }
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Erreur lors de la récupération du rôle par ID, utilisation des rôles par défaut.", e);
            // Fallback si table role n'existe pas encore
            if (id == 1) return new Role(1, "ADMIN");
            if (id == 2) return new Role(2, "CLIENT");
            throw e; // Re-throw if not a default ID
        }
        // Fallback for default IDs if not found in DB
        if (id == 1) return new Role(1, "ADMIN");
        if (id == 2) return new Role(2, "CLIENT");
        return null;
    }

    public Role getByName(String nomRole) throws SQLException {
        String sql = "SELECT * FROM role WHERE nom_role = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nomRole);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapResultSet(rs);
            }
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Erreur lors de la récupération du rôle par nom, utilisation des rôles par défaut.", e);
            // Fallback
            if ("ADMIN".equalsIgnoreCase(nomRole)) return new Role(1, "ADMIN");
            if ("CLIENT".equalsIgnoreCase(nomRole)) return new Role(2, "CLIENT");
            throw e; // Re-throw if not a default name
        }
        if ("ADMIN".equalsIgnoreCase(nomRole)) return new Role(1, "ADMIN");
        if ("CLIENT".equalsIgnoreCase(nomRole)) return new Role(2, "CLIENT");
        return null;
    }

    private Role mapResultSet(ResultSet rs) throws SQLException {
        return new Role(
            rs.getInt("id"),
            rs.getString("nom_role")
        );
    }
}
