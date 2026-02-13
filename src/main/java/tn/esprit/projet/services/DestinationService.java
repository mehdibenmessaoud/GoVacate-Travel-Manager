package tn.esprit.projet.services;

import tn.esprit.projet.entities.Destination;
import tn.esprit.projet.utils.MyDBConnexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DestinationService {

    private Connection connection;

    public DestinationService() {
        connection = MyDBConnexion.getInstance().getConnection();
    }

    public List<Destination> getAll() throws SQLException {
        List<Destination> list = new ArrayList<>();
        // Querying based on your image columns: id, name_destination, pays, ville
        String query = "SELECT id, name_destination, pays, ville FROM destination";

        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                list.add(new Destination(
                        rs.getInt("id"),
                        rs.getString("name_destination"),
                        rs.getString("pays"),
                        rs.getString("ville")
                ));
            }
        }
        return list;
    }
}