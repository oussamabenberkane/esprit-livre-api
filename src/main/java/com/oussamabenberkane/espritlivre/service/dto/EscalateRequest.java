package com.oussamabenberkane.espritlivre.service.dto;

import com.oussamabenberkane.espritlivre.domain.enumeration.Channel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Payload the bot POSTs to flag a conversation as needing a human.
 */
public record EscalateRequest(@NotNull Channel channel, @NotBlank String senderId, String reason) {}
