package tn.esprit.projet.services;

import tn.esprit.projet.entities.Excursion;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ExcursionService implements IService<Excursion> {
    private Connection connection;

    public ExcursionService(Connection connection) {
        this.connection = connection;
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

    @Override
    public Excursion getById(int id) throws SQLException {
        String query = "SELECT e.*, d.ville AS destination_name FROM Excursion e JOIN Destination d ON e.locationId = d.id WHERE e.id=?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToExcursion(rs);
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

        // Indispensable pour ton contrôleur météo
        e.setDestinationName(rs.getString("destination_name"));
        return e;
    }
}