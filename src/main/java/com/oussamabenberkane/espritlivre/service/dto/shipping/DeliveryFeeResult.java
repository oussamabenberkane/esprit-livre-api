package com.oussamabenberkane.espritlivre.service.dto.shipping;

import com.oussamabenberkane.espritlivre.domain.enumeration.DeliveryFeeMethod;
import com.oussamabenberkane.espritlivre.domain.enumeration.ShippingProvider;
import java.math.BigDecimal;

/**
 * Result of delivery fee calculation.
 */
public class DeliveryFeeResult {

    private final boolean success;
    private final BigDecimal fee;
    private final DeliveryFeeMethod method;
    private final ShippingProvider provider;
    private final String errorMessage;

    private DeliveryFeeResult(boolean success, BigDecimal fee, DeliveryFeeMethod method,
                              ShippingProvider provider, String errorMessage) {
        this.success = success;
        this.fee = fee;
        this.method = method;
        this.provider = provider;
        this.errorMessage = errorMessage;
    }

    /**
     * Create a successful result with automatic calculation.
     */
    public static DeliveryFeeResult automatic(BigDecimal fee, ShippingProvider provider) {
        return new DeliveryFeeResult(true, fee, DeliveryFeeMethod.AUTOMATIC, provider, null);
    }

    /**
     * Create a successful result with fixed fee.
     */
    public static DeliveryFeeResult fixed(BigDecimal fee) {
        return new DeliveryFeeResult(true, fee, DeliveryFeeMethod.FIXED, null, null);
    }

    /**
     * Create a failure result.
     */
    public static DeliveryFeeResult failure(String errorMessage) {
        return new DeliveryFeeResult(false, null, null, null, errorMessage);
    }

    public boolean isSuccess() {
        return success;
    }

    public BigDecimal getFee() {
        return fee;
    }

    public DeliveryFeeMethod getMethod() {
        return method;
    }

    public ShippingProvider getProvider() {
        return provider;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public String toString() {
        if (success) {
            return "DeliveryFeeResult{success=true, fee=" + fee + ", method=" + method + ", provider=" + provider + "}";
        } else {
            return "DeliveryFeeResult{success=false, error='" + errorMessage + "'}";
        }
    }
}
