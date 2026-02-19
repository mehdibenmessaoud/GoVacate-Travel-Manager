package tn.esprit.projet.entities;



import jakarta.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "reservation_pack")
public class ReservationPack {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long reservation_id;
    private Long pack_id;
    private double prix_pack;

    public ReservationPack() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public double getPrix_pack() {
        return prix_pack;
    }

    public void setPrix_pack(double prix_pack) {
        this.prix_pack = prix_pack;
    }

    public Long getPack_id() {
        return pack_id;
    }

    public void setPack_id(Long pack_id) {
        this.pack_id = pack_id;
    }

    public Long getReservation_id() {
        return reservation_id;
    }

    public void setReservation_id(Long reservation_id) {
        this.reservation_id = reservation_id;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ReservationPack rp && Objects.equals(id, rp.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
