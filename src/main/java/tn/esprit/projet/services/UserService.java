package tn.esprit.projet.services;

import tn.esprit.projet.entities.Role;
import tn.esprit.projet.entities.User;
import tn.esprit.projet.utils.PasswordHasher;
import tn.esprit.projet.utils.govacate_connect;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Fixed: Implements CRUD with 2 type arguments (User, Integer)
 */
public class UserService implements CRUD<User, Integer> {
    private static final Logger LOG = Logger.getLogger(UserService.class.getName());

    private final Connection conn;
    private final RoleService roleService;

    public UserService() {
        // Fixed: Use govacate_connect singleton to resolve connection issues
        this.conn = govacate_connect.getInstance().getConnection();
        this.roleService = new RoleService();
    }

    // ============================================
    // CRUD INTERFACE METHODS
    // ============================================

    @Override
    public User insert(User u) throws SQLException {
        if (emailExiste(u.getEmail())) {
            throw new SQLException("Email déjà utilisé: " + u.getEmail());
        }
        if (!doInsertUser(u)) {
            throw new SQLException("Échec de l'ajout de l'utilisateur");
        }
        return u; // Fixed: Now returns User object
    }

    @Override
    public User update(User u) throws SQLException {
        String sql = "UPDATE utilisateur SET nom=?, email=?, telephone=?, date_naissance=?, role_id=?, status=? WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, u.getNom());
            ps.setString(2, u.getEmail());
            ps.setString(3, u.getTelephone());
            ps.setDate(4, u.getDateNaissance() != null ? Date.valueOf(u.getDateNaissance()) : null);
            ps.setInt(5, u.getRoleId());
            ps.setString(6, u.getStatus() != null ? u.getStatus() : "actif");
            ps.setInt(7, u.getId());
            if (ps.executeUpdate() == 0) {
                throw new SQLException("Aucune ligne mise à jour pour l'utilisateur id=" + u.getId());
            }
            return u; // Fixed: Returns User object
        }
    }

    @Override
    public void delete(Integer id) throws SQLException {
        String sql = "DELETE FROM utilisateur WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            if (ps.executeUpdate() == 0) {
                throw new SQLException("Aucun utilisateur supprimé pour id=" + id);
            }
        }
    }

    @Override
    public List<User> selectAll() throws SQLException {
        // Fixed: Renamed from getAll() to selectAll()
        return queryAllUsers("SELECT u.*, r.nom_role FROM utilisateur u LEFT JOIN role r ON u.role_id = r.id",
                "SELECT * FROM utilisateur");
    }

    @Override
    public User getById(Integer id) throws SQLException {
        return queryOneUserById(id);
    }

    // ============================================
    // CORE LOGIC (Auth, Verification)
    // ============================================

    public User authenticate(String email, String password) {
        String sql = "SELECT u.*, r.nom_role FROM utilisateur u LEFT JOIN role r ON u.role_id = r.id WHERE u.email = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String storedHash = rs.getString("password");
                if (storedHash == null) return null;

                boolean isValid = PasswordHasher.isHashed(storedHash) ?
                        PasswordHasher.verifyPassword(password, storedHash) :
                        password.equals(storedHash);

                if (isValid) {
                    User user = mapResultSet(rs);
                    updateLastLogin(user.getId());
                    return user;
                }
            }
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Auth error for: " + email, e);
        }
        return null;
    }

    public boolean emailExiste(String email) {
        String sql = "SELECT count(*) FROM utilisateur WHERE email = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Email check failed", e);
        }
        return false;
    }

    // ============================================
    // INTERNAL HELPERS & MAPPING
    // ============================================

    private boolean doInsertUser(User u) {
        String sql = "INSERT INTO utilisateur (nom, email, password, telephone, date_naissance, role_id, status, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            String hashedPassword = u.getPassword();
            if (!PasswordHasher.isHashed(hashedPassword)) {
                hashedPassword = PasswordHasher.hashPassword(hashedPassword);
            }
            ps.setString(1, u.getNom());
            ps.setString(2, u.getEmail());
            ps.setString(3, hashedPassword);
            ps.setString(4, u.getTelephone());
            ps.setDate(5, u.getDateNaissance() != null ? Date.valueOf(u.getDateNaissance()) : null);
            ps.setInt(6, u.getRoleId());
            ps.setString(7, u.getStatus() != null ? u.getStatus() : "actif");
            ps.setTimestamp(8, Timestamp.valueOf(LocalDateTime.now()));

            if (ps.executeUpdate() > 0) {
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) u.setId(rs.getInt(1));
                return true;
            }
        } catch (SQLException e) {
            return false;
        }
        return false;
    }

    private List<User> queryAllUsers(String sqlWithJoin, String sqlNoJoin) throws SQLException {
        List<User> users = new ArrayList<>();
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sqlWithJoin)) {
            while (rs.next()) users.add(mapResultSet(rs));
            return users;
        } catch (SQLException e) {
            try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sqlNoJoin)) {
                while (rs.next()) users.add(mapResultSetNoJoin(rs));
                return users;
            }
        }
    }

    private User queryOneUserById(int id) {
        String sql = "SELECT u.*, r.nom_role FROM utilisateur u LEFT JOIN role r ON u.role_id = r.id WHERE u.id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapResultSet(rs);
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "GetById error: " + id, e);
        }
        return null;
    }

    private void updateLastLogin(int userId) {
        String sql = "UPDATE utilisateur SET last_login = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException ignored) {}
    }

    /**
     * Fixed: Resolved 'Cannot resolve constructor User()'
     * by using the parameterized constructor instead of a non-existent default one.
     */
    private User mapResultSet(ResultSet rs) throws SQLException {
        User user = mapResultSetNoJoin(rs);
        try {
            String nomRole = rs.getString("nom_role");
            if (nomRole != null) user.setRole(new Role(user.getRoleId(), nomRole));
        } catch (SQLException ignored) {}
        return user;
    }

    private User mapResultSetNoJoin(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String nom = rs.getString("nom");
        String email = rs.getString("email");
        String pass = rs.getString("password");
        String tel = rs.getString("telephone");
        Date dateSql = rs.getDate("date_naissance");
        java.time.LocalDate dateN = dateSql != null ? dateSql.toLocalDate() : null;
        int roleId = rs.getInt("role_id");
        String status = rs.getString("status");

        // Use the existing constructor to avoid 'User()' resolution errors
        User user = new User(nom, email, pass, tel, dateN, roleId, status != null ? status : "actif");
        user.setId(id);

        try {
            Timestamp login = rs.getTimestamp("last_login");
            if (login != null) user.setLastLogin(login.toLocalDateTime());
            Timestamp created = rs.getTimestamp("created_at");
            if (created != null) user.setCreatedAt(created.toLocalDateTime());
        } catch (SQLException ignored) {}

        return user;
    }
}