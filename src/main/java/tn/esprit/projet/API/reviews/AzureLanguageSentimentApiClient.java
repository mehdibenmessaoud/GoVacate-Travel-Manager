package tn.esprit.projet.API.reviews;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import tn.esprit.projet.API.common.ApiConfig;
import tn.esprit.projet.API.common.ApiException;
import tn.esprit.projet.API.common.ExternalHttpUtil;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class AzureLanguageSentimentApiClient {

    private static final String DEFAULT_API_VERSION = "2023-04-01";

    private final HttpClient httpClient;

    public AzureLanguageSentimentApiClient() {
        this.httpClient = ExternalHttpUtil.createHttpClient();
    }

    public boolean isConfigured() {
        return ApiConfig.hasAll("AZURE_LANGUAGE_ENDPOINT", "AZURE_LANGUAGE_KEY");
    }

    public SentimentResult analyzeSentiment(String text, String languageCode) throws ApiException {
        if (!isConfigured()) {
            return new SentimentResult(0.0, 0.0, "NEU", false);
        }

        String safeText = text == null ? "" : text.trim();
        if (safeText.isEmpty()) {
            return new SentimentResult(0.0, 0.0, "NEU", false);
        }

        String endpoint = normalizeEndpoint(ApiConfig.read("AZURE_LANGUAGE_ENDPOINT")
                .orElseThrow(() -> new ApiException("Missing AZURE_LANGUAGE_ENDPOINT")));
        String key = ApiConfig.read("AZURE_LANGUAGE_KEY")
                .orElseThrow(() -> new ApiException("Missing AZURE_LANGUAGE_KEY"));
        String apiVersion = ApiConfig.readOrDefault("AZURE_LANGUAGE_API_VERSION", DEFAULT_API_VERSION);
        String language = (languageCode == null || languageCode.isBlank()) ? "fr" : languageCode.trim();

        String uri = endpoint + "/language/:analyze-text?api-version=" + ExternalHttpUtil.urlEncode(apiVersion);
        HttpRequest request = ExternalHttpUtil.requestBuilder(URI.create(uri))
                .header("Ocp-Apim-Subscription-Key", key)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(buildRequestBody(safeText, language), StandardCharsets.UTF_8))
                .build();

        JsonNode json = ExternalHttpUtil.sendForJson(httpClient, request, "Azure Language sentiment");
        JsonNode errorNode = json.path("error");
        if (!errorNode.isMissingNode() && !errorNode.isNull()) {
            String message = errorNode.path("message").asText("");
            throw new ApiException("Azure Language sentiment error: " + (message.isBlank() ? "unknown error" : message));
        }

        JsonNode documents = json.path("results").path("documents");
        if (!documents.isArray() || documents.isEmpty()) {
            return new SentimentResult(0.0, 0.0, "NEU", false);
        }

        JsonNode first = documents.get(0);
        String sentiment = first.path("sentiment").asText("neutral").toUpperCase(Locale.ROOT);
        JsonNode confidence = first.path("confidenceScores");
        double positive = clamp(confidence.path("positive").asDouble(0.0));
        double neutral = clamp(confidence.path("neutral").asDouble(0.0));
        double negative = clamp(confidence.path("negative").asDouble(0.0));

        double score;
        double magnitude;
        switch (sentiment) {
            case "POSITIVE" -> {
                score = positive;
                magnitude = Math.max(positive, Math.max(neutral, negative));
            }
            case "NEGATIVE" -> {
                score = -negative;
                magnitude = Math.max(negative, Math.max(neutral, positive));
            }
            case "MIXED" -> {
                score = clamp(positive - negative);
                magnitude = Math.max(positive, negative);
            }
            default -> {
                score = 0.0;
                magnitude = neutral;
                sentiment = "NEU";
            }
        }

        return new SentimentResult(score, magnitude, sentiment, true);
    }

    private String buildRequestBody(String text, String language) {
        ObjectNode root = ExternalHttpUtil.MAPPER.createObjectNode();
        root.put("kind", "SentimentAnalysis");

        ObjectNode parameters = root.putObject("parameters");
        parameters.put("modelVersion", "latest");
        parameters.put("opinionMining", false);

        ObjectNode analysisInput = root.putObject("analysisInput");
        ArrayNode documents = analysisInput.putArray("documents");
        ObjectNode doc = documents.addObject();
        doc.put("id", "1");
        doc.put("language", language);
        doc.put("text", text);

        return root.toString();
    }

    private String normalizeEndpoint(String rawEndpoint) {
        String endpoint = rawEndpoint == null ? "" : rawEndpoint.trim();
        while (endpoint.endsWith("/")) {
            endpoint = endpoint.substring(0, endpoint.length() - 1);
        }
        return endpoint;
    }

    private double clamp(double value) {
        return Math.max(-1.0, Math.min(1.0, value));
    }

    public record SentimentResult(double score, double magnitude, String label, boolean analyzed) {
    }
}
