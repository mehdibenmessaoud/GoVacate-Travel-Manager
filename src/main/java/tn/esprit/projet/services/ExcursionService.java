package tn.esprit.projet.services;

import tn.esprit.projet.entities.Excursion;
import tn.esprit.projet.utils.MyDBConnexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class ExcursionService implements IService<Excursion> {
    private Connection connection;

    public ExcursionService(Connection connection) {
        this.connection = connection;
    }
    // Dans ExcursionService.java
    public ExcursionService() {
        this.connection = MyDBConnexion.getInstance().getConnection(); // Adaptez selon votre code
    }

    @Override
    public void create(Excursion e) throws SQLException {
        String query = "INSERT INTO Excursion (name, description, duration, price, maxParticipants, status, locationId, activite, dateDebut, dateFin, images) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, e.getName());
            ps.setString(2, e.getDescription());
            ps.setInt(3, e.getDuration());
            ps.setDouble(4, e.getPrice());
            ps.setInt(5, e.getMaxParticipants());
            ps.setString(6, e.getStatus());
            ps.setInt(7, e.getLocationId());
            ps.setString(8, e.getActivite());
            ps.setDate(9, e.getDateDebut() != null ? java.sql.Date.valueOf(e.getDateDebut()) : null);
            ps.setDate(10, e.getDateFin() != null ? java.sql.Date.valueOf(e.getDateFin()) : null);
            ps.setString(11, e.getImages());
            ps.executeUpdate();
        }
    }

    @Override
    public List<Excursion> getAll() throws SQLException {
        List<Excursion> excursions = new ArrayList<>();
        String query = "SELECT e.*, d.ville AS destination_name FROM Excursion e JOIN Destination d ON e.locationId = d.id";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                excursions.add(mapResultSetToExcursion(rs));
            }
        }
        return excursions;
    }

    // --- LA MÉTHODE QUE J'AVAIS OUBLIÉE (Réintégrée et corrigée) ---
    public List<Excursion> getByLocation(int locationId) throws SQLException {
        List<Excursion> excursions = new ArrayList<>();
        // On ajoute la jointure ici aussi pour que destination_name soit disponible
        String query = "SELECT e.*, d.ville AS destination_name FROM Excursion e " +
                "JOIN Destination d ON e.locationId = d.id " +
                "WHERE e.locationId = ? AND e.status = 'Disponible'";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, locationId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    excursions.add(mapResultSetToExcursion(rs));
                }
            }
        }
        return excursions;
    }

    @Override
    public void update(Excursion e) throws SQLException {
        String query = "UPDATE Excursion SET name=?, description=?, duration=?, price=?, maxParticipants=?, status=?, locationId=?, activite=?, dateDebut=?, dateFin=?, images=? WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, e.getName());
            ps.setString(2, e.getDescription());
            ps.setInt(3, e.getDuration());
            ps.setDouble(4, e.getPrice());
            ps.setInt(5, e.getMaxParticipants());
            ps.setString(6, e.getStatus());
            ps.setInt(7, e.getLocationId());
            ps.setString(8, e.getActivite());
            ps.setDate(9, e.getDateDebut() != null ? java.sql.Date.valueOf(e.getDateDebut()) : null);
            ps.setDate(10, e.getDateFin() != null ? java.sql.Date.valueOf(e.getDateFin()) : null);
            ps.setString(11, e.getImages());
            ps.setInt(12, e.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(int id) throws SQLException {
        String query = "DELETE FROM Excursion WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    /*public Excursion getById(int id) throws SQLException {
        // Utilisation de 'locationId' pour la jointure et alias pour éviter les erreurs
        String query = "SELECT e.*, d.ville, d.pays, e.name, FROM Excursion e " +
                "JOIN Destination d ON e.locationId = d.id WHERE e.id=?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // On mappe les données de base
                    Excursion e = mapResultSetToExcursion(rs);

                    // On récupère spécifiquement ville et pays pour le label
                    String ville = rs.getString("ville");
                    String pays = rs.getString("nom_pays");

                    e.setVille(ville);
                    e.setFullLocation(ville + ", " + pays);

                    // On s'assure que destinationName est aussi rempli pour la météo
                    e.setDestinationName(ville);

                    return e;
                }
            }
        }
        return null;
    }
