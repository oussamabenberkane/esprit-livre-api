package com.oussamabenberkane.espritlivre.service.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.oussamabenberkane.espritlivre.domain.enumeration.DeliveryFeeMethod;
import com.oussamabenberkane.espritlivre.domain.enumeration.ShippingProvider;
import java.math.BigDecimal;

/**
 * Response DTO for delivery fee calculation.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeliveryFeeCalculationResponse {

    private boolean success;
    private BigDecimal fee;
    private DeliveryFeeMethod method;
    private ShippingProvider provider;
    private String errorMessage;

    private DeliveryFeeCalculationResponse() {}

    public static DeliveryFeeCalculationResponse success(BigDecimal fee, DeliveryFeeMethod method,
                                                          ShippingProvider provider) {
        DeliveryFeeCalculationResponse response = new DeliveryFeeCalculationResponse();
        response.success = true;
        response.fee = fee;
        response.method = method;
        response.provider = provider;
        return response;
    }

    public static DeliveryFeeCalculationResponse failure(String errorMessage) {
        DeliveryFeeCalculationResponse response = new DeliveryFeeCalculationResponse();
        response.success = false;
        response.errorMessage = errorMessage;
        return response;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public BigDecimal getFee() {
        return fee;
    }

    public void setFee(BigDecimal fee) {
        this.fee = fee;
    }

    public DeliveryFeeMethod getMethod() {
        return method;
    }

    public void setMethod(DeliveryFeeMethod method) {
        this.method = method;
    }

    public ShippingProvider getProvider() {
        return provider;
    }

    public void setProvider(ShippingProvider provider) {
        this.provider = provider;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
