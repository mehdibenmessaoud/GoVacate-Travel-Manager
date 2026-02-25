package tn.esprit.projet.gui;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;
import tn.esprit.projet.entities.Pack;
import tn.esprit.projet.services.PackService;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import javafx.stage.Stage;
import javafx.scene.Scene;

public class DashboardController implements Initializable {
    @FXML private Label lblTotalPacks, lblTotalDest, lblAvgPrice;
    @FXML private PieChart categoryChart;

    private final PackService packService = new PackService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        try {
            List<Pack> packs = packService.getAll();

            // 1. Calcul des chiffres
            lblTotalPacks.setText(String.valueOf(packs.size()));

            double avg = packs.stream().mapToDouble(Pack::getPrix).average().orElse(0.0);
            lblAvgPrice.setText(String.format("%.1f DT", avg));

            // Pour les destinations, tu pourras appeler ton DestinationService ici
            lblTotalDest.setText("5");

            // 2. Remplissage du Graphique par catégorie
            Map<String, Long> counts = packs.stream()
                    .collect(Collectors.groupingBy(Pack::getCategorie, Collectors.counting()));

            counts.forEach((cat, count) ->
                    categoryChart.getData().add(new PieChart.Data(cat, count))
            );

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}