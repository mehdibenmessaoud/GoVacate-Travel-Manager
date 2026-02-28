package tn.esprit.projet.gui;

import java.util.Locale;

/**
 * Shared utility/helper methods used across Admin sub-controllers.
 */
public final class AdminUtils {

    private AdminUtils() {}

    public static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    public static String trimToNull(String value) {
        String trimmed = trimToEmpty(value);
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static boolean isBlank(String value) {
        return trimToEmpty(value).isEmpty();
    }

    public static Double parsePrice(String value) {
        try {
            return Double.parseDouble(trimToEmpty(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static String formatPrice(double price) {
        if (price == Math.rint(price)) {
            return String.format(Locale.US, "%.0f", price);
        }
        return String.format(Locale.US, "%.2f", price);
    }

    public static String renderStars(int count) {
        int safe = Math.max(0, Math.min(5, count));
        return "\u2605".repeat(safe) + "\u2606".repeat(5 - safe);
    }

    public static String formatStarsWithScore(int count) {
        int safe = Math.max(1, Math.min(5, count));
        return renderStars(safe) + " (" + safe + "/5)";
    }

    public static String getStatusLabel(String status) {
        return switch (status) {
            case "AVAILABLE" -> "Disponible";
            case "OCCUPIED" -> "Occupe";
            case "MAINTENANCE" -> "Maintenance";
            default -> status;
        };
    }

    /**
     * Returns a CSS style-class name for a room status badge.
     * Apply via node.getStyleClass().add(getStatusStyleClass(status))
     * instead of using inline styles — this is consistent with the design system.
     */
    public static String getStatusStyleClass(String status) {
        return switch (status) {
            case "AVAILABLE"   -> "badge-available";
            case "OCCUPIED"    -> "badge-occupied";
            case "MAINTENANCE" -> "badge-maintenance";
            default            -> "badge-maintenance";
        };
    }

    /**
     * @deprecated Use {@link #getStatusStyleClass(String)} instead.
     * Kept for backward compatibility only.
     */
    @Deprecated
    public static String getStatusStyle(String status) {
        return switch (status) {
            case "AVAILABLE"   -> "-fx-background-color: #28a745; -fx-text-fill: white; -fx-padding: 4 12; -fx-background-radius: 20; -fx-font-size: 11px; -fx-font-weight: 700;";
            case "OCCUPIED"    -> "-fx-background-color: #FF8210; -fx-text-fill: white; -fx-padding: 4 12; -fx-background-radius: 20; -fx-font-size: 11px; -fx-font-weight: 700;";
            default            -> "-fx-background-color: #6C6D6F; -fx-text-fill: white; -fx-padding: 4 12; -fx-background-radius: 20; -fx-font-size: 11px; -fx-font-weight: 700;";
        };
    }

    public static String formatApiValue(String value, String fallback) {
        String cleaned = trimToEmpty(value);
        return cleaned.isEmpty() ? fallback : cleaned;
    }

    public static String formatStayRange(String checkIn, String checkOut) {
        String in = trimToEmpty(checkIn);
        String out = trimToEmpty(checkOut);
        return in.isEmpty() || out.isEmpty() ? "N/A" : in + " -> " + out;
    }

    public static String formatApiToken(String value, String fallback) {
        String cleaned = trimToEmpty(value);
        if (cleaned.isEmpty()) return fallback;
        String normalizedUpper = cleaned.toUpperCase(Locale.ROOT);
        if ("N/A".equals(normalizedUpper) || "N/".equals(normalizedUpper) || "NA".equals(normalizedUpper)) {
            return fallback;
        }
        String normalized = cleaned
                .replace('_', ' ')
                .replace('-', ' ')
                .replace('/', ' ')
                .trim()
                .replaceAll("\\s+", " ");
        if (normalized.isEmpty()) return fallback;
        String[] words = normalized.toLowerCase(Locale.ROOT).split(" ");
        StringBuilder builder = new StringBuilder();
        for (String word : words) {
            if (word.isBlank()) continue;
            if (!builder.isEmpty()) builder.append(' ');
            builder.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return builder.isEmpty() ? fallback : builder.toString();
    }

    public static String trimToMaxLength(String value, int maxLength) {
        String safeValue = formatApiValue(value, "");
        int safeMax = Math.max(4, maxLength);
        if (safeValue.length() <= safeMax) return safeValue;
        return safeValue.substring(0, safeMax - 3) + "...";
    }

    public static String formatImportanceScore(double importance) {
        if (importance <= 0) return "N/A";
        return String.format(Locale.ROOT, "%.2f", importance);
    }

    public static String formatCoordinates(String lat, String lon) {
        String safeLat = trimToEmpty(lat);
        String safeLon = trimToEmpty(lon);
        if (safeLat.isBlank() || safeLon.isBlank()) return "N/A";
        return safeLat + ", " + safeLon;
    }

    public static String extractErrorMessage(Throwable error) {
        if (error == null) return "Erreur inconnue.";
        Throwable current = error;
        while (current.getCause() != null) current = current.getCause();
        String message = trimToEmpty(current.getMessage());
        return message.isEmpty() ? "Erreur API." : message;
    }

    public static String firstNonBlank(String first, String second) {
        if (first != null && !first.trim().isEmpty()) return first.trim();
        if (second != null && !second.trim().isEmpty()) return second.trim();
        return null;
    }

    public static Double parseCoordinate(String value) {
        try {
            String trimmed = trimToEmpty(value);
            if (trimmed.isBlank()) return null;
            return Double.parseDouble(trimmed);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static double clampLatitude(double value) {
        return Math.max(-85.05112878, Math.min(85.05112878, value));
    }

    public static double clampLongitude(double value) {
        return Math.max(-179.999999, Math.min(179.999999, value));
    }
}
