package tn.esprit.projet.services;

import tn.esprit.projet.models.Like;
import tn.esprit.projet.utils.govacate_connect;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

// Fixed: Added Integer as the second type argument to match CRUD<T, ID>
public class LikeService implements CRUD<Like, Integer> {
    private final Connection cnx = govacate_connect.getInstance().getConnection();

    @Override
    public Like insert(Like like) throws SQLException {
        // Changed return type from void to Like to match interface
        String req = "INSERT INTO likes (id_blogue, userid) VALUES (?, ?)";
        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setInt(1, like.getPostid());
            pst.setInt(2, like.getUser_id());
            pst.executeUpdate();
            return like;
        }
    }

    @Override
    public void delete(Integer id) throws SQLException {
        // Updated parameter to Integer to match the generic ID type in the interface
        String req = "DELETE FROM likes WHERE id = ?";
        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setInt(1, id);
            pst.executeUpdate();
        }
    }

    // Specialized delete for Like logic (PostID + UserID)
    public void deleteLike(Like like) throws SQLException {
        String req = "DELETE FROM likes WHERE id_blogue = ? AND userid = ?";
        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setInt(1, like.getPostid());
            pst.setInt(2, like.getUser_id());
            pst.executeUpdate();
        }
    }

    @Override
    public Like update(Like like) throws SQLException {
        // Changed from void to return Like
        throw new UnsupportedOperationException("Update non supporté pour les Likes");
    }

    @Override
    public List<Like> selectAll() throws SQLException {
        // Removed parameter 'Like filter' to match interface signature
        List<Like> list = new ArrayList<>();
        String req = "SELECT * FROM likes";
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(req)) {
            while (rs.next()) {
                list.add(new Like(rs.getInt("id_blogue"), rs.getInt("userid")));
            }
        }
        return list;
    }

    @Override
    public Like getById(Integer id) throws SQLException {
        // Added to satisfy interface requirements
        String req = "SELECT * FROM likes WHERE id = ?";
        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setInt(1, id);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) return new Like(rs.getInt("id_blogue"), rs.getInt("userid"));
            }
        }
        return null;
    }
}