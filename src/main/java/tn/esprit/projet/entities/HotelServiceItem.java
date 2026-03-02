package tn.esprit.projet.entities;

/**
 * Represents a service offered by a hotel.
 */
public class HotelServiceItem {

    private int id;
    private int hotelId;
    private String name;
    private boolean active;

    public HotelServiceItem() {
    }

    public HotelServiceItem(int id, int hotelId, String name, boolean active) {
        this.id = id;
        this.hotelId = hotelId;
        this.name = name;
        this.active = active;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getHotelId() {
        return hotelId;
    }

    public void setHotelId(int hotelId) {
        this.hotelId = hotelId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public String toString() {
        return name == null ? "Service" : name;
    }
}
