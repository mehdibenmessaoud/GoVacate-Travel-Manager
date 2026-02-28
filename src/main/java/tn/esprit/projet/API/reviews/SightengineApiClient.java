package tn.esprit.projet.API.reviews;

import com.fasterxml.jackson.databind.JsonNode;
import tn.esprit.projet.API.common.ApiConfig;
import tn.esprit.projet.API.common.ApiException;
import tn.esprit.projet.API.common.ExternalHttpUtil;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public class SightengineApiClient {

    private final HttpClient httpClient;

    public SightengineApiClient() {
        this.httpClient = HttpClient.newHttpClient();
    }

    public boolean isConfigured() {
        return ApiConfig.hasAll("SIGHTENGINE_API_USER", "SIGHTENGINE_API_SECRET");
    }

    public ModerationResult moderateText(String text) throws ApiException {
        if (!isConfigured()) {
            return new ModerationResult(false, 0.0, "Sightengine not configured", false);
        }

        String safeText = text == null ? "" : text.trim();
        if (safeText.isEmpty()) {
            return new ModerationResult(false, 0.0, "Empty comment", true);
        }

        Map<String, String> form = new LinkedHashMap<>();
        form.put("text", safeText);
        form.put("lang", "fr");
        form.put("mode", "ml");
        form.put("api_user", ApiConfig.read("SIGHTENGINE_API_USER")
                .orElseThrow(() -> new ApiException("Missing SIGHTENGINE_API_USER")));
        form.put("api_secret", ApiConfig.read("SIGHTENGINE_API_SECRET")
                .orElseThrow(() -> new ApiException("Missing SIGHTENGINE_API_SECRET")));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.sightengine.com/1.0/text/check.json"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(ExternalHttpUtil.toFormBody(form), StandardCharsets.UTF_8))
                .build();

        JsonNode json = ExternalHttpUtil.sendForJson(httpClient, request, "Sightengine text moderation");
        if (!"success".equalsIgnoreCase(json.path("status").asText())) {
            throw new ApiException("Sightengine moderation did not return success.");
        }

        int profanityMatches = json.at("/profanity/matches").isArray() ? json.at("/profanity/matches").size() : 0;
        double riskScore = Math.max(extractRiskScore(json, ""), profanityMatches > 0 ? 1.0 : 0.0);

        boolean blocked = profanityMatches > 0 || riskScore >= 0.65;
        String reason = blocked
                ? "risk_score=" + String.format("%.2f", riskScore)
                : "risk_score=" + String.format("%.2f", riskScore);

        return new ModerationResult(blocked, riskScore, reason, true);
    }

    private double extractRiskScore(JsonNode node, String path) {
        if (node == null || node.isMissingNode()) {
            return 0.0;
        }

        double max = 0.0;
        if (node.isObject()) {
            var fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String key = entry.getKey();
                JsonNode value = entry.getValue();
                String childPath = path + "/" + key;

                if (value.isNumber() && isRiskKey(key)) {
                    double score = value.asDouble(0.0);
                    if (score >= 0.0 && score <= 1.0) {
                        max = Math.max(max, score);
                    }
                }
                max = Math.max(max, extractRiskScore(value, childPath));
            }
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                max = Math.max(max, extractRiskScore(child, path));
            }
        }
        return max;
    }

    private boolean isRiskKey(String key) {
        if (key == null) {
            return false;
        }
        String lower = key.toLowerCase();
        return lower.contains("tox")
                || lower.contains("offens")
                || lower.contains("profan")
                || lower.contains("hate")
                || lower.contains("insult")
                || lower.contains("sexual")
                || lower.contains("violence")
                || lower.contains("threat");
    }

    public record ModerationResult(boolean blocked, double riskScore, String reason, boolean checked) {
    }
}
