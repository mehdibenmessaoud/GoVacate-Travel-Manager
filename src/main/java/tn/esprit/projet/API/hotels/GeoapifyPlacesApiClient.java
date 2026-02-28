package tn.esprit.projet.API.hotels;

import com.fasterxml.jackson.databind.JsonNode;
import tn.esprit.projet.API.common.ApiConfig;
import tn.esprit.projet.API.common.ApiException;
import tn.esprit.projet.API.common.ExternalHttpUtil;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.util.ArrayList;
import java.util.List;

/**
 * Geoapify Places API client — second hotel search provider.
 *
 * Why Geoapify over Amadeus:
 *  - Free tier: 3,000 API calls/day — no credit card required
 *  - Works with any city name (no IATA codes needed)
 *  - Returns hotels, motels, hostels, resorts, B&Bs in one query
 *  - Rich POI data: name, address, coordinates, website, phone, opening hours
 *  - OpenStreetMap-based data: global coverage, frequently updated
 *  - Simple Bearer API key — no OAuth token refresh required
 *
 * Setup: add GEOAPIFY_API_KEY=<your-key> to your .env file
 * Get a free key (no credit card) at: https://myprojects.geoapify.com/
 *
 * Search strategy:
 *  1. Geocode the city name → get center lat/lon (via Geoapify Geocoding API)
 *  2. Query Places API for accommodation within 15 km radius of city center
 *
 * Accommodation categories used:
 *   accommodation.hotel, accommodation.motel, accommodation.hostel,
 *   accommodation.guest_house, accommodation.hut, accommodation.chalet
 */
public class GeoapifyPlacesApiClient {

    private static final String GEOCODING_URL = "https://api.geoapify.com/v1/geocode/search";
    private static final String PLACES_URL    = "https://api.geoapify.com/v2/places";

    /** Hotel/accommodation category filter. */
    /** Hotel/accommodation category filter — only Geoapify-supported values.
     *  See: https://apidocs.geoapify.com/docs/places/#categories
     */
    private static final String ACCOMMODATION_CATEGORIES =
            "accommodation.hotel,accommodation.motel,accommodation.hostel," +
                    "accommodation.guest_house,accommodation.hut,accommodation.chalet";

    /** Search radius around city center in meters. */
    private static final int SEARCH_RADIUS_M = 15_000;

    private final HttpClient httpClient;

    public GeoapifyPlacesApiClient() {
        this.httpClient = ExternalHttpUtil.createHttpClient();
    }

