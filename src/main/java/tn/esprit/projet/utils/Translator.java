package tn.esprit.projet.utils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.json.JSONObject;

public class Translator {
    // Public instances often change; you can also host your own locally
    private static final String API_URL = "https://libretranslate.de/translate";

    public static String translate(String text, String targetLang) {
        try {
            JSONObject json = new JSONObject();
            json.put("q", text);
            json.put("source", "auto"); // Auto-detect source language
            json.put("target", targetLang);
            json.put("format", "text");

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return new JSONObject(response.body()).getString("translatedText");
            }
        } catch (Exception e) {
            System.err.println("Translation Error: " + e.getMessage());
        }
        return text; // Return original text if translation fails
    }
}