package com.oussamabenberkane.espritlivre.service.dto;

import java.math.BigDecimal;

public record PixelAddToCartRequestDTO(
    String eventId,
    String contentId,
    String contentType,
    BigDecimal value,
    int numItems,
    String eventSourceUrl,
    String fbc,
    String fbp,
    String externalId,
    String em,
    String ph,
    String fn,
    String ln
) implements PixelIdentity {}
