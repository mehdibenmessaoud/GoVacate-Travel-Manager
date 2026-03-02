package tn.esprit.projet.utils;

import javafx.collections.ObservableList;
import tn.esprit.projet.entities.Hotel;
import tn.esprit.projet.entities.Room;

import java.sql.*;

/**
 * Admin-specific shared state. Extends SharedState with:
 * – Observable hotel/room lists for table binding
 * – Selection state for detail views
 * – ReviewUserOption support
 * – Destination creation in DB
 */
public class AdminSharedState extends SharedState {

    // ── Observable lists ──────────────────────────────────────────────────────
    private ObservableList<Hotel> hotelsList;
    private ObservableList<Room>  roomsList;

    // ── Selection state ───────────────────────────────────────────────────────
    private Hotel selectedHotel;
    private Room  selectedRoom;

    // ─────────────────────────────────────────────────────────────────────────
    //  Override: add hotel-based destination fallbacks
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    protected void addHotelDestinationFallbacks() {
        if (hotelsList != null) {
            for (Hotel h : hotelsList) {
                addDestinationOption(h.getLocationId(), "Destination #" + h.getLocationId());
            }
        }
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

    public java.util.List<ReviewUserOption> loadReviewUserOptions() {
        java.util.List<ReviewUserOption> options = new java.util.ArrayList<>();
        try {
            Connection cnx = MyDBConnexion.getInstance().getConnection();
            if (cnx == null) return options;

            DatabaseMetaData meta    = cnx.getMetaData();
            String                    catalog = cnx.getCatalog();

            java.util.List<String> tables = java.util.List.of("user","users","client","clients","utilisateur","utilisateurs");
            java.util.List<String> idCols = java.util.List.of("id","userId","user_id","clientId","client_id");
            java.util.List<String> nameCols = java.util.List.of("username","name","nom","email","prenom","login","pseudo");

            for (String table : tables) {
                String idCol   = null;
                String nameCol = null;

                for (String col : idCols) {
                    try (ResultSet rs = meta.getColumns(catalog, null, table, col)) {
                        if (rs.next()) { idCol = col; break; }
                    } catch (SQLException ignored) {}
                }
                if (idCol == null) continue;

                for (String col : nameCols) {
                    try (ResultSet rs = meta.getColumns(catalog, null, table, col)) {
                        if (rs.next()) { nameCol = col; break; }
                    } catch (SQLException ignored) {}
                }

                String sel = nameCol != null
                        ? "SELECT `" + idCol + "`, `" + nameCol + "` FROM `" + table + "` ORDER BY `" + idCol + "`"
                        : "SELECT `" + idCol + "`, NULL FROM `" + table + "` ORDER BY `" + idCol + "`";
                try (PreparedStatement ps = cnx.prepareStatement(sel);
                     ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int    uid   = rs.getInt(1);
                        String uname = rs.getString(2);
                        String label = (uname != null && !uname.isBlank())
                                ? uname.trim() + " (#" + uid + ")"
                                : "User #" + uid;
                        options.add(new ReviewUserOption(uid, label));
                    }
                    if (!options.isEmpty()) return options;
                } catch (SQLException ignored) {}
            }
        } catch (SQLException ignored) {}
        return options;
    }

    public String resolveUserLabel(int userId) {
        if (userId <= 0) return "User #" + userId;
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
    //  Destination creation
    // ─────────────────────────────────────────────────────────────────────────

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
                String label = buildDestinationLabel(n, c, p, newId);
                destinationDisplayCache.put(newId, label);
                destinationIdByDisplay.put(label, newId);
                return newId;
            }
        }
    }
}