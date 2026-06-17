package com.oussamabenberkane.espritlivre.service.dto;

import com.oussamabenberkane.espritlivre.domain.enumeration.Channel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.ZonedDateTime;

/**
 * Payload the bot POSTs to persist an inbound customer message and upsert the conversation.
 */
public record InboundMessageRequest(
    @NotNull Channel channel,
    @NotBlank String senderId,
    String customerName,
    @NotBlank String text,
    String externalMessageId,
    ZonedDateTime sentAt
) {}
