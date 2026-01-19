package com.oussamabenberkane.espritlivre.service.dto.shipping;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for Yalidine parcel GET response.
 * Used to retrieve parcel status for status polling.
 *
 * GET /v1/parcels/{tracking} returns this structure.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class YalidineParcelStatusResponse {

    private String tracking;

    @JsonProperty("order_id")
    private String orderId;

    @JsonProperty("last_status")
    private String lastStatus;

    @JsonProperty("date_last_status")
    private String dateLastStatus;

    @JsonProperty("payment_status")
    private String paymentStatus;

    private String label;

    public YalidineParcelStatusResponse() {}

    public String getTracking() {
        return tracking;
    }

    public void setTracking(String tracking) {
        this.tracking = tracking;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getLastStatus() {
        return lastStatus;
    }

    public void setLastStatus(String lastStatus) {
        this.lastStatus = lastStatus;
    }

    public String getDateLastStatus() {
        return dateLastStatus;
    }

    public void setDateLastStatus(String dateLastStatus) {
        this.dateLastStatus = dateLastStatus;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