    public boolean isConfigured() {
        return ApiConfig.hasAll("GEOAPIFY_API_KEY");
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Search for hotels in a city.
     *
     * @param city  City name (e.g. "Paris", "Rome", "Tokyo")
     * @param limit Max results (1–20)
     * @return hotels sorted by relevance / importance
     */
    public List<HotelPlace> searchHotels(String city, int limit) throws ApiException {
        String apiKey = ApiConfig.read("GEOAPIFY_API_KEY")
                .orElseThrow(() -> new ApiException("Missing GEOAPIFY_API_KEY in .env — " +
                        "Get a free key at https://myprojects.geoapify.com/"));

        String safecity = (city == null || city.isBlank()) ? "Paris" : city.trim();
        int safeLimit   = Math.max(1, Math.min(limit, 20));

        // Step 1 — Geocode city
        CityCenter center = geocodeCity(safecity, apiKey);

        // Step 2 — Search hotels near center
        return searchNearCoords(center.lat(), center.lon(), safeLimit, apiKey, safecity);
    }

    // ── Geocoding ─────────────────────────────────────────────────────────────

    private CityCenter geocodeCity(String city, String apiKey) throws ApiException {
        String uri = GEOCODING_URL
                + "?text="   + ExternalHttpUtil.urlEncode(city)
                + "&type=city"
                + "&format=json"
                + "&limit=1"
                + "&apiKey=" + apiKey;

        HttpRequest request = ExternalHttpUtil.requestBuilder(URI.create(uri))
                .header("Accept", "application/json")
                .GET()
                .build();

        JsonNode json = ExternalHttpUtil.sendForJson(httpClient, request, "Geoapify Geocoding");

        // format=json returns {"results":[{lat, lon, ...}]}
        JsonNode results = json.path("results");
        if (!results.isArray() || results.size() == 0) {
            // Fallback: try without type=city
            return geocodeCityFallback(city, apiKey);
        }
        JsonNode first = results.get(0);
        double lat = first.path("lat").asDouble(Double.NaN);
        double lon = first.path("lon").asDouble(Double.NaN);
        if (Double.isNaN(lat) || Double.isNaN(lon)) {
            throw new ApiException("Geoapify: coordonnées introuvables pour \"" + city + "\"");
        }
        return new CityCenter(lat, lon);
    }

    private CityCenter geocodeCityFallback(String city, String apiKey) throws ApiException {
        String uri = GEOCODING_URL
                + "?text="   + ExternalHttpUtil.urlEncode(city)
                + "&format=json"
                + "&limit=1"
                + "&apiKey=" + apiKey;

        HttpRequest request = ExternalHttpUtil.requestBuilder(URI.create(uri))
                .header("Accept", "application/json")
                .GET()
                .build();

        JsonNode json = ExternalHttpUtil.sendForJson(httpClient, request, "Geoapify Geocoding fallback");
        JsonNode results = json.path("results");
        if (!results.isArray() || results.size() == 0) {
            throw new ApiException("Geoapify: ville introuvable — \"" + city + "\"");
        }
        JsonNode first = results.get(0);
        double lat = first.path("lat").asDouble(Double.NaN);
        double lon = first.path("lon").asDouble(Double.NaN);
        if (Double.isNaN(lat) || Double.isNaN(lon)) {
            throw new ApiException("Geoapify: coordonnées introuvables pour \"" + city + "\"");
        }
        return new CityCenter(lat, lon);
    }

    // ── Places search ─────────────────────────────────────────────────────────

    private List<HotelPlace> searchNearCoords(double lat, double lon, int limit,
                                              String apiKey, String cityHint) throws ApiException {
        // filter=circle:lon,lat,radiusMeters  (note: Geoapify uses lon,lat order in circle filter)
        String filter = "circle:" + String.format(java.util.Locale.ROOT, "%.6f,%.6f,%d", lon, lat, SEARCH_RADIUS_M);

        String uri = PLACES_URL
                + "?categories=" + ExternalHttpUtil.urlEncode(ACCOMMODATION_CATEGORIES)
                + "&filter="     + ExternalHttpUtil.urlEncode(filter)
                + "&limit="      + limit
                + "&apiKey="     + apiKey;

        HttpRequest request = ExternalHttpUtil.requestBuilder(URI.create(uri))
                .header("Accept", "application/json")
                .GET()
                .build();

        JsonNode json = ExternalHttpUtil.sendForJson(httpClient, request, "Geoapify Places");

        // GeoJSON FeatureCollection
        JsonNode features = json.path("features");
        if (!features.isArray() || features.size() == 0) {
            return List.of();
        }

        List<HotelPlace> places = new ArrayList<>();
        for (JsonNode feature : features) {
            HotelPlace p = parseFeature(feature, cityHint);
            if (p != null) places.add(p);
        }
        return places;
    }

    // ── Parsing ───────────────────────────────────────────────────────────────

    private HotelPlace parseFeature(JsonNode feature, String cityHint) {
        JsonNode props = feature.path("properties");
        if (props.isMissingNode()) return null;

        String name = props.path("name").asText("").trim();
        if (name.isBlank()) return null;

        // Coordinates (GeoJSON geometry: [lon, lat])
        JsonNode geom    = feature.path("geometry").path("coordinates");
        double lon = geom.isArray() && geom.size() >= 2 ? geom.get(0).asDouble(Double.NaN) : Double.NaN;
        double lat = geom.isArray() && geom.size() >= 2 ? geom.get(1).asDouble(Double.NaN) : Double.NaN;

        // Address
        String street   = props.path("address_line1").asText("").trim();
        String address2 = props.path("address_line2").asText("").trim();
        String address  = street.isBlank() ? address2 : street;

        String city     = props.path("city").asText(
                props.path("county").asText(cityHint)).trim();
        String country  = props.path("country").asText("").trim();
        String postcode = props.path("postcode").asText("").trim();

        // Category
        JsonNode catsNode = props.path("categories");
        String categoryLabel = buildCategoryLabel(catsNode);

        // Contact
        String website = props.path("website").asText("").trim();
        String phone   = props.path("contact").path("phone").asText(
                props.path("datasource").path("raw").path("phone").asText("")).trim();

        // Opening hours
        String openingHours = props.path("opening_hours").asText("").trim();

        // Place ID (OSM-based)
        String placeId = props.path("place_id").asText("").trim();

        // Datasource for OSM link
        String osmType = props.path("datasource").path("sourcename").asText("").trim();
        long   osmId   = props.path("datasource").path("raw").path("@id").asLong(0L);

        return new HotelPlace(
                placeId, name, address, city, country, postcode,
                lat, lon, categoryLabel, website, phone, openingHours, osmId
        );
    }

    private String buildCategoryLabel(JsonNode catsNode) {
        if (!catsNode.isArray() || catsNode.size() == 0) return "Hôtel";
        // Pick the most specific category (longest string typically)
        String best = "";
        for (JsonNode c : catsNode) {
            String s = c.asText("");
            if (s.length() > best.length()) best = s;
        }
        // Transform "accommodation.hotel" → "Hôtel"
        if (best.isBlank()) return "Hôtel";
        String last = best.contains(".") ? best.substring(best.lastIndexOf('.') + 1) : best;
        return switch (last) {
            case "hotel"            -> "Hôtel";
            case "motel"            -> "Motel";
            case "hostel"           -> "Auberge";
            case "guest_house"      -> "Maison d'hôtes";
            case "hut"              -> "Chalet / Refuge";
            case "chalet"           -> "Chalet";
            default                 -> capitalize(last.replace('_', ' '));
        };
    }

    private static String capitalize(String s) {
        if (s == null || s.isBlank()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    // ── Records ───────────────────────────────────────────────────────────────

    private record CityCenter(double lat, double lon) {}

    /**
     * Hotel data returned by Geoapify Places.
     *
     * @param placeId       Geoapify place ID (unique)
     * @param categoryLabel Human-readable type: "Hôtel", "Resort", "B&B", etc.
     */
    public record HotelPlace(
            String placeId,
            String name,
            String address,
            String city,
            String country,
            String postcode,
            double lat,
            double lon,
            String categoryLabel,
            String website,
            String phone,
            String openingHours,
            long   osmId
    ) {
        public boolean hasCoords() {
            return !Double.isNaN(lat) && !Double.isNaN(lon);
        }

        public String latStr() {
            return hasCoords() ? String.format(java.util.Locale.ROOT, "%.4f", lat) : "";
        }

        public String lonStr() {
            return hasCoords() ? String.format(java.util.Locale.ROOT, "%.4f", lon) : "";
        }

        /** Google Maps directions URL using coordinates. */
        public String googleMapsUrl() {
            if (!hasCoords()) return null;
            return "https://www.google.com/maps/search/?api=1&query="
                    + String.format(java.util.Locale.ROOT, "%.6f,%.6f", lat, lon);
        }

        /** Full human-readable location line. */
        public String locationLine() {
            if (!city.isBlank() && !country.isBlank()) return city + ", " + country;
            if (!city.isBlank()) return city;
            return country;
        }
    }
}