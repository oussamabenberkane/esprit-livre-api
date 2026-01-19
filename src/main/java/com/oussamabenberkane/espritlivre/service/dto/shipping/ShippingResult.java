package com.oussamabenberkane.espritlivre.service.dto.shipping;

/**
 * Result wrapper for shipping provider operations.
 */
public record ShippingResult(
    boolean success,
    String trackingNumber,
    String labelUrl,
    String errorMessage
) {
    /**
     * Create a successful result with tracking information.
     *
     * @param trackingNumber the tracking number from the provider
     * @param labelUrl the URL to the shipping label PDF
     * @return a successful ShippingResult
     */
    public static ShippingResult success(String trackingNumber, String labelUrl) {
        return new ShippingResult(true, trackingNumber, labelUrl, null);
    }

    /**
     * Create a failure result with an error message.
     *
     * @param errorMessage the error message describing what went wrong
     * @return a failed ShippingResult
     */
    public static ShippingResult failure(String errorMessage) {
        return new ShippingResult(false, null, null, errorMessage);
    }
}
