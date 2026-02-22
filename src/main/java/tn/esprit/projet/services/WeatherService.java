package tn.esprit.projet.services;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.json.JSONObject;

public class WeatherService {
    private static final String API_KEY = "c4f8cae49ccd9bd1619c99287b054b9e";
    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5/weather?q=";

    // Nouvelle méthode pour PackDetailsController
    public static WeatherData getWeather(String city) {
        JSONObject json = getWeatherByCity(city);
        if (json != null) {
            double temp = json.getJSONObject("main").getDouble("temp");
            String desc = json.getJSONArray("weather").getJSONObject(0).getString("description");
            String iconCode = json.getJSONArray("weather").getJSONObject(0).getString("icon");
            return new WeatherData(temp, desc, iconCode);
        }
        return null;
    }

    // Garder cette méthode pour AdminExcursionController
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

    // La classe interne pour les détails
    public static class WeatherData {
        private final double temp;
        private final String description;
        private final String iconUrl;

        public WeatherData(double temp, String description, String iconCode) {
            this.temp = temp;
            this.description = description;
            this.iconUrl = "https://openweathermap.org/img/wn/" + iconCode + "@2x.png";
        }

        public double getTemp() { return temp; }
        public String getDescription() { return description; }
        public String getIconUrl() { return iconUrl; }
    }
}