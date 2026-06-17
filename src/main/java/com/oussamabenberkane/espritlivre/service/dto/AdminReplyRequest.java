package com.oussamabenberkane.espritlivre.service.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Admin composer payload: the free-text reply to send to the customer.
 */
public record AdminReplyRequest(@NotBlank String content) {}
