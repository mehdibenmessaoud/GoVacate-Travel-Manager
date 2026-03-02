package tn.esprit.projet.services;

import tn.esprit.projet.API.common.ApiException;
import tn.esprit.projet.API.hotels.GeoapifyPlacesApiClient;
import tn.esprit.projet.API.hotels.NominatimHotelApiClient;
import tn.esprit.projet.entities.Hotel;
import tn.esprit.projet.utils.MyDBConnexion1;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service class for Hotel CRUD operations.
 */
public class HotelService implements IService<Hotel> {

    private Connection cnx;
    private final GeoapifyPlacesApiClient geoapifyApiClient;
    private final NominatimHotelApiClient nominatimHotelApiClient;

    public HotelService() {
        cnx = MyDBConnexion1.getInstance().getConnection();
        geoapifyApiClient      = new GeoapifyPlacesApiClient();
        nominatimHotelApiClient = new NominatimHotelApiClient();
    }

    @Override
    public void create(Hotel hotel) throws SQLException {
        String sql = "INSERT INTO hotel (name, description, stars, status, locationId) VALUES (?, ?, ?, ?, ?)";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, hotel.getName());
        ps.setString(2, hotel.getDescription());
        ps.setInt(3, hotel.getStars());
        ps.setString(4, hotel.getStatus());
        ps.setInt(5, hotel.getLocationId());
        ps.executeUpdate();
    }

    @Override
    public List<Hotel> getAll() throws SQLException {
        List<Hotel> hotels = new ArrayList<>();
        String sql = "SELECT * FROM hotel";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);

        while (rs.next()) {
            Hotel h = new Hotel(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("description"),
                    rs.getInt("stars"),
                    rs.getString("status"),
                    rs.getInt("locationId")
            );
            h.setLatitude((Double) rs.getObject("latitude"));
            h.setLongitude((Double) rs.getObject("longitude"));
            hotels.add(h);
        }
        return hotels;
    }

    @Override
    public Hotel getById(int id) throws SQLException {
        String sql = "SELECT * FROM hotel WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            Hotel h = new Hotel(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("description"),
                    rs.getInt("stars"),
                    rs.getString("status"),
                    rs.getInt("locationId")
            );
            h.setLatitude((Double) rs.getObject("latitude"));
            h.setLongitude((Double) rs.getObject("longitude"));
            return h;
        }
        return null;
    }

    @Override
    public void update(Hotel hotel) throws SQLException {
        String sql = "UPDATE hotel SET name=?, description=?, stars=?, status=?, locationId=? WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, hotel.getName());
        ps.setString(2, hotel.getDescription());
        ps.setInt(3, hotel.getStars());
        ps.setString(4, hotel.getStatus());
        ps.setInt(5, hotel.getLocationId());
        ps.setInt(6, hotel.getId());
        ps.executeUpdate();
    }

    @Override
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM hotel WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    /**
     * Get hotels by destination/location.
     */
    public List<Hotel> getByLocation(int locationId) throws SQLException {
        List<Hotel> hotels = new ArrayList<>();
        String sql = "SELECT * FROM hotel WHERE locationId = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, locationId);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            hotels.add(new Hotel(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("description"),
                    rs.getInt("stars"),
                    rs.getString("status"),
                    rs.getInt("locationId")
            ));
        }
        return hotels;
    }

    /**
     * Get hotels by status (AVAILABLE, OCCUPIED, MAINTENANCE).
     */
    public List<Hotel> getByStatus(String status) throws SQLException {
        List<Hotel> hotels = new ArrayList<>();
        String sql = "SELECT * FROM hotel WHERE status = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, status);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            hotels.add(new Hotel(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("description"),
                    rs.getInt("stars"),
                    rs.getString("status"),
                    rs.getInt("locationId")
            ));
        }
        return hotels;
    }

    /**
     * Get hotels with minimum star rating.
     */
    public List<Hotel> getByMinStars(int minStars) throws SQLException {
        List<Hotel> hotels = new ArrayList<>();
        String sql = "SELECT * FROM hotel WHERE stars >= ? ORDER BY stars DESC";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, minStars);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            hotels.add(new Hotel(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("description"),
                    rs.getInt("stars"),
                    rs.getString("status"),
                    rs.getInt("locationId")
            ));
        }
        return hotels;
    }

    /**
     * Search hotels by name.
     */
    public List<Hotel> searchByName(String keyword) throws SQLException {
        List<Hotel> hotels = new ArrayList<>();
        String sql = "SELECT * FROM hotel WHERE name LIKE ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, "%" + keyword + "%");
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            hotels.add(new Hotel(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("description"),
                    rs.getInt("stars"),
                    rs.getString("status"),
                    rs.getInt("locationId")
            ));
        }
        return hotels;
    }

    /**
     * Returns the hotel with coordinates.
     * If not cached in DB yet, fetches from Nominatim API and saves to DB.
     */
    public Hotel getWithCoordinates(int hotelId) throws SQLException {
        Hotel hotel = getById(hotelId);
        if (hotel == null) return null;

        if (hotel.hasCoordinates()) return hotel; // ✅ Already in DB

        // 🌐 Fetch from API once, then cache in DB
        fetchLocationSummary(hotel.getName()).ifPresent(loc -> {
            try {
                hotel.setLatitude(Double.parseDouble(loc.lat()));
                hotel.setLongitude(Double.parseDouble(loc.lon()));
                saveCoordinates(hotel);
            } catch (NumberFormatException ignored) {}
        });

        return hotel;
    }

    /**
     * Saves latitude/longitude to hotel table (cache).
     */
    public void saveCoordinates(Hotel hotel) {
        try {
            String sql = "UPDATE hotel SET latitude=?, longitude=? WHERE id=?";
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setDouble(1, hotel.getLatitude());
            ps.setDouble(2, hotel.getLongitude());
            ps.setInt(3, hotel.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur sauvegarde coordonnees: " + e.getMessage());
        }
    }

    public Optional<NominatimHotelApiClient.LocationSummary> fetchLocationSummary(String hotelName) {
        try {
            return nominatimHotelApiClient.findLocationSummary(hotelName);
        } catch (ApiException e) {
            return Optional.empty();
        }
    }

    public List<NominatimHotelApiClient.LocationSummary> fetchOsmLocations(String query, int maxResults) {
        try {
            return nominatimHotelApiClient.searchLocationSummaries(query, maxResults);
        } catch (ApiException e) {
            return List.of();
        }
    }

    public List<GeoapifyPlacesApiClient.HotelPlace> fetchGeoapifyHotels(String city, int limit) throws ApiException {
        return geoapifyApiClient.searchHotels(city, limit);
    }

    public boolean isGeoapifyConfigured() {
        return geoapifyApiClient.isConfigured();
    }

    public HotelExternalInsight fetchExternalInsight(Hotel hotel) {
        if (hotel == null) {
            return new HotelExternalInsight(Optional.empty());
        }
        Optional<NominatimHotelApiClient.LocationSummary> locationSummary = fetchLocationSummary(hotel.getName());

        // Persist coordinates to DB if we got them and they aren't cached yet
        locationSummary.ifPresent(loc -> {
            if (!hotel.hasCoordinates()) {
                try {
                    hotel.setLatitude(Double.parseDouble(loc.lat()));
                    hotel.setLongitude(Double.parseDouble(loc.lon()));
                    saveCoordinates(hotel);
                } catch (NumberFormatException ignored) {}
            }
        });

        return new HotelExternalInsight(locationSummary);
    }

    public record HotelExternalInsight(Optional<NominatimHotelApiClient.LocationSummary> locationSummary) {
    }
}