package tn.esprit.projet.entities;

import java.time.LocalDateTime;

public class RestaurantReview {

    private int id;
    private int rating;
    private String comment;
    private int userId;
    private int restaurantId;
    private LocalDateTime createdAt;

    public RestaurantReview() {}

    public RestaurantReview(int id, int rating, String comment,
                            int userId, int restaurantId) {
        this.id = id;
        this.rating = rating;
        this.comment = comment;
        this.userId = userId;
        this.restaurantId = restaurantId;
        this.createdAt = LocalDateTime.now();
    }

    // Getters & Setters


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(int restaurantId) {
        this.restaurantId = restaurantId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "RestaurantReview{" +
                "id=" + id +
                ", rating=" + rating +
                ", comment='" + comment + '\'' +
                ", userId=" + userId +
                ", restaurantId=" + restaurantId +
                ", createdAt=" + createdAt +
                '}';
    }
}