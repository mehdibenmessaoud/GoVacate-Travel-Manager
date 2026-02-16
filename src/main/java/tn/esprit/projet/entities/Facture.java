package tn.esprit.projet.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "facture")
public class Facture {
    public void setId(Long id) {
        this.id = id;
    }

    public void setDate_facture(LocalDateTime date_facture) {
        this.date_facture = date_facture;
    }

    public void setDate_paiement(LocalDateTime date_paiement) {
        this.date_paiement = date_paiement;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private double montant;

    @Enumerated(EnumType.STRING)
    private MethodePaiement methode_paiement;

    @Enumerated(EnumType.STRING)
    private StatutFacture statut;

    private LocalDateTime date_facture;
    private LocalDateTime date_paiement;

    private Long reservation_id;

    public Facture() {}

    public Facture(double montant, MethodePaiement methode_paiement, StatutFacture statut, Long reservation_id) {
        this.montant = montant;
        this.methode_paiement = methode_paiement;
        this.statut = statut;
        this.reservation_id = reservation_id;
    }

    // Getters & Setters
    public Long getId() { return id; }
    public double getMontant() { return montant; }
    public void setMontant(double montant) { this.montant = montant; }

    public MethodePaiement getMethode_paiement() { return methode_paiement; }
    public void setMethode_paiement(MethodePaiement methode_paiement) { this.methode_paiement = methode_paiement; }

    public StatutFacture getStatut() { return statut; }
    public void setStatut(StatutFacture statut) { this.statut = statut; }


    public LocalDateTime getDate_facture() { return date_facture; }
    public LocalDateTime getDate_paiement() { return date_paiement; }

    public Long getReservation_id() { return reservation_id; }
    public void setReservation_id(Long reservation_id) { this.reservation_id = reservation_id; }

    @Override
    public boolean equals(Object o) {
        return o instanceof Facture f && Objects.equals(id, f.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

