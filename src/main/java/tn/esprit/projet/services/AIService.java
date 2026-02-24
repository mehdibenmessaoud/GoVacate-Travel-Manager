package tn.esprit.projet.services;

import org.json.JSONArray;
import org.json.JSONObject;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

public class AIService {
    // REMPLACEZ PAR VOTRE CLÉ GROQ
    private static final String API_KEY = "clé_goca";
    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";

    public CompletableFuture<String> getRecommendationAsync(String userQuery, String packsContext) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String prompt = "Tu es l'assistant de GoVacate. Voici nos packs : \n" + packsContext +
                        "\nRéponds à : " + userQuery + " en français (max 3 recommandations).";

                // Construction du JSON au format OpenAI (utilisé par Groq)
                JSONObject jsonBody = new JSONObject();
                jsonBody.put("model", "llama-3.3-70b-versatile");

                JSONArray messages = new JSONArray();
                messages.put(new JSONObject().put("role", "user").put("content", prompt));
                jsonBody.put("messages", messages);

                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(API_URL))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + API_KEY) // Groq utilise Bearer
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody.toString()))
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                String responseBody = response.body();

                System.out.println("DEBUG GROQ : " + responseBody);

                JSONObject responseJson = new JSONObject(responseBody);

                if (responseJson.has("choices")) {
                    return responseJson.getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content");
                } else {
                    return "Erreur Groq : " + responseBody;
                }

            } catch (Exception e) {
                e.printStackTrace();
                return "Désolé, problème de connexion au cerveau de l'IA.";
            }
        });
    }
}