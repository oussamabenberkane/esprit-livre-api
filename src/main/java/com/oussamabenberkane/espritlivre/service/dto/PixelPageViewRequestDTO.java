package com.oussamabenberkane.espritlivre.service.dto;

public record PixelPageViewRequestDTO(
    String eventId,
    String eventSourceUrl,
    String fbc,
    String fbp,
    String externalId,
    String em,
    String ph,
    String fn,
    String ln
) implements PixelIdentity {}
