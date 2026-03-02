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
import java.util.Optional;

public class NominatimHotelApiClient {

    private final HttpClient httpClient;
    private final String userAgent;

    public NominatimHotelApiClient() {
        this.httpClient = ExternalHttpUtil.createHttpClient();
        this.userAgent = ApiConfig.readOrDefault("NOMINATIM_USER_AGENT", "GoVacate/1.0");
    }

    public boolean isConfigured() {
        return true;
    }

    public Optional<LocationSummary> findLocationSummary(String hotelName) throws ApiException {
        List<LocationSummary> locations = searchLocationSummaries(hotelName, 1);
        if (locations.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(locations.get(0));
    }

    public List<LocationSummary> searchLocationSummaries(String query, int maxResults) throws ApiException {
        String safeQuery = query == null ? "" : query.trim();
        if (safeQuery.isEmpty()) {
            return List.of();
        }
        int safeLimit = Math.max(1, Math.min(maxResults, 15));

        String uri = "https://nominatim.openstreetmap.org/search"
                + "?format=jsonv2"
                + "&limit=" + safeLimit
                + "&addressdetails=1"
                + "&dedupe=1"
                + "&accept-language=" + ExternalHttpUtil.urlEncode("fr,en")
                + "&q=" + ExternalHttpUtil.urlEncode(safeQuery);

        HttpRequest request = ExternalHttpUtil.requestBuilder(URI.create(uri))
                .header("User-Agent", userAgent)
                .header("Accept", "application/json")
                .GET()
                .build();

        JsonNode json = ExternalHttpUtil.sendForJson(httpClient, request, "Nominatim search");
        if (!json.isArray() || json.size() == 0) {
            return List.of();
        }

        List<LocationSummary> results = new ArrayList<>();
        for (JsonNode item : json) {
            String displayName = item.path("display_name").asText("");
            if (displayName.isBlank()) {
                continue;
            }
            String lat = item.path("lat").asText("");
            String lon = item.path("lon").asText("");
            String osmType = item.path("osm_type").asText("");
            long osmId = item.path("osm_id").asLong(0L);
            String category = item.path("class").asText("");
            String placeType = item.path("type").asText("");
            double importance = item.path("importance").asDouble(0.0);
            results.add(new LocationSummary(displayName, lat, lon, osmType, osmId, category, placeType, importance));
        }
        return results;
    }

    public record LocationSummary(String displayName,
                                  String lat,
                                  String lon,
                                  String osmType,
                                  long osmId,
                                  String category,
                                  String placeType,
                                  double importance) {
    }
}