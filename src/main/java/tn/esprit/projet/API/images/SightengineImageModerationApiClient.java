package tn.esprit.projet.API.images;

import com.fasterxml.jackson.databind.JsonNode;
import tn.esprit.projet.API.common.ApiConfig;
import tn.esprit.projet.API.common.ApiException;
import tn.esprit.projet.API.common.ExternalHttpUtil;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SightengineImageModerationApiClient {

    private static final String DEFAULT_MODELS = "nudity-2.1,offensive,self-harm,violence,gore-2.0,weapon";
    private static final double DEFAULT_BLOCK_THRESHOLD = 0.70;
    private static final double DEFAULT_NUDITY_THRESHOLD = 0.10;
    private static final double DEFAULT_SELF_HARM_THRESHOLD = 0.10;
    private static final String[] ALL_RISK_TOKENS = {
            "nudity",
            "sexual",
            "erotica",
            "offensive",
            "weapon",
            "violence",
            "gore",
            "drugs",
            "self-harm",
            "self_harm",
            "selfharm",
            "suicide",
            "hate"
    };
    private static final String[] NUDITY_RISK_TOKENS = {"nudity", "sexual", "erotica"};
    private static final String[] SELF_HARM_RISK_TOKENS = {"self-harm", "self_harm", "selfharm", "suicide"};

    private final HttpClient httpClient;

    public SightengineImageModerationApiClient() {
        this.httpClient = HttpClient.newHttpClient();
    }

    public boolean isConfigured() {
        return ApiConfig.hasAll("SIGHTENGINE_API_USER", "SIGHTENGINE_API_SECRET");
    }

    public ModerationResult moderateImage(String imageInput) throws ApiException {
        if (!isConfigured()) {
            return new ModerationResult(true, false, 0.0, "Sightengine image moderation not configured");
        }

        String safeInput = imageInput == null ? "" : imageInput.trim();
        if (safeInput.isEmpty()) {
            throw new ApiException("Image input cannot be empty.");
        }

        String apiUser = ApiConfig.read("SIGHTENGINE_API_USER")
                .orElseThrow(() -> new ApiException("Missing SIGHTENGINE_API_USER"));
        String apiSecret = ApiConfig.read("SIGHTENGINE_API_SECRET")
                .orElseThrow(() -> new ApiException("Missing SIGHTENGINE_API_SECRET"));
        String models = ApiConfig.readOrDefault("SIGHTENGINE_IMAGE_MODELS", DEFAULT_MODELS);
        if (models.isBlank()) {
            models = DEFAULT_MODELS;
        }

        JsonNode json;
        if (safeInput.startsWith("http://") || safeInput.startsWith("https://")) {
            Map<String, String> form = new LinkedHashMap<>();
            form.put("models", models);
            form.put("url", safeInput);
            form.put("api_user", apiUser);
            form.put("api_secret", apiSecret);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.sightengine.com/1.0/check.json"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(ExternalHttpUtil.toFormBody(form), StandardCharsets.UTF_8))
                    .build();
            json = ExternalHttpUtil.sendForJson(httpClient, request, "Sightengine image moderation");
        } else {
            Path path = Path.of(safeInput);
            if (!Files.exists(path)) {
                throw new ApiException("Local image file not found: " + safeInput);
            }
            String boundary = "----GoVacateSightengineBoundary" + UUID.randomUUID();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.sightengine.com/1.0/check.json"))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(buildMultipartBody(boundary, path, models, apiUser, apiSecret))
                    .build();
            json = ExternalHttpUtil.sendForJson(httpClient, request, "Sightengine image moderation");
        }

        if (!"success".equalsIgnoreCase(json.path("status").asText(""))) {
            throw new ApiException("Sightengine image moderation did not return success.");
        }

        double generalBlockThreshold = readThreshold("SIGHTENGINE_IMAGE_BLOCK_THRESHOLD", DEFAULT_BLOCK_THRESHOLD);
        double nudityBlockThreshold = readThreshold("SIGHTENGINE_NUDITY_BLOCK_THRESHOLD", DEFAULT_NUDITY_THRESHOLD);
        double selfHarmBlockThreshold = readThreshold("SIGHTENGINE_SELF_HARM_BLOCK_THRESHOLD", DEFAULT_SELF_HARM_THRESHOLD);
        double risk = extractMaxRisk(json, "");
        double nudityRisk = extractCategoryRisk(json, "", NUDITY_RISK_TOKENS);
        double selfHarmRisk = extractCategoryRisk(json, "", SELF_HARM_RISK_TOKENS);
        boolean safe = risk < generalBlockThreshold
                && nudityRisk < nudityBlockThreshold
                && selfHarmRisk < selfHarmBlockThreshold;
        String summary = "risk=" + String.format("%.2f", risk)
                + ", nudity=" + String.format("%.2f", nudityRisk)
                + ", self_harm=" + String.format("%.2f", selfHarmRisk);
        return new ModerationResult(safe, true, risk, summary);
    }

    private HttpRequest.BodyPublisher buildMultipartBody(String boundary,
                                                         Path imagePath,
                                                         String models,
                                                         String apiUser,
                                                         String apiSecret) throws ApiException {
        try {
            byte[] fileBytes = Files.readAllBytes(imagePath);
            String fileName = imagePath.getFileName().toString();
            List<byte[]> parts = new ArrayList<>();

            addFormField(parts, boundary, "models", models);
            addFormField(parts, boundary, "api_user", apiUser);
            addFormField(parts, boundary, "api_secret", apiSecret);

            String fileHeader = "--" + boundary + "\r\n"
                    + "Content-Disposition: form-data; name=\"media\"; filename=\"" + fileName + "\"\r\n"
                    + "Content-Type: application/octet-stream\r\n\r\n";
            parts.add(fileHeader.getBytes(StandardCharsets.UTF_8));
            parts.add(fileBytes);
            parts.add("\r\n".getBytes(StandardCharsets.UTF_8));
            parts.add(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

            return HttpRequest.BodyPublishers.ofByteArrays(parts);
        } catch (IOException e) {
            throw new ApiException("Failed to read image for Sightengine moderation: " + e.getMessage(), e);
        }
    }

    private void addFormField(List<byte[]> parts, String boundary, String name, String value) {
        String part = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n"
                + value + "\r\n";
        parts.add(part.getBytes(StandardCharsets.UTF_8));
    }

    private double extractMaxRisk(JsonNode node, String parentKey) {
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
                String currentPath = parentKey.isBlank() ? key : parentKey + "." + key;

                if (value.isNumber()) {
                    double score = value.asDouble(0.0);
                    if (score >= 0.0 && score <= 1.0 && isRiskKey(currentPath)) {
                        max = Math.max(max, score);
                    }
                }
                max = Math.max(max, extractMaxRisk(value, currentPath));
            }
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                max = Math.max(max, extractMaxRisk(child, parentKey));
            }
        }
        return max;
    }

    private double extractCategoryRisk(JsonNode node, String parentKey, String... categoryTokens) {
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
                String currentPath = parentKey.isBlank() ? key : parentKey + "." + key;

                if (value.isNumber()
                        && containsCategoryToken(currentPath, categoryTokens)
                        && isRiskKey(currentPath)) {
                    double score = value.asDouble(0.0);
                    if (score >= 0.0 && score <= 1.0) {
                        max = Math.max(max, score);
                    }
                }
                max = Math.max(max, extractCategoryRisk(value, currentPath, categoryTokens));
            }
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                max = Math.max(max, extractCategoryRisk(child, parentKey, categoryTokens));
            }
        }
        return max;
    }

    private boolean containsCategoryToken(String keyPath, String... tokens) {
        String lower = keyPath == null ? "" : keyPath.toLowerCase();
        if (tokens == null || tokens.length == 0) {
            return false;
        }
        for (String token : tokens) {
            if (token != null && !token.isBlank() && lower.contains(token.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private double readThreshold(String key, double defaultValue) {
        String raw = ApiConfig.read(key).orElse("");
        if (raw.isBlank()) {
            return defaultValue;
        }
        try {
            double parsed = Double.parseDouble(raw.trim());
            return Math.max(0.0, Math.min(1.0, parsed));
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    private boolean isRiskKey(String keyPath) {
        String lower = keyPath == null ? "" : keyPath.toLowerCase();
        if (isIgnoredSafetyKey(lower)) {
            return false;
        }
        return containsCategoryToken(lower, ALL_RISK_TOKENS);
    }

    private boolean isIgnoredSafetyKey(String lowerKeyPath) {
        String lower = lowerKeyPath == null ? "" : lowerKeyPath.toLowerCase();
        // Ignore non-risk context nodes such as nudity.context.sea_lake_pool=0.99.
        if (lower.contains(".context.") || lower.endsWith(".context")) {
            return true;
        }
        return lower.contains("safe") || lower.endsWith(".none");
    }

    public record ModerationResult(boolean safe, boolean checked, double riskScore, String summary) {
    }
}
