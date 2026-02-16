package tn.esprit.projet.entities;

import java.time.LocalDateTime;

public class HotelReview {

    private int id;
    private int rating;
    private String comment;
    private int userId;
    private int hotelId;
    private LocalDateTime createdAt;

    public HotelReview() {
    }

    public HotelReview(int id, int rating, String comment, int userId, int hotelId) {
        this.id = id;
        this.rating = rating;
        this.comment = comment;
        this.userId = userId;
        this.hotelId = hotelId;
        this.createdAt = LocalDateTime.now();
    }

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

    public int getHotelId() {
        return hotelId;
    }

    public void setHotelId(int hotelId) {
        this.hotelId = hotelId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "HotelReview{" +
                "id=" + id +
                ", rating=" + rating +
                ", comment='" + comment + '\'' +
                ", userId=" + userId +
                ", hotelId=" + hotelId +
                ", createdAt=" + createdAt +
                '}';
    }
}
