package com.oussamabenberkane.espritlivre.domain.enumeration;

/**
 * The BookStatus enumeration for filtering books by stock availability.
 */
public enum BookStatus {
    AVAILABLE,
    OUT_OF_STOCK;

    /**
     * Parse a string to BookStatus, case-insensitive.
     *
     * @param value the string value to parse.
     * @return the BookStatus enum value, or null if invalid.
     */
    public static BookStatus fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return BookStatus.valueOf(value.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
