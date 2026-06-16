package com.oussamabenberkane.espritlivre.service.util;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Utility class for text normalization in search operations.
 * Provides methods for accent-insensitive text matching and phone number normalization.
 */
public final class TextNormalizationUtils {

    /**
     * Book binding/format descriptors (accent-free, lowercase) that catalog feeds and external
     * product names sometimes append to a title, e.g. "La femme de ménage-broché".
     * These formats are not stored on the Book entity, so when present in a search term they
     * make the contiguous LIKE match fail and the search returns nothing. They are stripped
     * from the end of the search term before matching.
     */
    private static final Set<String> BOOK_FORMAT_KEYWORDS = Set.of(
        "broche", // broché - paperback
        "relie", // relié - hardcover
        "poche", // livre de poche
        "cartonne", // cartonné
        "souple", // couverture souple
        "rigide", // couverture rigide
        "paperback",
        "hardcover"
    );

    private TextNormalizationUtils() {
        // Utility class, prevent instantiation
    }

    /**
     * Normalize text for search by removing accents, normalizing hyphens/dashes to spaces,
     * collapsing whitespace, and converting to lowercase.
     * Used for in-memory filtering (e.g., relay points search, commune resolution).
     *
     * Examples:
     * - "Béjaia" -> "bejaia"
     * - "Tizi-Ouzou" -> "tizi ouzou"
     * - "Dely-Brahim" -> "dely brahim"
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
        // Normalize hyphens/dashes to spaces so "Dely-Brahim" matches "Dely Brahim"
        return normalized.toLowerCase()
            .replaceAll("[\\-–—]", " ")
            .replaceAll("\\s+", " ")
            .trim();
    }

    /**
     * Remove trailing book-format descriptors (broché, relié, poche, ...) from an already
     * accent-normalized, space-separated search term so a query like "la femme de menage broche"
     * still matches the stored title "La femme de ménage".
     *
     * Only trailing format tokens are stripped, and at least one token is always kept, so that a
     * deliberate search for "broché" alone still works. Because the remaining prefix is still a
     * substring of the full title, a contiguous LIKE match can never be lost by this step.
     *
     * Examples:
     * - "la femme de menage broche" -> "la femme de menage"
     * - "la femme de menage" -> "la femme de menage" (unchanged)
     * - "broche" -> "broche" (kept, nothing else to fall back to)
     *
     * @param normalizedText accent-normalized, lowercase, space-separated text
     * @return the text with trailing format descriptors removed
     */
    public static String stripBookFormatKeywords(String normalizedText) {
        if (normalizedText == null || normalizedText.isBlank()) {
            return normalizedText == null ? "" : normalizedText;
        }
        List<String> tokens = new ArrayList<>(Arrays.asList(normalizedText.trim().split("\\s+")));
        // Strip trailing format descriptors while keeping at least one meaningful token.
        while (tokens.size() > 1 && BOOK_FORMAT_KEYWORDS.contains(tokens.get(tokens.size() - 1))) {
            tokens.remove(tokens.size() - 1);
        }
        return String.join(" ", tokens);
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
