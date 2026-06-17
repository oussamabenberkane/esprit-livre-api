package com.oussamabenberkane.espritlivre.service.dto;

import com.oussamabenberkane.espritlivre.domain.enumeration.ConversationStatus;

/**
 * Response to the bot after persisting an inbound message: the resulting conversation
 * status and whether the bot is allowed to auto-reply (only when BOT_HANDLING).
 */
public record RecordInboundResponse(Long conversationId, ConversationStatus status, boolean botShouldReply) {}
