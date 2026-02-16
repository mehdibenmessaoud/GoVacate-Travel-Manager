package tn.esprit.projet.entities;

import java.util.Objects;

public class Hotel {

    private int id;
    private String name;
    private String description;
    private int stars;
    private String status; // OPEN / CLOSED / UNDER_MAINTENANCE
    private int locationId;

    // Constructors
    public Hotel() {}

    public Hotel(int id, String name, String description, int stars,
                 String status, int locationId) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.stars = stars;
        this.status = status;
        this.locationId = locationId;
    }

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getStars() {
        return stars;
    }

    public void setStars(int stars) {
        this.stars = stars;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getLocationId() {
        return locationId;
    }

    public void setLocationId(int locationId) {
        this.locationId = locationId;
    }

    @Override
    public String toString() {
        return "Hotel{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", stars=" + stars +
                ", status='" + status + '\'' +
                ", locationId=" + locationId +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Hotel hotel = (Hotel) o;
        return id == hotel.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}



