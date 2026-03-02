package tn.esprit.projet.entities;

import java.util.Objects;

public class HotelReviewImage {

    private int id;
    private String imageUrl;
    private int hotelReviewId;

    public HotelReviewImage() {}

    public HotelReviewImage(int id, String imageUrl, int hotelReviewId) {
        this.id = id;
        this.imageUrl = imageUrl;
        this.hotelReviewId = hotelReviewId;
    }

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

    public int getHotelReviewId() {
        return hotelReviewId;
    }

    public void setHotelReviewId(int hotelReviewId) {
        this.hotelReviewId = hotelReviewId;
    }

    @Override
    public String toString() {
        return "HotelReviewImage{" +
                "id=" + id +
                ", imageUrl='" + imageUrl + '\'' +
                ", hotelReviewId=" + hotelReviewId +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HotelReviewImage that = (HotelReviewImage) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
