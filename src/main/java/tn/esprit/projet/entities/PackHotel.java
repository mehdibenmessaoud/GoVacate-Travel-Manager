package tn.esprit.projet.entities;

import java.util.Objects;

public class PackHotel {
    private int id; // [cite: 51]
    private int packId; // [cite: 52]
    private int hotelId; // [cite: 53]
    private int nightsIncluded; //

    public PackHotel() {}

    public PackHotel(int id, int packId, int hotelId, int nightsIncluded) {
        this.id = id;
        this.packId = packId;
        this.hotelId = hotelId;
        this.nightsIncluded = nightsIncluded;
    }

    // Getters et Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getPackId() { return packId; }
    public void setPackId(int packId) { this.packId = packId; }

    public int getHotelId() { return hotelId; }
    public void setHotelId(int hotelId) { this.hotelId = hotelId; }

    public int getNightsIncluded() { return nightsIncluded; }
    public void setNightsIncluded(int nightsIncluded) { this.nightsIncluded = nightsIncluded; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PackHotel that = (PackHotel) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "PackHotel{" + "id=" + id + ", nights=" + nightsIncluded + "}";
    }
}
