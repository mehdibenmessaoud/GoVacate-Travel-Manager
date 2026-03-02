package tn.esprit.projet.entities;

import java.time.LocalDate;

public class ReservationHotelChambre {

    private int       id;
    private int       reservationId;
    private int       hotelId;
    private Integer   chambreId;
    private LocalDate dateCheckin;
    private LocalDate dateCheckout;
    private double    prix;
    private int       nbPersonnes = 1;
    private int       userId      = 1;


    public ReservationHotelChambre(int id, int reservationId, int hotelId, Integer chambreId, LocalDate dateCheckin, LocalDate dateCheckout, double prix) {
        this.id = id;
        this.reservationId = reservationId;
        this.hotelId = hotelId;
        this.chambreId = chambreId;
        this.dateCheckin = dateCheckin;
        this.dateCheckout = dateCheckout;
        this.prix = prix;
    }


    public ReservationHotelChambre() {}


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getReservationId() {
        return reservationId;
    }

    public void setReservationId(int reservationId) {
        this.reservationId = reservationId;
    }

    public int getHotelId() {
        return hotelId;
    }

    public void setHotelId(int hotelId) {
        this.hotelId = hotelId;
    }

    public Integer getChambreId() {
        return chambreId;
    }

    public void setChambreId(Integer chambreId) {
        this.chambreId = chambreId;
    }

    public LocalDate getDateCheckin() {
        return dateCheckin;
    }

    public void setDateCheckin(LocalDate dateCheckin) {
        this.dateCheckin = dateCheckin;
    }

    public LocalDate getDateCheckout() {
        return dateCheckout;
    }

    public void setDateCheckout(LocalDate dateCheckout) {
        this.dateCheckout = dateCheckout;
    }

    public double getPrix() {
        return prix;
    }

    public void setPrix(double prix) {
        this.prix = prix;
    }

    public int getNbPersonnes() {
        return nbPersonnes;
    }

    public void setNbPersonnes(int nbPersonnes) {
        this.nbPersonnes = nbPersonnes;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }


    @Override
    public String toString() {
        return "Reservation{" +
                "id=" + id +
                ", reservationId=" + reservationId +
                ", hotelId=" + hotelId +
                ", chambreId=" + chambreId +
                ", dateCheckin=" + dateCheckin +
                ", dateCheckout=" + dateCheckout +
                ", prix=" + prix +
                '}';
    }
}