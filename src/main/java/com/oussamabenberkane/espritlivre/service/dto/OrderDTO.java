package com.oussamabenberkane.espritlivre.service.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.oussamabenberkane.espritlivre.domain.enumeration.DeliveryFeeMethod;
import com.oussamabenberkane.espritlivre.domain.enumeration.OrderOrigin;
import com.oussamabenberkane.espritlivre.domain.enumeration.OrderStatus;
import com.oussamabenberkane.espritlivre.domain.enumeration.ShippingMethod;
import com.oussamabenberkane.espritlivre.domain.enumeration.ShippingProvider;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * A DTO for the {@link com.oussamabenberkane.espritlivre.domain.Order} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class OrderDTO implements Serializable {

    private Long id;

    private String uniqueId;

    private OrderStatus status;

    private BigDecimal totalAmount;

    private BigDecimal shippingCost;

    private ShippingProvider shippingProvider;

    private ShippingMethod shippingMethod;

    private String fullName;

    @NotNull
    private String phone;

    private String email;

    private String streetAddress;

    private String wilaya;

    private String city;

    private String postalCode;

    private ZonedDateTime createdAt;

    private String createdBy;

    private ZonedDateTime updatedAt;

    private String providerOrderId;

    private String trackingNumber;

    private String shippingLabelUrl;

    private Boolean isStopDesk;

    private String stopDeskId;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String shippingProviderError;

    private DeliveryFeeMethod deliveryFeeMethod;

    private ShippingProvider deliveryFeeProvider;

    private OrderOrigin orderOrigin;

    // Meta Pixel cookies forwarded by the browser at checkout — used for the server-side
    // CAPI Purchase event (attribution + dedup). Input-only, never persisted.
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String fbc;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String fbp;

    // Pseudonymous visitor id (SHA-256 hex), same value the browser pixel reports
    // as external_id — lets Meta tie the CAPI Purchase to the visitor's history.
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String externalId;

    // Page URL at checkout, forwarded to the server-side CAPI Purchase event so the
    // event_source_url matches the browser pixel (improves Meta event match quality).
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String eventSourceUrl;

    private UserDTO user;

    private Set<OrderItemDTO> orderItems = new HashSet<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getShippingCost() {
        return shippingCost;
    }

    public void setShippingCost(BigDecimal shippingCost) {
        this.shippingCost = shippingCost;
    }

    public ShippingProvider getShippingProvider() {
        return shippingProvider;
    }

    public void setShippingProvider(ShippingProvider shippingProvider) {
        this.shippingProvider = shippingProvider;
    }

    public ShippingMethod getShippingMethod() {
        return shippingMethod;
    }

    public void setShippingMethod(ShippingMethod shippingMethod) {
        this.shippingMethod = shippingMethod;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getStreetAddress() {
        return streetAddress;
    }

    public void setStreetAddress(String streetAddress) {
        this.streetAddress = streetAddress;
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

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(ZonedDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public ZonedDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(ZonedDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getProviderOrderId() {
        return providerOrderId;
    }

    public void setProviderOrderId(String providerOrderId) {
        this.providerOrderId = providerOrderId;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

    public String getShippingLabelUrl() {
        return shippingLabelUrl;
    }

    public void setShippingLabelUrl(String shippingLabelUrl) {
        this.shippingLabelUrl = shippingLabelUrl;
    }

    public Boolean getIsStopDesk() {
        return isStopDesk;
    }

    public void setIsStopDesk(Boolean isStopDesk) {
        this.isStopDesk = isStopDesk;
    }

    public String getStopDeskId() {
        return stopDeskId;
    }

    public void setStopDeskId(String stopDeskId) {
        this.stopDeskId = stopDeskId;
    }

    public String getShippingProviderError() {
        return shippingProviderError;
    }

    public void setShippingProviderError(String shippingProviderError) {
        this.shippingProviderError = shippingProviderError;
    }

    public DeliveryFeeMethod getDeliveryFeeMethod() {
        return deliveryFeeMethod;
    }

    public void setDeliveryFeeMethod(DeliveryFeeMethod deliveryFeeMethod) {
        this.deliveryFeeMethod = deliveryFeeMethod;
    }

    public ShippingProvider getDeliveryFeeProvider() {
        return deliveryFeeProvider;
    }

    public void setDeliveryFeeProvider(ShippingProvider deliveryFeeProvider) {
        this.deliveryFeeProvider = deliveryFeeProvider;
    }

    public OrderOrigin getOrderOrigin() {
        return orderOrigin;
    }

    public void setOrderOrigin(OrderOrigin orderOrigin) {
        this.orderOrigin = orderOrigin;
    }

    public String getFbc() {
        return fbc;
    }

    public void setFbc(String fbc) {
        this.fbc = fbc;
    }

    public String getFbp() {
        return fbp;
    }

    public void setFbp(String fbp) {
        this.fbp = fbp;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getEventSourceUrl() {
        return eventSourceUrl;
    }

    public void setEventSourceUrl(String eventSourceUrl) {
        this.eventSourceUrl = eventSourceUrl;
    }

    public UserDTO getUser() {
        return user;
    }

    public void setUser(UserDTO user) {
        this.user = user;
    }

    public Set<OrderItemDTO> getOrderItems() {
        return orderItems;
    }

    public void setOrderItems(Set<OrderItemDTO> orderItems) {
        this.orderItems = orderItems;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof OrderDTO)) {
            return false;
        }

        OrderDTO orderDTO = (OrderDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, orderDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "OrderDTO{" +
            "id=" + getId() +
            ", uniqueId='" + getUniqueId() + "'" +
            ", status='" + getStatus() + "'" +
            ", totalAmount=" + getTotalAmount() +
            ", shippingCost=" + getShippingCost() +
            ", shippingProvider='" + getShippingProvider() + "'" +
            ", shippingMethod='" + getShippingMethod() + "'" +
            ", fullName='" + getFullName() + "'" +
            ", phone='" + getPhone() + "'" +
            ", email='" + getEmail() + "'" +
            ", streetAddress='" + getStreetAddress() + "'" +
            ", wilaya='" + getWilaya() + "'" +
            ", city='" + getCity() + "'" +
            ", postalCode='" + getPostalCode() + "'" +
            ", providerOrderId='" + getProviderOrderId() + "'" +
            ", trackingNumber='" + getTrackingNumber() + "'" +
            ", isStopDesk=" + getIsStopDesk() +
            ", createdAt='" + getCreatedAt() + "'" +
            ", createdBy='" + getCreatedBy() + "'" +
            ", updatedAt='" + getUpdatedAt() + "'" +
            ", user=" + getUser() +
            ", orderItems=" + getOrderItems() +
            "}";
    }
}
