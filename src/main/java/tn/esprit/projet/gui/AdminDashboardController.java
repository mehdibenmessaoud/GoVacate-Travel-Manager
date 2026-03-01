/*
package tn.esprit.projet.gui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;
import tn.esprit.projet.entities.Pack;
import tn.esprit.projet.services.PackService;
import tn.esprit.projet.services.UserService; // Import du service de ton amie

import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class DashboardController implements Initializable {

    // --- Éléments de ton module (Packs) ---
    @FXML private Label lblTotalPacks, lblTotalDest, lblAvgPrice;
    @FXML private PieChart categoryChart;

    // --- Éléments du module de ton amie (Utilisateurs) ---
    @FXML private Label lblTotalUsers;
    @FXML private PieChart userRoleChart;

    private final PackService packService = new PackService();
    private final UserService userService = new UserService(); // Instance du service fourni

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Chargement de tes statistiques habituelles
        chargerStatsPacks();

        // Chargement des nouvelles statistiques utilisateurs
        chargerStatsUtilisateurs();
    }

    private void chargerStatsPacks() {
        try {
            List<Pack> packs = packService.getAll();

            // 1. Calcul des chiffres
            lblTotalPacks.setText(String.valueOf(packs.size()));

            double avg = packs.stream().mapToDouble(Pack::getPrix).average().orElse(0.0);
            lblAvgPrice.setText(String.format("%.1f DT", avg));

            lblTotalDest.setText("5"); // Gardé tel quel selon ton code original

            // 2. Remplissage du Graphique par catégorie
            Map<String, Long> counts = packs.stream()
                    .collect(Collectors.groupingBy(Pack::getCategorie, Collectors.counting()));

            categoryChart.getData().clear();
            counts.forEach((cat, count) ->
                    categoryChart.getData().add(new PieChart.Data(cat, count))
            );

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void chargerStatsUtilisateurs() {
        // On utilise un Thread pour ne pas ralentir le lancement de l'application
        new Thread(() -> {
            // Utilisation des méthodes de comptage directes du UserService de ton amie
            int totalUsers = userService.countUsers();
            int adminCount = userService.countUsersByRole("ADMIN");
            int clientCount = userService.countUsersByRole("CLIENT");

            // Mise à jour de l'interface graphique (UI)
            Platform.runLater(() -> {
                lblTotalUsers.setText(String.valueOf(totalUsers));

                userRoleChart.getData().clear();
                if (totalUsers > 0) {
                    userRoleChart.getData().add(new PieChart.Data("Administrateurs (" + adminCount + ")", adminCount));
                    userRoleChart.getData().add(new PieChart.Data("Clients (" + clientCount + ")", clientCount));
                }
            });
        }).start();
    }
}*/

package tn.esprit.projet.gui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;
import tn.esprit.projet.entities.Destination;
import tn.esprit.projet.entities.Excursion;
import tn.esprit.projet.entities.Pack;
import tn.esprit.projet.services.DestinationService;
import tn.esprit.projet.services.ExcursionService;
import tn.esprit.projet.services.PackService;
import tn.esprit.projet.services.UserService;
import tn.esprit.projet.utils.MyDBConnexion;

