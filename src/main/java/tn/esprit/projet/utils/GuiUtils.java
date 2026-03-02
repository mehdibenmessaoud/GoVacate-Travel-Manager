package tn.esprit.projet.utils;

import java.util.Locale;

/**
 * Unified utility methods for all GUI controllers (Admin + Client).
 * Replaces the duplicated GuiUtils and GuiUtils classes.
 */
public final class GuiUtils {

    private GuiUtils() {}

    // ── String helpers ────────────────────────────────────────────────────────

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

    // ── Number helpers ────────────────────────────────────────────────────────

    public static Double parsePrice(String value) {
        try {
            return Double.parseDouble(trimToEmpty(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static String formatPrice(double price) {
        return price == Math.rint(price)
                ? String.format(Locale.US, "%.0f", price)
                : String.format(Locale.US, "%.2f", price);
    }

    // ── Star rating helpers ───────────────────────────────────────────────────

    public static int clampStars(int stars) {
        return Math.max(1, Math.min(5, stars));
    }

    public static String renderStars(int count) {
        int safe = Math.max(0, Math.min(5, count));
        return "\u2605".repeat(safe) + "\u2606".repeat(5 - safe);
    }

    /**
     * Backward-compatible alias used by former ClientUtils call sites.
     */
    public static String renderStarsVisual(int stars) {
        return renderStars(stars);
    }

    public static String formatStarsWithScore(int count) {
        int safe = clampStars(count);
        return renderStars(safe) + " (" + safe + "/5)";
    }

    // ── Status helpers ────────────────────────────────────────────────────────

    public static String getStatusLabel(String status) {
        return switch (status == null ? "" : status) {
            case "AVAILABLE" -> "Disponible";
            case "OCCUPIED" -> "Occupee";
            case "MAINTENANCE" -> "Maintenance";
            default -> status;
        };
    }

    public static String getStatusStyleClass(String status) {
        return switch (status == null ? "" : status) {
            case "AVAILABLE" -> "badge-available";
            case "OCCUPIED" -> "badge-occupied";
            case "MAINTENANCE" -> "badge-maintenance";
            default -> "badge-maintenance";
        };
    }

    /**
     * Backward-compatible alias used by former ClientUtils call sites.
     */
    public static String getStatusBadgeClass(String status) {
        return getStatusStyleClass(status);
    }

    // ── API / formatting helpers ──────────────────────────────────────────────

    public static String formatApiValue(String value, String fallback) {
        String cleaned = trimToEmpty(value);
        return cleaned.isEmpty() ? fallback : cleaned;
    }

    public static String extractErrorMessage(Throwable error) {
        if (error == null) return "Erreur inconnue.";
        Throwable current = error;
        while (current.getCause() != null) current = current.getCause();
        String msg = trimToEmpty(current.getMessage());
        return msg.isEmpty() ? "Erreur API." : msg;
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

    // ── Coordinate helpers ────────────────────────────────────────────────────

    public static Double parseCoordinate(String value) {
        try {
            String trimmed = trimToEmpty(value);
            return trimmed.isBlank() ? null : Double.parseDouble(trimmed);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static String formatCoordinates(String lat, String lon) {
        String safeLat = trimToEmpty(lat);
        String safeLon = trimToEmpty(lon);
        if (safeLat.isBlank() || safeLon.isBlank()) return "N/A";
        return safeLat + ", " + safeLon;
    }

    public static double clampLatitude(double value) {
        return Math.max(-85.05112878, Math.min(85.05112878, value));
    }

    public static double clampLongitude(double value) {
        return Math.max(-179.999999, Math.min(179.999999, value));
    }

    // ── OSM tile helpers (from GuiUtils) ───────────────────────────────────

    public static final int    OSM_TILE_SIZE            = 256;
    public static final String OSM_TILE_URL_TEMPLATE    = "https://basemaps.cartocdn.com/rastertiles/voyager/%d/%d/%d.png";
    public static final int    OSM_PREVIEW_MIN_ZOOM     = 3;
    public static final int    OSM_PREVIEW_MAX_ZOOM     = 18;
    public static final int    OSM_PREVIEW_INITIAL_ZOOM = 11;

    public static double longitudeToWorldPixelX(double lon, int zoom) {
        double worldSize = OSM_TILE_SIZE * (double) (1 << zoom);
        return ((clampLongitude(lon) + 180.0) / 360.0) * worldSize;
    }

    public static double latitudeToWorldPixelY(double lat, int zoom) {
        double clamped = clampLatitude(lat);
        double latRad = Math.toRadians(clamped);
        double worldSize = OSM_TILE_SIZE * (double) (1 << zoom);
        double mercator = Math.log(Math.tan(Math.PI / 4.0 + latRad / 2.0));
        return (1.0 - mercator / Math.PI) * worldSize / 2.0;
    }

    public static double worldPixelXToLongitude(double pixelX, int zoom) {
        double worldSize = OSM_TILE_SIZE * (double) (1 << zoom);
        return (pixelX / worldSize) * 360.0 - 180.0;
    }

    public static double worldPixelYToLatitude(double pixelY, int zoom) {
        double worldSize = OSM_TILE_SIZE * (double) (1 << zoom);
        double clampedPixelY = Math.max(0.0, Math.min(worldSize, pixelY));
        double n = Math.PI - (2.0 * Math.PI * clampedPixelY / worldSize);
        return Math.toDegrees(Math.atan(Math.sinh(n)));
    }

    public static int wrapTileIndex(int value, int tileCount) {
        int wrapped = value % tileCount;
        return wrapped < 0 ? wrapped + tileCount : wrapped;
    }

    public static double wrapPixelValue(double value, double modulo) {
        double wrapped = value % modulo;
        return wrapped < 0 ? wrapped + modulo : wrapped;
    }

    // ── Location helpers (from GuiUtils) ───────────────────────────────────

    public static String extractLocationTitle(String displayName) {
        String safe = formatApiValue(displayName, "Lieu inconnu");
        String[] segments = safe.split(",");
        return trimToMaxLength(segments.length == 0 ? safe : segments[0].trim(), 36);
    }

    public static String formatImportanceScore(double importance) {
        if (importance <= 0) return "N/A";
        return String.format(Locale.ROOT, "%.2f", importance);
    }

    // ── Input parsing helpers (from GuiUtils) ──────────────────────────────

    public static int parseNightsInput(String text) {
        if (text == null || text.isBlank()) return 1;
        try {
            int n = Integer.parseInt(text.trim());
            return n < 1 ? 1 : Math.min(n, 30);
        } catch (NumberFormatException ignored) {
            return 1;
        }
    }

    public static int parseGuestsInput(String text) {
        if (text == null || text.isBlank()) return 1;
        try {
            int n = Integer.parseInt(text.trim());
            return n < 1 ? 1 : Math.min(n, 8);
        } catch (NumberFormatException ignored) {
            return 1;
        }
    }
}
