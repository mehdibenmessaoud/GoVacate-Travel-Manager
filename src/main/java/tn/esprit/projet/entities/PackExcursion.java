package tn.esprit.projet.entities;

import java.util.Objects;

public class PackExcursion {
    private int id; // [cite: 79]
    private int packId; // [cite: 80]
    private int excursionId; // [cite: 81]
    private boolean isIncluded; //

    public PackExcursion() {}

    public PackExcursion(int id, int packId, int excursionId, boolean isIncluded) {
        this.id = id;
        this.packId = packId;
        this.excursionId = excursionId;
        this.isIncluded = isIncluded;
    }

    // Getters et Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getPackId() { return packId; }
    public void setPackId(int packId) { this.packId = packId; }

    public int getExcursionId() { return excursionId; }
    public void setExcursionId(int excursionId) { this.excursionId = excursionId; }

    public boolean isIncluded() { return isIncluded; }
    public void setIncluded(boolean included) { isIncluded = included; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PackExcursion that = (PackExcursion) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "PackExcursion{" + "id=" + id + ", included=" + isIncluded + "}";
    }
}
