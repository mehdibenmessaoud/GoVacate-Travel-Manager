package tn.esprit.projet.entities;


public class HotelImage {

    private int id;
    private String imageUrl;
    private int hotelId;

    public HotelImage() {
    }

    public HotelImage(int id, String imageUrl, int hotelId) {
        this.id = id;
        this.imageUrl = imageUrl;
        this.hotelId = hotelId;
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

    public int getHotelId() {
        return hotelId;
    }

    public void setHotelId(int hotelId) {
        this.hotelId = hotelId;
    }

    @Override
    public String toString() {
        return "HotelImage{" +
                "id=" + id +
                ", imageUrl='" + imageUrl + '\'' +
                ", hotelId=" + hotelId +
                '}';
    }
}
