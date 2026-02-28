package tn.esprit.projet.API.images;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class ResourceImageStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif", "bmp");
    private static final DateTimeFormatter TS_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    public String storeImage(String imageInput, String category) {
        String safeInput = imageInput == null ? "" : imageInput.trim();
        if (safeInput.isEmpty()) {
            throw new IllegalArgumentException("Image path is empty.");
        }

        String safeCategory = normalizeCategory(category);
        byte[] data = readImageBytes(safeInput);
        if (data.length == 0) {
            throw new IllegalArgumentException("Image content is empty.");
        }

        String extension = resolveExtension(safeInput);
        String filename = safeCategory + "_" + TS_FORMAT.format(LocalDateTime.now())
                + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8)
                + "." + extension;

        Path resourcesDir = Path.of("src", "main", "resources", "images", safeCategory);
        Path targetClassesDir = Path.of("target", "classes", "images", safeCategory);
        try {
            Files.createDirectories(resourcesDir);
            Files.write(resourcesDir.resolve(filename), data);

            // Keep runtime classpath folder in sync when running from IDE without a rebuild.
            Files.createDirectories(targetClassesDir);
            Files.copy(resourcesDir.resolve(filename), targetClassesDir.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to store image in resources: " + e.getMessage(), e);
        }

        return safeCategory + "/" + filename;
    }

    private String normalizeCategory(String category) {
        String raw = category == null ? "" : category.trim().toLowerCase(Locale.ROOT);
        return switch (raw) {
            case "hotels", "rooms", "reviews" -> raw;
            default -> throw new IllegalArgumentException("Invalid image category: " + category);
        };
    }

    private byte[] readImageBytes(String imageInput) {
        try {
            if (imageInput.startsWith("http://") || imageInput.startsWith("https://")) {
                URL url = URI.create(imageInput).toURL();
                try (InputStream in = url.openStream()) {
                    return in.readAllBytes();
                }
            }

            Path path = resolveLocalPath(imageInput);
            return Files.readAllBytes(path);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to read image source: " + e.getMessage(), e);
        }
    }

    private Path resolveLocalPath(String imageInput) {
        Path direct = Path.of(imageInput);
        if (Files.exists(direct)) {
            return direct;
        }

        Path resourcesPath = Path.of("src", "main", "resources", "images", imageInput);
        if (Files.exists(resourcesPath)) {
            return resourcesPath;
        }

        Path classesPath = Path.of("target", "classes", "images", imageInput);
        if (Files.exists(classesPath)) {
            return classesPath;
        }

        throw new IllegalArgumentException("Local image file not found: " + imageInput);
    }

    private String resolveExtension(String imageInput) {
        String base = imageInput == null ? "" : imageInput;
        int queryIdx = base.indexOf('?');
        if (queryIdx >= 0) {
            base = base.substring(0, queryIdx);
        }
        int hashIdx = base.indexOf('#');
        if (hashIdx >= 0) {
            base = base.substring(0, hashIdx);
        }

        int dot = base.lastIndexOf('.');
        if (dot < 0 || dot == base.length() - 1) {
            return "jpg";
        }
        String ext = base.substring(dot + 1).toLowerCase(Locale.ROOT).trim();
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            return "jpg";
        }
        return ext;
    }
}
