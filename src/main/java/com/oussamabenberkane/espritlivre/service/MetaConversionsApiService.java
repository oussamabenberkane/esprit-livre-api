package com.oussamabenberkane.espritlivre.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.oussamabenberkane.espritlivre.config.ApplicationProperties;
import com.oussamabenberkane.espritlivre.domain.PixelEvent;
import com.oussamabenberkane.espritlivre.repository.PixelEventRepository;
import com.oussamabenberkane.espritlivre.service.dto.PixelEventSummaryDTO;
import com.oussamabenberkane.espritlivre.service.dto.PixelIdentity;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Sends server-side events to the Meta Conversions API (CAPI).
 * Events use the same eventID as the browser pixel for deduplication.
 */
@Service
public class MetaConversionsApiService {

    private static final Logger LOG = LoggerFactory.getLogger(MetaConversionsApiService.class);
    private static final String CAPI_URL = "https://graph.facebook.com/v19.0/%s/events";
    private static final Pattern SHA256_HEX = Pattern.compile("[0-9a-fA-F]{64}");
    private static final List<String> ALL_EVENT_NAMES = List.of(
        "PageView", "ViewContent", "Search", "AddToCart",
        "InitiateCheckout", "Purchase", "CompleteRegistration", "Contact"
    );

    private final ApplicationProperties applicationProperties;
    private final PixelEventRepository pixelEventRepository;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public MetaConversionsApiService(ApplicationProperties applicationProperties,
                                     PixelEventRepository pixelEventRepository,
                                     ObjectMapper objectMapper) {
        this.applicationProperties = applicationProperties;
        this.pixelEventRepository = pixelEventRepository;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    }

    public void sendPageViewEvent(String eventId, String eventSourceUrl, PixelIdentity user) {
        if (isDisabled()) return;
        try {
            ObjectNode event = baseEvent("PageView", eventId, eventSourceUrl);
            event.set("user_data", baseUserData(user));
            event.set("custom_data", objectMapper.createObjectNode());
            dispatch("PageView", eventId, event);
        } catch (Exception e) {
            LOG.error("Failed to build Meta CAPI PageView payload for eventId: {}", eventId, e);
        }
    }

    public void sendViewContentEvent(String eventId, String contentId, String contentType, BigDecimal value, String eventSourceUrl, PixelIdentity user) {
        if (isDisabled()) return;
        try {
            ObjectNode event = baseEvent("ViewContent", eventId, eventSourceUrl);
            event.set("user_data", baseUserData(user));

            ObjectNode customData = objectMapper.createObjectNode();
            customData.put("value", value != null ? value.doubleValue() : 0.0);
            customData.put("currency", "DZD");
            customData.put("content_type", StringUtils.hasText(contentType) ? contentType : "product");
            ArrayNode ids = objectMapper.createArrayNode();
            ids.add(contentId);
            customData.set("content_ids", ids);
            event.set("custom_data", customData);

            dispatch("ViewContent", eventId, event);
        } catch (Exception e) {
            LOG.error("Failed to build Meta CAPI ViewContent payload for eventId: {}", eventId, e);
        }
    }

    public void sendSearchEvent(String eventId, String searchString, String eventSourceUrl, PixelIdentity user) {
        if (isDisabled()) return;
        try {
            ObjectNode event = baseEvent("Search", eventId, eventSourceUrl);
            event.set("user_data", baseUserData(user));

            ObjectNode customData = objectMapper.createObjectNode();
            customData.put("search_string", searchString);
            event.set("custom_data", customData);

            dispatch("Search", eventId, event);
        } catch (Exception e) {
            LOG.error("Failed to build Meta CAPI Search payload for eventId: {}", eventId, e);
        }
    }

