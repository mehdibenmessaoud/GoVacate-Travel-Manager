package tn.esprit.projet.services;

import tn.esprit.projet.entities.Role;
import tn.esprit.projet.entities.User;
import tn.esprit.projet.utils.MyDBConnexion;
import tn.esprit.projet.utils.PasswordHasher;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UserService implements IService<User> {
    private static final Logger LOG = Logger.getLogger(UserService.class.getName());

    private final Connection conn;
    private final RoleService roleService;

    public UserService() {
        conn = MyDBConnexion.getInstance().getConnection();
        roleService = new RoleService();
    }

    // ============================================
    // CRUD DE BASE
    // ============================================

    /**
     * Ajoute un utilisateur avec hashage du mot de passe (contrat IService).
     */
    @Override
    public void ajouterUser(User u) throws SQLException {
        if (emailExiste(u.getEmail())) {
            throw new SQLException("Email déjà utilisé: " + u.getEmail());
        }
        if (!doInsertUser(u)) {
            throw new SQLException("Échec de l'ajout de l'utilisateur");
        }
        System.out.println("✅ Utilisateur ajouté: " + u.getEmail());
    }

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
            int affectedRows = ps.executeUpdate();
            if (affectedRows > 0) {
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) u.setId(rs.getInt(1));
                return true;
            }
        } catch (SQLException e) {
            return doInsertUserWithoutCreatedAt(u);
        }
        return false;
    }

    private boolean doInsertUserWithoutCreatedAt(User u) {
        String sql = "INSERT INTO utilisateur (nom, email, password, telephone, date_naissance, role_id, status) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            String hashedPassword = u.getPassword();
            if (!PasswordHasher.isHashed(hashedPassword)) hashedPassword = PasswordHasher.hashPassword(hashedPassword);
            ps.setString(1, u.getNom());
            ps.setString(2, u.getEmail());
            ps.setString(3, hashedPassword);
            ps.setString(4, u.getTelephone());
            ps.setDate(5, u.getDateNaissance() != null ? Date.valueOf(u.getDateNaissance()) : null);
            ps.setInt(6, u.getRoleId());
            ps.setString(7, u.getStatus() != null ? u.getStatus() : "actif");
            if (ps.executeUpdate() > 0) {
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) u.setId(rs.getInt(1));
                LOG.info("Utilisateur ajouté (sans created_at): " + u.getEmail());
                return true;
            }
        } catch (SQLException e2) {
            LOG.log(Level.SEVERE, "Erreur ajout utilisateur: " + u.getEmail(), e2);
        }
        return false;
    }

    /**
     * Authentification sécurisée avec BCrypt
     */
    public User authenticate(String email, String password) {
        String sql = "SELECT u.*, r.nom_role FROM utilisateur u LEFT JOIN role r ON u.role_id = r.id WHERE u.email = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String storedHash = rs.getString("password");
                if (storedHash == null) return null;
                if (PasswordHasher.isHashed(storedHash)) {
                    if (!PasswordHasher.verifyPassword(password, storedHash)) return null;
                } else {
                    if (!password.equals(storedHash)) return null;
                }
                User user = mapResultSet(rs);
                updateLastLogin(user.getId());
                System.out.println("✅ Authentification réussie: " + user.getNom());
                return user;
            }
        } catch (SQLException e) {
            try {
                sql = "SELECT * FROM utilisateur WHERE email = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, email);
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        String storedHash = rs.getString("password");
                        if (PasswordHasher.isHashed(storedHash)) {
                            if (!PasswordHasher.verifyPassword(password, storedHash)) return null;
                        } else {
                            if (!password.equals(storedHash)) return null;
                        }
                        User user = mapResultSetNoJoin(rs);
                        updateLastLogin(user.getId());
                        return user;
                    }
                }
            } catch (SQLException e2) {
                LOG.log(Level.WARNING, "Erreur authenticate fallback: " + email, e2);
            }
        }
        return null;
    }

    private void updateLastLogin(int userId) {
        try {
            String sql = "UPDATE utilisateur SET last_login = ? WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
                ps.setInt(2, userId);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            // Colonne last_login peut être absente
        }
    }

    @Override
    public List<User> recupererUser() throws SQLException {
        return queryAllUsers("SELECT u.*, r.nom_role FROM utilisateur u LEFT JOIN role r ON u.role_id = r.id",
            "SELECT * FROM utilisateur");
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
            } catch (SQLException e2) {
                LOG.log(Level.WARNING, "Erreur queryAllUsers fallback", e2);
                throw e2;
            }
        }
    }

    public User getById(int id) {
        return queryOneUserById(id);
    }

    private User queryOneUserById(int id) {
        try {
            String sql = "SELECT u.*, r.nom_role FROM utilisateur u LEFT JOIN role r ON u.role_id = r.id WHERE u.id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) return mapResultSet(rs);
            }
        } catch (SQLException e) {
            try {
                String sql = "SELECT * FROM utilisateur WHERE id = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, id);
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) return mapResultSetNoJoin(rs);
                }
            } catch (SQLException e2) {
                LOG.log(Level.WARNING, "Erreur getById: " + id, e2);
            }
        }
        return null;
    }

    public User getByEmail(String email) {
        return queryOneUserByEmail(email);
    }

    /** Alias pour compatibilité avec l'ancien code (ex. MotDePasseOublie dans gui/). */
    public User getUserByEmail(String email) {
        return getByEmail(email);
    }

    /** Alias pour compatibilité avec l'ancien code. */
    public boolean updatePassword(int userId, String newPassword) {
        return changerMotDePasse(userId, newPassword);
    }

    private User queryOneUserByEmail(String email) {
        try {
            String sql = "SELECT u.*, r.nom_role FROM utilisateur u LEFT JOIN role r ON u.role_id = r.id WHERE u.email = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, email);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) return mapResultSet(rs);
            }
        } catch (SQLException e) {
            try {
                String sql = "SELECT * FROM utilisateur WHERE email = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, email);
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) return mapResultSetNoJoin(rs);
                }
            } catch (SQLException e2) {
                LOG.log(Level.WARNING, "Erreur getByEmail: " + email, e2);
            }
        }
        return null;
    }

    @Override
    public void modifierUser(User u) throws SQLException {
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
        }
    }

    /**
     * Change le mot de passe (avec hashage). Utilisé aussi pour "mot de passe oublié".
     */
    public boolean changerMotDePasse(int userId, String nouveauMotDePasse) {
        String hashedPassword = PasswordHasher.hashPassword(nouveauMotDePasse);
        String sql = "UPDATE utilisateur SET password = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, hashedPassword);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Erreur changerMotDePasse", e);
        }
        return false;
    }

    @Override
    public void supprimerUser(int id) throws SQLException {
        String sql = "DELETE FROM utilisateur WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            if (ps.executeUpdate() == 0) {
                throw new SQLException("Aucun utilisateur supprimé pour id=" + id);
            }
        }
    }

    // ============================================
    // VÉRIFICATIONS
    // ============================================

    public boolean emailExiste(String email) {
        String sql = "SELECT count(*) FROM utilisateur WHERE email = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Erreur emailExiste", e);
        }
        return false;
    }

    public boolean verifierTelephone(String email, String telephone) {
        String sql = "SELECT count(*) FROM utilisateur WHERE email = ? AND telephone = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, telephone);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Erreur verifierTelephone", e);
        }
        return false;
    }

    // ============================================
    // STATISTIQUES
    // ============================================

    public int countUsers() {
        String sql = "SELECT count(*) FROM utilisateur";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Erreur countUsers", e);
        }
        return 0;
    }

    public int countUsersByRole(String roleName) {
        try {
            String sql = "SELECT count(*) FROM utilisateur u JOIN role r ON u.role_id = r.id WHERE r.nom_role = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, roleName);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            return countUsersByRoleId(roleName.equalsIgnoreCase("ADMIN") ? 1 : 2);
        }
        return 0;
    }

    private int countUsersByRoleId(int roleId) {
        String sql = "SELECT count(*) FROM utilisateur WHERE role_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, roleId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Erreur countUsersByRoleId", e);
        }
        return 0;
    }

    public int countActiveUsers() {
        String sql = "SELECT count(*) FROM utilisateur WHERE status = 'actif'";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Erreur countActiveUsers", e);
        }
        return 0;
    }

    public List<User> getRecentUsers(int limit) {
        List<User> users = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT u.*, r.nom_role FROM utilisateur u LEFT JOIN role r ON u.role_id = r.id ORDER BY u.created_at DESC LIMIT ?")) {
            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) users.add(mapResultSet(rs));
        } catch (SQLException e) {
            try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM utilisateur ORDER BY id DESC LIMIT ?")) {
                ps.setInt(1, limit);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) users.add(mapResultSetNoJoin(rs));
            } catch (SQLException e2) {
                LOG.log(Level.WARNING, "Erreur getRecentUsers fallback", e2);
            }
        }
        return users;
    }

    // ============================================
    // MAPPER
    // ============================================

    private User mapResultSet(ResultSet rs) throws SQLException {
        String nom = rs.getString("nom");
        String email = rs.getString("email");
        String pass = rs.getString("password");
        String tel = rs.getString("telephone");
        Date dateSql = rs.getDate("date_naissance");
        java.time.LocalDate dateN = dateSql != null ? dateSql.toLocalDate() : null;
        int roleId = rs.getInt("role_id");
        String status = rs.getString("status");
        if (status == null) status = "actif";
        User user = new User(nom, email, pass, tel, dateN, roleId, status);
        user.setId(rs.getInt("id"));
        user.setNom(nom);
        user.setEmail(email);
        user.setPassword(pass);
        user.setTelephone(tel);
        if (dateN != null) user.setDateNaissance(dateN);
        user.setStatus(status);
        try {
            Timestamp t = rs.getTimestamp("last_login");
            if (t != null) user.setLastLogin(t.toLocalDateTime());
        } catch (SQLException ignored) {}
        try {
            Timestamp t = rs.getTimestamp("created_at");
            if (t != null) user.setCreatedAt(t.toLocalDateTime());
        } catch (SQLException ignored) {}
        String nomRole = null;
        try { nomRole = rs.getString("nom_role"); } catch (SQLException ignored) {}
        Role role = (nomRole != null && !nomRole.isEmpty()) ? new Role(roleId, nomRole) : roleService.getById(roleId);
        user.setRole(role);
        return user;
    }

    private User mapResultSetNoJoin(ResultSet rs) throws SQLException {
        String nom = rs.getString("nom");
        String email = rs.getString("email");
        String pass = rs.getString("password");
        String tel = rs.getString("telephone");
        Date dateSql = rs.getDate("date_naissance");
        java.time.LocalDate dateN = dateSql != null ? dateSql.toLocalDate() : null;
        int roleId = rs.getInt("role_id");
        String status = rs.getString("status");
        if (status == null) status = "actif";
        User user = new User(nom, email, pass, tel, dateN, roleId, status);
        user.setId(rs.getInt("id"));
        user.setNom(nom);
        user.setEmail(email);
        user.setPassword(pass);
        user.setTelephone(tel);
        if (dateN != null) user.setDateNaissance(dateN);
        user.setStatus(status);
        try {
            Timestamp t = rs.getTimestamp("last_login");
            if (t != null) user.setLastLogin(t.toLocalDateTime());
        } catch (SQLException ignored) {}
        try {
            Timestamp t = rs.getTimestamp("created_at");
            if (t != null) user.setCreatedAt(t.toLocalDateTime());
        } catch (SQLException ignored) {}
        user.setRole(roleService.getById(roleId));
        return user;
    }
}
