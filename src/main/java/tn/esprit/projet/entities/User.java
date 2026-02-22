package tn.esprit.projet.entities;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class User {
    private int id;
    private String nom;
    private String email;
    private String password;  // Hashé avec BCrypt
    private Role role;
    private String telephone;
    private LocalDate dateNaissance;
    private String status;  // "actif", "inactif", "en_attente"
    private String imageUrl;  // URL ou chemin vers la photo de profil (colonne image_url en BDD)
    private LocalDateTime lastLogin;  // Dernière connexion
    private LocalDateTime createdAt;  // Date création

    /** Constructeur pour création (inscription, CRUD). */
    public User(String nom, String email, String pass, String tel, LocalDate dateN, int roleId, String status) {
        this.nom = nom;
        this.email = email;
        this.password = pass;
        this.telephone = tel;
        this.dateNaissance = dateN;
        this.role = new Role(roleId, roleId == 1 ? "ADMIN" : "CLIENT");
        this.status = (status != null && !status.isEmpty()) ? status : "actif";
        this.createdAt = LocalDateTime.now();
    }

    // Constructeur complet
    public User(int id, String nom, String email, String password,
                Role role, String telephone, LocalDate dateNaissance,
                String status, LocalDateTime lastLogin, LocalDateTime createdAt) {
        this.id = id;
        this.nom = nom;
        this.email = email;
        this.password = password;
        this.role = role;
        this.telephone = telephone;
        this.dateNaissance = dateNaissance;
        this.status = status;
        this.lastLogin = lastLogin;
        this.createdAt = createdAt;
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    // Helper pour compatibilité avec ancien code
    public void setRoleId(int roleId) {
        this.role = new Role(roleId, roleId == 1 ? "ADMIN" : "CLIENT");
    }

    public int getRoleId() {
        return role != null ? role.getId() : 2;
    }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public LocalDate getDateNaissance() { return dateNaissance; }
    public void setDateNaissance(LocalDate dateNaissance) { this.dateNaissance = dateNaissance; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public LocalDateTime getLastLogin() { return lastLogin; }
    public void setLastLogin(LocalDateTime lastLogin) { this.lastLogin = lastLogin; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    // ============================================
    // MÉTHODES DE VÉRIFICATION DE RÔLE
    // ============================================

    public boolean isAdmin() {
        return role != null && role.isAdmin();
    }

    public boolean isClient() {
        return role != null && role.isClient();
    }

    public String getRoleName() {
        return role != null ? role.getNomRole() : "UNKNOWN";
    }

    public boolean isActive() {
        return "actif".equalsIgnoreCase(status);
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", nom='" + nom + '\'' +
                ", email='" + email + '\'' +
                ", role=" + getRoleName() +
                ", status='" + status + '\'' +
                ", lastLogin=" + lastLogin +
                '}';
    }
}
