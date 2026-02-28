package tn.esprit.projet.services;

import tn.esprit.projet.entities.Reservation;
import tn.esprit.projet.utils.MyDBConnexion;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * CRUD service for the reservation_hotel table.
 * Also provides {@link #getAllWithDetails()} which JOINs room and hotel
 * so the UI can display human-readable names without extra lookups.
 */
public class ReservationService implements CRUD<Reservation> {

    private final Connection cnx;

    public ReservationService() {
        cnx = MyDBConnexion.getInstance().getConnection();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CRUD
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    public void create(Reservation r) throws SQLException {
        boolean autoCommitBefore = cnx.getAutoCommit();
        cnx.setAutoCommit(false);
        try {
            // Step 1: insert a parent row into `reservation` to satisfy the FK constraint
            // reservation_hotel.reservation_id REFERENCES reservation(id)
            int parentId = insertParentReservation(r);
            r.setReservationId(parentId);

            // Step 2: insert into reservation_hotel using the valid parent id
            String sql = "INSERT INTO reservation_hotel " +
                    "(reservation_id, hotel_id, chambre_id, date_checkin, date_checkout, prix) " +
                    "VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, r.getReservationId());
                ps.setInt(2, r.getHotelId());
                if (r.getChambreId() != null) ps.setInt(3, r.getChambreId());
                else ps.setNull(3, Types.INTEGER);
                ps.setDate(4, Date.valueOf(r.getDateCheckin()));
                ps.setDate(5, Date.valueOf(r.getDateCheckout()));
                ps.setDouble(6, r.getPrix());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) r.setId(keys.getInt(1));
                }
            }
            cnx.commit();
        } catch (SQLException e) {
            cnx.rollback();
            throw e;
        } finally {
            cnx.setAutoCommit(autoCommitBefore);
        }
    }

    /**
     * Inserts a minimal row into the parent {@code reservation} table and
     * returns its generated id.
     *
     * <p>Strategy: introspect the table with DESCRIBE to find the exact
     * NOT-NULL columns that have no default, then build the INSERT
     * dynamically so we never miss a required field regardless of the exact
     * schema used in the project.</p>
     */
    private int insertParentReservation(Reservation r) throws SQLException {
        // ── 1. Discover all NOT NULL / no-default columns ──────────────────
        List<String> requiredCols = new ArrayList<>();
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery("DESCRIBE reservation")) {
            while (rs.next()) {
                String field   = rs.getString("Field");
                String nullOk  = rs.getString("Null");   // "YES" or "NO"
                String dflt    = rs.getString("Default"); // null if no default
                String extra   = rs.getString("Extra");   // "auto_increment", …
                boolean autoInc = extra != null && extra.toLowerCase().contains("auto_increment");
                boolean hasDefault = dflt != null;
                boolean notNull    = "NO".equalsIgnoreCase(nullOk);
                if (!autoInc && notNull && !hasDefault) {
                    requiredCols.add(field.toLowerCase());
                }
            }
        }

        if (requiredCols.isEmpty()) {
            // All columns nullable or have defaults — bare insert works
            try (PreparedStatement ps = cnx.prepareStatement(
                    "INSERT INTO reservation () VALUES ()", Statement.RETURN_GENERATED_KEYS)) {
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) return keys.getInt(1);
                }
            }
        }

        // ── 2. Map well-known column names to values ───────────────────────
        // Aliases cover the most common French / English naming conventions.
        java.util.Map<String, Object> known = new java.util.LinkedHashMap<>();
        // date columns
        known.put("date_debut",    Date.valueOf(r.getDateCheckin()));
        known.put("date_checkin",  Date.valueOf(r.getDateCheckin()));
        known.put("checkin",       Date.valueOf(r.getDateCheckin()));
        known.put("date_arrivee",  Date.valueOf(r.getDateCheckin()));
        known.put("date_fin",      Date.valueOf(r.getDateCheckout()));
        known.put("date_checkout", Date.valueOf(r.getDateCheckout()));
        known.put("checkout",      Date.valueOf(r.getDateCheckout()));
        known.put("date_depart",   Date.valueOf(r.getDateCheckout()));
        // price columns
        known.put("prix_total",    r.getPrix());
        known.put("montant_total", r.getPrix());
        known.put("montant",       r.getPrix());
        known.put("prix",          r.getPrix());
        known.put("total",         r.getPrix());
        known.put("price",         r.getPrix());
        known.put("amount",        r.getPrix());
        // integer / status defaults
        known.put("nb_personnes",  r.getNbPersonnes());
        known.put("nombre_personnes", r.getNbPersonnes());
        known.put("capacite",      r.getNbPersonnes());
        known.put("nbre_personnes", r.getNbPersonnes());
        known.put("statut",        "EN_ATTENTE");
        known.put("status",        "EN_ATTENTE");
        known.put("etat",          "EN_ATTENTE");
        known.put("hotel_id",      r.getHotelId());
        known.put("chambre_id",    r.getChambreId() != null ? r.getChambreId() : 0);
        known.put("user_id",       r.getUserId());
        known.put("client_id",     r.getUserId());
        known.put("userid",        r.getUserId());
        known.put("clientid",      r.getUserId());

        // Build INSERT only for required columns we know how to fill
        StringBuilder cols = new StringBuilder();
        StringBuilder vals = new StringBuilder();
        List<Object> params = new ArrayList<>();
        for (String col : requiredCols) {
            if (!known.containsKey(col)) {
                throw new SQLException(
                        "Colonne requise inconnue dans la table reservation: '" + col +
                                "'. Ajoutez un mapping dans insertParentReservation().");
            }
            if (cols.length() > 0) { cols.append(", "); vals.append(", "); }
            cols.append(col);
            vals.append("?");
            params.add(known.get(col));
        }

        String sql = "INSERT INTO reservation (" + cols + ") VALUES (" + vals + ")";
        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        throw new SQLException("Impossible de récupérer l'id généré pour la table reservation.");
    }

    /**
     * Persists a status change into the reservation_hotel table.
     * Requires a 'statut' or 'status' column — if neither exists this is a no-op.
     */
    public void updateStatus(int id, String status) throws SQLException {
        // Try both common column names
        for (String col : new String[]{"statut", "status"}) {
            try (PreparedStatement ps = cnx.prepareStatement(
                    "UPDATE reservation_hotel SET `" + col + "` = ? WHERE id = ?")) {
                ps.setString(1, status);
                ps.setInt(2, id);
                ps.executeUpdate();
                return;
            } catch (SQLException ignored) { /* column doesn't exist, try next */ }
        }
    }

    @Override
    public List<Reservation> getAll() throws SQLException {
        List<Reservation> list = new ArrayList<>();
        String sql = "SELECT * FROM reservation_hotel ORDER BY id DESC";
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(extract(rs));
        }
        return list;
    }

    @Override
    public Reservation getById(int id) throws SQLException {
        String sql = "SELECT * FROM reservation_hotel WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return extract(rs);
            }
        }
        return null;
    }

    @Override
    public void update(Reservation r) throws SQLException {
        String sql = "UPDATE reservation_hotel " +
                "SET hotel_id=?, chambre_id=?, date_checkin=?, date_checkout=?, prix=? " +
                "WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, r.getHotelId());
            if (r.getChambreId() != null) ps.setInt(2, r.getChambreId());
            else ps.setNull(2, Types.INTEGER);
            ps.setDate(3, Date.valueOf(r.getDateCheckin()));
            ps.setDate(4, Date.valueOf(r.getDateCheckout()));
            ps.setDouble(5, r.getPrix());
            ps.setInt(6, r.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM reservation_hotel WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // JOINED QUERY — returns everything the UI needs in one shot
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Returns all reservations enriched with room number/type and hotel name
     * via LEFT JOINs, newest first.
     */
    public List<ReservationDetail> getAllWithDetails() throws SQLException {
        List<ReservationDetail> list = new ArrayList<>();
        String sql =
                "SELECT rh.id, rh.reservation_id, rh.hotel_id, rh.chambre_id, " +
                        "       rh.date_checkin, rh.date_checkout, rh.prix, " +
                        "       r.roomNumber, r.roomType, " +
                        "       h.name AS hotelName " +
                        "FROM reservation_hotel rh " +
                        "LEFT JOIN room   r ON rh.chambre_id = r.id " +
                        "LEFT JOIN hotel  h ON rh.hotel_id   = h.id " +
                        "ORDER BY rh.id DESC";
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                ReservationDetail d = new ReservationDetail();
                d.id            = rs.getInt("id");
                d.reservationId = rs.getInt("reservation_id");
                d.hotelId       = rs.getInt("hotel_id");
                d.chambreId     = rs.getObject("chambre_id") != null ? rs.getInt("chambre_id") : null;
                d.dateCheckin   = rs.getDate("date_checkin").toLocalDate();
                d.dateCheckout  = rs.getDate("date_checkout").toLocalDate();
                d.prix          = rs.getDouble("prix");
                d.roomNumber    = rs.getString("roomNumber");
                d.roomType      = rs.getString("roomType");
                d.hotelName     = rs.getString("hotelName");
                list.add(d);
            }
        }
        return list;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════════════

    private Reservation extract(ResultSet rs) throws SQLException {
        Integer chambreId = rs.getObject("chambre_id") != null ? rs.getInt("chambre_id") : null;
        return new Reservation(
                rs.getInt("id"),
                rs.getInt("reservation_id"),
                rs.getInt("hotel_id"),
                chambreId,
                rs.getDate("date_checkin").toLocalDate(),
                rs.getDate("date_checkout").toLocalDate(),
                rs.getDouble("prix")
        );
    }

    // ═══════════════════════════════════════════════════════════════════════
    // DTO — flat view of a reservation row + joined columns
    // ═══════════════════════════════════════════════════════════════════════

    public static class ReservationDetail {
        public int       id;
        public int       reservationId;
        public int       hotelId;
        public Integer   chambreId;
        public LocalDate dateCheckin;
        public LocalDate dateCheckout;
        public double    prix;
        public String    roomNumber;   // from room table (nullable)
        public String    roomType;     // from room table (nullable)
        public String    hotelName;    // from hotel table (nullable)
        /** In-memory status — not persisted (no status column in DB). */
        public String    status = "EN_ATTENTE";

        /** Number of nights between check-in and check-out. */
        public long nights() {
            return java.time.temporal.ChronoUnit.DAYS.between(dateCheckin, dateCheckout);
        }

        public String formattedCheckIn() {
            return dateCheckin.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        }

        public String formattedCheckOut() {
            return dateCheckout.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        }

        public String dateRange() {
            return formattedCheckIn() + "  →  " + formattedCheckOut();
        }

        public String safeRoomNumber() {
            return roomNumber != null ? roomNumber : (chambreId != null ? "#" + chambreId : "—");
        }

        public String safeRoomType() {
            return roomType != null ? roomType : "—";
        }

        public String safeHotelName() {
            return hotelName != null ? hotelName : "Hôtel #" + hotelId;
        }
    }
}