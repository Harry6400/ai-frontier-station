package com.harry.aifrontier.util;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class SlugUtil {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private SlugUtil() {
    }

    public static String resolveSlug(String raw, String fallbackText, String prefix) {
        String base = normalize(raw);
        if (!base.isBlank()) {
            return base;
        }
        String fallback = normalize(fallbackText);
        if (!fallback.isBlank()) {
            return fallback;
        }
        return prefix + "-" + LocalDateTime.now().format(FORMATTER);
    }

    private static String normalize(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String normalized = Normalizer.normalize(text.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "")
                .replaceAll("-{2,}", "-");
        return normalized;
    }
}
