package tn.esprit.projet.gui;

import javafx.collections.ObservableList;
import tn.esprit.projet.entities.Hotel;
import tn.esprit.projet.entities.Room;
import tn.esprit.projet.services.*;
import tn.esprit.projet.utils.MyDBConnexion;

import java.sql.*;
import java.util.*;

/**
 * Shared state for AdminController and its sub-controllers.
 * Holds service instances, DB availability flag, observable lists,
 * selection state, and localisation lookup caches.
 *
 * Mirrors ClientSharedState: initServices() wraps all construction
 * so AdminController never sees a raw SQLException.
 */
public class AdminSharedState {

    // ── Services (public like ClientSharedState) ──────────────────────────────
    public HotelService            hotelService;
    public HotelImageService       hotelImageService;
    public HotelServiceItemService hotelServiceItemService;
    public RoomService             roomService;
    public RoomImageService        roomImageService;
    public HotelReviewService      hotelReviewService;
    public ReservationService      reservationService;

    // ── DB state (mirrors ClientSharedState) ──────────────────────────────────
    public boolean databaseAvailable    = true;
    public String  databaseErrorMessage = "";

    // ── Observable lists ──────────────────────────────────────────────────────
    private ObservableList<Hotel> hotelsList;
    private ObservableList<Room>  roomsList;

    // ── Selection state ───────────────────────────────────────────────────────
    private Hotel selectedHotel;
    private Room  selectedRoom;

    // ── Localisation / destination cache ─────────────────────────────────────
    private final Map<Integer, String> localisationDisplayCache = new HashMap<>();
    private final Map<String, Integer> localisationIdByDisplay  = new HashMap<>();

    // ─────────────────────────────────────────────────────────────────────────
    //  Initialization  (mirrors ClientSharedState.initServices)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Initialise all services. Sets databaseAvailable = false on any failure.
     * Called once by AdminController.initialize() — same pattern as ClientSharedState.
     */
    public void initServices() {
        try {
            hotelService            = new HotelService();
            hotelImageService       = new HotelImageService();
            hotelServiceItemService = new HotelServiceItemService();
            roomService             = new RoomService();
            roomImageService        = new RoomImageService();
            hotelReviewService      = new HotelReviewService();
            reservationService      = new ReservationService();
            refreshLocalisationLookup();
        } catch (RuntimeException e) {
            databaseAvailable    = false;
            databaseErrorMessage = extractRootCauseMessage(e);
        }
    }

