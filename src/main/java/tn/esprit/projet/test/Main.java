package tn.esprit.projet.test;

import tn.esprit.projet.entities.*;
import tn.esprit.projet.services.*;
import tn.esprit.projet.utils.MyDBConnexion;

import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;

public class Main {
    public static void main(String[] args) {

        // --- 1. CONNEXION (ACTIVE) ---
        MyDBConnexion db = MyDBConnexion.getInstance();
        Connection conn = db.getConnection();
        if (conn == null) return;
        System.out.println("✅ Connexion active !");

        /* // --- TOUS LES ANCIENS TESTS (COMMENTÉS) ---
           // ReservationServiceImpl ...
           // FactureServiceImpl ...
           // ReservationExcursionServiceImpl ...
           // ReservationHotelServiceImpl ...
           // PackServiceImpl ...
           // ReservationPackServiceImpl ...
        */

        // --- 2. NOUVEAU TEST : RESERVATION_RESTAURANT ---
        System.out.println("\n--- DÉBUT DU TEST RESERVATION_RESTAURANT ---");
        ReservationRestaurantServiceImpl rrService = new ReservationRestaurantServiceImpl();

        // Étape 1 : Création
        ReservationRestaurant rr = new ReservationRestaurant();
        rr.setReservation_id(17L);       // Doit exister en base
        rr.setRestaurant_id(1L);        // Doit exister en base
        rr.setDate_reservation(LocalDate.now().plusDays(2));
        rr.setNombre_personnes(4);
        rr.setPrix(120.0);

        System.out.println(">> Création d'une réservation de restaurant pour Reservation #17...");
        ReservationRestaurant created = rrService.create(rr);

        if (created != null && created.getId() != null) {
            System.out.println("   ✅ Succès ! ID: " + created.getId());

            // Étape 2 : Lecture
            System.out.println("\n>> Liste des réservations restaurant :");
            List<ReservationRestaurant> list = rrService.findAll();
            for (ReservationRestaurant item : list) {
                System.out.println("   - ID: " + item.getId() + " | Resto ID: " + item.getRestaurant_id() + " | Personnes: " + item.getNombre_personnes());
            }

            // Étape 3 : Update (Ajout d'une personne supplémentaire)
            System.out.println("\n>> Mise à jour (Ajout d'une personne) pour l'ID " + created.getId());
            created.setNombre_personnes(5);
            created.setPrix(150.0);
            rrService.update(created);
            System.out.println("   ✅ Nouvelle config : 5 personnes / 150.0€");

        } else {
            System.out.println("   ❌ ÉCHEC : Vérifiez que la Reservation 17 et le Restaurant 1 existent.");
        }

        System.out.println("\n--- FIN DES TESTS ---");
    }
}