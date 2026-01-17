package com.oussamabenberkane.espritlivre.service.shipping;

import com.oussamabenberkane.espritlivre.config.ShippingProperties;
import com.oussamabenberkane.espritlivre.domain.Order;
import com.oussamabenberkane.espritlivre.domain.OrderItem;
import com.oussamabenberkane.espritlivre.domain.enumeration.OrderItemType;
import com.oussamabenberkane.espritlivre.domain.enumeration.OrderStatus;
import com.oussamabenberkane.espritlivre.domain.enumeration.ShippingProvider;
import com.oussamabenberkane.espritlivre.service.dto.shipping.ShippingResult;
import com.oussamabenberkane.espritlivre.service.dto.shipping.YalidineParcelRequest;
import com.oussamabenberkane.espritlivre.service.dto.shipping.YalidineParcelResponse;
import com.oussamabenberkane.espritlivre.service.dto.shipping.YalidineWebhookPayload;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * Service implementation for Yalidine shipping provider.
 */
@Service
public class YalidineService implements ShippingProviderService {

    private static final Logger LOG = LoggerFactory.getLogger(YalidineService.class);

    private final ShippingProperties shippingProperties;
    private final RestTemplate restTemplate;

    public YalidineService(ShippingProperties shippingProperties) {
        this.shippingProperties = shippingProperties;
        this.restTemplate = new RestTemplate();
    }

    @Override
    public ShippingProvider getProvider() {
        return ShippingProvider.YALIDINE;
    }