    private String extractRootCauseMessage(Throwable error) {
        Throwable root = error;
        while (root.getCause() != null && root.getCause() != root) root = root.getCause();
        String msg = root.getMessage();
        return (msg == null || msg.isBlank()) ? root.getClass().getSimpleName() : msg;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Observable list accessors
    // ─────────────────────────────────────────────────────────────────────────

    public ObservableList<Hotel> getHotelsList() { return hotelsList; }
    public void setHotelsList(ObservableList<Hotel> list) { this.hotelsList = list; }

    public ObservableList<Room> getRoomsList() { return roomsList; }
    public void setRoomsList(ObservableList<Room> list) { this.roomsList = list; }

    // ─────────────────────────────────────────────────────────────────────────
    //  Review user support
    // ─────────────────────────────────────────────────────────────────────────

    /** Lightweight record used to populate the user combo box in the review dialog. */
    public record ReviewUserOption(int id, String label) {
        @Override public String toString() { return label; }
    }

    /**
     * Queries the database for all users and returns them as ReviewUserOption list.
     * Falls back to an empty list on any error.
     */
    public java.util.List<ReviewUserOption> loadReviewUserOptions() {
        java.util.List<ReviewUserOption> options = new java.util.ArrayList<>();
        try {
            java.sql.Connection cnx = tn.esprit.projet.utils.MyDBConnexion.getInstance().getConnection();
            if (cnx == null) return options;

            java.sql.DatabaseMetaData meta    = cnx.getMetaData();
            String                    catalog = cnx.getCatalog();

            java.util.List<String> tables = java.util.List.of("user","users","client","clients","utilisateur","utilisateurs");
            java.util.List<String> idCols = java.util.List.of("id","userId","user_id","clientId","client_id");
            java.util.List<String> nameCols = java.util.List.of("username","name","nom","email","prenom","login","pseudo");

            for (String table : tables) {
                String idCol   = null;
                String nameCol = null;

                for (String col : idCols) {
                    try (java.sql.ResultSet rs = meta.getColumns(catalog, null, table, col)) {
                        if (rs.next()) { idCol = col; break; }
                    } catch (java.sql.SQLException ignored) {}
                }
                if (idCol == null) continue;

                for (String col : nameCols) {
                    try (java.sql.ResultSet rs = meta.getColumns(catalog, null, table, col)) {
                        if (rs.next()) { nameCol = col; break; }
                    } catch (java.sql.SQLException ignored) {}
                }

                String sel = nameCol != null
                        ? "SELECT `" + idCol + "`, `" + nameCol + "` FROM `" + table + "` ORDER BY `" + idCol + "`"
                        : "SELECT `" + idCol + "`, NULL FROM `" + table + "` ORDER BY `" + idCol + "`";
                try (java.sql.PreparedStatement ps = cnx.prepareStatement(sel);
                     java.sql.ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int    uid   = rs.getInt(1);
                        String uname = rs.getString(2);
                        String label = (uname != null && !uname.isBlank())
                                ? uname.trim() + " (#" + uid + ")"
                                : "User #" + uid;
                        options.add(new ReviewUserOption(uid, label));
                    }
                    if (!options.isEmpty()) return options;
                } catch (java.sql.SQLException ignored) {}
            }
        } catch (java.sql.SQLException ignored) {}
        return options;
    }

    /**
     * Returns a human-readable label for a user ID.
     * Checks already-loaded options first, then falls back to "User #N".
     */
    public String resolveUserLabel(int userId) {
        if (userId <= 0) return "User #" + userId;
        // Try to find label from a freshly loaded list
        for (ReviewUserOption opt : loadReviewUserOptions()) {
            if (opt.id() == userId) return opt.label();
        }
        return "User #" + userId;
    }

    /** Looks up the hotel name for a given hotel ID from the cached list. */
    public String getHotelName(int hotelId) {
        if (hotelsList == null) return "Hotel #" + hotelId;
        return hotelsList.stream()
                .filter(h -> h.getId() == hotelId)
                .map(Hotel::getName)
                .findFirst()
                .orElse("Hotel #" + hotelId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Selection accessors
    // ─────────────────────────────────────────────────────────────────────────

    public Hotel getSelectedHotel() { return selectedHotel; }
    public void  setSelectedHotel(Hotel hotel) { this.selectedHotel = hotel; }

    public Room  getSelectedRoom()  { return selectedRoom; }
    public void  setSelectedRoom(Room room)   { this.selectedRoom = room; }

    // ─────────────────────────────────────────────────────────────────────────
    //  Localisation / destination lookup  (both naming conventions supported)
    // ─────────────────────────────────────────────────────────────────────────

    /** Alias – British spelling (Localisation) */
    public void refreshLocalisationLookup() {
        doRefreshLookup();
    }

    /** Alias used by AdminController */
    public void refreshDestinationLookup() {
        doRefreshLookup();
    }

    private void doRefreshLookup() {
        localisationDisplayCache.clear();
        localisationIdByDisplay.clear();

        try {
            Connection cnx = MyDBConnexion.getInstance().getConnection();
            String sql = "SELECT id, name_destination, pays, ville FROM destination ORDER BY name_destination, ville, id";
            PreparedStatement ps = cnx.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int    id    = rs.getInt("id");
                String label = buildLocalisationLabel(
                        rs.getString("name_destination"),
                        rs.getString("ville"),
                        rs.getString("pays"),
                        id
                );
                addLocalisationOption(id, label);
            }
        } catch (Exception ignored) { /* keep what we have */ }

        if (hotelsList != null) {
            for (Hotel h : hotelsList) {
                addLocalisationOption(h.getLocationId(), "Destination #" + h.getLocationId());
            }
        }
    }

    /** Alias – British spelling (Localisation) */
    public String resolveLocalisationLabel(int locationId) {
        if (locationId <= 0) return "Localisation inconnue";
        String label = localisationDisplayCache.get(locationId);
        if (label == null) {
            label = "Destination #" + locationId;
            addLocalisationOption(locationId, label);
        }
        return localisationDisplayCache.getOrDefault(locationId, label);
    }

    /** Alias – British spelling (Localisation) */
    public List<String> getSortedLocalisationLabels() {
        return localisationDisplayCache.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(String::compareToIgnoreCase))
                .map(Map.Entry::getValue)
                .toList();
    }

