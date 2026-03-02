package tn.esprit.projet.entities;

import java.math.BigDecimal;
import java.util.Objects;

public class Menu {

    private int id;
    private String name;
    private String description;
    private BigDecimal price;
    private String status; // AVAILABLE / UNAVAILABLE
    private int restaurantId;

    public Menu() {}

    public Menu(int id, String name, String description,
                BigDecimal price, String status, int restaurantId) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.status = status;
        this.restaurantId = restaurantId;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(int restaurantId) {
        this.restaurantId = restaurantId;
    }

    @Override
    public String toString() {
        return "Menu{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", price=" + price +
                ", status='" + status + '\'' +
                ", restaurantId=" + restaurantId +
                '}';
    }


}
