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

        String targetType = isStopDesk ? "PickupPoint" : "HomeDelivery";

        for (DeliveryPrice price : deliveryPrices) {
            if (price.getDeliveryType() != null &&
                price.getDeliveryType().equalsIgnoreCase(targetType)) {
                return price.getPrice() != null ? BigDecimal.valueOf(price.getPrice()) : null;
            }
        }

        // If exact type not found, try to find any price
        // Some territories might only have one delivery type
        if (!deliveryPrices.isEmpty() && deliveryPrices.get(0).getPrice() != null) {
            return BigDecimal.valueOf(deliveryPrices.get(0).getPrice());
        }

        return null;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DeliveryPrice {

        private String deliveryType;
        private Double price;

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
    }
}