    public void sendAddToCartEvent(String eventId, String contentId, String contentType, BigDecimal value, int numItems, String eventSourceUrl, PixelIdentity user) {
        if (isDisabled()) return;
        try {
            ObjectNode event = baseEvent("AddToCart", eventId, eventSourceUrl);
            event.set("user_data", baseUserData(user));

            ObjectNode customData = objectMapper.createObjectNode();
            customData.put("value", value != null ? value.doubleValue() : 0.0);
            customData.put("currency", "DZD");
            customData.put("content_type", StringUtils.hasText(contentType) ? contentType : "product");
            customData.put("num_items", numItems);
            ArrayNode ids = objectMapper.createArrayNode();
            ids.add(contentId);
            customData.set("content_ids", ids);
            event.set("custom_data", customData);

            dispatch("AddToCart", eventId, event);
        } catch (Exception e) {
            LOG.error("Failed to build Meta CAPI AddToCart payload for eventId: {}", eventId, e);
        }
    }

    public void sendInitiateCheckoutEvent(String eventId, BigDecimal value, int numItems, List<String> contentIds, String eventSourceUrl, PixelIdentity user) {
        if (isDisabled()) return;
        try {
            ObjectNode event = baseEvent("InitiateCheckout", eventId, eventSourceUrl);
            event.set("user_data", baseUserData(user));

            ObjectNode customData = objectMapper.createObjectNode();
            customData.put("value", value != null ? value.doubleValue() : 0.0);
            customData.put("currency", "DZD");
            customData.put("num_items", numItems);
            customData.put("content_type", "product");
            ArrayNode ids = objectMapper.createArrayNode();
            if (contentIds != null) contentIds.forEach(ids::add);
            customData.set("content_ids", ids);
            event.set("custom_data", customData);

            dispatch("InitiateCheckout", eventId, event);
        } catch (Exception e) {
            LOG.error("Failed to build Meta CAPI InitiateCheckout payload for eventId: {}", eventId, e);
        }
    }

    public void sendCompleteRegistrationEvent(String eventId, String eventSourceUrl, PixelIdentity user) {
        if (isDisabled()) return;
        try {
            ObjectNode event = baseEvent("CompleteRegistration", eventId, eventSourceUrl);
            event.set("user_data", baseUserData(user));

            ObjectNode customData = objectMapper.createObjectNode();
            customData.put("status", true);
            event.set("custom_data", customData);

            dispatch("CompleteRegistration", eventId, event);
        } catch (Exception e) {
            LOG.error("Failed to build Meta CAPI CompleteRegistration payload for eventId: {}", eventId, e);
        }
    }

    public void sendContactEvent(String eventId, String eventSourceUrl, PixelIdentity user) {
        if (isDisabled()) return;
        try {
            ObjectNode event = baseEvent("Contact", eventId, eventSourceUrl);
            event.set("user_data", baseUserData(user));
            event.set("custom_data", objectMapper.createObjectNode());
            dispatch("Contact", eventId, event);
        } catch (Exception e) {
            LOG.error("Failed to build Meta CAPI Contact payload for eventId: {}", eventId, e);
        }
    }

    /**
     * Sends a Purchase event to Meta CAPI asynchronously (fire-and-forget).
     * Unlike the relayed events above, PII arrives raw from the order and is
     * normalized + SHA-256 hashed here. The order uniqueId is the event_id,
     * deduplicating against the browser pixel Purchase.
     */
    public void sendPurchaseEvent(String orderId, BigDecimal value, int numItems, List<String> contentIds,
                                  String phone, String email, String firstName, String lastName,
                                  String city, String wilaya, String postalCode,
                                  String eventSourceUrl, String fbc, String fbp, String externalId) {
        if (isDisabled()) return;
        try {
            String resolvedUrl = StringUtils.hasText(eventSourceUrl) ? eventSourceUrl : "https://espritlivre.com/cart";
            ObjectNode event = baseEvent("Purchase", orderId, resolvedUrl);

            ObjectNode userData = baseUserData(new OrderIdentity(fbc, fbp, externalId));
            addHashedArray(userData, "ph", normalizePhone(phone));
            addHashedArray(userData, "em", normalizeEmail(email));
            addHashedArray(userData, "fn", normalizeName(firstName));
            addHashedArray(userData, "ln", normalizeName(lastName));
            addHashedArray(userData, "ct", normalizeGeo(city));
            addHashedArray(userData, "st", normalizeGeo(wilaya));
            addHashedArray(userData, "zp", normalizeZip(postalCode));
            // All customers are in Algeria
            addHashedArray(userData, "country", "dz");
            event.set("user_data", userData);

            ObjectNode customData = objectMapper.createObjectNode();
            customData.put("value", value != null ? value.doubleValue() : 0.0);
            customData.put("currency", "DZD");
            customData.put("content_type", "product");
            customData.put("num_items", numItems);
            ArrayNode ids = objectMapper.createArrayNode();
            contentIds.forEach(ids::add);
            customData.set("content_ids", ids);
            event.set("custom_data", customData);

            dispatch("Purchase", orderId, event);
        } catch (Exception e) {
            LOG.error("Failed to build Meta CAPI payload for order: {}", orderId, e);
        }
    }

