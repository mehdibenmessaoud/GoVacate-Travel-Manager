package tn.esprit.projet.test;

import tn.esprit.projet.entities.Restaurant;
import tn.esprit.projet.services.RestaurantService;
import tn.esprit.projet.utils.MyDBConnexion;


import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class Main {
    public static void main(String[] args) {

        // 1. Initialisation de la connexion
        MyDBConnexion db = MyDBConnexion.getInstance();
        Connection conn = db.getConnection();

        if (conn == null) {
            System.out.println("❌ Erreur : Connexion à la base de données impossible.");
            return;
        }

        RestaurantService rs = new RestaurantService();

        try {
            // --- 1. TEST CREATE ---
            System.out.println("--- Insertion d'un nouveau restaurant ---");
            Restaurant r1 = new Restaurant(0, "Le Gourmet", "Française", "Tuniis",
                    "21600000", "info@gourmet.tn", 50, "OPEN", 1);
            rs.create(r1);
            System.out.println("Insertion réussie !");

            // --- 2. TEST READ ALL ---
            System.out.println("\n--- Liste des restaurants ---");
            List<Restaurant> list = rs.getAll();
            list.forEach(System.out::println);

            // On récupère le dernier ID pour les tests suivants (si ta liste n'est pas vide)
            if (!list.isEmpty()) {
                int lastId = list.get(list.size() - 1).getId();

                // --- 3. TEST GET BY ID ---
                System.out.println("\n--- Récupération du restaurant ID : " + lastId + " ---");
                Restaurant found = rs.getById(lastId);
                System.out.println("Trouvé : " + found.getName());

                // --- 4. TEST UPDATE ---
                System.out.println("\n--- Mise à jour du restaurant ---");
                found.setName("Le Gourmet Updated");
                found.setCapacity(100);
                rs.update(found);
                System.out.println("Mise à jour effectuée : " + rs.getById(lastId).getName());

                // --- 5. TEST DELETE ---
                // System.out.println("\n--- Suppression du restaurant ---");
                // rs.delete(lastId);
                // System.out.println("Suppression réussie.");
            }

        } catch (SQLException e) {
            System.err.println("Erreur SQL : " + e.getMessage());
        }

    }
}