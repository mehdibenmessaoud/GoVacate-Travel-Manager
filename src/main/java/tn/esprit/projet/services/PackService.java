package tn.esprit.projet.services;

import tn.esprit.projet.entities.Pack;
import tn.esprit.projet.utils.MyDBConnexion;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PackService implements IService<Pack> {
    private Connection connection;

    public PackService() {
        this.connection = MyDBConnexion.getInstance().getConnection();
        if (this.connection == null) {
            System.err.println("❌ Erreur : Impossible d'établir la connexion SQL dans PackService.");
        }
    }

    public PackService(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void create(Pack p) throws SQLException {
        String query = "INSERT INTO pack (name, description, categorie, prix, duree, status, date_depart, date_arriver, imageName, destination_id, hotel_id, excursion_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, p.getName());
            ps.setString(2, p.getDescription());
            ps.setString(3, p.getCategorie());
            ps.setDouble(4, p.getPrix());
            ps.setInt(5, p.getDuree());
            ps.setString(6, p.getStatus());
            ps.setDate(7, Date.valueOf(p.getDateDepart()));
            ps.setDate(8, Date.valueOf(p.getDateArriver()));
            ps.setString(9, p.getImageName());
            ps.setInt(10, p.getDestinationId());

            if (p.getHotelId() == -1) {
                ps.setNull(11, java.sql.Types.INTEGER);
            } else {
                ps.setInt(11, p.getHotelId());
            }

            if (p.getExcursionId() == -1) {
                ps.setNull(12, java.sql.Types.INTEGER);
            } else {
                ps.setInt(12, p.getExcursionId());
            }

            ps.executeUpdate();
        }
    }

    @Override
    public List<Pack> getAll() throws SQLException {
        List<Pack> packs = new ArrayList<>();
        // MODIFICATION : Ajout de la jointure pour récupérer la ville
        String query = "SELECT p.*, d.ville AS destination_name FROM pack p " +
                "JOIN destination d ON p.destination_id = d.id";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                packs.add(mapResultSetToPack(rs));
            }
        }
        return packs;
    }

    @Override
    public void update(Pack p) throws SQLException {
        String query = "UPDATE pack SET name=?, description=?, categorie=?, prix=?, duree=?, status=?, date_depart=?, date_arriver=?, imageName=?, destination_id=?, hotel_id=?, excursion_id=? WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, p.getName());
            ps.setString(2, p.getDescription());
            ps.setString(3, p.getCategorie());
            ps.setDouble(4, p.getPrix());
            ps.setInt(5, p.getDuree());
            ps.setString(6, p.getStatus());
            ps.setDate(7, Date.valueOf(p.getDateDepart()));
            ps.setDate(8, Date.valueOf(p.getDateArriver()));
            ps.setString(9, p.getImageName());
            ps.setInt(10, p.getDestinationId());
            if (p.getHotelId() == -1) {
                ps.setNull(11, java.sql.Types.INTEGER);
            } else {
                ps.setInt(11, p.getHotelId());
            }

            if (p.getExcursionId() == -1) {
                ps.setNull(12, java.sql.Types.INTEGER);
            } else {
                ps.setInt(12, p.getExcursionId());
            }
            ps.setInt(13, p.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(int id) throws SQLException {
        String query = "DELETE FROM pack WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    @Override
    public Pack getById(int id) throws SQLException {
        // MODIFICATION : Ajout de la jointure ici aussi
        String query = "SELECT p.*, d.ville AS destination_name FROM pack p " +
                "JOIN destination d ON p.destination_id = d.id WHERE p.id=?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToPack(rs);
                }
            }
        }
        return null;
    }

    private Pack mapResultSetToPack(ResultSet rs) throws SQLException {
        Pack p = new Pack(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getString("categorie"),
                rs.getDouble("prix"),
                rs.getInt("duree"),
                rs.getString("status"),
                rs.getDate("date_depart").toLocalDate(),
                rs.getDate("date_arriver").toLocalDate(),
                rs.getString("imageName"),
                rs.getInt("destination_id"),
                rs.getInt("hotel_id"),
                rs.getInt("excursion_id")
        );

        // AJOUT : On récupère le nom de la ville pour la météo
        p.setDestinationName(rs.getString("destination_name"));

        return p;
    }
}