import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class AdminDashboardController implements Initializable {

    // Nouveaux labels Utilisateurs
    @FXML private Label lblTotalUsers, lblActiveUsers, lblAdminCount, lblClientCount, lblSessions;

    // Labels existants
    @FXML private Label lblTotalPacks, lblTotalDest, lblAvgPrice;
    @FXML private PieChart categoryChart, userRoleChart;

    private final UserService userService = new UserService();
    private final PackService packService = new PackService();
    private final DestinationService destinationService = new DestinationService(MyDBConnexion.getInstance().getConnection());
    private final ExcursionService excursionService = new ExcursionService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadUserStats();
        loadPackStats();
    }

    /*private void loadUserStats() {
        // Récupération des données réelles depuis ton UserService
        int total = userService.countUsers();
        int actifs = userService.countActiveUsers();
        int admins = userService.countUsersByRole("ADMIN");
        int clients = userService.countUsersByRole("CLIENT");

        lblTotalUsers.setText(String.valueOf(total));
        lblActiveUsers.setText(String.valueOf(actifs));
        lblAdminCount.setText(String.valueOf(admins));
        lblClientCount.setText(String.valueOf(clients));
        lblSessions.setText("1"); // Valeur par défaut pour la session actuelle

        // Mise à jour du PieChart des rôles
        ObservableList<PieChart.Data> roleData = FXCollections.observableArrayList(
                new PieChart.Data("Administrateurs", admins),
                new PieChart.Data("Clients", clients)
        );
        userRoleChart.setData(roleData);
    }*/

    private void loadUserStats() {
        // 1. Récupération des données réelles
        int total = userService.countUsers();
        int actifs = userService.countActiveUsers();
        int admins = userService.countUsersByRole("ADMIN");
        int clients = userService.countUsersByRole("CLIENT");

        // 2. Mise à jour des labels (ceci fonctionne immédiatement)
        lblTotalUsers.setText(String.valueOf(total));
        lblActiveUsers.setText(String.valueOf(actifs));
        lblAdminCount.setText(String.valueOf(admins));
        lblClientCount.setText(String.valueOf(clients));
        lblSessions.setText("1");

        // 3. Préparation des données du graphique
        ObservableList<PieChart.Data> roleData = FXCollections.observableArrayList();

        if (total > 0) {
            if (admins > 0) {
                roleData.add(new PieChart.Data("Admins (" + admins + ")", admins));
            }
            if (clients > 0) {
                roleData.add(new PieChart.Data("Clients (" + clients + ")", clients));
            }

            userRoleChart.setData(roleData);

            /**
             * IMPORTANT : On utilise Platform.runLater pour s'assurer que JavaFX
             * a fini de dessiner les segments avant de changer leurs couleurs.
             */
            javafx.application.Platform.runLater(this::applyCustomPieChartColors);

        } else {
            userRoleChart.setData(FXCollections.observableArrayList());
        }

        System.out.println("✅ Statistiques : " + admins + " Admins et " + clients + " Clients affichés.");
    }

    private void applyCustomPieChartColors() {
        int i = 0;
        // Utilisation de new String[] pour éviter l'erreur de syntaxe vue précédemment
        String[] colors = new String[]{"#e67e22", "#FF0000"}; // Orange pour Admin, Violet pour Client

        if (userRoleChart != null && !userRoleChart.getData().isEmpty()) {
            for (PieChart.Data data : userRoleChart.getData()) {
                if (data.getNode() != null) {
                    data.getNode().setStyle("-fx-pie-color: " + colors[i % colors.length] + ";");
                }
                i++;
            }
        }
    }


    // Assurez-vous d'avoir cette instance dans votre classe
    private void loadPackStats() {
        try {
            // 1. Récupération des données réelles
            List<Pack> allPacks = packService.getAll();
            List<Destination> allDestinations = destinationService.getAll();
            List<Excursion> allExcursions = excursionService.getAll();

            // 2. Mise à jour des chiffres (Labels)

            // Total des Packs
            lblTotalPacks.setText(String.valueOf(allPacks.size()));

            // Total des Destinations uniques
            lblTotalDest.setText(String.valueOf(allDestinations.size()));

            // Prix moyen des Packs
            double avgPrice = allPacks.stream()
                    .mapToDouble(Pack::getPrix)
                    .average()
                    .orElse(0.0);
            lblAvgPrice.setText(String.format("%.1f DT", avgPrice));

            // Optionnel : Si vous avez un label pour les excursions
            // lblTotalExcursions.setText(String.valueOf(allExcursions.size()));

            // 3. Mise à jour du Graphique (PieChart) par Catégorie de Pack
            if (!allPacks.isEmpty()) {
                // Groupement par catégorie (Couple, Famille, Individuel, etc.)
                Map<String, Long> categoryCounts = allPacks.stream()
                        .collect(Collectors.groupingBy(Pack::getCategorie, Collectors.counting()));

                ObservableList<PieChart.Data> packData = FXCollections.observableArrayList();

                categoryCounts.forEach((category, count) -> {
                    // On ajoute le nom de la catégorie et le nombre entre parenthèses pour plus de clarté
                    packData.add(new PieChart.Data(category + " (" + count + ")", count.doubleValue()));
                });

                categoryChart.setData(packData);
            } else {
                categoryChart.setData(FXCollections.observableArrayList());
            }

            System.out.println("✅ Statistiques des packs chargées avec succès.");

        } catch (SQLException e) {
            System.err.println("❌ Erreur SQL dans loadPackStats : " + e.getMessage());
            e.printStackTrace();
        }
    }


}
