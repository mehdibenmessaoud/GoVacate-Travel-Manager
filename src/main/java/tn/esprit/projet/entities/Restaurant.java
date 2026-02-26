package tn.esprit.projet.entities;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public class Restaurant {

    private int id;
    private String name;
    private String category;
    private String address;
    private String phone;
    private String email;
    private int capacity;
    private String status; // OPEN / CLOSED / SUSPENDED
    private int destinationId;
    private String destinationName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructors
    public Restaurant() {}

    public Restaurant(int id, String name, String category, String address,
                      String phone, String email, int capacity,
                      String status, int destinationId) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.address = address;
        this.phone = phone;
        this.email = email;
        this.capacity = capacity;
        this.status = status;
        this.destinationId = destinationId;
    }

    // Getters & Setters

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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getDestinationId() {
        return destinationId;
    }

    public void setDestinationId(int destinationId) {
        this.destinationId = destinationId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    public String getDestinationName() {
        return destinationName;
    }

    // Add this Setter to fix the error
    public void setDestinationName(String destinationName) {
        this.destinationName = destinationName;
    }
    @Override
    public String toString() {
        return "Restaurant{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", category='" + category + '\'' +
                ", address='" + address + '\'' +
                ", phone='" + phone + '\'' +
                ", email='" + email + '\'' +
                ", capacity=" + capacity +
                ", status='" + status + '\'' +
                ", destinationId=" + destinationId +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }


}
