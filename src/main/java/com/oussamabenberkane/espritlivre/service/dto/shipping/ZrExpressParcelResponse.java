package com.oussamabenberkane.espritlivre.service.dto.shipping;

/**
 * DTO for ZR Express create parcel response.
 * The create endpoint only returns the parcel ID - tracking number must be fetched via GET.
 */
public record ZrExpressParcelResponse(String id) {}