*/

    public Excursion getById(int id) throws SQLException {
        // 1. Correction de la requête : suppression de la virgule après e.name et correction de nom_pays -> pays
        String query = "SELECT e.*, d.ville, d.pays, e.name AS excursion_name FROM Excursion e " +
                "JOIN Destination d ON e.locationId = d.id WHERE e.id=?";

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // 2. Mappage des données de base (id, prix, etc.)
                    Excursion e = mapResultSetToExcursion(rs);

                    // 3. Récupération explicite du nom de l'excursion
                    // rs.getString("name") fonctionne aussi si mapResultSetToExcursion ne le fait pas déjà
                    e.setName(rs.getString("name"));

                    // 4. Récupération sécurisée de la localisation (Correction de l'erreur Unknown Column)
                    String ville = rs.getString("ville");
                    String pays = rs.getString("pays"); // Changé de "nom_pays" à "pays"

                    e.setVille(ville);
                    e.setPays(pays);
                    e.setFullLocation(ville + ", " + pays);

                    // 5. Pour la météo et l'affichage dans PackDetailsController
                    e.setDestinationName(ville);

                    return e;
                }
            }
        }
        return null;
    }


    private Excursion mapResultSetToExcursion(ResultSet rs) throws SQLException {
        Excursion e = new Excursion(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getInt("duration"),
                rs.getDouble("price"),
                rs.getInt("maxParticipants"),
                rs.getString("status"),
                rs.getInt("locationId"),
                rs.getString("activite"),
                rs.getDate("dateDebut") != null ? rs.getDate("dateDebut").toLocalDate() : null,
                rs.getDate("dateFin") != null ? rs.getDate("dateFin").toLocalDate() : null,
                rs.getString("images")
        );

        // Protection : on ne lit destination_name que si la colonne existe dans le ResultSet
        try {
            e.setDestinationName(rs.getString("ville"));
        } catch (SQLException ex) {
            // Si on vient d'une méthode qui utilise l'alias AS destination_name
            try {
                e.setDestinationName(rs.getString("destination_name"));
            } catch (SQLException ex2) {
                e.setDestinationName(null);
            }
        }
        return e;
    }

    public double calculateDynamicPrice(Excursion e, int currentReservations) {
        if (e == null || e.getDateDebut() == null) {
            return 0.0;
        }

        double finalPrice = e.getPrice();
        // Calcul de la différence en jours entre aujourd'hui et le départ
        long daysUntilDeparture = ChronoUnit.DAYS.between(LocalDate.now(), e.getDateDebut());

        // Règle 1 : Last Minute - Moins de 48h et beaucoup de places libres -> -20%
        if (daysUntilDeparture <= 2 && (e.getMaxParticipants() - currentReservations) >= (e.getMaxParticipants()/2)) {
            finalPrice = finalPrice * 0.8;
        }
        // Règle 2 : High Demand - 90% des places vendues -> +10%
        else if (currentReservations >= (e.getMaxParticipants() * 0.8)) {
            finalPrice = finalPrice * 1.1;
        }

        return finalPrice;
    }

    // Dans ExcursionService.java
    public int getCurrentReservations(int excursionId) {
        int totalParticipants = 0;
        // On utilise SUM pour additionner tous les participants de l'excursion ID 9
        String query = "SELECT SUM(nombre_personnes) FROM reservation_excursion WHERE excursion_id = ?";

        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, excursionId);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                totalParticipants = rs.getInt(1);
            }
        } catch (SQLException ex) {
            System.err.println("Erreur SQL lors du calcul des places : " + ex.getMessage());
        }
        return totalParticipants;
    }
}