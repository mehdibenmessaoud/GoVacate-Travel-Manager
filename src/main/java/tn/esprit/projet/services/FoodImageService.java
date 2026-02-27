package tn.esprit.projet.services;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.URI;
import java.net.URL;
import java.net.http.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class FoodImageService {

    private final String pexel_api = getProp("pexel_api");
    private final String API_KEY = pexel_api;
    private static final String SEARCH_URL = "https://api.pexels.com/v1/search";
    private static final String SAVE_DIR = "src/main/resources/images/dishes/";

    public List<String> searchImages(String dishName) throws Exception {
        // CLEANING: Remove numbers/prices (e.g., "Pizza 12.500" becomes "Pizza")
        String cleanName = dishName.replaceAll("\\d+\\.\\d+|\\d+", "").trim();

        // OPTIMIZATION: Contextual keywords for professional food photography
        String query = java.net.URLEncoder.encode("plated " + cleanName + " food restaurant", "UTF-8");
        String urlString = SEARCH_URL + "?query=" + query + "&per_page=10&orientation=landscape";

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(urlString))
                .header("Authorization", API_KEY)
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        JSONObject json = new JSONObject(response.body());

        List<String> urls = new ArrayList<>();
        if (json.has("photos")) {
            JSONArray photos = json.getJSONArray("photos");
            for (int i = 0; i < photos.length(); i++) {
                urls.add(photos.getJSONObject(i).getJSONObject("src").getString("medium"));
            }
        }
        return urls;
    }

    public String saveImageLocally(String imageUrl, String dishName) throws IOException {
        String fileName = dishName.toLowerCase().replaceAll("[^a-z0-9]", "_") + "_" + System.currentTimeMillis() + ".jpg";
        Path path = Paths.get(SAVE_DIR + fileName);
        Files.createDirectories(path.getParent());

        // Using HttpClient for reliable downloading
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(imageUrl)).build();
        try {
            client.send(request, HttpResponse.BodyHandlers.ofFile(path));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return "/images/dishes/" + fileName;
    }

    private String getProp(String key) {
        Properties prop = new Properties();
        try (InputStream input = new FileInputStream("config.properties")) {
            prop.load(input);
            return prop.getProperty(key);
        } catch (IOException ex) {
            System.err.println("Could not find config.properties! Using defaults.");
            return null;
        }
    }
}