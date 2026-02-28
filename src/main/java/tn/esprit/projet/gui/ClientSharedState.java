package tn.esprit.projet.gui;

import tn.esprit.projet.entities.Hotel;
import tn.esprit.projet.services.*;
import tn.esprit.projet.services.ReservationService;
import tn.esprit.projet.utils.MyDBConnexion;

import java.sql.*;
import java.util.*;

/**
 * Shared state for all ClientController sub-controllers.
 * Holds service instances, DB availability flag, and destination lookup caches.
 */
public class ClientSharedState {

    // ── Services ──────────────────────────────────────────────────────────────
    public HotelService            hotelService;
    public RoomService             roomService;
    public HotelImageService       hotelImageService;
    public RoomImageService        roomImageService;
    public HotelReviewService      hotelReviewService;
    public HotelReviewImageService hotelReviewImageService;
    public HotelServiceItemService hotelServiceItemService;
    public ReservationService       reservationService;

    // ── DB state ──────────────────────────────────────────────────────────────
    public boolean databaseAvailable    = true;
    public String  databaseErrorMessage = "";

    // ── Destination cache ─────────────────────────────────────────────────────
    private final Map<Integer, String> destinationDisplayCache = new HashMap<>();
    private final Map<String, Integer> destinationIdByDisplay  = new HashMap<>();

    // ─────────────────────────────────────────────────────────────────────────
    //  Initialization
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Initialise all services. Sets databaseAvailable = false on any failure.
     */
    public void initServices() {
        try {
            hotelService            = new HotelService();
            roomService             = new RoomService();
            hotelImageService       = new HotelImageService();
            roomImageService        = new RoomImageService();
            hotelReviewService      = new HotelReviewService();
            hotelReviewImageService = new HotelReviewImageService();
            hotelServiceItemService = new HotelServiceItemService();
            reservationService      = new ReservationService();
            refreshDestinationLookup();
        } catch (RuntimeException e) {
            databaseAvailable    = false;
            databaseErrorMessage = extractRootCauseMessage(e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Destination lookup
    // ─────────────────────────────────────────────────────────────────────────

    /** Alias – British spelling (Localisation) */
    public void refreshLocalisationLookup() {
        refreshDestinationLookup();
    }

    public void refreshDestinationLookup() {
        destinationDisplayCache.clear();
        destinationIdByDisplay.clear();

        if (!databaseAvailable) return;

        try {
            Connection cnx = MyDBConnexion.getInstance().getConnection();
            String sql = "SELECT id, name_destination, pays, ville FROM destination ORDER BY name_destination, ville, id";
            PreparedStatement ps = cnx.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int    id    = rs.getInt("id");
                String label = buildDestinationLabel(
                        rs.getString("name_destination"),
                        rs.getString("ville"),
                        rs.getString("pays"),
                        id
                );
                addDestinationOption(id, label);
            }
        } catch (Exception ignored) { /* keep what we have */ }

        if (hotelService != null) {
            try {
                for (Hotel h : hotelService.getAll()) {
                    addDestinationOption(h.getLocationId(), "Destination #" + h.getLocationId());
                }
            } catch (SQLException ignored) { /* keep available labels */ }
        }
    }

    public String resolveDestinationLabel(int locationId) {
        if (locationId <= 0) return "Destination inconnue";
        String label = destinationDisplayCache.get(locationId);
        if (label == null) {
            label = "Destination #" + locationId;
            addDestinationOption(locationId, label);
        }
        return destinationDisplayCache.getOrDefault(locationId, label);
    }

    /** Alias – British spelling (Localisation) */
    public List<String> getSortedLocalisationLabels() {
        return getSortedDestinationLabels();
    }

    public List<String> getSortedDestinationLabels() {
        return destinationDisplayCache.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(String::compareToIgnoreCase))
                .map(Map.Entry::getValue)
                .toList();
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

    // ─────────────────────────────────────────────────────────────────────────
    //  Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void addDestinationOption(int id, String rawLabel) {
        if (id <= 0 || destinationDisplayCache.containsKey(id)) return;
        String label = (rawLabel == null || rawLabel.isBlank()) ? "Destination #" + id : rawLabel.trim();

        Integer existing = destinationIdByDisplay.get(label);
        if (existing != null && existing != id) label = label + " (#" + id + ")";

        destinationDisplayCache.put(id, label);
        destinationIdByDisplay.put(label, id);
    }

    private String buildDestinationLabel(String name, String city, String country, int id) {
        String n = name    == null ? "" : name.trim();
        String c = city    == null ? "" : city.trim();
        String p = country == null ? "" : country.trim();

        String base = !n.isEmpty() ? n : "Destination #" + id;
        if (!c.isEmpty() && !p.isEmpty()) return base + " - " + c + " (" + p + ")";
        if (!c.isEmpty())                 return base + " - " + c;
        if (!p.isEmpty())                 return base + " (" + p + ")";
        return base;
    }

    private String extractRootCauseMessage(Throwable error) {
        Throwable root = error;
        while (root.getCause() != null && root.getCause() != root) root = root.getCause();
        String msg = root.getMessage();
        return (msg == null || msg.isBlank()) ? root.getClass().getSimpleName() : msg;
    }
}