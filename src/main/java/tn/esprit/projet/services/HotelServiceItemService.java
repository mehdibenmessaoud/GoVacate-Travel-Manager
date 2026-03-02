package tn.esprit.projet.services;

import tn.esprit.projet.entities.HotelServiceItem;
import tn.esprit.projet.utils.MyDBConnexion1;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for HotelServiceItem CRUD operations with MySQL persistence.
 */
public class HotelServiceItemService implements IService<HotelServiceItem> {

    private final Connection cnx;

    public HotelServiceItemService() {
        cnx = MyDBConnexion1.getInstance().getConnection();
    }


    @Override
    public void create(HotelServiceItem item) throws SQLException {
        String sql = "INSERT INTO hotel_service_item (hotelId, name, isActive) VALUES (?, ?, ?)";
        PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        ps.setInt(1, item.getHotelId());
        ps.setString(2, item.getName());
        ps.setBoolean(3, item.isActive());
        ps.executeUpdate();

        ResultSet rs = ps.getGeneratedKeys();
        if (rs.next()) {
            item.setId(rs.getInt(1));
        }
    }

    @Override
    public List<HotelServiceItem> getAll() throws SQLException {
        List<HotelServiceItem> items = new ArrayList<>();
        String sql = "SELECT * FROM hotel_service_item ORDER BY hotelId, name";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);

        while (rs.next()) {
            items.add(extract(rs));
        }
        return items;
    }

    @Override
    public HotelServiceItem getById(int id) throws SQLException {
        String sql = "SELECT * FROM hotel_service_item WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            return extract(rs);
        }
        return null;
    }

    @Override
    public void update(HotelServiceItem item) throws SQLException {
        String sql = "UPDATE hotel_service_item SET hotelId = ?, name = ?, isActive = ? WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, item.getHotelId());
        ps.setString(2, item.getName());
        ps.setBoolean(3, item.isActive());
        ps.setInt(4, item.getId());
        ps.executeUpdate();
    }

    @Override
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM hotel_service_item WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    public List<HotelServiceItem> getByHotelId(int hotelId) throws SQLException {
        List<HotelServiceItem> items = new ArrayList<>();
        String sql = "SELECT * FROM hotel_service_item WHERE hotelId = ? ORDER BY isActive DESC, name";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, hotelId);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            items.add(extract(rs));
        }
        return items;
    }

    public List<HotelServiceItem> getActiveByHotelId(int hotelId) throws SQLException {
        List<HotelServiceItem> items = new ArrayList<>();
        String sql = "SELECT * FROM hotel_service_item WHERE hotelId = ? AND isActive = 1 ORDER BY name";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, hotelId);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            items.add(extract(rs));
        }
        return items;
    }

    private HotelServiceItem extract(ResultSet rs) throws SQLException {
        return new HotelServiceItem(
                rs.getInt("id"),
                rs.getInt("hotelId"),
                rs.getString("name"),
                rs.getBoolean("isActive")
        );
    }
}