    @Override
    public ShippingResult createParcel(Order order) {
        if (!shippingProperties.getYalidine().isEnabled()) {
            LOG.warn("Yalidine integration is disabled, skipping parcel creation for order: {}", order.getUniqueId());
            return ShippingResult.failure("Yalidine integration is disabled");
        }

        try {
            YalidineParcelRequest request = buildParcelRequest(order);
            LOG.debug("Creating Yalidine parcel for order {}: {}", order.getUniqueId(), request);

            HttpHeaders headers = createHeaders();
            HttpEntity<List<YalidineParcelRequest>> entity = new HttpEntity<>(List.of(request), headers);

            String url = shippingProperties.getYalidine().getBaseUrl() + "/parcels/";

            ResponseEntity<YalidineParcelResponse[]> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                YalidineParcelResponse[].class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null && response.getBody().length > 0) {
                YalidineParcelResponse parcelResponse = response.getBody()[0];
                LOG.info("Yalidine parcel created successfully for order {}: tracking={}",
                    order.getUniqueId(), parcelResponse.tracking());
                return ShippingResult.success(parcelResponse.tracking(), parcelResponse.label());
            } else {
                LOG.error("Yalidine API returned unexpected response for order {}: status={}",
                    order.getUniqueId(), response.getStatusCode());
                return ShippingResult.failure("Yalidine API returned unexpected response: " + response.getStatusCode());
            }
        } catch (HttpClientErrorException e) {
            LOG.error("Yalidine API client error for order {}: {} - {}",
                order.getUniqueId(), e.getStatusCode(), e.getResponseBodyAsString());
            return ShippingResult.failure("Yalidine API error: " + e.getResponseBodyAsString());
        } catch (HttpServerErrorException e) {
            LOG.error("Yalidine API server error for order {}: {} - {}",
                order.getUniqueId(), e.getStatusCode(), e.getResponseBodyAsString());
            return ShippingResult.failure("Yalidine server error: " + e.getStatusCode());
        } catch (Exception e) {
            LOG.error("Unexpected error creating Yalidine parcel for order {}", order.getUniqueId(), e);
            return ShippingResult.failure("Unexpected error: " + e.getMessage());
        }
    }

    @Override
    public boolean validateWebhook(YalidineWebhookPayload payload, String secret) {
        if (payload == null || payload.securityToken() == null || secret == null) {
            return false;
        }
        return Objects.equals(payload.securityToken(), secret);
    }

    @Override
    public OrderStatus mapProviderStatus(String statusCode, String event) {
        if (statusCode == null && event == null) {
            return null;
        }

        // Handle by event type first
        if (event != null) {
            switch (event.toLowerCase()) {
                case "delivered":
                    return OrderStatus.DELIVERED;
                case "in_transit":
                case "collected":
                    return OrderStatus.SHIPPED;
                case "returned":
                    return OrderStatus.CANCELLED;
                case "delivery_failed":
                    // Keep as SHIPPED, needs attention but not cancelled
                    return null;
            }
        }

        // Handle by status code
        if (statusCode != null) {
            switch (statusCode) {
                case "10": // En preparation
                    return OrderStatus.CONFIRMED;
                case "15": // En transit
                    return OrderStatus.SHIPPED;
                case "20": // Livre
                    return OrderStatus.DELIVERED;
                case "30": // Echec livraison - no status change
                    return null;
            }
        }

        return null;
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-API-ID", shippingProperties.getYalidine().getApiId());
        headers.set("X-API-TOKEN", shippingProperties.getYalidine().getApiToken());
        return headers;
    }

    private YalidineParcelRequest buildParcelRequest(Order order) {
        String[] nameParts = splitFullName(order.getFullName());

        return YalidineParcelRequest.builder()
            .orderId(order.getUniqueId())
            .firstname(nameParts[0])
            .familyname(nameParts[1])
            .contactPhone(formatPhoneForYalidine(order.getPhone()))
            .address(order.getStreetAddress())
            .fromWilayaName(shippingProperties.getOriginWilaya())
            .toWilayaName(order.getWilaya())
            .toCommuneName(order.getCity())
            .productList(buildProductList(order))
            .price(order.getTotalAmount())
            .freeshipping(isFreeShipping(order))
            .isStopdesk(order.getIsStopdesk() != null ? order.getIsStopdesk() : false)
            .stopdeskId(order.getStopdeskId())
            .hasExchange(false)
            .build();
    }

    /**
     * Split full name into first name and family name.
     * If only one word, use it as first name with empty family name.
     */
    private String[] splitFullName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return new String[]{"", ""};
        }

        String[] parts = fullName.trim().split("\\s+", 2);
        if (parts.length == 1) {
            return new String[]{parts[0], ""};
        }
        return parts;
    }

    /**
     * Format phone number for Yalidine API.
     * Yalidine expects format: 0XXXXXXXXX (10 digits starting with 0)
     * Our system stores as: +213XXXXXXXXX (E.164)
     */
    private String formatPhoneForYalidine(String phone) {
        if (phone == null) {
            return null;
        }
        // +213555123456 -> 0555123456
        if (phone.startsWith("+213")) {
            return "0" + phone.substring(4);
        }
        return phone;
    }

    /**
     * Build product list description from order items.
     */
    private String buildProductList(Order order) {
        if (order.getOrderItems() == null || order.getOrderItems().isEmpty()) {
            return "Books";
        }

        return order.getOrderItems().stream()
            .map(this::formatOrderItem)
            .collect(Collectors.joining(", "));
    }

    private String formatOrderItem(OrderItem item) {
        String title;
        if (item.getItemType() == OrderItemType.BOOK && item.getBook() != null) {
            title = item.getBook().getTitle();
        } else if (item.getItemType() == OrderItemType.PACK && item.getBookPack() != null) {
            title = item.getBookPack().getTitle() + " (Pack)";
        } else {
            title = "Item";
        }

        if (item.getQuantity() != null && item.getQuantity() > 1) {
            return title + " x" + item.getQuantity();
        }
        return title;
    }

    /**
     * Determine if shipping is free (shipping cost is zero or null).
     */
    private boolean isFreeShipping(Order order) {
        return order.getShippingCost() == null ||
               order.getShippingCost().compareTo(BigDecimal.ZERO) == 0;
    }
}
