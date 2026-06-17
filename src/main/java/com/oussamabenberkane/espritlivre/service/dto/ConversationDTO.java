package com.oussamabenberkane.espritlivre.service.dto;

import com.oussamabenberkane.espritlivre.domain.enumeration.Channel;
import com.oussamabenberkane.espritlivre.domain.enumeration.ConversationStatus;
import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * A DTO for the {@link com.oussamabenberkane.espritlivre.domain.Conversation} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ConversationDTO implements Serializable {

    private Long id;

    private Channel channel;

    private String senderId;

    private String customerName;

    private String customerPhone;

    private ConversationStatus status;

    private ZonedDateTime lastMessageAt;

    private String lastMessageSnippet;

    private ZonedDateTime lastInboundAt;

    private Integer unreadCount;

    private String escalationReason;

    private String assignedTo;

    private ZonedDateTime createdAt;

    private ZonedDateTime updatedAt;

    // Computed (not persisted): WhatsApp 24h customer-care window, derived from lastInboundAt.
    private Boolean withinCustomerCareWindow;

    private ZonedDateTime customerCareWindowExpiresAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public ConversationStatus getStatus() {
        return status;
    }

    public void setStatus(ConversationStatus status) {
        this.status = status;
    }

    public ZonedDateTime getLastMessageAt() {
        return lastMessageAt;
    }

    public void setLastMessageAt(ZonedDateTime lastMessageAt) {
        this.lastMessageAt = lastMessageAt;
    }

    public String getLastMessageSnippet() {
        return lastMessageSnippet;
    }

    public void setLastMessageSnippet(String lastMessageSnippet) {
        this.lastMessageSnippet = lastMessageSnippet;
    }

    public ZonedDateTime getLastInboundAt() {
        return lastInboundAt;
    }

    public void setLastInboundAt(ZonedDateTime lastInboundAt) {
        this.lastInboundAt = lastInboundAt;
    }

    public Integer getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(Integer unreadCount) {
        this.unreadCount = unreadCount;
    }

    public String getEscalationReason() {
        return escalationReason;
    }

    public void setEscalationReason(String escalationReason) {
        this.escalationReason = escalationReason;
    }

    public String getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(String assignedTo) {
        this.assignedTo = assignedTo;
    }

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(ZonedDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public ZonedDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(ZonedDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Boolean getWithinCustomerCareWindow() {
        return withinCustomerCareWindow;
    }

    public void setWithinCustomerCareWindow(Boolean withinCustomerCareWindow) {
        this.withinCustomerCareWindow = withinCustomerCareWindow;
    }

    public ZonedDateTime getCustomerCareWindowExpiresAt() {
        return customerCareWindowExpiresAt;
    }

    public void setCustomerCareWindowExpiresAt(ZonedDateTime customerCareWindowExpiresAt) {
        this.customerCareWindowExpiresAt = customerCareWindowExpiresAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ConversationDTO)) {
            return false;
        }
        ConversationDTO that = (ConversationDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ConversationDTO{" +
            "id=" + getId() +
            ", channel='" + getChannel() + "'" +
            ", senderId='" + getSenderId() + "'" +
            ", customerName='" + getCustomerName() + "'" +
            ", customerPhone='" + getCustomerPhone() + "'" +
            ", status='" + getStatus() + "'" +
            ", lastMessageAt='" + getLastMessageAt() + "'" +
            ", unreadCount=" + getUnreadCount() +
            ", withinCustomerCareWindow=" + getWithinCustomerCareWindow() +
            "}";
    }
}
