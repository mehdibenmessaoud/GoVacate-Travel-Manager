package tn.esprit.projet.services;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.json.JSONObject;

public class WeatherService {
    // Utilisation de ta clé API récupérée sur l'image
    private static final String API_KEY = "c4f8cae49ccd9bd1619c99287b054b9e";
    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5/weather?q=";

    public static JSONObject getWeatherByCity(String city) {
        try {
            String url = BASE_URL + city.replace(" ", "%20") + "&appid=" + API_KEY + "&units=metric&lang=fr";

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return new JSONObject(response.body());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}