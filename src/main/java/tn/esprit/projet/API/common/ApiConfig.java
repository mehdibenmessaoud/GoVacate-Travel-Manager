package tn.esprit.projet.API.common;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class ApiConfig {

    private static final Map<String, String> DOTENV_VALUES = loadDotEnvFile();

    private ApiConfig() {
    }

    public static Optional<String> read(String key) {
        String value = System.getProperty(key);
        if (value == null || value.isBlank()) {
            value = DOTENV_VALUES.get(key);
        }
        if (value == null || value.isBlank()) {
            value = System.getenv(key);
        }
        if (value == null) {
            return Optional.empty();
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? Optional.empty() : Optional.of(trimmed);
    }

    public static String readOrDefault(String key, String defaultValue) {
        return read(key).orElse(defaultValue);
    }

    public static boolean hasAll(String... keys) {
        if (keys == null || keys.length == 0) {
            return false;
        }
        for (String key : keys) {
            if (read(key).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static Map<String, String> loadDotEnvFile() {
        Path dotenvPath = Path.of(".env");
        if (!Files.exists(dotenvPath)) {
            return Map.of();
        }

        Map<String, String> values = new LinkedHashMap<>();
        try {
            for (String line : Files.readAllLines(dotenvPath)) {
                String trimmed = line == null ? "" : line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }

                int equalsIndex = trimmed.indexOf('=');
                if (equalsIndex <= 0) {
                    continue;
                }

                String key = trimmed.substring(0, equalsIndex).trim();
                String value = trimmed.substring(equalsIndex + 1).trim();
                if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
                    value = value.substring(1, value.length() - 1);
                }

                if (!key.isBlank() && !value.isBlank()) {
                    values.put(key, value);
                }
            }
        } catch (IOException ignored) {
            return Map.of();
        }
        return Collections.unmodifiableMap(values);
    }
}
