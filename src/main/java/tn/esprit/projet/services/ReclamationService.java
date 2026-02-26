package tn.esprit.projet.services;

import tn.esprit.projet.models.Reclamation;
import tn.esprit.projet.utils.govacate_connect;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

// Fixed: Implements CRUD with both Reclamation and Integer to satisfy generic requirements
public class ReclamationService implements CRUD<Reclamation, Integer> {

    private final Connection cnx;

    public ReclamationService() {
        this.cnx = govacate_connect.getInstance().getConnection();
    }

    @Override
    public Reclamation insert(Reclamation r) throws SQLException {
        // Changed return type from void to Reclamation
        String req = "INSERT INTO reclamation (subject, description, userid, postid) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(req, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, r.getSubject());
            ps.setString(2, r.getDescription());
            ps.setInt(3, r.getUserid());
            ps.setInt(4, r.getPostid());
            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                r.setId(rs.getInt(1));
            }
            return r;
        }
    }

    @Override
    public Reclamation update(Reclamation r) throws SQLException {
        // Changed return type from void to Reclamation
        String req = "UPDATE reclamation SET subject = ?, description = ? WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setString(1, r.getSubject());
            ps.setString(2, r.getDescription());
            ps.setInt(3, r.getId());
            ps.executeUpdate();
            return r;
        }
    }

    @Override
    public void delete(Integer id) throws SQLException {
        // Fixed: Changed parameter from Reclamation to Integer ID
        String req = "DELETE FROM reclamation WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    @Override
    public List<Reclamation> selectAll() throws SQLException {
        // Fixed: Removed parameter to match interface signature
        List<Reclamation> list = new ArrayList<>();
        String req = "SELECT * FROM reclamation";
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(req)) {
            while (rs.next()) {
                list.add(mapResultSetToReclamation(rs));
            }
        }
        return list;
    }

    @Override
    public Reclamation getById(Integer id) throws SQLException {
        String req = "SELECT * FROM reclamation WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapResultSetToReclamation(rs);
            }
        }
        return null;
    }

    private Reclamation mapResultSetToReclamation(ResultSet rs) throws SQLException {
        Reclamation r = new Reclamation();
        r.setId(rs.getInt("id"));
        r.setSubject(rs.getString("subject"));
        r.setDescription(rs.getString("description"));
        r.setUserid(rs.getInt("userid"));
        r.setPostid(rs.getInt("postid"));
        return r;
    }
}