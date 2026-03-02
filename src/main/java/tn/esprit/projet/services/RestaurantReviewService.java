package tn.esprit.projet.services;

import tn.esprit.projet.entities.RestaurantReview;
import tn.esprit.projet.utils.MyDBConnexion;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class RestaurantReviewService implements IService<RestaurantReview> {

    private Connection cnx;

    public RestaurantReviewService() {
        cnx = MyDBConnexion.getInstance().getConnection();
    }

    @Override
    public void create(RestaurantReview review) throws SQLException {
        String sql = "INSERT INTO restaurant_review (rating, comment, user_id, restaurant_id, created_at) VALUES (?, ?, ?, ?, ?)";

        PreparedStatement ps = cnx.prepareStatement(sql);

        ps.setInt(1, review.getRating());
        ps.setString(2, review.getComment());
        ps.setInt(3, review.getUserId());
        ps.setInt(4, review.getRestaurantId());

        // On utilise la date actuelle si review.getCreatedAt() est null
        LocalDateTime date = (review.getCreatedAt() != null) ? review.getCreatedAt() : LocalDateTime.now();
        ps.setTimestamp(5, Timestamp.valueOf(date));

        ps.executeUpdate();
    }

    @Override
    public List<RestaurantReview> getAll() throws SQLException {
        List<RestaurantReview> reviews = new ArrayList<>();
        String sql = "SELECT * FROM restaurant_review";

        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);

        while (rs.next()) {
            RestaurantReview r = new RestaurantReview();
            r.setId(rs.getInt("id"));
            r.setRating(rs.getInt("rating"));
            r.setComment(rs.getString("comment"));
            r.setUserId(rs.getInt("user_id"));
            r.setRestaurantId(rs.getInt("restaurant_id"));

            Timestamp ts = rs.getTimestamp("created_at");
            if (ts != null) {
                r.setCreatedAt(ts.toLocalDateTime());
            }

            reviews.add(r);
        }

        return reviews;
    }

    @Override
    public void update(RestaurantReview review) throws SQLException {
        String sql = "UPDATE restaurant_review SET rating=?, comment=?, user_id=?, restaurant_id=? WHERE id=?";

        PreparedStatement ps = cnx.prepareStatement(sql);

        ps.setInt(1, review.getRating());
        ps.setString(2, review.getComment());
        ps.setInt(3, review.getUserId());
        ps.setInt(4, review.getRestaurantId());
        ps.setInt(5, review.getId());

        ps.executeUpdate();
    }

    @Override
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM restaurant_review WHERE id=?";

        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);

        ps.executeUpdate();
    }

    @Override
    public RestaurantReview getById(int id) throws SQLException {
        String sql = "SELECT * FROM restaurant_review WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);

        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            RestaurantReview r = new RestaurantReview();
            r.setId(rs.getInt("id"));
            r.setRating(rs.getInt("rating"));
            r.setComment(rs.getString("comment"));
            r.setUserId(rs.getInt("user_id"));
            r.setRestaurantId(rs.getInt("restaurant_id"));

            Timestamp ts = rs.getTimestamp("created_at");
            if (ts != null) {
                r.setCreatedAt(ts.toLocalDateTime());
            }

            return r;
        }

        return null;
    }
}