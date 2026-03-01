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
import tn.esprit.projet.services.UserService;

import java.net.URL;
import java.util.ResourceBundle;

public class AdminDashboardController implements Initializable {

    // Nouveaux labels Utilisateurs
    @FXML private Label lblTotalUsers, lblActiveUsers, lblAdminCount, lblClientCount, lblSessions;

    // Labels existants
    @FXML private Label lblTotalPacks, lblTotalDest, lblAvgPrice;
    @FXML private PieChart categoryChart, userRoleChart;

    private final UserService userService = new UserService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadUserStats();
        loadPackStats();
    }

    private void loadUserStats() {
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
    }

    private void loadPackStats() {
        // Ici tu peux appeler tes services de packs/destinations
        // Exemple statique pour illustrer :
        lblTotalPacks.setText("2");
        lblTotalDest.setText("5");
        lblAvgPrice.setText("1300.0 DT");

        ObservableList<PieChart.Data> packData = FXCollections.observableArrayList(
                new PieChart.Data("Couple", 1),
                new PieChart.Data("Individuel", 1)
        );
        categoryChart.setData(packData);
    }
}
