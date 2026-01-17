package com.oussamabenberkane.espritlivre.service.dto.shipping;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

/**
 * Request DTO for creating a parcel in Yalidine API.
 */
public class YalidineParcelRequest {

    @JsonProperty("order_id")
    private String orderId;

    private String firstname;

    private String familyname;

    @JsonProperty("contact_phone")
    private String contactPhone;

    private String address;

    @JsonProperty("from_wilaya_name")
    private String fromWilayaName;

    @JsonProperty("to_wilaya_name")
    private String toWilayaName;

    @JsonProperty("to_commune_name")
    private String toCommuneName;

    @JsonProperty("product_list")
    private String productList;

    private BigDecimal price;

    private Boolean freeshipping;

    @JsonProperty("is_stopdesk")
    private Boolean isStopdesk;

    @JsonProperty("stopdesk_id")
    private String stopdeskId;

    @JsonProperty("has_exchange")
    private Boolean hasExchange;

    @JsonProperty("do_insurance")
    private Boolean doInsurance;

    @JsonProperty("declared_value")
    private Integer declaredValue;

    private Integer length;

    private Integer width;

    private Integer height;

    private Integer weight;

    // Getters and setters

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getFamilyname() {
        return familyname;
    }

    public void setFamilyname(String familyname) {
        this.familyname = familyname;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getFromWilayaName() {
        return fromWilayaName;
    }

    public void setFromWilayaName(String fromWilayaName) {
        this.fromWilayaName = fromWilayaName;
    }

    public String getToWilayaName() {
        return toWilayaName;
    }

    public void setToWilayaName(String toWilayaName) {
        this.toWilayaName = toWilayaName;
    }

    public String getToCommuneName() {
        return toCommuneName;
    }

    public void setToCommuneName(String toCommuneName) {
        this.toCommuneName = toCommuneName;
    }

    public String getProductList() {
        return productList;
    }

    public void setProductList(String productList) {
        this.productList = productList;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Boolean getFreeshipping() {
        return freeshipping;
    }

    public void setFreeshipping(Boolean freeshipping) {
        this.freeshipping = freeshipping;
    }

    public Boolean getIsStopdesk() {
        return isStopdesk;
    }

    public void setIsStopdesk(Boolean isStopdesk) {
        this.isStopdesk = isStopdesk;
    }

    public String getStopdeskId() {
        return stopdeskId;
    }

    public void setStopdeskId(String stopdeskId) {
        this.stopdeskId = stopdeskId;
    }

    public Boolean getHasExchange() {
        return hasExchange;
    }

    public void setHasExchange(Boolean hasExchange) {
        this.hasExchange = hasExchange;
    }

    public Boolean getDoInsurance() {
        return doInsurance;
    }

    public void setDoInsurance(Boolean doInsurance) {
        this.doInsurance = doInsurance;
    }

    public Integer getDeclaredValue() {
        return declaredValue;
    }

    public void setDeclaredValue(Integer declaredValue) {
        this.declaredValue = declaredValue;
    }

    public Integer getLength() {
        return length;
    }

    public void setLength(Integer length) {
        this.length = length;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public Integer getWeight() {
        return weight;
    }

    public void setWeight(Integer weight) {
        this.weight = weight;
    }

    // Builder pattern for convenience
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final YalidineParcelRequest request = new YalidineParcelRequest();

        public Builder orderId(String orderId) {
            request.setOrderId(orderId);
            return this;
        }

        public Builder firstname(String firstname) {
            request.setFirstname(firstname);
            return this;
        }

        public Builder familyname(String familyname) {
            request.setFamilyname(familyname);
            return this;
        }

        public Builder contactPhone(String contactPhone) {
            request.setContactPhone(contactPhone);
            return this;
        }

        public Builder address(String address) {
            request.setAddress(address);
            return this;
        }

        public Builder fromWilayaName(String fromWilayaName) {
            request.setFromWilayaName(fromWilayaName);
            return this;
        }

        public Builder toWilayaName(String toWilayaName) {
            request.setToWilayaName(toWilayaName);
            return this;
        }

        public Builder toCommuneName(String toCommuneName) {
            request.setToCommuneName(toCommuneName);
            return this;
        }

        public Builder productList(String productList) {
            request.setProductList(productList);
            return this;
        }

        public Builder price(BigDecimal price) {
            request.setPrice(price);
            return this;
        }

        public Builder freeshipping(Boolean freeshipping) {
            request.setFreeshipping(freeshipping);
            return this;
        }

        public Builder isStopdesk(Boolean isStopdesk) {
            request.setIsStopdesk(isStopdesk);
            return this;
        }

        public Builder stopdeskId(String stopdeskId) {
            request.setStopdeskId(stopdeskId);
            return this;
        }

        public Builder hasExchange(Boolean hasExchange) {
            request.setHasExchange(hasExchange);
            return this;
        }

        public Builder doInsurance(Boolean doInsurance) {
            request.setDoInsurance(doInsurance);
            return this;
        }

        public Builder declaredValue(Integer declaredValue) {
            request.setDeclaredValue(declaredValue);
            return this;
        }

        public Builder length(Integer length) {
            request.setLength(length);
            return this;
        }

        public Builder width(Integer width) {
            request.setWidth(width);
            return this;
        }

        public Builder height(Integer height) {
            request.setHeight(height);
            return this;
        }

        public Builder weight(Integer weight) {
            request.setWeight(weight);
            return this;
        }

        public YalidineParcelRequest build() {
            return request;
        }
    }
}
