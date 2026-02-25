package tn.esprit.projet.entities;

public class RestaurantImage {

    private int id;
    private String imageUrl;
    private int restaurantId;

    public RestaurantImage() {}

    public RestaurantImage(int id, String imageUrl, int restaurantId) {
        this.id = id;
        this.imageUrl = imageUrl;
        this.restaurantId = restaurantId;
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

    public int getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(int restaurantId) {
        this.restaurantId = restaurantId;
    }

    @Override
    public String toString() {
        return "RestaurantImage{" +
                "id=" + id +
                ", imageUrl='" + imageUrl + '\'' +
                ", restaurantId=" + restaurantId +
                '}';
    }
}