package com.oussamabenberkane.espritlivre.service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * The bot's response to {@code POST /internal/send}: the Meta message id of the delivered message.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BotSendResponse(String messageId) {}
