package tn.esprit.projet.entities;

import java.time.LocalDate;

public class Utilisateur {
    private int id;
    private String nom;
    private String email;
    private String password;
    private Integer roleId; // role_id dans la DB
    private String telephone;
    private LocalDate dateNaissance;
    private String status;

    public Utilisateur() {}

    // Getters et Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Integer getRoleId() { return roleId; }
    public void setRoleId(Integer roleId) { this.roleId = roleId; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public LocalDate getDateNaissance() { return dateNaissance; }
    public void setDateNaissance(LocalDate dateNaissance) { this.dateNaissance = dateNaissance; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}