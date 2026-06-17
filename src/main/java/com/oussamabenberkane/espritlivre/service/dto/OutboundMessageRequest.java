package com.oussamabenberkane.espritlivre.service.dto;

import com.oussamabenberkane.espritlivre.domain.enumeration.Channel;
import com.oussamabenberkane.espritlivre.domain.enumeration.MessageSenderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.ZonedDateTime;

/**
 * Payload the bot POSTs to persist one of its own outbound replies.
 * {@code senderType} defaults to {@code BOT} when omitted.
 */
public record OutboundMessageRequest(
    @NotNull Channel channel,
    @NotBlank String senderId,
    @NotBlank String text,
    MessageSenderType senderType,
    String externalMessageId,
    ZonedDateTime sentAt
) {}
