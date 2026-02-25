package tn.esprit.projet.entities;

public class Destination {
    private int id;
    private String nameDestination; // Matches 'name_destination' in DB
    private String pays;
    private String ville;

    public Destination() {}

    public Destination(int id, String nameDestination, String pays, String ville) {
        this.id = id;
        this.nameDestination = nameDestination;
        this.pays = pays;
        this.ville = ville;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNameDestination() { return nameDestination; }
    public void setNameDestination(String nameDestination) { this.nameDestination = nameDestination; }

    public String getPays() { return pays; }
    public void setPays(String pays) { this.pays = pays; }

    public String getVille() { return ville; }
    public void setVille(String ville) { this.ville = ville; }

    // This is useful if you don't use a StringConverter in the ComboBox
    @Override
    public String toString() {
        return nameDestination + " (" + ville + ", " + pays + ")";
    }
}