package com.oussamabenberkane.espritlivre.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.oussamabenberkane.espritlivre.domain.enumeration.MessageDirection;
import com.oussamabenberkane.espritlivre.domain.enumeration.MessageSenderType;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.ZonedDateTime;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * A single message inside a {@link Conversation}.
 */
@Entity
@Table(name = "whatsapp_message")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Message implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "messageSequenceGenerator")
    @SequenceGenerator(name = "messageSequenceGenerator", sequenceName = "whatsapp_message_seq", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false, length = 20)
    private MessageDirection direction;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "sender_type", nullable = false, length = 20)
    private MessageSenderType senderType;

    @NotNull
    @Column(name = "content", nullable = false, length = 4096)
    private String content;

    /** Meta message id — used for idempotent persistence (unique when present). */
    @Column(name = "external_message_id")
    private String externalMessageId;

    /** Keycloak login of the admin who sent this message (ADMIN messages only). */
    @Column(name = "admin_login")
    private String adminLogin;

    @Column(name = "sent_at")
    private ZonedDateTime sentAt;

    @Column(name = "created_at")
    private ZonedDateTime createdAt;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    @JsonIgnoreProperties(value = { "messages" }, allowSetters = true)
    private Conversation conversation;

    public Long getId() {
        return this.id;
    }

    public Message id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public MessageDirection getDirection() {
        return this.direction;
    }

    public Message direction(MessageDirection direction) {
        this.setDirection(direction);
        return this;
    }

    public void setDirection(MessageDirection direction) {
        this.direction = direction;
    }

    public MessageSenderType getSenderType() {
        return this.senderType;
    }

    public Message senderType(MessageSenderType senderType) {
        this.setSenderType(senderType);
        return this;
    }

    public void setSenderType(MessageSenderType senderType) {
        this.senderType = senderType;
    }

    public String getContent() {
        return this.content;
    }

    public Message content(String content) {
        this.setContent(content);
        return this;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getExternalMessageId() {
        return this.externalMessageId;
    }

    public Message externalMessageId(String externalMessageId) {
        this.setExternalMessageId(externalMessageId);
        return this;
    }

    public void setExternalMessageId(String externalMessageId) {
        this.externalMessageId = externalMessageId;
    }

    public String getAdminLogin() {
        return this.adminLogin;
    }

    public Message adminLogin(String adminLogin) {
        this.setAdminLogin(adminLogin);
        return this;
    }

    public void setAdminLogin(String adminLogin) {
        this.adminLogin = adminLogin;
    }

    public ZonedDateTime getSentAt() {
        return this.sentAt;
    }

    public Message sentAt(ZonedDateTime sentAt) {
        this.setSentAt(sentAt);
        return this;
    }

    public void setSentAt(ZonedDateTime sentAt) {
        this.sentAt = sentAt;
    }

    public ZonedDateTime getCreatedAt() {
        return this.createdAt;
    }

    public Message createdAt(ZonedDateTime createdAt) {
        this.setCreatedAt(createdAt);
        return this;
    }

    public void setCreatedAt(ZonedDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Conversation getConversation() {
        return this.conversation;
    }

    public void setConversation(Conversation conversation) {
        this.conversation = conversation;
    }

    public Message conversation(Conversation conversation) {
        this.setConversation(conversation);
        return this;
    }

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = ZonedDateTime.now();
        }
        if (this.sentAt == null) {
            this.sentAt = this.createdAt;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Message)) {
            return false;
        }
        return getId() != null && getId().equals(((Message) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Message{" +
            "id=" + getId() +
            ", direction='" + getDirection() + "'" +
            ", senderType='" + getSenderType() + "'" +
            ", externalMessageId='" + getExternalMessageId() + "'" +
            ", adminLogin='" + getAdminLogin() + "'" +
            ", sentAt='" + getSentAt() + "'" +
            ", createdAt='" + getCreatedAt() + "'" +
            "}";
    }
}
