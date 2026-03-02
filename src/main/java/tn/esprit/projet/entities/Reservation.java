package tn.esprit.projet.entities;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "reservation")
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private StatutReservation statut;

    private LocalDate date_debut;
    private LocalDate date_fin;

    private int nombre_personnes;
    private double prix_total;

    @Column(columnDefinition = "TEXT")
    private String commentaire_client;

    private LocalDateTime created_at;
    private LocalDateTime updated_at;

    private Long user_id;
    private String type_res;
    // Constructors
    public Reservation() {}

    public Reservation(StatutReservation statut, LocalDate date_debut, LocalDate date_fin,
                       int nombre_personnes, double prix_total, String commentaire_client, Long user_id) {
        this.statut = statut;
        this.date_debut = date_debut;
        this.date_fin = date_fin;
        this.nombre_personnes = nombre_personnes;
        this.prix_total = prix_total;
        this.commentaire_client = commentaire_client;
        this.user_id = user_id;
    }

    // Getters & Setters
    public Long getId() { return id; }
    public StatutReservation getStatut() { return statut; }
    public void setStatut(StatutReservation statut) { this.statut = statut; }

    public LocalDate getDate_debut() { return date_debut; }
    public void setDate_debut(LocalDate date_debut) { this.date_debut = date_debut; }

    public LocalDate getDate_fin() { return date_fin; }
    public void setDate_fin(LocalDate date_fin) { this.date_fin = date_fin; }

    public int getNombre_personnes() { return nombre_personnes; }
    public void setNombre_personnes(int nombre_personnes) { this.nombre_personnes = nombre_personnes; }

    public double getPrix_total() { return prix_total; }
    public void setPrix_total(double prix_total) { this.prix_total = prix_total; }

    public String getCommentaire_client() { return commentaire_client; }
    public void setCommentaire_client(String commentaire_client) { this.commentaire_client = commentaire_client; }

    public LocalDateTime getCreated_at() { return created_at; }
    public LocalDateTime getUpdated_at() { return updated_at; }

    public Long getUser_id() { return user_id; }
    public void setUser_id(Long user_id) { this.user_id = user_id; }

    public void setId(Long id) {
        this.id = id;
    }

    public void setCreated_at(LocalDateTime created_at) {
        this.created_at = created_at;
    }

    public void setUpdated_at(LocalDateTime updated_at) {
        this.updated_at = updated_at;
    }
    public String getType_res() {
        return type_res;
    }

    public void setType_res(String type_res) {
        this.type_res = type_res;
    }
    // equals & hashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Reservation)) return false;
        Reservation that = (Reservation) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    // toString
    @Override
    public String toString() {
        return "Reservation{id=" + id + ", statut=" + statut + ", prix_total=" + prix_total + "}";
    }
}
