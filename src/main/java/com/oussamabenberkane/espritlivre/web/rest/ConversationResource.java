package com.oussamabenberkane.espritlivre.web.rest;

import com.oussamabenberkane.espritlivre.domain.enumeration.Channel;
import com.oussamabenberkane.espritlivre.domain.enumeration.ConversationStatus;
import com.oussamabenberkane.espritlivre.security.AuthoritiesConstants;
import com.oussamabenberkane.espritlivre.security.SecurityUtils;
import com.oussamabenberkane.espritlivre.service.ConversationService;
import com.oussamabenberkane.espritlivre.service.MessageService;
import com.oussamabenberkane.espritlivre.service.dto.AdminReplyRequest;
import com.oussamabenberkane.espritlivre.service.dto.ConversationDTO;
import com.oussamabenberkane.espritlivre.service.dto.MessageDTO;
import jakarta.validation.Valid;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.PaginationUtil;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for the WhatsApp/messaging admin inbox.
 * All endpoints require {@code ROLE_ADMIN}.
 */
@RestController
@RequestMapping("/api/conversations")
public class ConversationResource {

    private static final Logger LOG = LoggerFactory.getLogger(ConversationResource.class);

    private final ConversationService conversationService;
    private final MessageService messageService;

    public ConversationResource(ConversationService conversationService, MessageService messageService) {
        this.conversationService = conversationService;
        this.messageService = messageService;
    }

    /**
     * {@code GET /api/conversations} : paginated inbox, filterable by channel + status + search,
     * sorted by most recent activity by default.
     */
    @GetMapping("")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<Page<ConversationDTO>> getAllConversations(
        @org.springdoc.core.annotations.ParameterObject Pageable pageable,
        @RequestParam(required = false) Channel channel,
        @RequestParam(required = false) ConversationStatus status,
        @RequestParam(required = false) String search
    ) {
        LOG.debug("REST request to get Conversations (channel={}, status={}, search={})", channel, status, search);
        Pageable effective = pageable.getSort().isSorted()
            ? pageable
            : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "lastMessageAt"));
        Page<ConversationDTO> page = conversationService.findAll(effective, channel, status, search);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page);
    }

    /**
     * {@code GET /api/conversations/unread-count} : total unread inbound messages (nav badge).
     */
    @GetMapping("/unread-count")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<Map<String, Long>> getUnreadCount() {
        return ResponseEntity.ok(Map.of("unreadCount", conversationService.totalUnread()));
    }

    /**
     * {@code GET /api/conversations/:id} : a single conversation.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<ConversationDTO> getConversation(@PathVariable("id") Long id) {
        LOG.debug("REST request to get Conversation : {}", id);
        return ResponseUtil.wrapOrNotFound(conversationService.findOne(id));
    }

    /**
     * {@code GET /api/conversations/:id/messages} : the ordered message thread (oldest first).
     * Pass {@code afterId} to fetch only newer messages (incremental polling).
     */
    @GetMapping("/{id}/messages")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<Page<MessageDTO>> getMessages(
        @PathVariable("id") Long id,
        @RequestParam(required = false) Long afterId,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        Page<MessageDTO> page = messageService.getThread(id, afterId, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page);
    }

    /**
     * {@code POST /api/conversations/:id/reply} : send a free-text reply to the customer.
     * Implicitly takes over the conversation. Returns 422 if the 24h care window is closed.
     */
    @PostMapping("/{id}/reply")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<MessageDTO> reply(@PathVariable("id") Long id, @Valid @RequestBody AdminReplyRequest request) {
        LOG.debug("REST request to reply to Conversation : {}", id);
        String adminLogin = SecurityUtils.getCurrentUserLogin().orElse(null);
        MessageDTO message = conversationService.adminReply(id, request.content(), adminLogin);
        return ResponseEntity.ok(message);
    }

    /**
     * {@code POST /api/conversations/:id/take-over} : pause the bot (HUMAN_HANDLING).
     */
    @PostMapping("/{id}/take-over")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<ConversationDTO> takeOver(@PathVariable("id") Long id) {
        return ResponseEntity.ok(conversationService.takeOver(id));
    }

    /**
     * {@code POST /api/conversations/:id/hand-back} : return control to the bot (BOT_HANDLING).
     */
    @PostMapping("/{id}/hand-back")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<ConversationDTO> handBack(@PathVariable("id") Long id) {
        return ResponseEntity.ok(conversationService.handBack(id));
    }

    /**
     * {@code POST /api/conversations/:id/resolve} : mark the conversation resolved.
     */
    @PostMapping("/{id}/resolve")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<ConversationDTO> resolve(@PathVariable("id") Long id) {
        return ResponseEntity.ok(conversationService.resolve(id));
    }

    /**
     * {@code POST /api/conversations/:id/read} : reset the unread counter.
     */
    @PostMapping("/{id}/read")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<ConversationDTO> markRead(@PathVariable("id") Long id) {
        return ResponseEntity.ok(conversationService.markRead(id));
    }
}
