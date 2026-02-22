package tn.esprit.projet.services;

import tn.esprit.projet.models.Like;
import tn.esprit.projet.utils.govacate_connect;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LikeService implements CRUD<Like> {
    private Connection cnx = govacate_connect.getInstance().getConnection();

    @Override
    public void insert(Like like) throws SQLException {
        String req = "INSERT INTO likes (id_blogue, userid) VALUES (?, ?)";
        PreparedStatement pst = cnx.prepareStatement(req);
        pst.setInt(1, like.getPostid());
        pst.setInt(2, like.getUser_id());
        pst.executeUpdate();
    }

    @Override
    public void delete(Like like) throws SQLException {
        String req = "DELETE FROM likes WHERE id_blogue = ? AND userid = ?";
        PreparedStatement pst = cnx.prepareStatement(req);
        pst.setInt(1, like.getPostid());
        pst.setInt(2, like.getUser_id());
        pst.executeUpdate();
    }

    @Override
    public void update(Like like) throws SQLException {
        // Souvent inutile pour un Like (on ne modifie pas un like, on l'enlève ou on l'ajoute)
        throw new UnsupportedOperationException("Update non supporté pour les Likes");
    }

    @Override
    public List<Like> selectAll(Like filter) throws SQLException {
        List<Like> list = new ArrayList<>();
        String req = "SELECT * FROM likes WHERE postid = ?";
        PreparedStatement pst = cnx.prepareStatement(req);
        pst.setInt(1, filter.getPostid());
        ResultSet rs = pst.executeQuery();
        while (rs.next()) {
            list.add(new Like(rs.getInt("id_blogue"), rs.getInt("userid")));
        }
        return list;
    }
}