    /** Alias used by AdminController */
    public List<String> getSortedDestinationLabels() {
        return getSortedLocalisationLabels();
    }

    /**
     * Resolve a display label back to its integer location ID.
     *
     * @param displayValue  the label shown in the combo box
     * @param fallbackId    value to return when the label cannot be resolved
     */
    public int resolveLocalisationId(String displayValue, int fallbackId) {
        if (displayValue == null || displayValue.isBlank()) return fallbackId;
        Integer id = localisationIdByDisplay.get(displayValue.trim());
        return (id != null && id > 0) ? id : fallbackId;
    }

    /**
     * Returns the first destination ID in the cache, or 1 as a safe default.
     * Used when no hotel is selected and we need a sensible initial value.
     */
    public int getDefaultDestinationId() {
        return localisationDisplayCache.keySet().stream()
                .min(Integer::compareTo)
                .orElse(1);
    }

    /** Public so HotelViewController can register ad-hoc entries. */
    public void addLocalisationOption(int id, String rawLabel) {
        if (id <= 0 || localisationDisplayCache.containsKey(id)) return;
        String label = (rawLabel == null || rawLabel.isBlank()) ? "Destination #" + id : rawLabel.trim();

        Integer existing = localisationIdByDisplay.get(label);
        if (existing != null && existing != id) label = label + " (#" + id + ")";

        localisationDisplayCache.put(id, label);
        localisationIdByDisplay.put(label, id);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Destination creation
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Inserts a new destination row into the DB and registers it in the local caches.
     *
     * @param name    value for name_destination (required, non-blank)
     * @param city    value for ville  (may be empty)
     * @param country value for pays   (may be empty)
     * @return the generated id of the new destination
     * @throws SQLException on any DB error
     * @throws IllegalArgumentException if name is blank
     */
    public int createDestinationInDB(String name, String city, String country) throws SQLException {
        String n = name    == null ? "" : name.trim();
        String c = city    == null ? "" : city.trim();
        String p = country == null ? "" : country.trim();
        if (n.isEmpty()) throw new IllegalArgumentException("Le nom de la destination ne peut pas être vide.");

        Connection cnx = MyDBConnexion.getInstance().getConnection();
        String sql = "INSERT INTO destination (name_destination, ville, pays) VALUES (?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, n);
            ps.setString(2, c.isEmpty() ? null : c);
            ps.setString(3, p.isEmpty() ? null : p);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("Aucun ID généré pour la destination.");
                int newId = keys.getInt(1);
                // Register in caches immediately so the combo can use it
                String label = buildLocalisationLabel(n, c, p, newId);
                // Force add even if id already cached (it won't be — brand new)
                localisationDisplayCache.put(newId, label);
                localisationIdByDisplay.put(label, newId);
                return newId;
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    private String buildLocalisationLabel(String name, String city, String country, int id) {
        String n = name    == null ? "" : name.trim();
        String c = city    == null ? "" : city.trim();
        String p = country == null ? "" : country.trim();

        String base = !n.isEmpty() ? n : "Destination #" + id;
        if (!c.isEmpty() && !p.isEmpty()) return base + " - " + c + " (" + p + ")";
        if (!c.isEmpty())                 return base + " - " + c;
        if (!p.isEmpty())                 return base + " (" + p + ")";
        return base;
    }
}