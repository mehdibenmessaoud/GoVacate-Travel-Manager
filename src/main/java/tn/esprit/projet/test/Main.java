package tn.esprit.projet.test;

import tn.esprit.projet.entities.Restaurant;
import tn.esprit.projet.services.RestaurantService;
import tn.esprit.projet.utils.MyDBConnexion;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        MyDBConnexion db = MyDBConnexion.getInstance();
        Connection conn = db.getConnection();

        if (conn == null) {
            System.out.println("❌ Erreur : Connexion à la base de données impossible.");
            return;
        }
        System.out.println("✅ Connexion à la base de données réussie.");

        RestaurantService rs = new RestaurantService();

        try {
            // --- 1. TEST CREATE ---
            System.out.println("\n--- Insertion d'un nouveau restaurant ---");
            // This now matches the 9-parameter constructor we just added
            Restaurant r1 = new Restaurant(0, "Le Gourmet", "Française", "Tunis",
                    "21600000", "info@gourmet.tn", 50, "OPEN", 1);

            rs.create(r1);
            System.out.println("✅ Insertion réussie !");

            // --- 2. TEST READ ALL ---
            System.out.println("\n--- Liste des restaurants (Join avec Destination) ---");
            List<Restaurant> list = rs.getAll();

            if (list.isEmpty()) {
                System.out.println("Aucun restaurant trouvé.");
            } else {
                for (Restaurant r : list) {
                    System.out.println("ID: " + r.getId() +
                            " | Nom: " + r.getName() +
                            " | Destination: " + r.getDestinationName() +
                            " | Status: " + r.getStatus());
                }

                int lastId = list.get(list.size() - 1).getId();

                // --- 3. TEST GET BY ID ---
                System.out.println("\n--- Récupération du restaurant ID : " + lastId + " ---");
                Restaurant found = rs.getById(lastId);

                if (found != null) {
                    System.out.println("✅ Trouvé : " + found.getName() + " (Localisé à: " + found.getDestinationName() + ")");

                    // --- 4. TEST UPDATE ---
                    System.out.println("\n--- Mise à jour du restaurant ---");
                    found.setName("Le Gourmet Updated");
                    found.setCapacity(100);
                    rs.update(found);

                    Restaurant updated = rs.getById(lastId);
                    System.out.println("✅ Mise à jour effectuée : " + updated.getName());
                }
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur SQL : " + e.getMessage());
            if (e.getMessage().contains("foreign key constraint fails")) {
                System.err.println("👉 Erreur de clé étrangère ! Assurez-vous qu'une destination avec l'ID 1 existe.");
            }
        }
    }
}