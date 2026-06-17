package com.oussamabenberkane.espritlivre.service;

import com.oussamabenberkane.espritlivre.domain.Conversation;
import com.oussamabenberkane.espritlivre.domain.Message;
import com.oussamabenberkane.espritlivre.domain.enumeration.Channel;
import com.oussamabenberkane.espritlivre.domain.enumeration.ConversationStatus;
import com.oussamabenberkane.espritlivre.domain.enumeration.MessageDirection;
import com.oussamabenberkane.espritlivre.domain.enumeration.MessageSenderType;
import com.oussamabenberkane.espritlivre.repository.ConversationRepository;
import com.oussamabenberkane.espritlivre.repository.MessageRepository;
import com.oussamabenberkane.espritlivre.service.dto.ConversationDTO;
import com.oussamabenberkane.espritlivre.service.dto.EscalateRequest;
import com.oussamabenberkane.espritlivre.service.dto.InboundMessageRequest;
import com.oussamabenberkane.espritlivre.service.dto.MessageDTO;
import com.oussamabenberkane.espritlivre.service.dto.OutboundMessageRequest;
import com.oussamabenberkane.espritlivre.service.dto.RecordInboundResponse;
import com.oussamabenberkane.espritlivre.service.mapper.ConversationMapper;
import com.oussamabenberkane.espritlivre.service.mapper.MessageMapper;
import com.oussamabenberkane.espritlivre.service.specs.ConversationSpecifications;
import java.time.ZonedDateTime;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Service for messaging conversations: the admin inbox, the human-handoff state machine,
 * admin replies (sent through the bot), and the bot-facing persistence/escalation hooks.
 */
@Service
@Transactional
public class ConversationService {

    private static final Logger LOG = LoggerFactory.getLogger(ConversationService.class);

    private static final int SNIPPET_MAX_LENGTH = 200;
    private static final int CONTENT_MAX_LENGTH = 4096;
    private static final int REASON_MAX_LENGTH = 500;
    /** WhatsApp free-text replies are only allowed within 24h of the customer's last message. */
    private static final int CARE_WINDOW_HOURS = 24;

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final ConversationMapper conversationMapper;
    private final MessageMapper messageMapper;
    private final BotClient botClient;

