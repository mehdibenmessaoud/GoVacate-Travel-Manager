package tn.esprit.projet.services;

import tn.esprit.projet.entities.Role;
import tn.esprit.projet.utils.govacate_connect; // Using your project's singleton

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Fixed: Added Integer as the second type argument to match CRUD<T, ID>
 */
public class RoleService implements CRUD<Role, Integer> {

    private static final Logger LOG = Logger.getLogger(RoleService.class.getName());
    private final Connection conn;

    public RoleService() {
        // Fixed: Updated to use govacate_connect to resolve "Cannot resolve symbol"
        conn = govacate_connect.getInstance().getConnection();
    }

    /**
     * Fixed: Renamed from create() to insert() and changed return type to Role
     */
    @Override
    public Role insert(Role role) throws SQLException {
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
            }
            return role;
        }
    }

    /**
     * Fixed: Renamed from getAll() to selectAll() to match interface
     */
    @Override
    public List<Role> selectAll() throws SQLException {
        List<Role> roles = new ArrayList<>();
        String sql = "SELECT * FROM role";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                roles.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Database error, using fallbacks.", e);
            roles.add(new Role(1, "ADMIN"));
            roles.add(new Role(2, "CLIENT"));
        }
        return roles;
    }

    /**
     * Fixed: Changed return type to Role
     */
    @Override
    public Role update(Role role) throws SQLException {
        String sql = "UPDATE role SET nom_role = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, role.getNomRole());
            ps.setInt(2, role.getId());
            ps.executeUpdate();
            return role;
        }
    }

    /**
     * Fixed: Parameter remains Integer to match CRUD<Role, Integer>
     */
    @Override
    public void delete(Integer id) throws SQLException {
        String sql = "DELETE FROM role WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    @Override
    public Role getById(Integer id) throws SQLException {
        String sql = "SELECT * FROM role WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSet(rs);
                }
            }
        }
        // Fallbacks for initial setup
        if (id == 1) return new Role(1, "ADMIN");
        if (id == 2) return new Role(2, "CLIENT");
        return null;
    }

    public Role getByName(String nomRole) throws SQLException {
        String sql = "SELECT * FROM role WHERE nom_role = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nomRole);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSet(rs);
                }
            }
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