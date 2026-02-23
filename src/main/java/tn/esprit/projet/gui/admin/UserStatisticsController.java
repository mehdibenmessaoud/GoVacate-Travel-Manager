package tn.esprit.projet.gui.admin;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import tn.esprit.projet.entities.User;
import tn.esprit.projet.services.UserService;
import tn.esprit.projet.utils.SceneManager;
import tn.esprit.projet.utils.SessionManager;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class UserStatisticsController implements Initializable {

    @FXML private Label totalUsersLabel;
    @FXML private Label adminCountLabel;
    @FXML private Label clientCountLabel;
    @FXML private Label activeCountLabel;
    @FXML private Label activeSessionsLabel;

    @FXML private PieChart roleChart;
    @FXML private PieChart statusChart;

    @FXML private TableView<User> recentUsersTable;
    @FXML private TableColumn<User, String> colRecentNom;
    @FXML private TableColumn<User, String> colRecentRole;
    @FXML private TableColumn<User, String> colRecentDate;

    private UserService userService;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (!SessionManager.isAdmin()) {
            SceneManager.goToLogin();
            return;
        }
        userService = new UserService();
        initializeCharts();
        initializeTable();
        loadStatisticsAsync();
    }

    private void initializeCharts() {
        if (roleChart != null) roleChart.setTitle("Répartition par Rôle");
        if (statusChart != null) statusChart.setTitle("Répartition par Statut");
    }

    private void initializeTable() {
        if (colRecentNom != null) colRecentNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        if (colRecentRole != null) colRecentRole.setCellValueFactory(new PropertyValueFactory<>("roleName"));
        if (colRecentDate != null) {
            colRecentDate.setCellValueFactory(cellData -> {
                if (cellData.getValue().getCreatedAt() != null) {
                    return new SimpleStringProperty(
                            cellData.getValue().getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                    );
                }
                return new SimpleStringProperty("-");
            });
        }
    }

    /**
     * Charge les statistiques en arrière-plan pour ne pas bloquer le thread JavaFX.
     */
    private void loadStatisticsAsync() {
        Task<StatsData> task = new Task<>() {
            @Override
            protected StatsData call() throws Exception {
                int total = userService.countUsers();
                int admins = userService.countUsersByRole("ADMIN");
                int clients = userService.countUsersByRole("CLIENT");
                int active = userService.countActiveUsers();
                int sessions = SessionManager.getActiveUsersCount();
                List<User> recent = userService.getRecentUsers(5);
                return new StatsData(total, admins, clients, active, sessions, recent);
            }
        };
        task.setOnSucceeded(e -> {
            StatsData d = task.getValue();
            if (totalUsersLabel != null) totalUsersLabel.setText(String.valueOf(d.total));
            if (adminCountLabel != null) adminCountLabel.setText(String.valueOf(d.admins));
            if (clientCountLabel != null) clientCountLabel.setText(String.valueOf(d.clients));
            if (activeCountLabel != null) activeCountLabel.setText(String.valueOf(d.active));
            if (activeSessionsLabel != null) activeSessionsLabel.setText(String.valueOf(d.sessions));
            if (roleChart != null) {
                roleChart.setData(FXCollections.observableArrayList(
                        new PieChart.Data("Administrateurs", d.admins),
                        new PieChart.Data("Clients", d.clients)
                ));
            }
            if (statusChart != null) {
                statusChart.setData(FXCollections.observableArrayList(
                        new PieChart.Data("Actifs", d.active),
                        new PieChart.Data("Autres", Math.max(0, d.total - d.active))
                ));
            }
            if (recentUsersTable != null) {
                recentUsersTable.setItems(FXCollections.observableArrayList(d.recentUsers));
            }
        });
        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            if (ex != null) {
                if (totalUsersLabel != null) totalUsersLabel.setText("Erreur");
                System.err.println("Erreur chargement statistiques: " + ex.getMessage());
            }
        });
        new Thread(task).start();
    }

    private static class StatsData {
        final int total, admins, clients, active, sessions;
        final List<User> recentUsers;
        StatsData(int total, int admins, int clients, int active, int sessions, List<User> recentUsers) {
            this.total = total;
            this.admins = admins;
            this.clients = clients;
            this.active = active;
            this.sessions = sessions;
            this.recentUsers = recentUsers;
        }
    }

    @FXML
    private void handleRefresh() {
        loadStatisticsAsync();
    }

    @FXML
    private void handleBack() {
        SceneManager.loadAdminContent("/fxml/admin/AdminDashboard.fxml");
    }
}
