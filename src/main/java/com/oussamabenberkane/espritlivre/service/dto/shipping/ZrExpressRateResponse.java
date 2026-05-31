package com.oussamabenberkane.espritlivre.service.dto.shipping;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.util.List;

/**
 * DTO for ZR Express delivery pricing rate response.
 * GET /api/v1/delivery-pricing/rates/{toTerritoryId}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ZrExpressRateResponse {

    private String toTerritoryId;
    private String toTerritoryName;
    private String toTerritoryLevel;
    private List<DeliveryPrice> deliveryPrices;

    public String getToTerritoryId() {
        return toTerritoryId;
    }

    public void setToTerritoryId(String toTerritoryId) {
        this.toTerritoryId = toTerritoryId;
    }

    public String getToTerritoryName() {
        return toTerritoryName;
    }

    public void setToTerritoryName(String toTerritoryName) {
        this.toTerritoryName = toTerritoryName;
    }

    public String getToTerritoryLevel() {
        return toTerritoryLevel;
    }

    public void setToTerritoryLevel(String toTerritoryLevel) {
        this.toTerritoryLevel = toTerritoryLevel;
    }

    public List<DeliveryPrice> getDeliveryPrices() {
        return deliveryPrices;
    }

    public void setDeliveryPrices(List<DeliveryPrice> deliveryPrices) {
        this.deliveryPrices = deliveryPrices;
    }

    /**
     * Get delivery fee for a specific delivery type.
     *
     * @param isStopDesk Whether this is a pickup point (stop desk) delivery
     * @return The delivery fee, or null if not found
     */
    public BigDecimal getDeliveryFee(boolean isStopDesk) {
        if (deliveryPrices == null || deliveryPrices.isEmpty()) {
            return null;
        }

        // Match the requested delivery type explicitly.
        // ZR Express returns deliveryType values "home", "pickup-point" and "return"
        // (NOT "HomeDelivery"/"PickupPoint"). We must never fall back to an arbitrary
        // entry: returning another type's price (e.g. the "pickup-point" or "return"
        // price for a home delivery) silently quotes the wrong fee to the customer.
        for (DeliveryPrice price : deliveryPrices) {
            if (matchesDeliveryType(price.getDeliveryType(), isStopDesk)) {
                return price.getEffectivePrice();
            }
        }

        // Requested delivery type not available for this territory — do not guess.
        return null;
    }

    /**
     * Match a ZR Express deliveryType string against the requested kind.
     * Case/separator-insensitive so "pickup-point", "PickupPoint" and "pickup_point"
     * all match. The "return" tariff is never a customer-facing delivery fee.
     *
     * @param deliveryType the raw deliveryType value from the API
     * @param isStopDesk   true to match pickup-point, false to match home delivery
     */
    private static boolean matchesDeliveryType(String deliveryType, boolean isStopDesk) {
        if (deliveryType == null) {
            return false;
        }
        String normalized = deliveryType.toLowerCase().replaceAll("[^a-z]", "");
        if (normalized.contains("return")) {
            return false;
        }
        if (isStopDesk) {
            return normalized.contains("pickup") || normalized.contains("stopdesk") || normalized.contains("desk");
        }
        return normalized.contains("home") || normalized.contains("domicile");
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DeliveryPrice {

        private String deliveryType;
        private Double price;
        private Double discountedPrice;

        public String getDeliveryType() {
            return deliveryType;
        }

        public void setDeliveryType(String deliveryType) {
            this.deliveryType = deliveryType;
        }

        public Double getPrice() {
            return price;
        }

        public void setPrice(Double price) {
            this.price = price;
        }

        public Double getDiscountedPrice() {
            return discountedPrice;
        }

        public void setDiscountedPrice(Double discountedPrice) {
            this.discountedPrice = discountedPrice;
        }

        /**
         * Effective price to charge: the discounted price when ZR Express has applied
         * one, otherwise the base price. Returns null if neither is set.
         */
        public BigDecimal getEffectivePrice() {
            Double effective = discountedPrice != null ? discountedPrice : price;
            return effective != null ? BigDecimal.valueOf(effective) : null;
        }
    }
}
