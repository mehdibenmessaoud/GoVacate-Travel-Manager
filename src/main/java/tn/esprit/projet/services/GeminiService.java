package tn.esprit.projet.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import tn.esprit.projet.entities.Menu;

import java.io.File;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

public class GeminiService {
    // Replace this with your freshly generated key from AI Studio
    private final String API_KEY = "apiiiii";
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<Menu> extractMenu(File imageFile, int restaurantId) throws Exception {
        // 1. Prepare Image Data
        String fileName = imageFile.getName().toLowerCase();
        String mimeType = fileName.endsWith(".png") ? "image/png" : "image/jpeg";
        String base64Image = Base64.getEncoder().encodeToString(Files.readAllBytes(imageFile.toPath()));

        // 2. Build JSON Payload
        ObjectNode root = objectMapper.createObjectNode();
        ArrayNode contents = root.putArray("contents");
        ObjectNode contentObj = contents.addObject();
        ArrayNode parts = contentObj.putArray("parts");

        // Set Prompt
        parts.addObject().put("text", "Extract menu items from this image. " +
                "Return ONLY a JSON array of objects with keys: 'name', 'description', 'price'. " +
                "If a price is missing, use 0. If description is missing, use an empty string. " +
                "Example format: [{\"name\": \"Pizza\", \"description\": \"Cheesy\", \"price\": 15.5}]");

        // Set Image
        ObjectNode inlineData = parts.addObject().putObject("inline_data");
        inlineData.put("mime_type", mimeType);
        inlineData.put("data", base64Image);

        // 3. Send Request
        String apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + API_KEY;
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(root.toString()))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Gemini API Error (" + response.statusCode() + "): " + response.body());
        }

        // 4. Parse JSON Response
        JsonNode responseTree = objectMapper.readTree(response.body());
        String rawOutput = responseTree.path("candidates").get(0)
                .path("content").path("parts").get(0)
                .path("text").asText().trim();

        // Clean any Markdown formatting if the AI ignores "ONLY JSON" instruction
        String jsonOutput = rawOutput.replaceAll("```json", "").replaceAll("```", "").trim();

        // 5. Convert to List of Menu Entities
        List<Map<String, Object>> rawItems = objectMapper.readValue(jsonOutput, new TypeReference<>() {});
        List<Menu> menuList = new ArrayList<>();

        for (Map<String, Object> map : rawItems) {
            Menu m = new Menu();
            m.setName(String.valueOf(map.getOrDefault("name", "Unknown Item")));
            m.setDescription(String.valueOf(map.getOrDefault("description", "")));

            // Clean price (removes currency symbols like DT, $, €, etc.)
            Object p = map.get("price");
            try {
                String priceStr = p.toString().replaceAll("[^\\d.]", "");
                m.setPrice(new BigDecimal(priceStr.isEmpty() ? "0" : priceStr));
            } catch (Exception e) {
                m.setPrice(BigDecimal.ZERO);
            }

            m.setRestaurantId(restaurantId);
            m.setStatus("AVAILABLE");
            menuList.add(m);
        }
        return menuList;
    }
}