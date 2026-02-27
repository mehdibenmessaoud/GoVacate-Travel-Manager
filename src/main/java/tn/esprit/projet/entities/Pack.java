package tn.esprit.projet.entities;

import java.time.LocalDate;
import java.util.Objects;

public class Pack {
    private int id;
    private String name;
    private String description;
    private String categorie;
    private double prix;
    private int duree;
    private String status;
    private LocalDate dateDepart;
    private LocalDate dateArriver;

    public Pack() {}

    public Pack(int id, String name, String description, String categorie, double prix, int duree, String status, LocalDate dateDepart, LocalDate dateArriver) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.categorie = categorie;
        this.prix = prix;
        this.duree = duree;
        this.status = status;
        this.dateDepart = dateDepart;
        this.dateArriver = dateArriver;
    }

    // Getters et Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategorie() { return categorie; }
    public void setCategorie(String categorie) { this.categorie = categorie; }

    public double getPrix() { return prix; }
    public void setPrix(double prix) { this.prix = prix; }

    public int getDuree() { return duree; }
    public void setDuree(int duree) { this.duree = duree; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDate getDateDepart() { return dateDepart; }
    public void setDateDepart(LocalDate dateDepart) { this.dateDepart = dateDepart; }

    public LocalDate getDateArriver() { return dateArriver; }
    public void setDateArriver(LocalDate dateArriver) { this.dateArriver = dateArriver; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pack pack = (Pack) o;
        return id == pack.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Pack{" + "id=" + id + ", name='" + name + '\'' + ", prix=" + prix + ", status='" + status + '\'' + '}';
    }
}
