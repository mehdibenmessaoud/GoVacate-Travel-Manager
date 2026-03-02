package tn.esprit.projet.utils;

import tn.esprit.projet.services.*;

import java.sql.*;
import java.util.*;

/**
 * Base shared state for Admin and Client controllers.
 * Holds service instances, DB availability, and destination lookup caches.
 * Subclasses add role-specific state (e.g. ReviewUserOption for admin).
 */
public abstract class SharedState {

    // ── Services ──────────────────────────────────────────────────────────────
    public HotelService            hotelService;
    public RoomService             roomService;
    public HotelImageService       hotelImageService;
    public RoomImageService        roomImageService;
    public HotelReviewService      hotelReviewService;
    public HotelServiceItemService hotelServiceItemService;
    public ReservationService      reservationService;

    // ── DB state ──────────────────────────────────────────────────────────────
    public boolean databaseAvailable    = true;
    public String  databaseErrorMessage = "";

    // ── Destination cache ─────────────────────────────────────────────────────
    protected final Map<Integer, String> destinationDisplayCache = new HashMap<>();
    protected final Map<String, Integer> destinationIdByDisplay  = new HashMap<>();

    // ─────────────────────────────────────────────────────────────────────────
    //  Initialization
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Initialise all services. Sets databaseAvailable = false on any failure.
     * Subclasses should call super.initServices() then add extra services.
     */
    public void initServices() {
        try {
            hotelService            = new HotelService();
            roomService             = new RoomService();
            hotelImageService       = new HotelImageService();
            roomImageService        = new RoomImageService();
            hotelReviewService      = new HotelReviewService();
            hotelServiceItemService = new HotelServiceItemService();
            reservationService      = new ReservationService();
            initExtraServices();
            refreshDestinationLookup();
        } catch (RuntimeException e) {
            databaseAvailable    = false;
            databaseErrorMessage = extractRootCauseMessage(e);
        }
    }

    /** Override to add extra services during init. */
    protected void initExtraServices() {}

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

        addHotelDestinationFallbacks();
    }

    /** Subclasses can override to add hotel-based fallback labels. */
    protected void addHotelDestinationFallbacks() {}

    /** Alias – British spelling (Localisation) */
    public String resolveLocalisationLabel(int locationId) {
        return resolveDestinationLabel(locationId);
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

    /**
     * Resolve a display label back to its integer destination ID.
     */
    public int resolveLocalisationId(String displayValue, int fallbackId) {
        if (displayValue == null || displayValue.isBlank()) return fallbackId;
        Integer id = destinationIdByDisplay.get(displayValue.trim());
        return (id != null && id > 0) ? id : fallbackId;
    }

    /**
     * Returns the first destination ID in the cache, or 1 as a safe default.
     */
    public int getDefaultDestinationId() {
        return destinationDisplayCache.keySet().stream()
                .min(Integer::compareTo)
                .orElse(1);
    }

    /** Public so sub-controllers can register ad-hoc entries. */
    public void addDestinationOption(int id, String rawLabel) {
        // Also named addLocalisationOption in the old code
        if (id <= 0 || destinationDisplayCache.containsKey(id)) return;
        String label = (rawLabel == null || rawLabel.isBlank()) ? "Destination #" + id : rawLabel.trim();

        Integer existing = destinationIdByDisplay.get(label);
        if (existing != null && existing != id) label = label + " (#" + id + ")";

        destinationDisplayCache.put(id, label);
        destinationIdByDisplay.put(label, id);
    }

    /** Alias – old name from AdminSharedState */
    public void addLocalisationOption(int id, String rawLabel) {
        addDestinationOption(id, rawLabel);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Protected helpers
    // ─────────────────────────────────────────────────────────────────────────

    protected String buildDestinationLabel(String name, String city, String country, int id) {
        String n = name    == null ? "" : name.trim();
        String c = city    == null ? "" : city.trim();
        String p = country == null ? "" : country.trim();

        String base = !n.isEmpty() ? n : "Destination #" + id;
        if (!c.isEmpty() && !p.isEmpty()) return base + " - " + c + " (" + p + ")";
        if (!c.isEmpty())                 return base + " - " + c;
        if (!p.isEmpty())                 return base + " (" + p + ")";
        return base;
    }

    protected String extractRootCauseMessage(Throwable error) {
        Throwable root = error;
        while (root.getCause() != null && root.getCause() != root) root = root.getCause();
        String msg = root.getMessage();
        return (msg == null || msg.isBlank()) ? root.getClass().getSimpleName() : msg;
    }
}
