package tn.esprit.projet.services;

import tn.esprit.projet.models.Post;
import tn.esprit.projet.utils.govacate_connect;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

// Fixed: Added Integer as the second type argument
public class BlogueService implements CRUD<Post, Integer> {

    Connection cnx = govacate_connect.getInstance().getConnection();

    // ========================================================================
    // 1. MÉTHODES DE L'INTERFACE CRUD (Corrected Signatures)
    // ========================================================================

    @Override
    public Post insert(Post post) throws SQLException {
        // Updated to return Post to match interface requirements
        String reqBlog = "INSERT INTO blogue (title, content, createdat, updatedat, userid, locationid, status) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pst = cnx.prepareStatement(reqBlog, Statement.RETURN_GENERATED_KEYS)) {
            pst.setString(1, post.getTitle());
            pst.setString(2, post.getContent());
            Date dateActuelle = new Date(System.currentTimeMillis());
            pst.setDate(3, dateActuelle);
            pst.setDate(4, dateActuelle);
            pst.setInt(5, post.getUserid());
            pst.setInt(6, post.getLocationid());
            pst.setString(7, post.getStatus());
            pst.executeUpdate();

            ResultSet rs = pst.getGeneratedKeys();
            if (rs.next()) {
                post.setId(rs.getInt(1));
            }
            return post;
        }
    }

    @Override
    public Post update(Post post) throws SQLException {
        // Changed return type from void to Post
        String req = "UPDATE blogue SET title=?, content=?, updatedat=?, status=? WHERE id_blogue=?";
        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setString(1, post.getTitle());
            pst.setString(2, post.getContent());
            pst.setDate(3, new Date(System.currentTimeMillis()));
            pst.setString(4, post.getStatus());
            pst.setInt(5, post.getId());
            pst.executeUpdate();
            return post;
        }
    }

    @Override
    public void delete(Integer id) throws SQLException {
        // Changed parameter from Post to Integer ID to match CRUD<T, ID>
        String req = "DELETE FROM blogue WHERE id_blogue=?";
        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setInt(1, id);
            pst.executeUpdate();
        }
    }

    @Override
    public List<Post> selectAll() throws SQLException {
        // Removed the 'Post t' parameter to match selectAll() signature
        List<Post> posts = new ArrayList<>();
        String req = "SELECT * FROM blogue";
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(req)) {
            while (rs.next()) {
                posts.add(mapResultSetToPost(rs));
            }
        }
        return posts;
    }

    @Override
    public Post getById(Integer id) throws SQLException {
        String req = "SELECT * FROM blogue WHERE id_blogue=?";
        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setInt(1, id);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) return mapResultSetToPost(rs);
            }
        }
        return null;
    }

    // ========================================================================
    // 2. MÉTHODES SPÉCIFIQUES ET DTO
    // ========================================================================

    private Post mapResultSetToPost(ResultSet rs) throws SQLException {
        return new Post(
                rs.getInt("id_blogue"),
                rs.getString("title"),
                rs.getString("content"),
                rs.getString("status"),
                rs.getDate("createdat"),
                rs.getDate("updatedat"),
                rs.getInt("userid"),
                rs.getInt("locationid")
        );
    }

    // Keep your specialized image methods as they are...
    public void ajouterBlogueAvecPlusieursImages(Post p, List<String> images) throws SQLException {
        insert(p); // Reuse the corrected insert
        String queryImg = "INSERT INTO blogue_images (id_blogue, image_url) VALUES (?, ?)";
        try (PreparedStatement psImg = cnx.prepareStatement(queryImg)) {
            for (String url : images) {
                psImg.setInt(1, p.getId());
                psImg.setString(2, url);
                psImg.addBatch();
            }
            psImg.executeBatch();
        }
    }
}