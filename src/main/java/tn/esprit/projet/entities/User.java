package tn.esprit.projet.entities;

import java.util.Objects;

public class User {
    private int id;
    private String nom;
    private String email;
    private String password;
    private int role_id; // 1 pour Admin, 2 pour Client

    // Constructeur pour l'ajout (sans ID)
    public User(String nom, String email, String password, int role_id) {
        this.nom = nom;
        this.email = email;
        this.password = password;
        this.role_id = role_id;
    }

    // Constructeur pour la récupération (avec ID)
    public User(int id, String nom, String email, String password, int role_id) {
        this.id = id;
        this.nom = nom;
        this.email = email;
        this.password = password;
        this.role_id = role_id;
    }

    // Getters et Setters (Indispensables pour JavaFX)
    public String getNom() { return nom; }
    public String getEmail() { return email; }
    public int getRole_id() { return role_id; }

    public int getId() {
        return id;
    }

    public String getPassword() {
        return password;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }
    public void setEmail(String email) {}
    public void setPassword(String password) {}
    public void setRole_id(int role_id) {
        this.role_id = role_id;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", nom='" + nom + '\'' +
                ", email='" + email + '\'' +
                ", password='" + password + '\'' +
                ", role_nom=" + role_id +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return id == user.id && role_id == user.role_id && Objects.equals(nom, user.nom) && Objects.equals(email, user.email) && Objects.equals(password, user.password);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, nom, email, password, role_id);
    }
}