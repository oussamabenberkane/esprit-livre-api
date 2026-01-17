package com.oussamabenberkane.espritlivre.service.dto.shipping;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response DTO from Yalidine API parcel creation.
 */
public record YalidineParcelResponse(
    String tracking,
    String label,

    @JsonProperty("order_id")
    String orderId,

    String status,
    String message
) {}
