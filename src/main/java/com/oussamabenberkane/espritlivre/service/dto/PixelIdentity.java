package com.oussamabenberkane.espritlivre.service.dto;

/**
 * Identity fields the browser relays with every pixel event for Meta CAPI matching.
 * em/ph/fn/ln and externalId arrive already SHA-256 hashed (lowercase hex) from the
 * frontend; fbc/fbp are the raw Meta cookie values. Pixel request DTO records
 * implement this by simply declaring components with matching names.
 */
public interface PixelIdentity {
    String fbc();

    String fbp();

    default String externalId() {
        return null;
    }

    default String em() {
        return null;
    }

    default String ph() {
        return null;
    }

    default String fn() {
        return null;
    }

    default String ln() {
        return null;
    }
}
