package com.oussamabenberkane.espritlivre.service.dto.shipping;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for creating a parcel in ZR Express API.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ZrExpressParcelRequest {

    private ZrCustomer customer;
    private ZrDeliveryAddress deliveryAddress;
    private String hubId;
    private String deliveryType; // "home" or "pickup-point"
    private Double amount;
    private ZrWeight weight;
    private String externalId;
    private String description;
    private java.util.List<ZrOrderedProduct> orderedProducts;

    public ZrExpressParcelRequest() {}

    public ZrCustomer getCustomer() {
        return customer;
    }

    public void setCustomer(ZrCustomer customer) {
        this.customer = customer;
    }

    public ZrDeliveryAddress getDeliveryAddress() {
        return deliveryAddress;
    }

    public void setDeliveryAddress(ZrDeliveryAddress deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }

    public String getHubId() {
        return hubId;
    }

    public void setHubId(String hubId) {
        this.hubId = hubId;
    }

    public String getDeliveryType() {
        return deliveryType;
    }

    public void setDeliveryType(String deliveryType) {
        this.deliveryType = deliveryType;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public ZrWeight getWeight() {
        return weight;
    }

    public void setWeight(ZrWeight weight) {
        this.weight = weight;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public java.util.List<ZrOrderedProduct> getOrderedProducts() {
        return orderedProducts;
    }

    public void setOrderedProducts(java.util.List<ZrOrderedProduct> orderedProducts) {
        this.orderedProducts = orderedProducts;
    }

    /**
     * Nested class for ordered products.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ZrOrderedProduct {
        private String productName;
        private Integer quantity;
        private Double unitPrice;
        private String stockType; // "local", "warehouse", or "none"

        public ZrOrderedProduct() {}

        public ZrOrderedProduct(String productName, Integer quantity, Double unitPrice, String stockType) {
            this.productName = productName;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
            this.stockType = stockType;
        }

        public String getProductName() {
            return productName;
        }

        public void setProductName(String productName) {
            this.productName = productName;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }

        public Double getUnitPrice() {
            return unitPrice;
        }

        public void setUnitPrice(Double unitPrice) {
            this.unitPrice = unitPrice;
        }

        public String getStockType() {
            return stockType;
        }

        public void setStockType(String stockType) {
            this.stockType = stockType;
        }
    }

    /**
     * Nested class for customer information.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ZrCustomer {
        private String customerId;  // UUID of existing customer, null for inline creation
        private String name;
        private ZrPhone phone;

        public ZrCustomer() {}

        public ZrCustomer(String name, ZrPhone phone) {
            this.name = name;
            this.phone = phone;
        }

        public String getCustomerId() {
            return customerId;
        }

        public void setCustomerId(String customerId) {
            this.customerId = customerId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public ZrPhone getPhone() {
            return phone;
        }

        public void setPhone(ZrPhone phone) {
            this.phone = phone;
        }
    }

    /**
     * Nested class for phone numbers.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ZrPhone {
        private String number1;
        private String number2;
        private String number3;

        public ZrPhone() {}

        public ZrPhone(String number1) {
            this.number1 = number1;
        }

        public String getNumber1() {
            return number1;
        }

        public void setNumber1(String number1) {
            this.number1 = number1;
        }

        public String getNumber2() {
            return number2;
        }

        public void setNumber2(String number2) {
            this.number2 = number2;
        }

        public String getNumber3() {
            return number3;
        }

        public void setNumber3(String number3) {
            this.number3 = number3;
        }
    }

    /**
     * Nested class for delivery address.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ZrDeliveryAddress {
        private String cityTerritoryId;
        private String districtTerritoryId;
        private String street;

        public ZrDeliveryAddress() {}

        public ZrDeliveryAddress(String cityTerritoryId, String districtTerritoryId, String street) {
            this.cityTerritoryId = cityTerritoryId;
            this.districtTerritoryId = districtTerritoryId;
            this.street = street;
        }

        public String getCityTerritoryId() {
            return cityTerritoryId;
        }

        public void setCityTerritoryId(String cityTerritoryId) {
            this.cityTerritoryId = cityTerritoryId;
        }

        public String getDistrictTerritoryId() {
            return districtTerritoryId;
        }

        public void setDistrictTerritoryId(String districtTerritoryId) {
            this.districtTerritoryId = districtTerritoryId;
        }

        public String getStreet() {
            return street;
        }

        public void setStreet(String street) {
            this.street = street;
        }
    }

    /**
     * Nested class for weight.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ZrWeight {
        private Double weight;

        public ZrWeight() {}

        public ZrWeight(Double weight) {
            this.weight = weight;
        }

        public Double getWeight() {
            return weight;
        }

        public void setWeight(Double weight) {
            this.weight = weight;
        }
    }

    /**
     * Builder for creating ZrExpressParcelRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final ZrExpressParcelRequest request = new ZrExpressParcelRequest();

        public Builder customer(String name, String phone) {
            request.customer = new ZrCustomer(name, new ZrPhone(phone));
            return this;
        }

        public Builder deliveryAddress(String cityTerritoryId, String districtTerritoryId, String street) {
            request.deliveryAddress = new ZrDeliveryAddress(cityTerritoryId, districtTerritoryId, street);
            return this;
        }

        public Builder hubId(String hubId) {
            request.hubId = hubId;
            return this;
        }

        public Builder deliveryType(String deliveryType) {
            request.deliveryType = deliveryType;
            return this;
        }

        public Builder amount(Double amount) {
            request.amount = amount;
            return this;
        }

        public Builder weight(Double weight) {
            request.weight = new ZrWeight(weight);
            return this;
        }

        public Builder externalId(String externalId) {
            request.externalId = externalId;
            return this;
        }

        public Builder description(String description) {
            request.description = description;
            return this;
        }

        public Builder orderedProducts(java.util.List<ZrOrderedProduct> products) {
            request.orderedProducts = products;
            return this;
        }

        public ZrExpressParcelRequest build() {
            return request;
        }
    }
}
