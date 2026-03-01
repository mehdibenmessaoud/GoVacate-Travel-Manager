package tn.esprit.projet.gui;

import tn.esprit.projet.entities.Hotel;
import tn.esprit.projet.services.*;
import tn.esprit.projet.utils.MyDBConnexion;

import java.sql.*;
import java.util.*;

/**
 * Client-specific shared state. Extends SharedState with:
 * – HotelReviewImageService (client-only)
 * – resolveClientReviewUserId (client-only)
 */
public class ClientSharedState extends SharedState {

    // ── Client-only services ──────────────────────────────────────────────────
    public HotelReviewImageService hotelReviewImageService;

    @Override
    protected void initExtraServices() {
        hotelReviewImageService = new HotelReviewImageService();
    }

    @Override
    protected void addHotelDestinationFallbacks() {
        if (hotelService != null) {
            try {
                for (Hotel h : hotelService.getAll()) {
                    addDestinationOption(h.getLocationId(), "Destination #" + h.getLocationId());
                }
            } catch (SQLException ignored) { /* keep available labels */ }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  User ID resolution (for reviews)
    // ─────────────────────────────────────────────────────────────────────────

    public int resolveClientReviewUserId() {
        try {
            Connection cnx = MyDBConnexion.getInstance().getConnection();
            if (cnx == null) return 1;

            DatabaseMetaData meta      = cnx.getMetaData();
            String           catalog   = cnx.getCatalog();
            List<String> tables  = List.of("user", "users", "client", "clients", "utilisateur", "utilisateurs");
            List<String> idCols  = List.of("id", "userId", "user_id", "clientId", "client_id");

            for (String table : tables) {
                String foundId = null;
                for (String col : idCols) {
                    try (ResultSet cols = meta.getColumns(catalog, null, table, col)) {
                        if (cols.next()) { foundId = col; break; }
                    }
                }
                if (foundId != null) {
                    String sql = "SELECT `" + foundId + "` FROM `" + table + "` ORDER BY `" + foundId + "` ASC LIMIT 1";
                    try (PreparedStatement ps = cnx.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            int id = rs.getInt(1);
                            if (id > 0) return id;
                        }
                    } catch (SQLException ignored) { /* try next */ }
                }
            }

            // Fallback: borrow a userId from existing reviews
            String fallback = "SELECT userId FROM hotel_review WHERE userId > 0 ORDER BY userId ASC LIMIT 1";
            try (PreparedStatement ps = cnx.prepareStatement(fallback); ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    if (id > 0) return id;
                }
            }
        } catch (SQLException ignored) { /* use default */ }
        return 1;
    }
}