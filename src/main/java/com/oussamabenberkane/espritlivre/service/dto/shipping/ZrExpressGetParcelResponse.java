package com.oussamabenberkane.espritlivre.service.dto.shipping;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * DTO for ZR Express get parcel response.
 * Used to retrieve tracking number after parcel creation.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ZrExpressGetParcelResponse {

    private String id;
    private String trackingNumber;
    private ZrState state;
    private String externalId;

    public ZrExpressGetParcelResponse() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

    public ZrState getState() {
        return state;
    }

    public void setState(ZrState state) {
        this.state = state;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    /**
     * Nested class for parcel state.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ZrState {
        private String id;
        private String name;

        public ZrState() {}

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
