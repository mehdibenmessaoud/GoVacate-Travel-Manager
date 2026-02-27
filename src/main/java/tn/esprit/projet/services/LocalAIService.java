package tn.esprit.projet.services;

import org.json.JSONArray;
import org.json.JSONObject;
import java.net.URI;
import java.net.http.*;
import java.util.*;
import java.util.stream.Collectors;
import tn.esprit.projet.entities.*;

public class LocalAIService {
    private static final String OLLAMA_URL = "http://localhost:11434/api/generate";

    public AIRecommendation getAutomatedAdvice(String userCravings, List<Restaurant> restaurants, List<Menu> menus, List<RestaurantReview> reviews) {

        System.out.println("\n================ AI DEBUG SESSION ================");
        System.out.println("USER QUERY: " + userCravings);

        StringBuilder contextBuilder = new StringBuilder();
        contextBuilder.append("RESTAURANTS AND MENUS AVAILABLE:\n");
        for (Restaurant r : restaurants) {
            contextBuilder.append(String.format("- %s (ID:%d) [%s]: ", r.getName(), r.getId(), r.getCategory()));
            String items = menus.stream()
                    .filter(m -> m.getRestaurantId() == r.getId())
                    .map(Menu::getName)
                    .collect(Collectors.joining(", "));
            contextBuilder.append(items.isEmpty() ? "No items listed." : items).append("\n");
        }

        String prompt = contextBuilder.toString() +
                "\n=== SYSTEM INSTRUCTIONS ===\n" +
                "1. You are a professional waiter. Speak in a friendly, natural way.\n" +
                "2. NEVER show database IDs, technical numbers, or [ID:XX] in your explanation.\n" +
                "3. NEVER say things like '(ID:20)'. Just say the name of the restaurant.\n" +
                "4. If the user asks for a specific food, mention the restaurant names that have it.\n" +
                "5. Your response must be valid JSON with:\n" +
                "   - \"ids\": [The integers of the matching restaurants]\n" +
                "   - \"explanation\": \"Your friendly message without any IDs\"\n" +
                "\nUSER INPUT: " + userCravings;

        try {
            JSONObject body = new JSONObject();
            body.put("model", "llama3");
            body.put("prompt", prompt);
            body.put("stream", false);
            body.put("format", "json");

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(OLLAMA_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // --- LOG 1: RAW HTTP RESPONSE ---
            System.out.println("DEBUG - STEP 1 (Raw JSON from Ollama): " + response.body());

            JSONObject fullRes = new JSONObject(response.body());
            String innerJson = fullRes.getString("response").trim();

            // --- LOG 2: CLEANED INNER JSON ---
            System.out.println("DEBUG - STEP 2 (Inner Content to be Parsed): " + innerJson);

            JSONObject aiJson = new JSONObject(innerJson);

            List<Integer> ids = new ArrayList<>();
            if (aiJson.has("ids")) {
                JSONArray arr = aiJson.getJSONArray("ids");
                for (int i = 0; i < arr.length(); i++) {
                    ids.add(arr.getInt(i));
                }
                System.out.println("DEBUG - STEP 3: Successfully extracted IDs: " + ids);
            } else {
                System.out.println("DEBUG - STEP 3: WARNING! Key 'ids' not found in Inner Content.");
            }

            String explanation = aiJson.optString("explanation", "Voici mes suggestions.");
            System.out.println("DEBUG - STEP 4: Explanation length: " + explanation.length());
            System.out.println("==================================================\n");

            return new AIRecommendation(ids, explanation);

        } catch (Exception e) {
            System.err.println("!!! AI CRITICAL ERROR !!!");
            System.err.println("Message: " + e.getMessage());
            e.printStackTrace();
            return new AIRecommendation(Collections.emptyList(), "Erreur technique IA.");
        }
    }
}