    public ConversationService(
        ConversationRepository conversationRepository,
        MessageRepository messageRepository,
        ConversationMapper conversationMapper,
        MessageMapper messageMapper,
        BotClient botClient
    ) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.conversationMapper = conversationMapper;
        this.messageMapper = messageMapper;
        this.botClient = botClient;
    }

    // ===================== Admin-facing =====================

    @Transactional(readOnly = true)
    public Page<ConversationDTO> findAll(Pageable pageable, Channel channel, ConversationStatus status, String search) {
        Specification<Conversation> spec = ConversationSpecifications.activeOnly()
            .and(ConversationSpecifications.hasChannel(channel))
            .and(ConversationSpecifications.hasStatus(status))
            .and(ConversationSpecifications.searchByText(search));
        return conversationRepository.findAll(spec, pageable).map(conversationMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Optional<ConversationDTO> findOne(Long id) {
        return conversationRepository.findById(id).map(conversationMapper::toDto);
    }

    @Transactional(readOnly = true)
    public long totalUnread() {
        return conversationRepository.sumUnreadCount();
    }

    public ConversationDTO markRead(Long id) {
        Conversation c = getOrThrow(id);
        c.setUnreadCount(0);
        c.setUpdatedAt(ZonedDateTime.now());
        return conversationMapper.toDto(conversationRepository.save(c));
    }

    /** Admin takes over: pause the bot for this conversation. */
    public ConversationDTO takeOver(Long id) {
        Conversation c = getOrThrow(id);
        c.setStatus(ConversationStatus.HUMAN_HANDLING);
        c.setUpdatedAt(ZonedDateTime.now());
        return conversationMapper.toDto(conversationRepository.save(c));
    }

    /** Admin hands back to the bot: resume auto-replies and clear the escalation reason. */
    public ConversationDTO handBack(Long id) {
        Conversation c = getOrThrow(id);
        c.setStatus(ConversationStatus.BOT_HANDLING);
        c.setEscalationReason(null);
        c.setUpdatedAt(ZonedDateTime.now());
        return conversationMapper.toDto(conversationRepository.save(c));
    }

    public ConversationDTO resolve(Long id) {
        Conversation c = getOrThrow(id);
        c.setStatus(ConversationStatus.RESOLVED);
        c.setUpdatedAt(ZonedDateTime.now());
        return conversationMapper.toDto(conversationRepository.save(c));
    }

    /**
     * Send a free-text admin reply to the customer through the bot, then persist it.
     * Sending an admin reply implicitly takes over the conversation (HUMAN_HANDLING) so the
     * bot and the admin never both answer.
     *
     * @throws ResponseStatusException 422 when the 24h care window is closed, 502 when the bot fails.
     */
    public MessageDTO adminReply(Long id, String content, String adminLogin) {
        Conversation c = getOrThrow(id);

        if (!isWithinCareWindow(c.getLastInboundAt())) {
            throw new ResponseStatusException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "La fenêtre de 24h est fermée : l'envoi d'un message libre n'est plus autorisé (un modèle approuvé serait requis)."
            );
        }

        if (c.getStatus() == ConversationStatus.BOT_HANDLING || c.getStatus() == ConversationStatus.NEEDS_HUMAN) {
            c.setStatus(ConversationStatus.HUMAN_HANDLING);
        }

        // Deliver first: if the bot fails this throws (502) and nothing is persisted.
        String metaId = botClient.sendText(c.getChannel(), c.getSenderId(), content);

        ZonedDateTime now = ZonedDateTime.now();
        Message message = new Message()
            .conversation(c)
            .direction(MessageDirection.OUTBOUND)
            .senderType(MessageSenderType.ADMIN)
            .content(truncate(content, CONTENT_MAX_LENGTH))
            .adminLogin(adminLogin)
            .externalMessageId(metaId)
            .sentAt(now);
        message = messageRepository.save(message);

        c.setLastMessageAt(now);
        c.setLastMessageSnippet(snippet(content));
        c.setUpdatedAt(now);
        conversationRepository.save(c);

        return messageMapper.toDto(message);
    }

    // ===================== Bot-facing (internal) =====================

    /**
     * Persist an inbound customer message and upsert its conversation. Idempotent on the Meta
     * message id. Returns the resulting status and whether the bot may auto-reply.
     */
    public RecordInboundResponse recordInbound(InboundMessageRequest req) {
        ZonedDateTime now = ZonedDateTime.now();

        // Idempotency: a re-delivered message must not double-count unread or re-insert.
        if (req.externalMessageId() != null && messageRepository.existsByExternalMessageId(req.externalMessageId())) {
            Conversation existing = conversationRepository.findByChannelAndSenderId(req.channel(), req.senderId()).orElse(null);
            ConversationStatus status = existing != null ? existing.getStatus() : ConversationStatus.BOT_HANDLING;
            Long convoId = existing != null ? existing.getId() : null;
            return new RecordInboundResponse(convoId, status, status == ConversationStatus.BOT_HANDLING);
        }

        Conversation c = conversationRepository
            .findByChannelAndSenderId(req.channel(), req.senderId())
            .orElseGet(() ->
                new Conversation()
                    .channel(req.channel())
                    .senderId(req.senderId())
                    .status(ConversationStatus.BOT_HANDLING)
                    .unreadCount(0)
                    .createdAt(now)
            );

        if (req.customerName() != null && !req.customerName().isBlank()) {
            c.setCustomerName(req.customerName());
        }
        if (req.channel() == Channel.WHATSAPP && c.getCustomerPhone() == null) {
            c.setCustomerPhone(req.senderId()); // the WhatsApp sender id IS the phone number
        }
        // A new inbound message after RESOLVED reopens the conversation to the bot.
        if (c.getStatus() == ConversationStatus.RESOLVED) {
            c.setStatus(ConversationStatus.BOT_HANDLING);
        }

        ZonedDateTime ts = req.sentAt() != null ? req.sentAt() : now;
        c.setLastInboundAt(ts);
        c.setLastMessageAt(ts);
        c.setLastMessageSnippet(snippet(req.text()));
        c.setUnreadCount((c.getUnreadCount() == null ? 0 : c.getUnreadCount()) + 1);
        c.setUpdatedAt(now);
        c = conversationRepository.save(c);

        Message message = new Message()
            .conversation(c)
            .direction(MessageDirection.INBOUND)
            .senderType(MessageSenderType.CUSTOMER)
            .content(truncate(req.text(), CONTENT_MAX_LENGTH))
            .externalMessageId(req.externalMessageId())
            .sentAt(ts);
        messageRepository.save(message);

        boolean botShouldReply = c.getStatus() == ConversationStatus.BOT_HANDLING;
        return new RecordInboundResponse(c.getId(), c.getStatus(), botShouldReply);
    }

    /** Persist one of the bot's own outbound replies. Idempotent on the Meta message id. */
    public void recordOutbound(OutboundMessageRequest req) {
        if (req.externalMessageId() != null && messageRepository.existsByExternalMessageId(req.externalMessageId())) {
            return;
        }
        ZonedDateTime now = ZonedDateTime.now();
        Conversation c = conversationRepository
            .findByChannelAndSenderId(req.channel(), req.senderId())
            .orElseGet(() ->
                conversationRepository.save(
                    new Conversation()
                        .channel(req.channel())
                        .senderId(req.senderId())
                        .status(ConversationStatus.BOT_HANDLING)
                        .unreadCount(0)
                        .createdAt(now)
                )
            );

        ZonedDateTime ts = req.sentAt() != null ? req.sentAt() : now;
        MessageSenderType senderType = req.senderType() != null ? req.senderType() : MessageSenderType.BOT;
        Message message = new Message()
            .conversation(c)
            .direction(MessageDirection.OUTBOUND)
            .senderType(senderType)
            .content(truncate(req.text(), CONTENT_MAX_LENGTH))
            .externalMessageId(req.externalMessageId())
            .sentAt(ts);
        messageRepository.save(message);

        c.setLastMessageAt(ts);
        c.setLastMessageSnippet(snippet(req.text()));
        c.setUpdatedAt(now);
        conversationRepository.save(c);
    }

    /** Flag a conversation as needing a human (bot escalation). */
    public void escalate(EscalateRequest req) {
        ZonedDateTime now = ZonedDateTime.now();
        Conversation c = conversationRepository
            .findByChannelAndSenderId(req.channel(), req.senderId())
            .orElseGet(() ->
                new Conversation().channel(req.channel()).senderId(req.senderId()).unreadCount(0).createdAt(now)
            );
        c.setStatus(ConversationStatus.NEEDS_HUMAN);
        if (req.reason() != null && !req.reason().isBlank()) {
            c.setEscalationReason(truncate(req.reason(), REASON_MAX_LENGTH));
        }
        c.setUpdatedAt(now);
        conversationRepository.save(c);
        LOG.info("Conversation {}:{} escalated to NEEDS_HUMAN", req.channel(), req.senderId());
    }

    // ===================== Helpers =====================

    private Conversation getOrThrow(Long id) {
        return conversationRepository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation introuvable"));
    }

    private boolean isWithinCareWindow(ZonedDateTime lastInboundAt) {
        return lastInboundAt != null && lastInboundAt.plusHours(CARE_WINDOW_HOURS).isAfter(ZonedDateTime.now());
    }

    private static String snippet(String text) {
        return truncate(text, SNIPPET_MAX_LENGTH);
    }

    private static String truncate(String text, int max) {
        if (text == null) {
            return null;
        }
        return text.length() <= max ? text : text.substring(0, max);
    }
}
