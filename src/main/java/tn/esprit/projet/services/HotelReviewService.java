package tn.esprit.projet.services;

import tn.esprit.projet.entities.HotelReview;
import tn.esprit.projet.API.reviews.ReviewIntelligenceService;
import tn.esprit.projet.utils.MyDBConnexion;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for managing HotelReview entities.
 * Provides SQL methods for single queries and Stream-based helper for multiple filters.
 */
public class HotelReviewService implements CRUD<HotelReview> {

    private final Connection cnx;
    private final ReviewIntelligenceService reviewIntelligenceService;

    public HotelReviewService() {
        cnx = MyDBConnexion.getInstance().getConnection();
        reviewIntelligenceService = new ReviewIntelligenceService();
    }

    @Override
    public void create(HotelReview review) throws SQLException {
        createWithAiProcessing(review);
    }

    public ReviewIntelligenceService.ReviewProcessingResult createWithAiProcessing(HotelReview review) throws SQLException {
        ReviewIntelligenceService.ReviewProcessingResult processing = reviewIntelligenceService.processReview(review.getComment());
        review.setComment(processing.finalComment());
        insertReview(review);
        return processing;
    }

    private void insertReview(HotelReview review) throws SQLException {
        String sql = "INSERT INTO hotel_review (rating, comment, userId, hotelId, createdAt) VALUES (?, ?, ?, ?, ?)";

        PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

        ps.setInt(1, review.getRating());
        ps.setString(2, review.getComment());
        ps.setInt(3, review.getUserId());
        ps.setInt(4, review.getHotelId());

        LocalDateTime date = (review.getCreatedAt() != null) ? review.getCreatedAt() : LocalDateTime.now();
        ps.setTimestamp(5, Timestamp.valueOf(date));

        ps.executeUpdate();
        try (ResultSet keys = ps.getGeneratedKeys()) {
            if (keys.next()) {
                review.setId(keys.getInt(1));
            }
        }
    }

    public ReviewIntelligenceService.ReviewProcessingResult previewReviewProcessing(String comment) {
        return reviewIntelligenceService.processReview(comment);
    }

    @Override
    public List<HotelReview> getAll() throws SQLException {
        List<HotelReview> reviews = new ArrayList<>();
        String sql = "SELECT * FROM hotel_review";

        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);

        while (rs.next()) {
            HotelReview r = new HotelReview();
            r.setId(rs.getInt("id"));
            r.setRating(rs.getInt("rating"));
            r.setComment(rs.getString("comment"));
            r.setUserId(rs.getInt("userId"));
            r.setHotelId(rs.getInt("hotelId"));

            Timestamp ts = rs.getTimestamp("createdAt");
            if (ts != null) {
                r.setCreatedAt(ts.toLocalDateTime());
            }

            reviews.add(r);
        }

