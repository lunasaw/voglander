package io.github.lunasaw.voglander.service.image;

import java.util.Locale;

/**
 * Allowlisted values for image-domain structured logs. Identifiers are deliberately
 * reduced to a short opaque suffix; secrets, tokens, URLs and filesystem paths never
 * pass through this helper.
 */
public final class ImageLogSanitizer {
    private ImageLogSanitizer() {
    }

    public static String code(String value) {
        if (value == null) {
            return "UNKNOWN";
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return normalized.matches("[A-Z][A-Z0-9_]{0,47}") ? normalized : "UNKNOWN";
    }

    public static String identifier(String value) {
        if (value == null || value.isBlank()) {
            return "UNKNOWN";
        }
        String lower = value.toLowerCase(Locale.ROOT);
        if (lower.contains("http://") || lower.contains("https://") || lower.contains("token")
            || lower.contains("secret") || lower.contains("jwt") || lower.contains("bearer")
            || value.indexOf('?') >= 0 || value.indexOf('/') >= 0) {
            return "PRESENT";
        }
        String compact = value.replaceAll("[^A-Za-z0-9_-]", "");
        if (compact.isBlank() || compact.length() > 96) {
            return "PRESENT";
        }
        return compact;
    }
}
