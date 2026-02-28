package tn.esprit.projet.gui;

import java.util.Locale;

/**
 * Pure static utility methods for the Client module.
 * No JavaFX scene graph dependencies — safe to unit-test in isolation.
 */
public final class ClientUtils {

    private ClientUtils() {}

    // ─────────────────────────────────────────────────────────────────────────
    //  Stars / ratings
    // ─────────────────────────────────────────────────────────────────────────

    public static int clampStars(int stars) {
        return Math.max(1, Math.min(5, stars));
    }

    public static String renderStarsVisual(int stars) {
        int s = clampStars(stars);
        return "\u2605".repeat(s) + "\u2606".repeat(5 - s);
    }

    public static String formatStarsWithScore(int stars) {
        int s = clampStars(stars);
        return renderStarsVisual(s) + " (" + s + "/5)";
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Price
    // ─────────────────────────────────────────────────────────────────────────

    public static String formatPrice(double price) {
        return price == Math.rint(price)
                ? String.format("%.0f", price)
                : String.format("%.2f", price);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Status labels
    // ─────────────────────────────────────────────────────────────────────────

    public static String getStatusLabel(String status) {
        return switch (status == null ? "" : status) {
            case "AVAILABLE"   -> "Disponible";
            case "OCCUPIED"    -> "Occupee";
            case "MAINTENANCE" -> "Maintenance";
            default            -> status;
        };
    }

    public static String getStatusBadgeClass(String status) {
        return switch (status == null ? "" : status) {
            case "AVAILABLE"   -> "badge-available";
            case "OCCUPIED"    -> "badge-occupied";
            case "MAINTENANCE" -> "badge-maintenance";
            default            -> "badge-available";
        };
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  String helpers
    // ─────────────────────────────────────────────────────────────────────────

    public static String trimToMaxLength(String value, int maxLength) {
        String safe = formatApiValue(value, "");
        int    max  = Math.max(4, maxLength);
        if (safe.length() <= max) return safe;
        return safe.substring(0, max - 3) + "...";
    }

    public static String formatApiValue(String value, String fallback) {
        String cleaned = value == null ? "" : value.trim();
        return cleaned.isEmpty() ? fallback : cleaned;
    }

    public static String formatApiToken(String value, String fallback) {
        String cleaned = formatApiValue(value, "");
        if (cleaned.isEmpty()) return fallback;
        String normalized = cleaned.replace('_', ' ').replace('-', ' ').replace('/', ' ')
                .trim().replaceAll("\\s+", " ");
        if (normalized.isEmpty()) return fallback;
        String[]      words   = normalized.toLowerCase(Locale.ROOT).split(" ");
        StringBuilder builder = new StringBuilder();
        for (String word : words) {
            if (word.isBlank()) continue;
            if (!builder.isEmpty()) builder.append(' ');
            builder.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return builder.isEmpty() ? fallback : builder.toString();
    }

    public static String formatImportanceScore(double importance) {
        return importance <= 0 ? "N/A" : String.format(Locale.ROOT, "%.2f", importance);
    }

    public static String formatCoordinates(String lat, String lon) {
        String sLat = lat == null ? "" : lat.trim();
        String sLon = lon == null ? "" : lon.trim();
        return (sLat.isBlank() || sLon.isBlank()) ? "N/A" : sLat + ", " + sLon;
    }

    public static String extractLocationTitle(String displayName) {
        String safe = formatApiValue(displayName, "Lieu inconnu");
        String[] segments = safe.split(",");
        return trimToMaxLength(segments.length == 0 ? safe : segments[0].trim(), 36);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Input parsing
    // ─────────────────────────────────────────────────────────────────────────

    public static int parseNightsInput(String text) {
        if (text == null || text.isBlank()) return 1;
        try {
            int n = Integer.parseInt(text.trim());
            return n < 1 ? 1 : Math.min(n, 30);
        } catch (NumberFormatException ignored) { return 1; }
    }

    public static int parseGuestsInput(String text) {
        if (text == null || text.isBlank()) return 1;
        try {
            int n = Integer.parseInt(text.trim());
            return n < 1 ? 1 : Math.min(n, 8);
        } catch (NumberFormatException ignored) { return 1; }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  OSM / Mercator math
    // ─────────────────────────────────────────────────────────────────────────

    public static final int    OSM_TILE_SIZE             = 256;
    public static final String OSM_TILE_URL_TEMPLATE     = "https://basemaps.cartocdn.com/rastertiles/voyager/%d/%d/%d.png";
    public static final int    OSM_PREVIEW_DEFAULT_WIDTH  = 980;
    public static final int    OSM_PREVIEW_DEFAULT_HEIGHT = 620;
    public static final int    OSM_PREVIEW_MIN_ZOOM       = 3;
    public static final int    OSM_PREVIEW_MAX_ZOOM       = 18;
    public static final int    OSM_PREVIEW_INITIAL_ZOOM   = 11;

    public static double clampLatitude(double v)  { return Math.max(-85.05112878, Math.min(85.05112878, v)); }
    public static double clampLongitude(double v) { return Math.max(-179.999999,  Math.min(179.999999,  v)); }

    public static double longitudeToWorldPixelX(double lon, int zoom) {
        double worldSize = OSM_TILE_SIZE * (double) (1 << zoom);
        return ((clampLongitude(lon) + 180.0) / 360.0) * worldSize;
    }

    public static double latitudeToWorldPixelY(double lat, int zoom) {
        double clamped  = clampLatitude(lat);
        double latRad   = Math.toRadians(clamped);
        double worldSize = OSM_TILE_SIZE * (double) (1 << zoom);
        double mercator = Math.log(Math.tan(Math.PI / 4.0 + latRad / 2.0));
        return (1.0 - mercator / Math.PI) * worldSize / 2.0;
    }

    public static double worldPixelXToLongitude(double pixelX, int zoom) {
        double worldSize = OSM_TILE_SIZE * (double) (1 << zoom);
        return (pixelX / worldSize) * 360.0 - 180.0;
    }

    public static double worldPixelYToLatitude(double pixelY, int zoom) {
        double worldSize     = OSM_TILE_SIZE * (double) (1 << zoom);
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

    public static Double parseCoordinate(String value) {
        try {
            String t = value == null ? "" : value.trim();
            return t.isBlank() ? null : Double.parseDouble(t);
        } catch (NumberFormatException e) { return null; }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Error extraction
    // ─────────────────────────────────────────────────────────────────────────

    public static String extractErrorMessage(Throwable error) {
        if (error == null) return "Erreur inconnue.";
        Throwable current = error;
        while (current.getCause() != null) current = current.getCause();
        String msg = current.getMessage() == null ? "" : current.getMessage().trim();
        return msg.isEmpty() ? "Erreur API." : msg;
    }
}