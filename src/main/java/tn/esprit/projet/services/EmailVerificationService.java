package tn.esprit.projet.services;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Génère un code à 6 chiffres, le stocke et l'envoie par email.
 * Priorité: 1) Brevo API (si clé configurée), 2) Backend local, 3) Mode dev (affiche le code).
 * Config Brevo: propriété système govacate.brevo.api.key ou variable d'environnement BREVO_API_KEY.
 */
public class EmailVerificationService {

    private static final int CODE_LENGTH = 6;
    private static final long CODE_EXPIRY_MS = 10 * 60 * 1000; // 10 minutes
    private static final String DEFAULT_API_URL = "http://localhost:8080/api/send-verification-code";
    private static final String BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";

    private final Map<String, CodeEntry> codesByEmail = new ConcurrentHashMap<>();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private String apiBaseUrl = DEFAULT_API_URL;
    private String brevoApiKey;
    private String brevoSenderEmail = "noreply@govacate.com";
    private String brevoSenderName = "GoVacate";
    private volatile boolean lastSendWasFallback;
    private volatile String lastSentCode;

    public EmailVerificationService() {
        brevoApiKey = System.getProperty("govacate.brevo.api.key");
        if (brevoApiKey == null || brevoApiKey.isBlank()) {
            brevoApiKey = System.getenv("BREVO_API_KEY");
        }
        String senderEmail = System.getProperty("govacate.brevo.sender.email");
        if (senderEmail != null && !senderEmail.isBlank()) brevoSenderEmail = senderEmail.trim();
        String senderName = System.getProperty("govacate.brevo.sender.name");
        if (senderName != null && !senderName.isBlank()) brevoSenderName = senderName.trim();

        // Load from config files (config.local override config.properties)
        loadFromConfig();
    }

    private void loadFromConfig() {
        for (String path : new String[]{"/config.properties", "/config.local.properties"}) {
            try (InputStream is = getClass().getResourceAsStream(path)) {
                if (is != null) {
                    Properties p = new Properties();
                    p.load(is);
                    if (brevoApiKey == null || brevoApiKey.isBlank()) {
                        String key = p.getProperty("govacate.brevo.api.key");
                        if (key != null && !key.isBlank()) brevoApiKey = key.trim();
                    }
                    String email = p.getProperty("govacate.brevo.sender.email");
                    if (email != null && !email.isBlank()) brevoSenderEmail = email.trim();
                    String name = p.getProperty("govacate.brevo.sender.name");
                    if (name != null && !name.isBlank()) brevoSenderName = name.trim();
                }
            } catch (Exception ignored) {}
        }
    }

    public void setApiBaseUrl(String url) {
        this.apiBaseUrl = url != null ? url : DEFAULT_API_URL;
    }

    public void setBrevoApiKey(String key) {
        this.brevoApiKey = key;
    }

    public boolean isLastSendFallback() {
        return lastSendWasFallback;
    }

    public String getLastSentCodeForDev() {
        return lastSendWasFallback ? lastSentCode : null;
    }

    public String sendVerificationCode(String email) {
        if (email == null || email.isBlank()) return null;
        String code = generateCode();
        codesByEmail.put(email, new CodeEntry(code, System.currentTimeMillis()));
        lastSendWasFallback = false;
        lastSentCode = code;

        if (sendViaBrevo(email, code) || sendCodeViaBackendApi(email, code)) {
            return code;
        }

        // Fallback for Development
        lastSendWasFallback = true;
        System.out.println("[DEV] Code pour " + email + " : " + code);
        return code;
    }

    public boolean resendCode(String email) {
        if (email == null || email.isBlank()) return false;
        String code = generateCode();
        codesByEmail.put(email, new CodeEntry(code, System.currentTimeMillis()));
        lastSendWasFallback = false;
        lastSentCode = code;

        if (sendViaBrevo(email, code) || sendCodeViaBackendApi(email, code)) {
            return true;
        }

        lastSendWasFallback = true;
        System.out.println("[DEV] Code renvoyé pour " + email + " : " + code);
        return true;
    }

    public boolean verifyCode(String email, String userInput) {
        if (email == null || userInput == null) return false;
        CodeEntry entry = codesByEmail.get(email);
        if (entry == null) return false;
        if (System.currentTimeMillis() - entry.timestamp > CODE_EXPIRY_MS) {
            codesByEmail.remove(email);
            return false;
        }
        boolean ok = entry.code.equals(userInput.trim());
        if (ok) codesByEmail.remove(email);
        return ok;
    }

    private String generateCode() {
        Random r = new Random();
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) sb.append(r.nextInt(10));
        return sb.toString();
    }

    private boolean sendViaBrevo(String email, String code) {
        if (brevoApiKey == null || brevoApiKey.isBlank()) return false;
        try {
            String html = "<html><body><p>Votre code GoVacate: <strong>" + code + "</strong></p>"
                    + "<p>Expire dans 10 minutes.</p></body></html>";
            String escapedHtml = html.replace("\\", "\\\\").replace("\"", "\\\"");
            String json = String.format(
                    "{\"sender\":{\"name\":\"%s\",\"email\":\"%s\"}," +
                            "\"to\":[{\"email\":\"%s\"}]," +
                            "\"subject\":\"Code de réinitialisation GoVacate\"," +
                            "\"htmlContent\":\"%s\"}",
                    escapeJson(brevoSenderName), escapeJson(brevoSenderEmail),
                    escapeJson(email), escapedHtml);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BREVO_API_URL))
                    .header("accept", "application/json")
                    .header("api-key", brevoApiKey)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(15))
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() >= 200 && response.statusCode() < 300;
        } catch (Exception e) {
            System.err.println("Erreur Brevo API: " + e.getMessage());
        }
        return false;
    }

    private boolean sendCodeViaBackendApi(String email, String code) {
        try {
            String body = String.format("{\"email\":\"%s\",\"code\":\"%s\"}", escapeJson(email), code);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiBaseUrl))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(15))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() >= 200 && response.statusCode() < 300;
        } catch (Exception e) {
            System.err.println("Erreur API backend: " + e.getMessage());
        }
        return false;
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static class CodeEntry {
        final String code;
        final long timestamp;

        CodeEntry(String code, long timestamp) {
            this.code = code;
            this.timestamp = timestamp;
        }
    }
}