package com.oussamabenberkane.espritlivre.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.oussamabenberkane.espritlivre.config.ApplicationProperties;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Sends server-side Purchase events to the Meta Conversions API (CAPI).
 * Events use the order uniqueId as eventID to deduplicate against the browser pixel.
 */
@Service
public class MetaConversionsApiService {

    private static final Logger LOG = LoggerFactory.getLogger(MetaConversionsApiService.class);
    private static final String CAPI_URL = "https://graph.facebook.com/v19.0/%s/events";

    private final ApplicationProperties applicationProperties;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public MetaConversionsApiService(ApplicationProperties applicationProperties, ObjectMapper objectMapper) {
        this.applicationProperties = applicationProperties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    }

    /**
     * Sends a Purchase event to Meta CAPI asynchronously (fire-and-forget).
     *
     * @param orderId    the order uniqueId — used as eventID for browser pixel deduplication
     * @param value      the total order amount in DZD
     * @param numItems   total item quantity across all order items
     * @param contentIds list of content IDs (book IDs or "pack-{id}")
     * @param phone      raw phone number to be SHA-256 hashed before sending
     */
    public void sendPurchaseEvent(String orderId, BigDecimal value, int numItems, List<String> contentIds, String phone) {
        ApplicationProperties.Meta meta = applicationProperties.getMeta();
        if (!meta.isEnabled() || !StringUtils.hasText(meta.getPixelId()) || !StringUtils.hasText(meta.getAccessToken())) {
            return;
        }

        String url = String.format(CAPI_URL, meta.getPixelId()) + "?access_token=" + meta.getAccessToken();

        try {
            String payload = buildPurchasePayload(orderId, value, numItems, contentIds, phone);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .timeout(Duration.ofSeconds(10))
                .build();

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        LOG.debug("Meta CAPI Purchase event sent for order: {}", orderId);
                    } else {
                        LOG.warn("Meta CAPI returned HTTP {} for order: {}. Body: {}", response.statusCode(), orderId, response.body());
                    }
                })
                .exceptionally(ex -> {
                    LOG.error("Meta CAPI request failed for order: {}", orderId, ex);
                    return null;
                });

        } catch (Exception e) {
            LOG.error("Failed to build Meta CAPI payload for order: {}", orderId, e);
        }
    }

    private String buildPurchasePayload(String orderId, BigDecimal value, int numItems, List<String> contentIds, String phone) throws Exception {
        long eventTime = System.currentTimeMillis() / 1000L;

        ObjectNode event = objectMapper.createObjectNode();
        event.put("event_name", "Purchase");
        event.put("event_time", eventTime);
        event.put("event_id", orderId);
        event.put("action_source", "website");

        // User data — only include hashed phone if available
        ObjectNode userData = objectMapper.createObjectNode();
        String hashedPhone = hashSha256(normalizePhone(phone));
        if (hashedPhone != null) {
            ArrayNode phArray = objectMapper.createArrayNode();
            phArray.add(hashedPhone);
            userData.set("ph", phArray);
        }
        event.set("user_data", userData);

        // Custom data
        ObjectNode customData = objectMapper.createObjectNode();
        customData.put("value", value != null ? value.doubleValue() : 0.0);
        customData.put("currency", "DZD");
        customData.put("content_type", "product");
        customData.put("num_items", numItems);
        ArrayNode ids = objectMapper.createArrayNode();
        contentIds.forEach(ids::add);
        customData.set("content_ids", ids);
        event.set("custom_data", customData);

        ObjectNode payload = objectMapper.createObjectNode();
        ArrayNode data = objectMapper.createArrayNode();
        data.add(event);
        payload.set("data", data);

        return objectMapper.writeValueAsString(payload);
    }

    private String normalizePhone(String phone) {
        if (!StringUtils.hasText(phone)) return null;
        return phone.replaceAll("\\s+", "").toLowerCase();
    }

    private String hashSha256(String input) {
        if (input == null) return null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            LOG.error("SHA-256 not available", e);
            return null;
        }
    }
}