    /** Purchase identity comes from the order payload rather than a relayed pixel request. */
    private record OrderIdentity(String fbc, String fbp, String externalId) implements PixelIdentity {}

    private boolean isDisabled() {
        ApplicationProperties.Meta meta = applicationProperties.getMeta();
        return !meta.isEnabled() || !StringUtils.hasText(meta.getPixelId()) || !StringUtils.hasText(meta.getAccessToken());
    }

    private ObjectNode baseEvent(String eventName, String eventId, String eventSourceUrl) {
        ObjectNode event = objectMapper.createObjectNode();
        event.put("event_name", eventName);
        event.put("event_time", System.currentTimeMillis() / 1000L);
        event.put("event_id", eventId);
        event.put("action_source", "website");
        if (StringUtils.hasText(eventSourceUrl)) {
            // Meta rejects event_source_url > 1024 chars; truncate defensively.
            event.put("event_source_url", eventSourceUrl.length() > 1024 ? eventSourceUrl.substring(0, 1024) : eventSourceUrl);
        }
        return event;
    }

    /**
     * Builds user_data with the caller's IP/user-agent plus the relayed identity:
     * fbc/fbp cookies, external_id and any pre-hashed PII (em/ph/fn/ln).
     */
    private ObjectNode baseUserData(PixelIdentity user) {
        ObjectNode userData = objectMapper.createObjectNode();
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest req = attrs.getRequest();
                String clientIp = resolveClientIp(req);
                String userAgent = req.getHeader("User-Agent");
                if (StringUtils.hasText(clientIp)) userData.put("client_ip_address", clientIp);
                if (StringUtils.hasText(userAgent)) userData.put("client_user_agent", userAgent);
            }
        } catch (Exception ignored) {}

        if (user != null) {
            addMetaCookies(userData, user.fbc(), user.fbp());
            if (isSha256Hex(user.externalId())) userData.put("external_id", user.externalId().toLowerCase());
            addPreHashedArray(userData, "em", user.em());
            addPreHashedArray(userData, "ph", user.ph());
            addPreHashedArray(userData, "fn", user.fn());
            addPreHashedArray(userData, "ln", user.ln());
        }
        return userData;
    }

    private void dispatch(String eventName, String eventId, ObjectNode event) throws JsonProcessingException {
        ApplicationProperties.Meta meta = applicationProperties.getMeta();
        String url = String.format(CAPI_URL, meta.getPixelId()) + "?access_token=" + meta.getAccessToken();

        ObjectNode payload = objectMapper.createObjectNode();
        ArrayNode data = objectMapper.createArrayNode();
        data.add(event);
        payload.set("data", data);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(serializePayload(payload)))
            .timeout(Duration.ofSeconds(10))
            .build();

        logEvent(eventName);
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenAccept(r -> {
                if (r.statusCode() != 200) {
                    LOG.warn("Meta CAPI returned HTTP {} for {} eventId: {}. Body: {}", r.statusCode(), eventName, eventId, r.body());
                } else {
                    LOG.debug("Meta CAPI {} event sent: {}", eventName, eventId);
                }
            })
            .exceptionally(ex -> {
                LOG.error("Meta CAPI {} request failed for eventId: {}", eventName, eventId, ex);
                return null;
            });
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xff)) return xff.split(",")[0].trim();
        String realIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(realIp)) return realIp.trim();
        return request.getRemoteAddr();
    }

    private void addMetaCookies(ObjectNode userData, String fbc, String fbp) {
        if (StringUtils.hasText(fbc)) userData.put("fbc", fbc);
        if (StringUtils.hasText(fbp)) userData.put("fbp", fbp);
    }

    /**
     * Serializes the CAPI payload, injecting {@code test_event_code} at the top level when configured.
     * When set, events appear in the Meta Events Manager "Test Events" tab for live debugging.
     * Leave {@code META_TEST_EVENT_CODE} empty in normal production operation.
     */
    private String serializePayload(ObjectNode payload) throws JsonProcessingException {
        String testEventCode = applicationProperties.getMeta().getTestEventCode();
        if (StringUtils.hasText(testEventCode)) {
            payload.put("test_event_code", testEventCode);
        }
        return objectMapper.writeValueAsString(payload);
    }

    private void addHashedArray(ObjectNode parent, String key, String normalizedValue) {
        String hashed = hashSha256(normalizedValue);
        if (hashed == null) return;
        ArrayNode arr = objectMapper.createArrayNode();
        arr.add(hashed);
        parent.set(key, arr);
    }

    /**
     * Browser-relayed PII arrives pre-hashed; accept only well-formed SHA-256 hex
     * so raw PII can never be forwarded to Meta by mistake.
     */
    private void addPreHashedArray(ObjectNode parent, String key, String hashedValue) {
        if (!isSha256Hex(hashedValue)) return;
        ArrayNode arr = objectMapper.createArrayNode();
        arr.add(hashedValue.toLowerCase());
        parent.set(key, arr);
    }

    private boolean isSha256Hex(String value) {
        return value != null && SHA256_HEX.matcher(value).matches();
    }

    /**
     * Meta requires the phone as digits only, including the country code, without the "+"
     * sign, symbols or non-significant leading zeros. Local Algerian numbers (0XXXXXXXXX)
     * are converted to the 213XXXXXXXXX international form before hashing.
     */
    private String normalizePhone(String phone) {
        if (!StringUtils.hasText(phone)) return null;
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) return null;
        if (digits.startsWith("0") && digits.length() == 10) {
            digits = "213" + digits.substring(1);
        }
        return digits;
    }

    private String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)) return null;
        return email.trim().toLowerCase();
    }

    private String normalizeName(String name) {
        if (!StringUtils.hasText(name)) return null;
        return name.trim().toLowerCase();
    }

    /**
     * Meta requires city/state as lowercase a-z only — no spaces, accents, digits or
     * punctuation — before hashing: "Sétif" → "setif", "16 - Alger" → "alger".
     */
    private String normalizeGeo(String value) {
        if (!StringUtils.hasText(value)) return null;
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .toLowerCase()
            .replaceAll("[^a-z]", "");
        return normalized.isEmpty() ? null : normalized;
    }

    /** Meta requires zip codes lowercase with spaces and dashes removed. */
    private String normalizeZip(String value) {
        if (!StringUtils.hasText(value)) return null;
        String normalized = value.trim().toLowerCase().replaceAll("[^a-z0-9]", "");
        return normalized.isEmpty() ? null : normalized;
    }

    private void logEvent(String eventName) {
        pixelEventRepository.save(new PixelEvent(eventName, Instant.now()));
    }

    public List<PixelEventSummaryDTO> getRecentEventSummaries(String period) {
        Instant cutoff = computeCutoff(period);

        Map<String, Long> counts = new HashMap<>();
        for (Object[] row : pixelEventRepository.countByEventNameAfter(cutoff)) {
            counts.put((String) row[0], (Long) row[1]);
        }

        Map<String, Instant> lastSeen = new HashMap<>();
        for (Object[] row : pixelEventRepository.findLastSeenPerEventName()) {
            lastSeen.put((String) row[0], (Instant) row[1]);
        }

        return ALL_EVENT_NAMES.stream()
            .map(name -> new PixelEventSummaryDTO(name, counts.getOrDefault(name, 0L), lastSeen.get(name)))
            .collect(Collectors.toList());
    }

    private Instant computeCutoff(String period) {
        return switch (period) {
            case "DAYS_7"  -> Instant.now().minus(7, ChronoUnit.DAYS);
            case "DAYS_30" -> Instant.now().minus(30, ChronoUnit.DAYS);
            default        -> Instant.now().minus(24, ChronoUnit.HOURS);
        };
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
