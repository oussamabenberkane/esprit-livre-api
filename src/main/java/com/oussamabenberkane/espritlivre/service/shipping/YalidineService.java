package com.oussamabenberkane.espritlivre.service.shipping;

import com.oussamabenberkane.espritlivre.config.ShippingProperties;
import com.oussamabenberkane.espritlivre.domain.Order;
import com.oussamabenberkane.espritlivre.domain.OrderItem;
import com.oussamabenberkane.espritlivre.domain.enumeration.DeliveryFeeMethod;
import com.oussamabenberkane.espritlivre.domain.enumeration.OrderItemType;
import com.oussamabenberkane.espritlivre.domain.enumeration.OrderStatus;
import com.oussamabenberkane.espritlivre.domain.enumeration.ShippingProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oussamabenberkane.espritlivre.service.dto.shipping.DeliveryFeeResult;
import com.oussamabenberkane.espritlivre.service.dto.shipping.RelayPointDTO;
import com.oussamabenberkane.espritlivre.service.dto.shipping.ShippingResult;
import com.oussamabenberkane.espritlivre.service.dto.shipping.YalidineCentersResponse;
import com.oussamabenberkane.espritlivre.service.dto.shipping.YalidineFeesResponse;
import com.oussamabenberkane.espritlivre.service.dto.shipping.YalidineParcelRequest;
import com.oussamabenberkane.espritlivre.service.dto.shipping.YalidineParcelStatusResponse;
import com.oussamabenberkane.espritlivre.service.dto.shipping.YalidineParcelsResponse;
import com.oussamabenberkane.espritlivre.service.dto.shipping.YalidineWebhookPayload;
import com.oussamabenberkane.espritlivre.service.util.TextNormalizationUtils;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
    private final ObjectMapper objectMapper;

    public YalidineService(ShippingProperties shippingProperties) {
        this.shippingProperties = shippingProperties;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
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
            LOG.debug("Creating Yalidine parcel for order {}", order.getUniqueId());

            HttpHeaders headers = createHeaders();
            HttpEntity<List<YalidineParcelRequest>> entity = new HttpEntity<>(List.of(request), headers);

            String url = shippingProperties.getYalidine().getBaseUrl() + "/parcels/";

            // Response is an object with order_id as key: { "ORDER_ID": { success, tracking, label, ... } }
            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode parcelNode = root.get(order.getUniqueId());

                if (parcelNode != null) {
                    boolean success = parcelNode.has("success") && parcelNode.get("success").asBoolean();
                    String tracking = parcelNode.has("tracking") ? parcelNode.get("tracking").asText(null) : null;
                    String label = parcelNode.has("label") ? parcelNode.get("label").asText(null) : null;
                    String message = parcelNode.has("message") ? parcelNode.get("message").asText("") : "";

                    if (success && tracking != null) {
                        LOG.info("Yalidine parcel created successfully for order {}: tracking={}",
                            order.getUniqueId(), tracking);
                        return ShippingResult.success(tracking, label);
                    } else {
                        LOG.error("Yalidine parcel creation failed for order {}: {}",
                            order.getUniqueId(), message);
                        return ShippingResult.failure("Yalidine error: " + message);
                    }
                } else {
                    LOG.error("Yalidine API response missing order_id key for order {}: {}",
                        order.getUniqueId(), response.getBody());
                    return ShippingResult.failure("Yalidine API response missing order data");
                }
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

    @Override
    public Optional<OrderStatus> fetchOrderStatus(Order order) {
        if (order.getTrackingNumber() == null || order.getTrackingNumber().isBlank()) {
            return Optional.empty();
        }

        if (!shippingProperties.getYalidine().isEnabled()) {
            LOG.debug("Yalidine integration is disabled, skipping status fetch for order: {}", order.getUniqueId());
            return Optional.empty();
        }

        try {
            HttpHeaders headers = createHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            String url = shippingProperties.getYalidine().getBaseUrl() + "/parcels/" + order.getTrackingNumber();
            LOG.debug("Fetching Yalidine parcel status for tracking: {}", order.getTrackingNumber());

            ResponseEntity<YalidineParcelStatusResponse> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                YalidineParcelStatusResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String lastStatus = response.getBody().getLastStatus();
                if (lastStatus != null) {
                    OrderStatus mappedStatus = mapYalidineLastStatus(lastStatus);
                    if (mappedStatus != null) {
                        LOG.debug("Yalidine status for order {}: {} -> {}",
                            order.getUniqueId(), lastStatus, mappedStatus);
                        return Optional.of(mappedStatus);
                    }
                }
            }

            return Optional.empty();

        } catch (HttpClientErrorException e) {
            LOG.debug("Yalidine API error fetching status for order {}: {} - {}",
                order.getUniqueId(), e.getStatusCode(), e.getResponseBodyAsString());
            return Optional.empty();
        } catch (HttpServerErrorException e) {
            LOG.debug("Yalidine server error fetching status for order {}: {}",
                order.getUniqueId(), e.getStatusCode());
            return Optional.empty();
        } catch (Exception e) {
            LOG.debug("Error fetching Yalidine status for order {}: {}", order.getUniqueId(), e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Map<String, OrderStatus> fetchOrderStatuses(List<Order> orders) {
        Map<String, OrderStatus> results = new HashMap<>();

        if (orders == null || orders.isEmpty()) {
            return results;
        }

        if (!shippingProperties.getYalidine().isEnabled()) {
            LOG.debug("Yalidine integration is disabled, skipping batch status fetch");
            return results;
        }

        // Filter orders with valid tracking numbers
        List<String> trackingNumbers = orders.stream()
            .filter(o -> o.getTrackingNumber() != null && !o.getTrackingNumber().isBlank())
            .map(Order::getTrackingNumber)
            .toList();

        if (trackingNumbers.isEmpty()) {
            return results;
        }

        try {
            HttpHeaders headers = createHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            // Build URL with comma-separated tracking numbers
            String trackingParam = String.join(",", trackingNumbers);
            String url = shippingProperties.getYalidine().getBaseUrl() + "/parcels/?tracking=" + trackingParam;
            LOG.debug("Fetching Yalidine batch status for {} parcels", trackingNumbers.size());

            ResponseEntity<YalidineParcelsResponse> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                YalidineParcelsResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                YalidineParcelsResponse parcelsResponse = response.getBody();
                if (parcelsResponse.getData() != null) {
                    for (YalidineParcelStatusResponse parcel : parcelsResponse.getData()) {
                        if (parcel.getTracking() != null && parcel.getLastStatus() != null) {
                            OrderStatus mappedStatus = mapYalidineLastStatus(parcel.getLastStatus());
                            if (mappedStatus != null) {
                                results.put(parcel.getTracking(), mappedStatus);
                                LOG.debug("Yalidine batch status: {} -> {}", parcel.getTracking(), mappedStatus);
                            }
                        }
                    }
                }
            }

            LOG.debug("Yalidine batch fetch completed: {}/{} statuses retrieved",
                results.size(), trackingNumbers.size());
            return results;

        } catch (HttpClientErrorException e) {
            LOG.debug("Yalidine API error in batch fetch: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            return results;
        } catch (HttpServerErrorException e) {
            LOG.debug("Yalidine server error in batch fetch: {}", e.getStatusCode());
            return results;
        } catch (Exception e) {
            LOG.debug("Error in Yalidine batch status fetch: {}", e.getMessage());
            return results;
        }
    }

    /**
     * Map Yalidine last_status string to internal OrderStatus.
     *
     * Status mapping based on Yalidine API:
     * - CONFIRMED: En préparation, Pas encore expédié, Prêt à expédier, A vérifier, Pas encore ramassé
     * - SHIPPED: Ramassé, En transit, Expédié, Sorti en livraison, Centre, Transfert, Vers Wilaya,
     *            Reçu à Wilaya, Prêt pour livreur, En passation, Bloqué, Débloqué, En localisation,
     *            En attente du client, En attente, En alerte, Tentative échouée
     * - DELIVERED: Livré
     * - CANCELLED: Retourné au vendeur, Echèc livraison, Retour vers centre, Retourné au centre,
     *              Retour transfert, Retour groupé, Retour à retirer, Retour vers vendeur, Echange échoué
     */
    private OrderStatus mapYalidineLastStatus(String lastStatus) {
        if (lastStatus == null || lastStatus.isBlank()) {
            return null;
        }

        // Normalize: lowercase and trim
        String normalized = lastStatus.toLowerCase().trim();

        // CONFIRMED statuses (preparation phase)
        if (normalized.contains("préparation") ||
            normalized.equals("pas encore expédié") ||
            normalized.equals("prêt à expédier") ||
            normalized.equals("a vérifier") ||
            normalized.equals("pas encore ramassé")) {
            return OrderStatus.CONFIRMED;
        }

        // DELIVERED status
        if (normalized.equals("livré")) {
            return OrderStatus.DELIVERED;
        }

        // CANCELLED/RETURNED statuses
        if (normalized.contains("retour") ||
            normalized.contains("retourné") ||
            normalized.equals("echèc livraison") ||
            normalized.equals("echange échoué")) {
            return OrderStatus.CANCELLED;
        }

        // SHIPPED statuses (in transit)
        if (normalized.equals("ramassé") ||
            normalized.contains("transit") ||
            normalized.equals("expédié") ||
            normalized.contains("livraison") ||
            normalized.equals("centre") ||
            normalized.equals("transfert") ||
            normalized.contains("wilaya") ||
            normalized.equals("prêt pour livreur") ||
            normalized.equals("en passation") ||
            normalized.equals("bloqué") ||
            normalized.equals("débloqué") ||
            normalized.equals("en localisation") ||
            normalized.contains("attente") ||
            normalized.equals("en alerte") ||
            normalized.contains("échouée")) {
            return OrderStatus.SHIPPED;
        }

        LOG.debug("Unknown Yalidine status: {}", lastStatus);
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
        BigDecimal price = adjustPriceForYalidine(order);
        int priceInt = price != null ? price.intValue() : 0;

        return YalidineParcelRequest.builder()
            .orderId(order.getUniqueId())
            .firstName(nameParts[0])
            .familyName(nameParts[1])
            .contactPhone(formatPhoneForYalidine(order.getPhone()))
            .address(order.getStreetAddress() != null ? order.getStreetAddress() : "N/A")
            .fromWilayaName(shippingProperties.getOriginWilaya())
            .toWilayaName(order.getWilaya())
            .toCommuneName(order.getCity())
            .productList(buildProductList(order))
            .price(price)
            .freeShipping(isFreeShipping(order))
            .isStopDesk(order.getIsStopDesk() != null ? order.getIsStopDesk() : false)
            .stopDeskId(order.getStopDeskId())
            .hasExchange(false)
            .doInsurance(false)
            .declaredValue(priceInt)
            .build();
    }

    /**
     * Adjust the price sent to Yalidine.
     * If the order used automatic delivery fee calculation, the price is already correct.
     * If the order used a fixed delivery fee, we need to swap our fee for Yalidine's actual fee:
     *   yalidinePrice = totalAmount - ourShippingCost + yalidineActualFee
     */
    private BigDecimal adjustPriceForYalidine(Order order) {
        BigDecimal totalAmount = order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO;

        if (order.getDeliveryFeeMethod() == DeliveryFeeMethod.AUTOMATIC) {
            return totalAmount;
        }

        // Fixed fee: swap our shipping cost for Yalidine's actual fee
        boolean isStopDesk = Boolean.TRUE.equals(order.getIsStopDesk());
        Integer wilayaId = getWilayaIdFromName(order.getWilaya());
        DeliveryFeeResult yalidineResult = getDeliveryFee(wilayaId, order.getCity(), isStopDesk);

        if (!yalidineResult.isSuccess()) {
            LOG.warn("Could not fetch Yalidine fee for order {}, using original totalAmount", order.getUniqueId());
            return totalAmount;
        }

        BigDecimal ourShippingCost = order.getShippingCost() != null ? order.getShippingCost() : BigDecimal.ZERO;
        BigDecimal yalidineActualFee = yalidineResult.getFee();
        BigDecimal adjustedPrice = totalAmount.subtract(ourShippingCost).add(yalidineActualFee);

        LOG.info("Yalidine price adjusted for order {}: {} -> {} (ourFee={}, yalidineFee={})",
            order.getUniqueId(), totalAmount, adjustedPrice, ourShippingCost, yalidineActualFee);

        return adjustedPrice;
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

    /**
     * Get all centers (relay points) from Yalidine.
     *
     * @return List of relay points
     */
    public List<RelayPointDTO> getCenters() {
        return getCenters(null);
    }

    /**
     * Get centers (relay points) from Yalidine, optionally filtered by wilaya.
     *
     * @param wilayaId Optional wilaya ID to filter by
     * @return List of relay points
     */
    public List<RelayPointDTO> getCenters(Integer wilayaId) {
        if (!shippingProperties.getYalidine().isEnabled()) {
            LOG.warn("Yalidine integration is disabled");
            return Collections.emptyList();
        }

        try {
            HttpHeaders headers = createHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            String url = shippingProperties.getYalidine().getBaseUrl() + "/centers/";
            if (wilayaId != null) {
                url += "?wilaya_id=" + wilayaId;
            }

            LOG.debug("Fetching Yalidine centers from: {}", url);

            ResponseEntity<YalidineCentersResponse> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                YalidineCentersResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                YalidineCentersResponse centersResponse = response.getBody();
                if (centersResponse.getData() != null) {
                    LOG.info("Retrieved {} centers from Yalidine", centersResponse.getData().size());
                    return centersResponse.getData().stream()
                        .map(YalidineCentersResponse.YalidineCenter::toRelayPointDTO)
                        .collect(Collectors.toList());
                }
            }

            LOG.warn("Empty or unsuccessful response from Yalidine centers API");
            return Collections.emptyList();

        } catch (HttpClientErrorException e) {
            LOG.error("Yalidine API client error fetching centers: {} - {}",
                e.getStatusCode(), e.getResponseBodyAsString());
            return Collections.emptyList();
        } catch (HttpServerErrorException e) {
            LOG.error("Yalidine API server error fetching centers: {} - {}",
                e.getStatusCode(), e.getResponseBodyAsString());
            return Collections.emptyList();
        } catch (Exception e) {
            LOG.error("Unexpected error fetching Yalidine centers", e);
            return Collections.emptyList();
        }
    }

    /**
     * Get a specific center by ID.
     *
     * @param centerId The center ID
     * @return The relay point, or null if not found
     */
    public RelayPointDTO getCenterById(Integer centerId) {
        if (!shippingProperties.getYalidine().isEnabled()) {
            LOG.warn("Yalidine integration is disabled");
            return null;
        }

        if (centerId == null) {
            return null;
        }

        try {
            HttpHeaders headers = createHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            String url = shippingProperties.getYalidine().getBaseUrl() + "/centers/" + centerId;

            LOG.debug("Fetching Yalidine center by ID: {}", centerId);

            ResponseEntity<YalidineCentersResponse> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                YalidineCentersResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                YalidineCentersResponse centersResponse = response.getBody();
                if (centersResponse.getData() != null && !centersResponse.getData().isEmpty()) {
                    return centersResponse.getData().get(0).toRelayPointDTO();
                }
            }

            LOG.warn("Center not found with ID: {}", centerId);
            return null;

        } catch (HttpClientErrorException e) {
            LOG.error("Yalidine API client error fetching center {}: {} - {}",
                centerId, e.getStatusCode(), e.getResponseBodyAsString());
            return null;
        } catch (HttpServerErrorException e) {
            LOG.error("Yalidine API server error fetching center {}: {} - {}",
                centerId, e.getStatusCode(), e.getResponseBodyAsString());
            return null;
        } catch (Exception e) {
            LOG.error("Unexpected error fetching Yalidine center {}", centerId, e);
            return null;
        }
    }

    /**
     * Search centers by query string with accent-insensitive matching.
     * Supports searching "bejaia" to find "Béjaia", "tizi" to find "Tizi-Ouzou", etc.
     *
     * @param query Search query (searches in name, address, commune name)
     * @param wilayaId Optional wilaya ID to filter by
     * @return List of matching relay points
     */
    public List<RelayPointDTO> searchCenters(String query, Integer wilayaId) {
        // Get centers (optionally filtered by wilaya)
        List<RelayPointDTO> centers = getCenters(wilayaId);

        // If no query, return all
        if (query == null || query.isBlank()) {
            return centers;
        }

        // Filter by query with accent-insensitive matching
        String normalizedQuery = TextNormalizationUtils.normalizeForSearch(query);
        return centers.stream()
            .filter(c -> {
                boolean matches = false;
                if (c.getName() != null) {
                    matches = TextNormalizationUtils.normalizeForSearch(c.getName()).contains(normalizedQuery);
                }
                if (!matches && c.getAddress() != null) {
                    matches = TextNormalizationUtils.normalizeForSearch(c.getAddress()).contains(normalizedQuery);
                }
                if (!matches && c.getCommuneName() != null) {
                    matches = TextNormalizationUtils.normalizeForSearch(c.getCommuneName()).contains(normalizedQuery);
                }
                return matches;
            })
            .collect(Collectors.toList());
    }

    /**
     * Get delivery fee for a specific destination.
     *
     * @param toWilayaId Destination wilaya ID (1-58)
     * @param communeName Commune name within the wilaya
     * @param isStopDesk Whether this is a stop desk (relay point) delivery
     * @return DeliveryFeeResult with the calculated fee or error
     */
    public DeliveryFeeResult getDeliveryFee(Integer toWilayaId, String communeName, boolean isStopDesk) {
        if (!shippingProperties.getYalidine().isEnabled()) {
            LOG.warn("Yalidine integration is disabled");
            return DeliveryFeeResult.failure("Yalidine integration is disabled");
        }

        if (toWilayaId == null) {
            return DeliveryFeeResult.failure("Destination wilaya ID is required");
        }

        try {
            HttpHeaders headers = createHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            // Get origin wilaya ID from config (Bejaia = 6 by default)
            Integer fromWilayaId = getWilayaIdFromName(shippingProperties.getOriginWilaya());

            String url = shippingProperties.getYalidine().getBaseUrl() +
                "/fees/?from_wilaya_id=" + fromWilayaId + "&to_wilaya_id=" + toWilayaId;

            LOG.debug("Fetching Yalidine fees from: {}", url);

            ResponseEntity<YalidineFeesResponse> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                YalidineFeesResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                YalidineFeesResponse feesResponse = response.getBody();

                // Try to find the commune's fee
                BigDecimal fee = feesResponse.getDeliveryFeeByName(communeName, isStopDesk);

                if (fee != null) {
                    LOG.debug("Yalidine delivery fee for wilaya {} commune {}: {} DA (stopDesk={})",
                        toWilayaId, communeName, fee, isStopDesk);
                    return DeliveryFeeResult.automatic(fee, ShippingProvider.YALIDINE);
                } else {
                    // If commune not found, try to get any commune's fee as fallback
                    if (feesResponse.getPerCommune() != null && !feesResponse.getPerCommune().isEmpty()) {
                        YalidineFeesResponse.CommuneFees firstCommune =
                            feesResponse.getPerCommune().values().iterator().next();
                        Integer fallbackFee = isStopDesk ? firstCommune.getExpressDesk() : firstCommune.getExpressHome();
                        if (fallbackFee != null) {
                            LOG.warn("Commune '{}' not found in wilaya {}, using fallback fee: {} DA",
                                communeName, toWilayaId, fallbackFee);
                            return DeliveryFeeResult.automatic(BigDecimal.valueOf(fallbackFee), ShippingProvider.YALIDINE);
                        }
                    }
                    return DeliveryFeeResult.failure("Could not find delivery fee for commune: " + communeName);
                }
            } else {
                LOG.error("Yalidine fees API returned unexpected response: status={}", response.getStatusCode());
                return DeliveryFeeResult.failure("Yalidine API returned unexpected response");
            }

        } catch (HttpClientErrorException e) {
            LOG.error("Yalidine fees API client error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            return DeliveryFeeResult.failure("Yalidine API error: " + e.getResponseBodyAsString());
        } catch (HttpServerErrorException e) {
            LOG.error("Yalidine fees API server error: {}", e.getStatusCode());
            return DeliveryFeeResult.failure("Yalidine server error");
        } catch (Exception e) {
            LOG.error("Error fetching Yalidine delivery fees", e);
            return DeliveryFeeResult.failure("Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Get wilaya ID from wilaya name.
     * Algeria has 58 wilayas, numbered 1-58.
     */
    private Integer getWilayaIdFromName(String wilayaName) {
        if (wilayaName == null) {
            return 6; // Default to Bejaia
        }

        // Map of common wilaya names to IDs
        String normalized = wilayaName.toLowerCase().trim();
        return switch (normalized) {
            case "adrar" -> 1;
            case "chlef" -> 2;
            case "laghouat" -> 3;
            case "oum el bouaghi" -> 4;
            case "batna" -> 5;
            case "bejaia", "béjaïa" -> 6;
            case "biskra" -> 7;
            case "bechar", "béchar" -> 8;
            case "blida" -> 9;
            case "bouira" -> 10;
            case "tamanrasset" -> 11;
            case "tebessa", "tébessa" -> 12;
            case "tlemcen" -> 13;
            case "tiaret" -> 14;
            case "tizi ouzou" -> 15;
            case "alger", "algiers" -> 16;
            case "djelfa" -> 17;
            case "jijel" -> 18;
            case "setif", "sétif" -> 19;
            case "saida", "saïda" -> 20;
            case "skikda" -> 21;
            case "sidi bel abbes", "sidi bel abbès" -> 22;
            case "annaba" -> 23;
            case "guelma" -> 24;
            case "constantine" -> 25;
            case "medea", "médéa" -> 26;
            case "mostaganem" -> 27;
            case "m'sila", "msila" -> 28;
            case "mascara" -> 29;
            case "ouargla" -> 30;
            case "oran" -> 31;
            case "el bayadh" -> 32;
            case "illizi" -> 33;
            case "bordj bou arreridj" -> 34;
            case "boumerdes", "boumerdès" -> 35;
            case "el tarf" -> 36;
            case "tindouf" -> 37;
            case "tissemsilt" -> 38;
            case "el oued" -> 39;
            case "khenchela" -> 40;
            case "souk ahras" -> 41;
            case "tipaza" -> 42;
            case "mila" -> 43;
            case "ain defla", "aïn defla" -> 44;
            case "naama", "naâma" -> 45;
            case "ain temouchent", "aïn témouchent" -> 46;
            case "ghardaia", "ghardaïa" -> 47;
            case "relizane" -> 48;
            case "timimoun" -> 49;
            case "bordj badji mokhtar" -> 50;
            case "ouled djellal" -> 51;
            case "beni abbes", "béni abbès" -> 52;
            case "in salah" -> 53;
            case "in guezzam" -> 54;
            case "touggourt" -> 55;
            case "djanet" -> 56;
            case "el meghaier", "el m'ghair" -> 57;
            case "el meniaa" -> 58;
            default -> 6; // Default to Bejaia
        };
    }
}
