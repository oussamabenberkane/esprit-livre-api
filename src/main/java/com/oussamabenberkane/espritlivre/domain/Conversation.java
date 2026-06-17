package com.oussamabenberkane.espritlivre.domain;

import com.oussamabenberkane.espritlivre.domain.enumeration.Channel;
import com.oussamabenberkane.espritlivre.domain.enumeration.ConversationStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.ZonedDateTime;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * A messaging conversation with a customer, identified by (channel, senderId).
 * Channel-agnostic so Messenger/Instagram can reuse it later.
 */
@Entity
@Table(
    name = "whatsapp_conversation",
    uniqueConstraints = @UniqueConstraint(name = "ux_whatsapp_conversation__channel_sender", columnNames = { "channel", "sender_id" })
)
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Conversation implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "conversationSequenceGenerator")
    @SequenceGenerator(name = "conversationSequenceGenerator", sequenceName = "whatsapp_conversation_seq", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    private Channel channel;

    /** Channel-specific user id: WhatsApp phone (wa_id) / Messenger PSID / Instagram IGSID. */
    @NotNull
    @Column(name = "sender_id", nullable = false)
    private String senderId;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "customer_phone")
    private String customerPhone;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ConversationStatus status;

    @Column(name = "last_message_at")
    private ZonedDateTime lastMessageAt;

    @Column(name = "last_message_snippet", length = 500)
    private String lastMessageSnippet;

    /** Timestamp of the customer's last inbound message — basis for the WhatsApp 24h care window. */
    @Column(name = "last_inbound_at")
    private ZonedDateTime lastInboundAt;

    @Column(name = "unread_count")
    private Integer unreadCount;

    @Column(name = "escalation_reason", length = 500)
    private String escalationReason;

    /** Optional assignee (future per-admin claiming). Unused while there is a single admin. */
    @Column(name = "assigned_to")
    private String assignedTo;

    @Column(name = "active", columnDefinition = "boolean default true")
    private Boolean active;

    @Column(name = "created_at")
    private ZonedDateTime createdAt;

    @Column(name = "updated_at")
    private ZonedDateTime updatedAt;

    public Long getId() {
        return this.id;
    }

    public Conversation id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Channel getChannel() {
        return this.channel;
    }

    public Conversation channel(Channel channel) {
        this.setChannel(channel);
        return this;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public String getSenderId() {
        return this.senderId;
    }

    public Conversation senderId(String senderId) {
        this.setSenderId(senderId);
        return this;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getCustomerName() {
        return this.customerName;
    }

    public Conversation customerName(String customerName) {
        this.setCustomerName(customerName);
        return this;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerPhone() {
        return this.customerPhone;
    }

    public Conversation customerPhone(String customerPhone) {
        this.setCustomerPhone(customerPhone);
        return this;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public ConversationStatus getStatus() {
        return this.status;
    }

    public Conversation status(ConversationStatus status) {
        this.setStatus(status);
        return this;
    }

    public void setStatus(ConversationStatus status) {
        this.status = status;
    }

    public ZonedDateTime getLastMessageAt() {
        return this.lastMessageAt;
    }

    public Conversation lastMessageAt(ZonedDateTime lastMessageAt) {
        this.setLastMessageAt(lastMessageAt);
        return this;
    }

    public void setLastMessageAt(ZonedDateTime lastMessageAt) {
        this.lastMessageAt = lastMessageAt;
    }

    public String getLastMessageSnippet() {
        return this.lastMessageSnippet;
    }

    public Conversation lastMessageSnippet(String lastMessageSnippet) {
        this.setLastMessageSnippet(lastMessageSnippet);
        return this;
    }

    public void setLastMessageSnippet(String lastMessageSnippet) {
        this.lastMessageSnippet = lastMessageSnippet;
    }

    public ZonedDateTime getLastInboundAt() {
        return this.lastInboundAt;
    }

    public Conversation lastInboundAt(ZonedDateTime lastInboundAt) {
        this.setLastInboundAt(lastInboundAt);
        return this;
    }

    public void setLastInboundAt(ZonedDateTime lastInboundAt) {
        this.lastInboundAt = lastInboundAt;
    }

    public Integer getUnreadCount() {
        return this.unreadCount;
    }

    public Conversation unreadCount(Integer unreadCount) {
        this.setUnreadCount(unreadCount);
        return this;
    }

    public void setUnreadCount(Integer unreadCount) {
        this.unreadCount = unreadCount;
    }

    public String getEscalationReason() {
        return this.escalationReason;
    }

    public Conversation escalationReason(String escalationReason) {
        this.setEscalationReason(escalationReason);
        return this;
    }

    public void setEscalationReason(String escalationReason) {
        this.escalationReason = escalationReason;
    }

    public String getAssignedTo() {
        return this.assignedTo;
    }

    public Conversation assignedTo(String assignedTo) {
        this.setAssignedTo(assignedTo);
        return this;
    }

    public void setAssignedTo(String assignedTo) {
        this.assignedTo = assignedTo;
    }

    public Boolean getActive() {
        return this.active;
    }

    public Conversation active(Boolean active) {
        this.setActive(active);
        return this;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public ZonedDateTime getCreatedAt() {
        return this.createdAt;
    }

    public Conversation createdAt(ZonedDateTime createdAt) {
        this.setCreatedAt(createdAt);
        return this;
    }

    public void setCreatedAt(ZonedDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public ZonedDateTime getUpdatedAt() {
        return this.updatedAt;
    }

    public Conversation updatedAt(ZonedDateTime updatedAt) {
        this.setUpdatedAt(updatedAt);
        return this;
    }

    public void setUpdatedAt(ZonedDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @PrePersist
    public void prePersist() {
        if (this.active == null) {
            this.active = true;
        }
        if (this.status == null) {
            this.status = ConversationStatus.BOT_HANDLING;
        }
        if (this.unreadCount == null) {
            this.unreadCount = 0;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Conversation)) {
            return false;
        }
        return getId() != null && getId().equals(((Conversation) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Conversation{" +
            "id=" + getId() +
            ", channel='" + getChannel() + "'" +
            ", senderId='" + getSenderId() + "'" +
            ", customerName='" + getCustomerName() + "'" +
            ", customerPhone='" + getCustomerPhone() + "'" +
            ", status='" + getStatus() + "'" +
            ", lastMessageAt='" + getLastMessageAt() + "'" +
            ", lastInboundAt='" + getLastInboundAt() + "'" +
            ", unreadCount=" + getUnreadCount() +
            ", escalationReason='" + getEscalationReason() + "'" +
            ", assignedTo='" + getAssignedTo() + "'" +
            ", active=" + getActive() +
            ", createdAt='" + getCreatedAt() + "'" +
            ", updatedAt='" + getUpdatedAt() + "'" +
            "}";
    }
}
