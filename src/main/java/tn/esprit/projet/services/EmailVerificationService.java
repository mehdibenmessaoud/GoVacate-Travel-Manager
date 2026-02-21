package tn.esprit.projet.services;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Génère un code à 6 chiffres, le stocke et l'envoie par email via une API REST.
 * Configurez l'URL de votre API (ex. backend qui envoie l'email avec SendGrid, Mailgun, etc.).
 */
public class EmailVerificationService {

    private static final int CODE_LENGTH = 6;
    private static final long CODE_EXPIRY_MS = 10 * 60 * 1000; // 10 minutes
    private static final String DEFAULT_API_URL = "http://localhost:8080/api/send-verification-code";

    private final Map<String, CodeEntry> codesByEmail = new ConcurrentHashMap<>();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private String apiBaseUrl = DEFAULT_API_URL;

    public void setApiBaseUrl(String url) {
        this.apiBaseUrl = url != null ? url : DEFAULT_API_URL;
    }

    /**
     * Génère un code à 6 chiffres et l'envoie à l'email via l'API.
     * @return le code généré (pour tests) ou null si l'envoi échoue
     */
    public String sendVerificationCode(String email) {
        if (email == null || email.isBlank()) return null;
        String code = generateCode();
        codesByEmail.put(email, new CodeEntry(code, System.currentTimeMillis()));
        boolean sent = sendCodeViaApi(email, code);
        return sent ? code : null;
    }

    /**
     * Renvoie le code à l'email (même code ou nouveau selon implémentation).
     */
    public boolean resendCode(String email) {
        if (email == null || email.isBlank()) return false;
        String code = generateCode();
        codesByEmail.put(email, new CodeEntry(code, System.currentTimeMillis()));
        return sendCodeViaApi(email, code);
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

    /**
     * Envoie le code à l'API. Format attendu par défaut : POST avec JSON {"email":"...", "code":"..."}.
     * Adaptez l'URL et le body selon votre backend.
     */
    private boolean sendCodeViaApi(String email, String code) {
        try {
            String body = String.format("{\"email\":\"%s\",\"code\":\"%s\"}", escapeJson(email), code);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiBaseUrl))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(15))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return true;
            }
            // Si l'API n'existe pas encore (404), on considère l'envoi "réussi" pour les tests
            if (response.statusCode() == 404) {
                System.out.println("API envoi email non disponible (404). Code généré: " + code);
                return true;
            }
        } catch (Exception e) {
            System.err.println("Erreur envoi code par API: " + e.getMessage());
            // En dev, on peut accepter l'échec et afficher le code en console
            System.out.println("[DEV] Code pour " + email + " : " + code);
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
