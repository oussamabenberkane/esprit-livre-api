package com.oussamabenberkane.espritlivre.service.dto;

public record PixelSearchRequestDTO(
    String eventId,
    String searchString,
    String eventSourceUrl,
    String fbc,
    String fbp,
    String externalId,
    String em,
    String ph,
    String fn,
    String ln
) implements PixelIdentity {}
