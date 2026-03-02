package tn.esprit.projet.services;

import tn.esprit.projet.entities.ReservationHotelChambre;
import tn.esprit.projet.utils.MyDBConnexion;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * CRUD service for the reservation_hotel table.
 * Uses a fresh connection per operation to avoid shared-state corruption.
 */
public class ReservationService implements IService<ReservationHotelChambre> {

    private final Connection cnx;

    public ReservationService() {
        cnx = MyDBConnexion.getInstance().getConnection();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SCHEMA — ensure statut column exists
    // ═══════════════════════════════════════════════════════════════════════

    private static boolean statutEnsured = false;
    private static final Object STATUT_LOCK = new Object();

    private void ensureStatutColumn() {
        if (statutEnsured) return;
        
        synchronized (STATUT_LOCK) {
            if (statutEnsured) return;
            
            if (cnx == null) {
                System.err.println("[ReservationService] Could not get database connection for statut column check");
                return;
            }
            
            // Test if column exists
            try (Statement st = cnx.createStatement();
                 ResultSet rs = st.executeQuery("SELECT statut FROM reservation_hotel LIMIT 1")) {
                statutEnsured = true; 
            } catch (SQLException e) {
                // Column missing — try to add it
                try (Statement st = cnx.createStatement()) {
                    st.executeUpdate(
                        "ALTER TABLE reservation_hotel " +
                        "ADD COLUMN statut VARCHAR(30) NOT NULL DEFAULT 'EN_ATTENTE'"
                    );
                    statutEnsured = true;
                    System.out.println("[ReservationService] ✅ statut column added to reservation_hotel");
                } catch (SQLException ex) {
                    System.err.println("[ReservationService] Could not add statut column: " + ex.getMessage());
                    statutEnsured = true; // don't keep retrying
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CRUD
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    public void create(ReservationHotelChambre r) throws SQLException {
        ensureStatutColumn();
        boolean prev = cnx.getAutoCommit();
        cnx.setAutoCommit(false);
        try {
            int parentId = insertParentReservation(r);
            r.setReservationId(parentId);

            String sql = "INSERT INTO reservation_hotel " +
                    "(reservation_id, hotel_id, chambre_id, date_checkin, date_checkout, prix, statut) " +
                    "VALUES (?, ?, ?, ?, ?, ?, 'EN_ATTENTE')";
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
            cnx.setAutoCommit(prev);
        }
    }

    private int insertParentReservation(ReservationHotelChambre r) throws SQLException {
        List<String> requiredCols = new ArrayList<>();
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery("DESCRIBE reservation")) {
            while (rs.next()) {
                String field   = rs.getString("Field");
                String nullOk  = rs.getString("Null");
                String dflt    = rs.getString("Default");
                String extra   = rs.getString("Extra");
                boolean autoInc    = extra != null && extra.toLowerCase().contains("auto_increment");
                boolean hasDefault = dflt != null;
                boolean notNull    = "NO".equalsIgnoreCase(nullOk);
                if (!autoInc && notNull && !hasDefault) requiredCols.add(field.toLowerCase());
            }
        }

        if (requiredCols.isEmpty()) {
            try (PreparedStatement ps = cnx.prepareStatement(
                    "INSERT INTO reservation () VALUES ()", Statement.RETURN_GENERATED_KEYS)) {
                ps.executeUpdate();
                try (ResultSet k = ps.getGeneratedKeys()) {
                    if (k.next()) return k.getInt(1);
                }
            }
        }

        java.util.Map<String, Object> known = new java.util.LinkedHashMap<>();
        known.put("date_debut",         Date.valueOf(r.getDateCheckin()));
        known.put("date_checkin",        Date.valueOf(r.getDateCheckin()));
        known.put("checkin",             Date.valueOf(r.getDateCheckin()));
        known.put("date_arrivee",        Date.valueOf(r.getDateCheckin()));
        known.put("date_fin",            Date.valueOf(r.getDateCheckout()));
        known.put("date_checkout",       Date.valueOf(r.getDateCheckout()));
        known.put("checkout",            Date.valueOf(r.getDateCheckout()));
        known.put("date_depart",         Date.valueOf(r.getDateCheckout()));
        known.put("prix_total",          r.getPrix());
        known.put("montant_total",       r.getPrix());
        known.put("montant",             r.getPrix());
        known.put("prix",                r.getPrix());
        known.put("total",               r.getPrix());
        known.put("price",               r.getPrix());
        known.put("amount",              r.getPrix());
        known.put("nb_personnes",        r.getNbPersonnes());
        known.put("nombre_personnes",    r.getNbPersonnes());
        known.put("capacite",            r.getNbPersonnes());
        known.put("nbre_personnes",      r.getNbPersonnes());
        known.put("statut",              "EN_ATTENTE");
        known.put("status",              "EN_ATTENTE");
        known.put("etat",                "EN_ATTENTE");
        known.put("hotel_id",            r.getHotelId());
        known.put("chambre_id",          r.getChambreId() != null ? r.getChambreId() : 0);
        known.put("user_id",             r.getUserId());
        known.put("client_id",           r.getUserId());
        known.put("userid",              r.getUserId());
        known.put("clientid",            r.getUserId());
        known.put("type_res",            "HOTEL");
        known.put("commentaire_client",  "");

        StringBuilder cols = new StringBuilder(), vals = new StringBuilder();
        List<Object> params = new ArrayList<>();
        for (String col : requiredCols) {
            if (!known.containsKey(col))
                throw new SQLException("Colonne requise inconnue: '" + col + "' dans reservation.");
            if (cols.length() > 0) { cols.append(", "); vals.append(", "); }
            cols.append(col); vals.append("?");
            params.add(known.get(col));
        }

        String sql = "INSERT INTO reservation (" + cols + ") VALUES (" + vals + ")";
        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) {
                if (k.next()) return k.getInt(1);
            }
        }
        throw new SQLException("Impossible de récupérer l'id généré pour la table reservation.");
    }

    public void updateStatus(int id, String status) throws SQLException {
        ensureStatutColumn();
        String sql = "UPDATE reservation_hotel SET statut = ? WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    @Override
    public List<ReservationHotelChambre> getAll() throws SQLException {
        List<ReservationHotelChambre> list = new ArrayList<>();
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM reservation_hotel ORDER BY id DESC")) {
            while (rs.next()) list.add(extract(rs));
        }
        return list;
    }

    @Override
    public ReservationHotelChambre getById(int id) throws SQLException {
        try (PreparedStatement ps = cnx.prepareStatement(
                "SELECT * FROM reservation_hotel WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return extract(rs);
            }
        }
        return null;
    }

    @Override
    public void update(ReservationHotelChambre r) throws SQLException {
        String sql = "UPDATE reservation_hotel SET hotel_id=?, chambre_id=?, date_checkin=?, date_checkout=?, prix=? WHERE id=?";
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
        try (PreparedStatement ps = cnx.prepareStatement(
                "DELETE FROM reservation_hotel WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // MAIN QUERY — with JOINs
    // ═══════════════════════════════════════════════════════════════════════

    public List<ReservationDetail> getAllWithDetails() throws SQLException {
        return queryWithDetails(null);
    }

    public List<ReservationDetail> getByUserIdWithDetails(int userId) throws SQLException {
        return queryWithDetails(userId);
    }

    private List<ReservationDetail> queryWithDetails(Integer userId) throws SQLException {
        ensureStatutColumn();

        String uidCol = "user_id";
        if (userId != null) {
            uidCol = resolveUserIdColumn(cnx);
            System.out.println("[ReservationService] Resolved user ID column: " + uidCol);
        }

        // Build query
        String sql =
            "SELECT rh.id, rh.reservation_id, rh.hotel_id, rh.chambre_id," +
            "       rh.date_checkin, rh.date_checkout, rh.prix, rh.statut," +
            "       r.roomNumber, r.roomType," +
            "       h.name AS hotelName" +
            " FROM reservation_hotel rh" +
            " LEFT JOIN room  r ON rh.chambre_id = r.id" +
            " LEFT JOIN hotel h ON rh.hotel_id   = h.id";

        if (userId != null) {
            // Also join with reservation to filter by user
            sql += " JOIN reservation res ON rh.reservation_id = res.id" +
                   " WHERE res." + uidCol + " = " + userId;
        }
        
        sql += " ORDER BY rh.id DESC";

        System.out.println("[ReservationService] queryWithDetails SQL: " + sql);

        List<ReservationDetail> list = new ArrayList<>();

        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                ReservationDetail d = new ReservationDetail();
                d.id            = rs.getInt("id");
                d.reservationId = rs.getInt("reservation_id");
                d.hotelId       = rs.getInt("hotel_id");
                d.chambreId     = rs.getObject("chambre_id") != null ? rs.getInt("chambre_id") : null;

                java.sql.Date ci = rs.getDate("date_checkin");
                java.sql.Date co = rs.getDate("date_checkout");
                d.dateCheckin  = ci != null ? ci.toLocalDate() : LocalDate.now();
                d.dateCheckout = co != null ? co.toLocalDate() : LocalDate.now();

                d.prix       = rs.getDouble("prix");
                d.roomNumber = rs.getString("roomNumber");
                d.roomType   = rs.getString("roomType");
                d.hotelName  = rs.getString("hotelName");

                String s = rs.getString("statut");
                d.status = (s != null && !s.isBlank()) ? s : "EN_ATTENTE";

                list.add(d);
            }
        }
        return list;
    }

    private String resolveUserIdColumn(Connection conn) {
        String[] candidates = {"user_id", "client_id", "userid", "clientid"};
        for (String col : candidates) {
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SHOW COLUMNS FROM reservation LIKE '" + col + "'")) {
                if (rs.next()) return col;
            } catch (SQLException e) {
                // ignore
            }
        }
        return "user_id"; // fallback
    }

    // ═══════════════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════════════

    private ReservationHotelChambre extract(ResultSet rs) throws SQLException {
        Integer chambreId = rs.getObject("chambre_id") != null ? rs.getInt("chambre_id") : null;
        java.sql.Date ci = rs.getDate("date_checkin");
        java.sql.Date co = rs.getDate("date_checkout");
        return new ReservationHotelChambre(
                rs.getInt("id"),
                rs.getInt("reservation_id"),
                rs.getInt("hotel_id"),
                chambreId,
                ci != null ? ci.toLocalDate() : LocalDate.now(),
                co != null ? co.toLocalDate() : LocalDate.now(),
                rs.getDouble("prix")
        );
    }

    // ═══════════════════════════════════════════════════════════════════════
    // DTO
    // ═══════════════════════════════════════════════════════════════════════

    public static class ReservationDetail {
        public int       id;
        public int       reservationId;
        public int       hotelId;
        public Integer   chambreId;
        public LocalDate dateCheckin;
        public LocalDate dateCheckout;
        public double    prix;
        public String    roomNumber;
        public String    roomType;
        public String    hotelName;
        public String    status = "EN_ATTENTE";

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
