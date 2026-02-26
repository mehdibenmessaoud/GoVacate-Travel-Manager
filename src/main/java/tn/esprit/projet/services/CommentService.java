package tn.esprit.projet.services;

import tn.esprit.projet.models.Comment;
import tn.esprit.projet.utils.govacate_connect;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

// Fixed: Implements CRUD with both Entity (Comment) and ID type (Integer)
public class CommentService implements CRUD<Comment, Integer> {
    private final Connection cnx = govacate_connect.getInstance().getConnection();

    @Override
    public Comment insert(Comment comment) throws SQLException {
        // Updated to return Comment to match interface return type requirements
        String req = "INSERT INTO comment (id_blogue, userid, content, createdat) VALUES (?, ?, ?, NOW())";
        try (PreparedStatement pst = cnx.prepareStatement(req, Statement.RETURN_GENERATED_KEYS)) {
            pst.setInt(1, comment.getPostid());
            pst.setInt(2, comment.getUserid());
            pst.setString(3, comment.getContent());
            pst.executeUpdate();

            ResultSet rs = pst.getGeneratedKeys();
            if (rs.next()) {
                comment.setId(rs.getInt(1));
            }
            return comment;
        }
    }

    @Override
    public Comment update(Comment comment) throws SQLException {
        // Changed from void to return Comment
        String req = "UPDATE comment SET content = ? WHERE id = ?";
        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setString(1, comment.getContent());
            pst.setInt(2, comment.getId());
            pst.executeUpdate();
            return comment;
        }
    }

    @Override
    public void delete(Integer id) throws SQLException {
        // Changed parameter from Comment to Integer ID to match CRUD<T, ID>
        String req = "DELETE FROM comment WHERE id = ?";
        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setInt(1, id);
            pst.executeUpdate();
        }
    }

    @Override
    public List<Comment> selectAll() throws SQLException {
        // Removed unused parameter to match standard interface signature
        List<Comment> list = new ArrayList<>();
        String req = "SELECT * FROM comment ORDER BY createdat DESC";
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(req)) {
            while (rs.next()) {
                list.add(mapResultSetToComment(rs));
            }
        }
        return list;
    }

    @Override
    public Comment getById(Integer id) throws SQLException {
        String req = "SELECT * FROM comment WHERE id = ?";
        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setInt(1, id);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) return mapResultSetToComment(rs);
            }
        }
        return null;
    }

    // Helper method to keep code clean and reusable
    private Comment mapResultSetToComment(ResultSet rs) throws SQLException {
        Comment c = new Comment();
        c.setId(rs.getInt("id"));
        c.setPostid(rs.getInt("id_blogue"));
        c.setContent(rs.getString("content"));
        c.setUserid(rs.getInt("userid"));
        return c;
    }
}