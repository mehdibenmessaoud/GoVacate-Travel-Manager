package tn.esprit.projet.API.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URLEncoder;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;

public final class ExternalHttpUtil {

    public static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int DEFAULT_CONNECT_TIMEOUT_SECONDS = readIntConfig("GOVACATE_API_CONNECT_TIMEOUT_SECONDS", 10, 3, 60);
    private static final int DEFAULT_REQUEST_TIMEOUT_SECONDS = readIntConfig("GOVACATE_API_TIMEOUT_SECONDS", 18, 5, 120);
    private static final int DEFAULT_MAX_RETRIES = readIntConfig("GOVACATE_API_MAX_RETRIES", 2, 0, 4);
    private static final long BASE_RETRY_BACKOFF_MILLIS = 350L;

    private ExternalHttpUtil() {
    }

    public static HttpClient createHttpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(DEFAULT_CONNECT_TIMEOUT_SECONDS))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public static HttpRequest.Builder requestBuilder(URI uri) {
        return HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofSeconds(DEFAULT_REQUEST_TIMEOUT_SECONDS));
    }

    public static String toFormBody(Map<String, String> data) {
        StringJoiner joiner = new StringJoiner("&");
        Map<String, String> safeMap = data == null ? new LinkedHashMap<>() : data;
        for (Map.Entry<String, String> entry : safeMap.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            joiner.add(urlEncode(entry.getKey()) + "=" + urlEncode(entry.getValue()));
        }
        return joiner.toString();
    }

    public static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    public static JsonNode sendForJson(HttpClient httpClient, HttpRequest request, String apiName) throws ApiException {
        int retries = Math.max(0, DEFAULT_MAX_RETRIES);
        int attempts = retries + 1;

        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                int status = response.statusCode();
                if (status >= 200 && status < 300) {
                    return MAPPER.readTree(response.body());
                }

                if (attempt < attempts && isRetriableStatus(status)) {
                    backoff(attempt);
                    continue;
                }

                String safeBody = truncate(response.body(), 400);
                throw new ApiException(apiName + " request failed with HTTP " + status + ": " + safeBody);
            } catch (HttpTimeoutException e) {
                if (attempt < attempts) {
                    backoff(attempt);
                    continue;
                }
                throw new ApiException(apiName + " request timed out after " + attempts + " attempt(s).", e);
            } catch (IOException e) {
                if (attempt < attempts) {
                    backoff(attempt);
                    continue;
                }
                throw new ApiException(apiName + " request failed: " + e.getMessage(), e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ApiException(apiName + " request interrupted", e);
            }
        }

        throw new ApiException(apiName + " request failed unexpectedly.");
    }

    private static boolean isRetriableStatus(int statusCode) {
        return statusCode == 408 || statusCode == 429 || (statusCode >= 500 && statusCode < 600);
    }

    private static void backoff(int attemptNumber) throws ApiException {
        long waitMillis = (long) (BASE_RETRY_BACKOFF_MILLIS * Math.pow(2, Math.max(0, attemptNumber - 1)));
        try {
            Thread.sleep(waitMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException("Retry backoff interrupted", e);
        }
    }

    private static int readIntConfig(String key, int defaultValue, int min, int max) {
        String raw = ApiConfig.read(key).orElse(String.valueOf(defaultValue));
        try {
            int value = Integer.parseInt(raw.trim());
            return Math.max(min, Math.min(max, value));
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.length() <= maxLength) {
            return trimmed;
        }
        return trimmed.substring(0, maxLength) + "...";
    }
}
