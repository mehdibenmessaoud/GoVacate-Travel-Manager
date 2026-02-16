package tn.esprit.projet.test;

// IMPORTATIONS INDISPENSABLES
import tn.esprit.projet.entities.Reservation;
import tn.esprit.projet.entities.ReservationRestaurant;
import tn.esprit.projet.entities.StatutReservation;
import tn.esprit.projet.services.ReservationRestaurantServiceImpl;
import java.time.LocalDate;

public class TestRes {
    public static void main(String[] args) {
        ReservationRestaurantServiceImpl service = new ReservationRestaurantServiceImpl();

        try {
            Reservation res = new Reservation();
            res.setPrix_total(120.0);
            res.setStatut(StatutReservation.EN_ATTENTE);
            res.setType_res("RESTAURANT");
            res.setDate_debut(LocalDate.now());
            res.setNombre_personnes(4);
            res.setUser_id(1L); // <-- TRÈS IMPORTANT : Doit exister dans ta table 'user'

            ReservationRestaurant rr = new ReservationRestaurant();
            rr.setRestaurant_id(1L);
            rr.setDate_reservation(LocalDate.now());
            rr.setNombre_personnes(4);
            rr.setPrix(120.0);

            service.createFullReservation(res, rr);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}