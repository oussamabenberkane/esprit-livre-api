package com.oussamabenberkane.espritlivre.service.util;

import java.text.Normalizer;

/**
 * Utility class for text normalization in search operations.
 * Provides methods for accent-insensitive text matching and phone number normalization.
 */
public final class TextNormalizationUtils {

    private TextNormalizationUtils() {
        // Utility class, prevent instantiation
    }

    /**
     * Normalize text for search by removing accents, converting to lowercase, and trimming.
     * Used for in-memory filtering (e.g., relay points search).
     *
     * Examples:
     * - "Béjaia" -> "bejaia"
     * - "Tizi-Ouzou" -> "tizi-ouzou"
     * - "ALGER" -> "alger"
     *
     * @param text the text to normalize
     * @return normalized text, or empty string if input is null
     */
    public static String normalizeForSearch(String text) {
        if (text == null) {
            return "";
        }
        // Use NFD normalization to decompose accented characters,
        // then remove the combining diacritical marks
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD)
            .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return normalized.toLowerCase().trim();
    }

    /**
     * Normalize phone number for flexible search matching.
     * Strips country code prefix (+213) and leading zeros to allow partial matching.
     *
     * Examples:
     * - "0549697533" -> "549697533"
     * - "+213549697533" -> "549697533"
     * - "549697533" -> "549697533"
     * - "7533" -> "7533" (partial number kept as-is)
     *
     * This allows searching "0549697533" to match stored "+213549697533".
     *
     * @param phone the phone number to normalize
     * @return normalized phone number (digits only, no country code or leading zero),
     *         or null if input is null or empty after normalization
     */
    public static String normalizePhoneForSearch(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }

        // Remove all non-digit characters
        String digitsOnly = phone.replaceAll("[^0-9]", "");

        if (digitsOnly.isEmpty()) {
            return null;
        }

        // Remove Algeria country code (213) if present at the start
        if (digitsOnly.startsWith("213") && digitsOnly.length() > 9) {
            digitsOnly = digitsOnly.substring(3);
        }

        // Remove leading zero if present (local format: 0XXXXXXXXX -> XXXXXXXXX)
        if (digitsOnly.startsWith("0") && digitsOnly.length() > 1) {
            digitsOnly = digitsOnly.substring(1);
        }

        return digitsOnly.isEmpty() ? null : digitsOnly;
    }
}