        return reviews;
    }

    @Override
    public void update(HotelReview review) throws SQLException {
        String sql = "UPDATE hotel_review SET rating=?, comment=?, userId=?, hotelId=? WHERE id=?";

        PreparedStatement ps = cnx.prepareStatement(sql);

        ps.setInt(1, review.getRating());
        ps.setString(2, review.getComment());
        ps.setInt(3, review.getUserId());
        ps.setInt(4, review.getHotelId());
        ps.setInt(5, review.getId());

        ps.executeUpdate();
    }

    @Override
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM hotel_review WHERE id=?";

        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);

        ps.executeUpdate();
    }

    @Override
    public HotelReview getById(int id) throws SQLException {
        String sql = "SELECT * FROM hotel_review WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);

        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            HotelReview r = new HotelReview();
            r.setId(rs.getInt("id"));
            r.setRating(rs.getInt("rating"));
            r.setComment(rs.getString("comment"));
            r.setUserId(rs.getInt("userId"));
            r.setHotelId(rs.getInt("hotelId"));

            Timestamp ts = rs.getTimestamp("createdAt");
            if (ts != null) {
                r.setCreatedAt(ts.toLocalDateTime());
            }

            return r;
        }

        return null;
    }

    // ==========================================
    // SQL-BASED METHODS (for single queries)
    // ==========================================

    public List<HotelReview> getReviewsByHotel(int hotelId) throws SQLException {
        List<HotelReview> reviews = new ArrayList<>();
        String sql = "SELECT * FROM hotel_review WHERE hotelId = ? ORDER BY createdAt DESC";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, hotelId);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            HotelReview r = new HotelReview();
            r.setId(rs.getInt("id"));
            r.setRating(rs.getInt("rating"));
            r.setComment(rs.getString("comment"));
            r.setUserId(rs.getInt("userId"));
            r.setHotelId(rs.getInt("hotelId"));

            Timestamp ts = rs.getTimestamp("createdAt");
            if (ts != null) {
                r.setCreatedAt(ts.toLocalDateTime());
            }

            reviews.add(r);
        }
        return reviews;
    }

    public double getAverageRating(int hotelId) throws SQLException {
        String sql = "SELECT AVG(rating) as avgRating FROM hotel_review WHERE hotelId = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, hotelId);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            return rs.getDouble("avgRating");
        }
        return 0.0;
    }

    public int countReviewsByHotel(int hotelId) throws SQLException {
        String sql = "SELECT COUNT(*) as total FROM hotel_review WHERE hotelId = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, hotelId);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            return rs.getInt("total");
        }
        return 0;
    }

    // ==========================================
    // STREAM-BASED METHODS (for multiple filters)
    // ==========================================

    public HotelReviewSearchHelper createSearchHelper() throws SQLException {
        return new HotelReviewSearchHelper(getAll());
    }

    public static class HotelReviewSearchHelper {
        private final List<HotelReview> reviews;

        public HotelReviewSearchHelper(List<HotelReview> reviews) {
            this.reviews = reviews;
        }

        public List<HotelReview> getAll() {
            return new ArrayList<>(reviews);
        }

        public List<HotelReview> filterByHotel(int hotelId) {
            return reviews.stream()
                    .filter(r -> r.getHotelId() == hotelId)
                    .collect(Collectors.toList());
        }

        public List<HotelReview> filterByUser(int userId) {
            return reviews.stream()
                    .filter(r -> r.getUserId() == userId)
                    .collect(Collectors.toList());
        }

        public List<HotelReview> filterByRating(int rating) {
            return reviews.stream()
                    .filter(r -> r.getRating() == rating)
                    .collect(Collectors.toList());
        }

        public List<HotelReview> filterByMinRating(int minRating) {
            return reviews.stream()
                    .filter(r -> r.getRating() >= minRating)
                    .collect(Collectors.toList());
        }

        public List<HotelReview> filterRecent(int days) {
            LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
            return reviews.stream()
                    .filter(r -> r.getCreatedAt() != null && r.getCreatedAt().isAfter(cutoff))
                    .collect(Collectors.toList());
        }

        public List<HotelReview> searchByComment(String keyword) {
            String lowerKeyword = keyword.toLowerCase();
            return reviews.stream()
                    .filter(r -> r.getComment() != null && r.getComment().toLowerCase().contains(lowerKeyword))
                    .collect(Collectors.toList());
        }

        public List<HotelReview> filterAdvanced(Integer hotelId, Integer userId, Integer minRating, Integer maxRating, Integer lastDays) {
            LocalDateTime cutoff = (lastDays != null) ? LocalDateTime.now().minusDays(lastDays) : null;

            return reviews.stream()
                    .filter(r -> hotelId == null || r.getHotelId() == hotelId)
                    .filter(r -> userId == null || r.getUserId() == userId)
                    .filter(r -> minRating == null || r.getRating() >= minRating)
                    .filter(r -> maxRating == null || r.getRating() <= maxRating)
                    .filter(r -> cutoff == null || (r.getCreatedAt() != null && r.getCreatedAt().isAfter(cutoff)))
                    .collect(Collectors.toList());
        }

        public List<HotelReview> sortByDateDesc() {
            return reviews.stream()
                    .sorted(Comparator.comparing(HotelReview::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                    .collect(Collectors.toList());
        }

        public List<HotelReview> sortByRatingDesc() {
            return reviews.stream()
                    .sorted(Comparator.comparingInt(HotelReview::getRating).reversed())
                    .collect(Collectors.toList());
        }

        public double getAverageRatingByHotel(int hotelId) {
            return reviews.stream()
                    .filter(r -> r.getHotelId() == hotelId)
                    .mapToInt(HotelReview::getRating)
                    .average()
                    .orElse(0.0);
        }

        public long countByHotel(int hotelId) {
            return reviews.stream()
                    .filter(r -> r.getHotelId() == hotelId)
                    .count();
        }

        public Map<Integer, Long> getRatingDistributionByHotel(int hotelId) {
            return reviews.stream()
                    .filter(r -> r.getHotelId() == hotelId)
                    .collect(Collectors.groupingBy(HotelReview::getRating, Collectors.counting()));
        }

        public Map<Integer, Double> getAverageRatingPerHotel() {
            return reviews.stream()
                    .collect(Collectors.groupingBy(HotelReview::getHotelId, Collectors.averagingInt(HotelReview::getRating)));
        }
    }
}
