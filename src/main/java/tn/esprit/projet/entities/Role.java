package tn.esprit.projet.entities;

public class Role {
    private int id;
    private String nomRole;  // "ADMIN" ou "CLIENT"

    public Role() {}

    public Role(int id, String nomRole) {
        this.id = id;
        this.nomRole = nomRole;
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNomRole() { return nomRole; }
    public void setNomRole(String nomRole) { this.nomRole = nomRole; }

    // Méthodes utilitaires
    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(nomRole);
    }

    public boolean isClient() {
        return "CLIENT".equalsIgnoreCase(nomRole);
    }

    @Override
    public String toString() {
        return nomRole;
    }
}
