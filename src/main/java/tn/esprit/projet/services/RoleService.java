package tn.esprit.projet.services;

import tn.esprit.projet.entities.Role;
import tn.esprit.projet.utils.MyDBConnexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RoleService {

    private Connection conn;

    public RoleService() {
        conn = MyDBConnexion.getInstance().getConnection();
    }

    public Role getById(int id) {
        String sql = "SELECT * FROM role WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapResultSet(rs);
            }
        } catch (SQLException e) {
            // Fallback si table role n'existe pas encore
            return id == 1 ? new Role(1, "ADMIN") : new Role(2, "CLIENT");
        }
        return id == 1 ? new Role(1, "ADMIN") : new Role(2, "CLIENT");
    }

    public Role getByName(String nomRole) {
        String sql = "SELECT * FROM role WHERE nom_role = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nomRole);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapResultSet(rs);
            }
        } catch (SQLException e) {
            // Fallback
            if ("ADMIN".equalsIgnoreCase(nomRole)) return new Role(1, "ADMIN");
            if ("CLIENT".equalsIgnoreCase(nomRole)) return new Role(2, "CLIENT");
        }
        if ("ADMIN".equalsIgnoreCase(nomRole)) return new Role(1, "ADMIN");
        if ("CLIENT".equalsIgnoreCase(nomRole)) return new Role(2, "CLIENT");
        return null;
    }

    public List<Role> getAll() {
        List<Role> roles = new ArrayList<>();
        String sql = "SELECT * FROM role";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                roles.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            roles.add(new Role(1, "ADMIN"));
            roles.add(new Role(2, "CLIENT"));
        }
        return roles;
    }

    private Role mapResultSet(ResultSet rs) throws SQLException {
        return new Role(
                rs.getInt("id"),
                rs.getString("nom_role")
        );
    }
}
