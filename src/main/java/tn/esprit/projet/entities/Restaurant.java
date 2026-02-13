package tn.esprit.projet.entities;

import java.time.LocalDateTime;

public class Restaurant {
    private int id;
    private String name;
    private String category;
    private String address;
    private String phone;
    private String email;
    private int capacity;
    private String status;
    private int destinationId;

    // Virtual field for UI Search and TableView
    private String destinationName;

    // Timestamps for DB tracking
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Restaurant() {}

    public Restaurant(int id, String name, String category, String address, String phone, String email, int capacity, String status, int destinationId, String destinationName, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.address = address;
        this.phone = phone;
        this.email = email;
        this.capacity = capacity;
        this.status = status;
        this.destinationId = destinationId;
        this.destinationName = destinationName;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Keep your 12-arg constructor, but ADD this one:
    public Restaurant(int id, String name, String category, String address, String phone, String email, int capacity, String status, int destinationId) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.address = address;
        this.phone = phone;
        this.email = email;
        this.capacity = capacity;
        this.status = status;
        this.destinationId = destinationId;
        // The other fields (destinationName, createdAt, updatedAt) stay null/default
        // until the Service populates them from the DB.
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getDestinationId() { return destinationId; }
    public void setDestinationId(int destinationId) { this.destinationId = destinationId; }

    public String getDestinationName() { return destinationName; }
    public void setDestinationName(String destinationName) { this.destinationName = destinationName; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}