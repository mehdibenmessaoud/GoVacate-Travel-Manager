package tn.esprit.projet.entities;

import java.util.Objects;

public class Destination {
    private int id;
    private String nameDestination;
    private String pays;
    private String ville;
    private String image;

    public Destination() {}

    public Destination(int id, String nameDestination, String pays, String ville, String image) {
        this.id = id;
        this.nameDestination = nameDestination;
        this.pays = pays;
        this.ville = ville;
        this.image = image;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    // Vérifiez que ce nom est bien celui utilisé dans PackDetailsController
    public String getNameDestination() { return nameDestination; }
    public void setNameDestination(String nameDestination) { this.nameDestination = nameDestination; }

    public String getPays() { return pays; }
    public void setPays(String pays) { this.pays = pays; }

    public String getVille() { return ville; }
    public void setVille(String ville) { this.ville = ville; }

    @Override
    public String toString() { return nameDestination; }
}