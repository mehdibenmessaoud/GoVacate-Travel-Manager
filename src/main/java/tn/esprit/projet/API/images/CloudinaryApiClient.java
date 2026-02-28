package tn.esprit.projet.API.images;

import com.fasterxml.jackson.databind.JsonNode;
import tn.esprit.projet.API.common.ApiConfig;
import tn.esprit.projet.API.common.ApiException;
import tn.esprit.projet.API.common.ExternalHttpUtil;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class CloudinaryApiClient {

    private final HttpClient httpClient;

    public CloudinaryApiClient() {
        this.httpClient = HttpClient.newHttpClient();
    }

    public boolean isConfigured() {
        try {
            CloudinaryCredentials credentials = resolveCredentials();
            return supportsSignedUpload(credentials);
        } catch (ApiException ignored) {
            return false;
        }
    }

    public UploadResult uploadImage(String imageInput) throws ApiException {
        return uploadImage(imageInput, "");
    }

    public UploadResult uploadImage(String imageInput, String folder) throws ApiException {
        String safeInput = imageInput == null ? "" : imageInput.trim();
        if (safeInput.isEmpty()) {
            throw new ApiException("Image input cannot be empty.");
        }

        CloudinaryCredentials credentials = resolveCredentials();
        if (!supportsSignedUpload(credentials)) {
            throw new ApiException(
                    "Cloudinary signed upload is not configured. Set CLOUDINARY_CLOUD_NAME, "
                            + "CLOUDINARY_API_KEY and CLOUDINARY_API_SECRET (or CLOUDINARY_URL)."
            );
        }
        String normalizedFolder = normalizeFolder(folder);
        if (normalizedFolder.isBlank()) {
            normalizedFolder = normalizeFolder(ApiConfig.read("CLOUDINARY_FOLDER").orElse(""));
        }

        String endpoint = "https://api.cloudinary.com/v1_1/"
                + ExternalHttpUtil.urlEncode(credentials.cloudName())
                + "/image/upload";
        Map<String, String> fields = new LinkedHashMap<>();
        if (!normalizedFolder.isBlank()) {
            fields.put("folder", normalizedFolder);
        }
        long timestamp = Instant.now().getEpochSecond();
        Map<String, String> signatureParams = new LinkedHashMap<>();
        signatureParams.put("timestamp", String.valueOf(timestamp));
        if (!normalizedFolder.isBlank()) {
            signatureParams.put("folder", normalizedFolder);
        }
        if (credentials.uploadPreset() != null && !credentials.uploadPreset().isBlank()) {
            signatureParams.put("upload_preset", credentials.uploadPreset());
            fields.put("upload_preset", credentials.uploadPreset());
        }
        fields.put("api_key", credentials.apiKey());
        fields.put("timestamp", String.valueOf(timestamp));
        fields.put("signature", signRequest(signatureParams, credentials.apiSecret()));

        JsonNode json;
        if (safeInput.startsWith("http://") || safeInput.startsWith("https://")) {
            Map<String, String> form = new LinkedHashMap<>(fields);
            form.put("file", safeInput);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(ExternalHttpUtil.toFormBody(form), StandardCharsets.UTF_8))
                    .build();
            json = ExternalHttpUtil.sendForJson(httpClient, request, "Cloudinary upload");
        } else {
            Path path = Path.of(safeInput);
            if (!Files.exists(path)) {
                throw new ApiException("Local image file not found: " + safeInput);
            }
            String boundary = "----GoVacateBoundary" + UUID.randomUUID();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(buildMultipartBody(boundary, path, fields))
                    .build();
            json = ExternalHttpUtil.sendForJson(httpClient, request, "Cloudinary upload");
        }

        String secureUrl = json.path("secure_url").asText("");
        String publicId = json.path("public_id").asText("");
        if (secureUrl.isBlank()) {
            throw new ApiException("Cloudinary did not return secure_url.");
        }
        return new UploadResult(secureUrl, publicId, true);
    }

    private HttpRequest.BodyPublisher buildMultipartBody(String boundary,
                                                         Path imagePath,
                                                         Map<String, String> fields) throws ApiException {
        try {
            byte[] fileBytes = Files.readAllBytes(imagePath);
            String fileName = imagePath.getFileName().toString();
            List<byte[]> parts = new ArrayList<>();

            for (Map.Entry<String, String> field : fields.entrySet()) {
                if (field.getValue() == null || field.getValue().isBlank()) {
                    continue;
                }
                addFormField(parts, boundary, field.getKey(), field.getValue());
            }

            String fileHeader = "--" + boundary + "\r\n"
                    + "Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"\r\n"
                    + "Content-Type: application/octet-stream\r\n\r\n";
            parts.add(fileHeader.getBytes(StandardCharsets.UTF_8));
            parts.add(fileBytes);
            parts.add("\r\n".getBytes(StandardCharsets.UTF_8));
            parts.add(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

            return HttpRequest.BodyPublishers.ofByteArrays(parts);
        } catch (IOException e) {
            throw new ApiException("Failed to read image for Cloudinary upload: " + e.getMessage(), e);
        }
    }

    private void addFormField(List<byte[]> parts, String boundary, String name, String value) {
        String part = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n"
                + value + "\r\n";
        parts.add(part.getBytes(StandardCharsets.UTF_8));
    }

    private CloudinaryCredentials resolveCredentials() throws ApiException {
        String cloudName = safeTrim(ApiConfig.read("CLOUDINARY_CLOUD_NAME").orElse(""));
        String uploadPreset = safeTrim(ApiConfig.read("CLOUDINARY_UPLOAD_PRESET").orElse(""));
        String apiKey = safeTrim(ApiConfig.read("CLOUDINARY_API_KEY").orElse(""));
        String apiSecret = safeTrim(ApiConfig.read("CLOUDINARY_API_SECRET").orElse(""));

        String cloudinaryUrl = safeTrim(ApiConfig.read("CLOUDINARY_URL").orElse(""));
        if (!cloudinaryUrl.isBlank()) {
            ParsedCloudinaryUrl parsed = parseCloudinaryUrl(cloudinaryUrl);
            if (cloudName.isBlank()) {
                cloudName = parsed.cloudName();
            }
            if (apiKey.isBlank()) {
                apiKey = parsed.apiKey();
            }
            if (apiSecret.isBlank()) {
                apiSecret = parsed.apiSecret();
            }
        }

        return new CloudinaryCredentials(cloudName, uploadPreset, apiKey, apiSecret);
    }

    private ParsedCloudinaryUrl parseCloudinaryUrl(String raw) throws ApiException {
        URI uri;
        try {
            uri = URI.create(raw);
        } catch (Exception e) {
            throw new ApiException("Invalid CLOUDINARY_URL format.", e);
        }

        if (!"cloudinary".equalsIgnoreCase(uri.getScheme())) {
            throw new ApiException("Invalid CLOUDINARY_URL scheme. Expected cloudinary://...");
        }

        String userInfo = uri.getUserInfo();
        String host = uri.getHost();
        if (userInfo == null || userInfo.isBlank() || host == null || host.isBlank()) {
            throw new ApiException("Invalid CLOUDINARY_URL. Expected cloudinary://api_key:api_secret@cloud_name");
        }

        String[] credentials = userInfo.split(":", 2);
        if (credentials.length != 2 || credentials[0].isBlank() || credentials[1].isBlank()) {
            throw new ApiException("Invalid CLOUDINARY_URL credentials section.");
        }

        String apiKey = URLDecoder.decode(credentials[0], StandardCharsets.UTF_8);
        String apiSecret = URLDecoder.decode(credentials[1], StandardCharsets.UTF_8);
        String cloudName = URLDecoder.decode(host, StandardCharsets.UTF_8);
        return new ParsedCloudinaryUrl(cloudName.trim(), apiKey.trim(), apiSecret.trim());
    }

    private String signRequest(Map<String, String> params, String apiSecret) throws ApiException {
        String toSign = params.entrySet().stream()
                .filter(entry -> entry.getValue() != null && !entry.getValue().isBlank())
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&"));
        return sha1Hex(toSign + apiSecret);
    }

    private String sha1Hex(String content) throws ApiException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] bytes = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new ApiException("Unable to initialize SHA-1 for Cloudinary signature.", e);
        }
    }

    private boolean supportsSignedUpload(CloudinaryCredentials credentials) {
        return credentials != null
                && !credentials.cloudName().isBlank()
                && !credentials.apiKey().isBlank()
                && !credentials.apiSecret().isBlank();
    }

    private String normalizeFolder(String folder) {
        String raw = safeTrim(folder).replace('\\', '/');
        if (raw.isBlank()) {
            return "";
        }

        StringBuilder sanitized = new StringBuilder(raw.length());
        boolean previousSlash = false;
        for (char c : raw.toCharArray()) {
            if (Character.isLetterOrDigit(c) || c == '_' || c == '-' || c == '/') {
                if (c == '/') {
                    if (!previousSlash) {
                        sanitized.append(c);
                    }
                    previousSlash = true;
                } else {
                    sanitized.append(c);
                    previousSlash = false;
                }
            }
        }

        String cleaned = sanitized.toString();
        while (cleaned.startsWith("/")) {
            cleaned = cleaned.substring(1);
        }
        while (cleaned.endsWith("/")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        }
        return cleaned;
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private record CloudinaryCredentials(String cloudName, String uploadPreset, String apiKey, String apiSecret) {
    }

    private record ParsedCloudinaryUrl(String cloudName, String apiKey, String apiSecret) {
    }

    public record UploadResult(String secureUrl, String publicId, boolean uploadedToCloudinary) {
    }
}
