package com.oussamabenberkane.espritlivre.service.dto;

public record PixelSearchRequestDTO(
    String eventId,
    String searchString,
    String eventSourceUrl,
    String fbc,
    String fbp
) {}
