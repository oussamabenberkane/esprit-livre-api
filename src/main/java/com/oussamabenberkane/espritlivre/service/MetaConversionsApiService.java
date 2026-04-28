package com.oussamabenberkane.espritlivre.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.oussamabenberkane.espritlivre.config.ApplicationProperties;
import com.oussamabenberkane.espritlivre.service.dto.PixelEventSummaryDTO;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Sends server-side Purchase events to the Meta Conversions API (CAPI).
 * Events use the order uniqueId as eventID to deduplicate against the browser pixel.
 */
@Service
public class MetaConversionsApiService {

    private static final Logger LOG = LoggerFactory.getLogger(MetaConversionsApiService.class);
    private static final String CAPI_URL = "https://graph.facebook.com/v19.0/%s/events";
    private static final int MAX_LOG_SIZE = 500;
    private static final List<String> ALL_EVENT_NAMES = List.of(
        "PageView", "ViewContent", "Search", "AddToCart",
        "InitiateCheckout", "Purchase", "CompleteRegistration", "Contact"
    );

    private record PixelEventEntry(String eventName, Instant firedAt) {}

    private final Deque<PixelEventEntry> eventLog = new ArrayDeque<>();

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

        String clientIp = null;
        String userAgent = null;
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest req = attrs.getRequest();
                clientIp = resolveClientIp(req);
                userAgent = req.getHeader("User-Agent");
            }
        } catch (Exception ignored) {}

        try {
            String payload = buildPurchasePayload(orderId, value, numItems, contentIds, phone, clientIp, userAgent);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .timeout(Duration.ofSeconds(10))
                .build();

            logEvent("Purchase");
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

    private String buildPurchasePayload(String orderId, BigDecimal value, int numItems, List<String> contentIds, String phone, String clientIp, String userAgent) throws Exception {
        long eventTime = System.currentTimeMillis() / 1000L;

        ObjectNode event = objectMapper.createObjectNode();
        event.put("event_name", "Purchase");
        event.put("event_time", eventTime);
        event.put("event_id", orderId);
        event.put("action_source", "website");
        event.put("event_source_url", "https://espritlivre.com/cart");

        // User data
        ObjectNode userData = objectMapper.createObjectNode();
        String hashedPhone = hashSha256(normalizePhone(phone));
        if (hashedPhone != null) {
            ArrayNode phArray = objectMapper.createArrayNode();
            phArray.add(hashedPhone);
            userData.set("ph", phArray);
        }
        if (StringUtils.hasText(clientIp)) userData.put("client_ip_address", clientIp);
        if (StringUtils.hasText(userAgent)) userData.put("client_user_agent", userAgent);
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

    private String resolveClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xff)) return xff.split(",")[0].trim();
        String realIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(realIp)) return realIp.trim();
        return request.getRemoteAddr();
    }

    private String normalizePhone(String phone) {
        if (!StringUtils.hasText(phone)) return null;
        return phone.replaceAll("\\s+", "").toLowerCase();
    }

    private synchronized void logEvent(String eventName) {
        eventLog.addFirst(new PixelEventEntry(eventName, Instant.now()));
        while (eventLog.size() > MAX_LOG_SIZE) {
            eventLog.pollLast();
        }
    }

    public synchronized List<PixelEventSummaryDTO> getRecentEventSummaries() {
        Instant cutoff = Instant.now().minus(24, ChronoUnit.HOURS);
        Map<String, List<PixelEventEntry>> byName = eventLog.stream()
            .collect(Collectors.groupingBy(PixelEventEntry::eventName));

        return ALL_EVENT_NAMES.stream().map(name -> {
            List<PixelEventEntry> entries = byName.getOrDefault(name, List.of());
            long count24h = entries.stream().filter(e -> e.firedAt().isAfter(cutoff)).count();
            Optional<Instant> lastSeen = entries.stream()
                .map(PixelEventEntry::firedAt)
                .max(Comparator.naturalOrder());
            return new PixelEventSummaryDTO(name, count24h, lastSeen.orElse(null));
        }).collect(Collectors.toList());
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
