package tn.esprit.projet.entities;

import java.util.Objects;

public class Destination {
    private int id;
    private String nameDestination;
    private String pays;
    private String ville;

    public Destination() {}

    public Destination(int id, String nameDestination, String pays, String ville) {
        this.id = id;
        this.nameDestination = nameDestination;
        this.pays = pays;
        this.ville = ville;
    }

    // Getters et Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNameDestination() { return nameDestination; }
    public void setNameDestination(String nameDestination) { this.nameDestination = nameDestination; }

    public String getPays() { return pays; }
    public void setPays(String pays) { this.pays = pays; }

    public String getVille() { return ville; }
    public void setVille(String ville) { this.ville = ville; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Destination that = (Destination) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Destination{" + "id=" + id + ", nameDestination='" + nameDestination + '\'' + ", pays='" + pays + '\'' + ", ville='" + ville + '\'' + '}';
    }
}
