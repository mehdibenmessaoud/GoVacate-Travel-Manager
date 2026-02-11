package tn.esprit.projet.entities;

import java.util.Objects;

public class ReviewImage {

    private int id;
    private String imageUrl;
    private int reviewId;

    // Constructeurs
    public ReviewImage() {}

    public ReviewImage(int id, String imageUrl, int reviewId) {
        this.id = id;
        this.imageUrl = imageUrl;
        this.reviewId = reviewId;
    }

    // Getters & Setters

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public int getReviewId() {
        return reviewId;
    }

    public void setReviewId(int reviewId) {
        this.reviewId = reviewId;
    }

    // toString pour le débogage
    @Override
    public String toString() {
        return "ReviewImage{" +
                "id=" + id +
                ", imageUrl='" + imageUrl + '\'' +
                ", reviewId=" + reviewId +
                '}';
    }

    // Equals & HashCode (Optionnel mais recommandé pour les listes)
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReviewImage that = (ReviewImage) o;
        return id == that.id && reviewId == that.reviewId && Objects.equals(imageUrl, that.imageUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, imageUrl, reviewId);
    }
}