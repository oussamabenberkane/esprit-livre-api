package com.oussamabenberkane.espritlivre.service.dto.shipping;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

/**
 * Webhook event payload from Yalidine API.
 */
public record YalidineWebhookPayload(
    String event,

    @JsonProperty("tracking_number")
    String trackingNumber,

    @JsonProperty("order_id")
    String orderId,

    @JsonProperty("status_code")
    String statusCode,

    String status,

    @JsonProperty("sub_status")
    String subStatus,

    String date,

    @JsonProperty("updated_at")
    String updatedAt,

    String comment,

    String wilaya,

    String commune,

    Integer attempts,

    @JsonProperty("amount_collected")
    BigDecimal amountCollected,

    @JsonProperty("amount_due")
    BigDecimal amountDue,

    @JsonProperty("receiver_name")
    String receiverName,

    String phone,

    @JsonProperty("signature_url")
    String signatureUrl,

    @JsonProperty("fail_cause")
    String failCause,

    @JsonProperty("security_token")
    String securityToken
) {}
