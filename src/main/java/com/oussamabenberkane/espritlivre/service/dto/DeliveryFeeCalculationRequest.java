package com.oussamabenberkane.espritlivre.service.dto;

import com.oussamabenberkane.espritlivre.domain.enumeration.ShippingProvider;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Request DTO for delivery fee calculation.
 */
public class DeliveryFeeCalculationRequest {

    @NotNull(message = "Shipping provider is required")
    private ShippingProvider shippingProvider;

    @NotNull(message = "Wilaya is required")
    private String wilaya;

    private String city;

    private Boolean isStopDesk;

    @NotEmpty(message = "At least one item is required")
    private List<CartItem> items;

    public ShippingProvider getShippingProvider() {
        return shippingProvider;
    }

    public void setShippingProvider(ShippingProvider shippingProvider) {
        this.shippingProvider = shippingProvider;
    }

    public String getWilaya() {
        return wilaya;
    }

    public void setWilaya(String wilaya) {
        this.wilaya = wilaya;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public Boolean getIsStopDesk() {
        return isStopDesk;
    }

    public void setIsStopDesk(Boolean isStopDesk) {
        this.isStopDesk = isStopDesk;
    }

    public List<CartItem> getItems() {
        return items;
    }

    public void setItems(List<CartItem> items) {
        this.items = items;
    }

    /**
     * A cart item for fee calculation.
     * Either bookId or bookPackId should be set, not both.
     */
    public static class CartItem {

        private Long bookId;
        private Long bookPackId;
        private Integer quantity;

        public Long getBookId() {
            return bookId;
        }

        public void setBookId(Long bookId) {
            this.bookId = bookId;
        }

        public Long getBookPackId() {
            return bookPackId;
        }

        public void setBookPackId(Long bookPackId) {
            this.bookPackId = bookPackId;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }
    }
}
