package tn.esprit.projet.entities;


import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name = "reservation_restaurant")
public class ReservationRestaurant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reservation_id", nullable = false)
    private Long reservation_id;

    @Column(name = "restaurant_id", nullable = false)
    private Long restaurant_id;

    @Column(name = "date_reservation", nullable = false)
    private LocalDate date_reservation;

    @Column(name = "nombre_personnes", nullable = false)
    private int nombre_personnes;

    @Column(name = "prix", nullable = false)
    private double prix;

    // --- Constructors ---
    public ReservationRestaurant() {}

    public ReservationRestaurant(Long reservation_id, Long restaurant_id, LocalDate date_reservation, int nombre_personnes, double prix) {
        this.reservation_id = reservation_id;
        this.restaurant_id = restaurant_id;
        this.date_reservation = date_reservation;
        this.nombre_personnes = nombre_personnes;
        this.prix = prix;
    }

    // --- Getters & Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getReservation_id() { return reservation_id; }
    public void setReservation_id(Long reservation_id) { this.reservation_id = reservation_id; }

    public Long getRestaurant_id() { return restaurant_id; }
    public void setRestaurant_id(Long restaurant_id) { this.restaurant_id = restaurant_id; }

    public LocalDate getDate_reservation() { return date_reservation; }
    public void setDate_reservation(LocalDate date_reservation) { this.date_reservation = date_reservation; }

    public int getNombre_personnes() { return nombre_personnes; }
    public void setNombre_personnes(int nombre_personnes) { this.nombre_personnes = nombre_personnes; }

    public double getPrix() { return prix; }
    public void setPrix(double prix) { this.prix = prix; }

    // --- Equals & HashCode ---
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReservationRestaurant that = (ReservationRestaurant) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "ReservationRestaurant{" +
                "id=" + id +
                ", reservation_id=" + reservation_id +
                ", restaurant_id=" + restaurant_id +
                ", date_reservation=" + date_reservation +
                ", nombre_personnes=" + nombre_personnes +
                ", prix=" + prix +
                '}';
    }
}
