package tn.esprit.projet.services;

import tn.esprit.projet.models.Comment;
import tn.esprit.projet.utils.govacate_connect;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CommentService implements CRUD<Comment> {
    private Connection cnx = govacate_connect.getInstance().getConnection();

    @Override
    public void insert(Comment comment) throws SQLException {
        String req = "INSERT INTO comment (id_blogue, userid, content, createdat) VALUES (?, ?, ?, NOW())";
        PreparedStatement pst = cnx.prepareStatement(req);
        pst.setInt(1, comment.getPostid());
        pst.setInt(2, comment.getUserid());
        pst.setString(3, comment.getContent());
        pst.executeUpdate();
    }

    @Override
    public void update(Comment comment) throws SQLException {
        String req = "UPDATE comment SET content = ? WHERE id = ?";
        PreparedStatement pst = cnx.prepareStatement(req);
        pst.setString(1, comment.getContent());
        pst.setInt(2, comment.getId());
        pst.executeUpdate();
    }

    @Override
    public void delete(Comment comment) throws SQLException {
        String req = "DELETE FROM comment WHERE id = ?";
        PreparedStatement pst = cnx.prepareStatement(req);
        pst.setInt(1, comment.getId());
        pst.executeUpdate();
    }

    @Override
    public List<Comment> selectAll(Comment filter) throws SQLException {
        List<Comment> list = new ArrayList<>();
        // On utilise l'objet filter pour récupérer les coms d'un blog précis
        String req = "SELECT * FROM comment WHERE postid = ? ORDER BY createdat DESC";
        PreparedStatement pst = cnx.prepareStatement(req);
        pst.setInt(1, filter.getPostid());
        ResultSet rs = pst.executeQuery();
        while (rs.next()) {
            Comment c = new Comment();
            c.setId(rs.getInt("id"));
            c.setContent(rs.getString("content"));
            c.setUserid(rs.getInt("userid"));
            list.add(c);
        }
        return list;
    }
}