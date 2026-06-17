package com.oussamabenberkane.espritlivre.web.rest.internal;

import com.oussamabenberkane.espritlivre.service.ConversationService;
import com.oussamabenberkane.espritlivre.service.dto.EscalateRequest;
import com.oussamabenberkane.espritlivre.service.dto.InboundMessageRequest;
import com.oussamabenberkane.espritlivre.service.dto.OutboundMessageRequest;
import com.oussamabenberkane.espritlivre.service.dto.RecordInboundResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Internal, bot-only endpoints to persist messages and drive the handoff status.
 * <p>
 * Secured by a shared-secret header (see {@code InternalAuthFilter} / authority {@code INTERNAL}),
 * NOT by Keycloak JWT. Reached service-to-service over the docker network and blocked at nginx
 * from the public internet — never call these from a browser.
 */
@RestController
@RequestMapping("/internal/conversations")
public class BotInternalResource {

    private static final Logger LOG = LoggerFactory.getLogger(BotInternalResource.class);

    private final ConversationService conversationService;

    public BotInternalResource(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    /** Persist an inbound customer message, upsert the conversation, and return whether the bot may reply. */
    @PostMapping("/inbound")
    public ResponseEntity<RecordInboundResponse> inbound(@Valid @RequestBody InboundMessageRequest request) {
        return ResponseEntity.ok(conversationService.recordInbound(request));
    }

    /** Persist one of the bot's own outbound replies. */
    @PostMapping("/outbound")
    public ResponseEntity<Void> outbound(@Valid @RequestBody OutboundMessageRequest request) {
        conversationService.recordOutbound(request);
        return ResponseEntity.ok().build();
    }

    /** Flag the conversation as needing a human (bot escalation). */
    @PostMapping("/escalate")
    public ResponseEntity<Void> escalate(@Valid @RequestBody EscalateRequest request) {
        LOG.debug("Internal escalate request for {}:{}", request.channel(), request.senderId());
        conversationService.escalate(request);
        return ResponseEntity.ok().build();
    }
}
