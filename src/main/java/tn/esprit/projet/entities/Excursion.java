package tn.esprit.projet.entities;

import java.time.LocalDate;
import java.util.Objects;

public class Excursion {
    private int id;
    private String name;
    private String description;
    private int duration;
    private double price;
    private int maxParticipants;
    private String status;
    private int locationId;
    private String activite;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private String images; // Stockera le chemin ou le nom du fichier



    public Excursion() {
    }

    public Excursion(int id, String name, String description, int duration, double price, int maxParticipants, String status, int locationId, String activite, LocalDate dateDebut, LocalDate dateFin, String images) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.duration = duration;
        this.price = price;
        this.maxParticipants = maxParticipants;
        this.status = status;
        this.locationId = locationId;
        this.activite = activite;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.images = images;

    }


    // Getters et Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getActivite() {
        return activite;
    }

    public void setActivite(String activite) {
        this.activite = activite;
    }

    public int getLocationId() {
        return locationId;
    }

    public void setLocationId(int locationId) {
        this.locationId = locationId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getMaxParticipants() {
        return maxParticipants;
    }

    public void setMaxParticipants(int maxParticipants) {
        this.maxParticipants = maxParticipants;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getDateDebut() {
        return dateDebut;
    }

    public void setDateDebut(LocalDate dateDebut) {
        this.dateDebut = dateDebut;
    }

    public LocalDate getDateFin() {
        return dateFin;
    }

    public void setDateFin(LocalDate dateFin) {
        this.dateFin = dateFin;
    }

    public String getImages() {
        return images;
    }

    public void setImages(String images) {
        this.images = images;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Excursion excursion = (Excursion) o;
        return id == excursion.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Excursion{" + "id=" + id + ", name='" + name + '\'' + ", price=" + price + '}';
    }
}
