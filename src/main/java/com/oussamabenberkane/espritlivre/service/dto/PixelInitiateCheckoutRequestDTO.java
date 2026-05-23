package com.oussamabenberkane.espritlivre.service.dto;

import java.math.BigDecimal;
import java.util.List;

public record PixelInitiateCheckoutRequestDTO(
    String eventId,
    BigDecimal value,
    int numItems,
    List<String> contentIds,
    String eventSourceUrl,
    String fbc,
    String fbp
) {}
