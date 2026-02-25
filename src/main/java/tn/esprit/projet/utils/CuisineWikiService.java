package tn.esprit.projet.utils;

import org.json.JSONObject;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

public class CuisineWikiService {

    // Wikipedia API endpoint
    private static final String WIKI_API_URL = "https://fr.wikipedia.org/api/rest_v1/page/summary/";

    public static class WikiData {
        public String description;
        public String imageUrl;

        public WikiData(String description, String imageUrl) {
            this.description = description;
            this.imageUrl = imageUrl;
        }
    }

    public CompletableFuture<WikiData> getCuisineInfo(String cuisineType) {

        String query = URLEncoder.encode(
                cuisineType.trim().replace(" ", "_"),
                StandardCharsets.UTF_8
        );

        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(WIKI_API_URL + query))
                .header("User-Agent", "JavaFXRestaurantApp/1.0 (contact@example.com)")
                .header("Accept", "application/json")
                .GET()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {

                    System.out.println("Status: " + response.statusCode());

                    if (response.statusCode() == 200) {
                        JSONObject json = new JSONObject(response.body());

                        String extract = json.optString(
                                "extract",
                                "Aucune description trouvée."
                        );

                        String imgUrl = "";

                        if (json.has("originalimage")) {
                            imgUrl = json.getJSONObject("originalimage").optString("source", "");
                        }

                        if (imgUrl.endsWith(".svg") || imgUrl.endsWith(".webp")) {
                            imgUrl = "";
                        }

                        return new WikiData(extract, imgUrl);
                    }

                    return new WikiData(
                            "Style de cuisine : " + cuisineType,
                            ""
                    );
                })
                .exceptionally(ex -> {
                    ex.printStackTrace();
                    return new WikiData(
                            "Impossible de charger les informations.",
                            ""
                    );
                });
    }
}