package tn.esprit.projet.test;

import tn.esprit.projet.entities.*;
import tn.esprit.projet.services.*;
import tn.esprit.projet.utils.MyDBConnexion1;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Test class for inserting sample data into the database.
 * Run this to populate the database with test hotels, rooms, reviews and images.
 */
public class Test {
    
    public static void main(String[] args) {
        System.out.println("=== GoVacate - Insertion des donnees de test ===\n");

        Connection cnx = MyDBConnexion1.getInstance().getConnection();
        
        HotelService hotelService = new HotelService();
        RoomService roomService = new RoomService();
        HotelReviewService reviewService = new HotelReviewService();
        HotelImageService hotelImageService = new HotelImageService();
        RoomImageService roomImageService = new RoomImageService();
        HotelServiceItemService hotelServiceItemService = new HotelServiceItemService();

        try {
            // Create hotels
            System.out.println("\n--- Creation des hotels ---");
            Hotel h1 = new Hotel(0, "Hotel du Louvre Paris", "Hotel business haut de gamme au coeur de Paris proche du Louvre", 5, "AVAILABLE", 1);
            Hotel h2 = new Hotel(0, "Le Marais Boutique Paris", "Boutique hotel chic dans le quartier du Marais avec spa et rooftop", 4, "AVAILABLE", 1);
            Hotel h3 = new Hotel(0, "Roma Centro Grand Hotel", "Hotel confortable en centre-ville de Rome ideal pour familles et couples", 3, "MAINTENANCE", 2);
            Hotel h4 = new Hotel(0, "Trastevere Palace Rome", "Resort urbain a Rome avec navette aeroport et service premium", 4, "OCCUPIED", 2);
            Hotel h5 = new Hotel(0, "Vatican View Suites Rome", "Hotel de charme avec vue sur Rome et acces rapide au Vatican", 5, "AVAILABLE", 3);

            hotelService.create(h1);
            hotelService.create(h2);
            hotelService.create(h3);
            hotelService.create(h4);
            hotelService.create(h5);
            System.out.println("5 hotels crees avec succes");

            var hotels = hotelService.getAll();
            int idH1 = hotels.get(0).getId();
            int idH2 = hotels.get(1).getId();
            int idH3 = hotels.get(2).getId();

            // Create hotel services (matching the provided list)
            System.out.println("\n--- Creation des services hotel ---");
            String[] hotelServices = {
                    "Reception 24h/24",
                    "Wi-Fi gratuit",
                    "Climatisation",
                    "Service chambre",
                    "Petit-dejeuner inclus",
                    "Parking",
                    "Conciergerie",
                    "Navette aeroport",
                    "Piscine",
                    "Spa"
            };
            for (String serviceName : hotelServices) {
                HotelServiceItem item = new HotelServiceItem(0, idH1, serviceName, true);
                hotelServiceItemService.create(item);
            }
            System.out.println("10 services hotel crees pour Hotel du Louvre Paris");

            // Create rooms
            System.out.println("\n--- Creation des chambres ---");
            Room r1 = new Room(0, "101", "SINGLE", 1, 100.0, "AVAILABLE", idH1);
            Room r2 = new Room(0, "102", "DOUBLE", 2, 150.0, "AVAILABLE", idH1);
            Room r3 = new Room(0, "103", "SUITE", 4, 300.0, "OCCUPIED", idH1);
            Room r4 = new Room(0, "201", "SINGLE", 1, 80.0, "AVAILABLE", idH2);
            Room r5 = new Room(0, "202", "DOUBLE", 2, 120.0, "MAINTENANCE", idH2);
            Room r6 = new Room(0, "301", "FAMILY", 5, 200.0, "AVAILABLE", idH3);

            roomService.create(r1);
            roomService.create(r2);
            roomService.create(r3);
            roomService.create(r4);
            roomService.create(r5);
            roomService.create(r6);
            System.out.println("6 chambres creees avec succes");

            var rooms = roomService.getAll();
            int idR1 = rooms.get(0).getId();
            int idR2 = rooms.get(1).getId();

            // Create reviews
            System.out.println("\n--- Creation des avis ---");
            HotelReview rev1 = new HotelReview(0, 5, "Excellent sejour a Paris, equipe professionnelle et chambre impeccable.", 1, idH1);
            HotelReview rev2 = new HotelReview(0, 4, "Tres belle experience dans le Marais, petit-dejeuner varie et service rapide.", 2, idH1);
            HotelReview rev3 = new HotelReview(0, 3, "Sejour correct a Rome, bon emplacement mais un peu de bruit le soir.", 1, idH2);
            HotelReview rev4 = new HotelReview(0, 5, "Weekend parfait a Rome, quartier superbe et personnel attentionne.", 3, idH3);
            HotelReview rev5 = new HotelReview(0, 4, "Tres bon rapport qualite prix entre Paris et Rome, hotel calme et propre.", 4, idH2);

            reviewService.create(rev1);
            reviewService.create(rev2);
            reviewService.create(rev3);
            reviewService.create(rev4);
            reviewService.create(rev5);
            System.out.println("5 avis crees avec succes");

            // Create hotel images
            System.out.println("\n--- Creation des images hotels ---");
            HotelImage hi1 = new HotelImage(0, "hotels/hotel_a_1.jpg", idH1);
            HotelImage hi2 = new HotelImage(0, "hotels/hotel_a_2.jpg", idH1);
            HotelImage hi3 = new HotelImage(0, "hotels/hotel_b_1.jpg", idH2);

            hotelImageService.create(hi1);
            hotelImageService.create(hi2);
            hotelImageService.create(hi3);
            System.out.println("3 images hotels creees");

            // Create room images
            System.out.println("\n--- Creation des images chambres ---");
            RoomImage ri1 = new RoomImage(0, "rooms/room_101_1.jpg", idR1);
            RoomImage ri2 = new RoomImage(0, "rooms/room_101_2.jpg", idR1);
            RoomImage ri3 = new RoomImage(0, "rooms/room_102_1.jpg", idR2);

            roomImageService.create(ri1);
            roomImageService.create(ri2);
            roomImageService.create(ri3);
            System.out.println("3 images chambres creees");

            // Summary
            System.out.println("\n=== Resume ===");
            System.out.println("Hotels: " + hotelService.getAll().size());
            System.out.println("Chambres: " + roomService.getAll().size());
            System.out.println("Avis: " + reviewService.getAll().size());
            System.out.println("Services hotel: " + hotelServiceItemService.getAll().size());
            System.out.println("Images hotels: " + hotelImageService.getAll().size());
            System.out.println("Images chambres: " + roomImageService.getAll().size());
            System.out.println("\nDonnees de test inserees avec succes!");

        } catch (SQLException e) {
            System.err.println("Erreur SQL: " + e.getMessage());
        }
    }
}
