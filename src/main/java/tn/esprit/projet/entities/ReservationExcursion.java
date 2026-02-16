package tn.esprit.projet.entities;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name = "reservation_excursion")
public class ReservationExcursion {
    private String heure_souhaitee; // Ajout de l'attribut
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

    public Long getExcursion_id() {
        return excursion_id;
    }

    public void setExcursion_id(Long excursion_id) {
        this.excursion_id = excursion_id;
    }

    public int getNombre_personnes() {
        return nombre_personnes;
    }

    public void setNombre_personnes(int nombre_personnes) {
        this.nombre_personnes = nombre_personnes;
    }

    public LocalDate getDate_excursion() {
        return date_excursion;
    }

    public void setDate_excursion(LocalDate date_excursion) {
        this.date_excursion = date_excursion;
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
    private Long excursion_id;

    private LocalDate date_excursion;
    private int nombre_personnes;
    private double prix;

    public ReservationExcursion() {}
    // Getter
    public String getHeure_souhaitee() {
        return heure_souhaitee;
    }

    // Setter
    public void setHeure_souhaitee(String heure_souhaitee) {
        this.heure_souhaitee = heure_souhaitee;
    }
    @Override
    public boolean equals(Object o) {
        return o instanceof ReservationExcursion re && Objects.equals(id, re.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
