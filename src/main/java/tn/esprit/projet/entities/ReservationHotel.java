package tn.esprit.projet.entities;



import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name = "reservation_hotel")
public class ReservationHotel {

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getReservation_id() {
        return reservation_id;
    }

    public void setReservation_id(Long reservation_id) {
        this.reservation_id = reservation_id;
    }

    public Long getHotel_id() {
        return hotel_id;
    }

    public void setHotel_id(Long hotel_id) {
        this.hotel_id = hotel_id;
    }

    public Long getChambre_id() {
        return chambre_id;
    }

    public void setChambre_id(Long chambre_id) {
        this.chambre_id = chambre_id;
    }

    public LocalDate getDate_checkin() {
        return date_checkin;
    }

    public void setDate_checkin(LocalDate date_checkin) {
        this.date_checkin = date_checkin;
    }

    public LocalDate getDate_checkout() {
        return date_checkout;
    }

    public void setDate_checkout(LocalDate date_checkout) {
        this.date_checkout = date_checkout;
    }

    public double getPrix() {
        return prix;
    }

    public void setPrix(double prix) {
        this.prix = prix;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long reservation_id;
    private Long hotel_id;
    private Long chambre_id;

    private LocalDate date_checkin;
    private LocalDate date_checkout;

    private double prix;

    public ReservationHotel() {}

    public ReservationHotel(Long reservation_id, Long hotel_id, LocalDate date_checkin,
                            LocalDate date_checkout, double prix) {
        this.reservation_id = reservation_id;
        this.hotel_id = hotel_id;
        this.date_checkin = date_checkin;
        this.date_checkout = date_checkout;
        this.prix = prix;
    }

    // getters setters + equals hashcode
    @Override
    public boolean equals(Object o) {
        return o instanceof ReservationHotel rh && Objects.equals(id, rh.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
