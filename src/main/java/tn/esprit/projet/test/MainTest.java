package tn.esprit.projet.test;

import tn.esprit.projet.entities.User;
import tn.esprit.projet.services.UserService;
import java.util.List;

public class MainTest {
    public static void main(String[] args) {
        UserService us = new UserService();
        System.out.println("--- DÉBUT DU TEST CRUD ---");

        // 1. AJOUT : Utilisez un email différent à chaque test pour éviter l'erreur "Duplicate entry"
        User u1 = new User("Mohamed Ladgham", "test" + System.currentTimeMillis() + "@govacate.tn", "esprit123", 2);
        us.ajouterUser(u1);
        System.out.println("Étape 1 : Utilisateur ajouté.");

        // 2. RÉCUPÉRATION
        List<User> list = us.recupererUser();
        System.out.println("Nombre d'utilisateurs trouvés : " + list.size());

        if (!list.isEmpty()) {
            // 3. MODIFICATION : On prend le DERNIER élément de la liste (index = taille - 1)
            // Cela évite de demander l'index_3 si la liste n'en a que 3.
            int dernierIndex = list.size() - 1;
            User userAModifier = list.get(dernierIndex);

            userAModifier.setNom("NOM_MODIFIE_TEST");
            us.modifierUser(userAModifier);
            System.out.println("Étape 3 : Utilisateur ID " + userAModifier.getId() + " modifié avec succès.");

            // 4. SUPPRESSION (Optionnel)
            us.supprimerUser(userAModifier.getId());
            System.out.println("Étape 4 : Utilisateur supprimé.");
        } else {
            System.out.println("La liste est vide, impossible de modifier ou supprimer.");
        }

        System.out.println("4. Liste des utilisateurs en base :");
        List<User> liste = us.recupererUser();
        if (liste.isEmpty()) {
            System.out.println("   Aucun utilisateur trouvé.");
        } else {
            for (User u : liste) {
                System.out.println("   - " + u.toString());
            }
        }

        System.out.println("--- FIN DU TEST ---");
    }
}