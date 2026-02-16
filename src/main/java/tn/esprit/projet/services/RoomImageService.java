package tn.esprit.projet.services;

import tn.esprit.projet.entities.RoomImage;
import tn.esprit.projet.utils.MyDBConnexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RoomImageService implements CRUD<RoomImage> {

    private Connection cnx;

    public RoomImageService() {
        cnx = MyDBConnexion.getInstance().getConnection();
    }

    @Override
    public void create(RoomImage image) throws SQLException {
        String sql = "INSERT INTO room_image (imageUrl, roomId) VALUES (?, ?)";

        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, image.getImageUrl());
        ps.setInt(2, image.getRoomId());

        ps.executeUpdate();
    }

    @Override
    public List<RoomImage> getAll() throws SQLException {
        List<RoomImage> images = new ArrayList<>();
        String sql = "SELECT * FROM room_image";

        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);

        while (rs.next()) {
            images.add(new RoomImage(
                    rs.getInt("id"),
                    rs.getString("imageUrl"),
                    rs.getInt("roomId")
            ));
        }

        return images;
    }

    @Override
    public void update(RoomImage image) throws SQLException {
        String sql = "UPDATE room_image SET imageUrl=?, roomId=? WHERE id=?";

        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, image.getImageUrl());
        ps.setInt(2, image.getRoomId());
        ps.setInt(3, image.getId());

        ps.executeUpdate();
    }

    @Override
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM room_image WHERE id=?";

        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);

        ps.executeUpdate();
    }

    @Override
    public RoomImage getById(int id) throws SQLException {
        String sql = "SELECT * FROM room_image WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);

        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            return new RoomImage(
                    rs.getInt("id"),
                    rs.getString("imageUrl"),
                    rs.getInt("roomId")
            );
        }

        return null;
    }

    // Get all images for a specific room
    public List<RoomImage> getByRoomId(int roomId) throws SQLException {
        List<RoomImage> images = new ArrayList<>();
        String sql = "SELECT * FROM room_image WHERE roomId = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, roomId);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            images.add(new RoomImage(
                    rs.getInt("id"),
                    rs.getString("imageUrl"),
                    rs.getInt("roomId")
            ));
        }
        return images;
    }
}
