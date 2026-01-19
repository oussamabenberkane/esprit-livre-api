package com.oussamabenberkane.espritlivre.service.shipping;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oussamabenberkane.espritlivre.config.ShippingProperties;
import com.oussamabenberkane.espritlivre.domain.Order;
import com.oussamabenberkane.espritlivre.domain.OrderItem;
import com.oussamabenberkane.espritlivre.domain.enumeration.OrderItemType;
import com.oussamabenberkane.espritlivre.domain.enumeration.OrderStatus;
import com.oussamabenberkane.espritlivre.domain.enumeration.ShippingProvider;
import com.oussamabenberkane.espritlivre.service.dto.shipping.*;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * Service implementation for ZR Express shipping provider.
 */
@Service
public class ZrExpressService implements ShippingProviderService {

    private static final Logger LOG = LoggerFactory.getLogger(ZrExpressService.class);

    private static final String DELIVERY_TYPE_HOME = "home";
    private static final String DELIVERY_TYPE_PICKUP_POINT = "pickup-point";

    private final ShippingProperties shippingProperties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    // Territory cache: wilaya name -> territory UUID
    private final Map<String, String> cityTerritoryCache = new ConcurrentHashMap<>();
    // District cache: cityTerritoryId -> (commune name -> territory UUID)
    private final Map<String, Map<String, String>> districtTerritoryCache = new ConcurrentHashMap<>();

    public ZrExpressService(ShippingProperties shippingProperties) {
        this.shippingProperties = shippingProperties;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public ShippingProvider getProvider() {
        return ShippingProvider.ZR;
    }

    @Override
    public ShippingResult createParcel(Order order) {
        if (!shippingProperties.getZrExpress().isEnabled()) {
            LOG.warn("ZR Express integration is disabled, skipping parcel creation for order: {}", order.getUniqueId());
            return ShippingResult.failure("ZR Express integration is disabled");
        }

        try {
            // 1. Resolve territory UUIDs from wilaya/city names
            String cityTerritoryId = resolveCityTerritoryId(order.getWilaya());
            if (cityTerritoryId == null) {
                LOG.error("Could not resolve city territory ID for wilaya: {} for order: {}", order.getWilaya(), order.getUniqueId());
                return ShippingResult.failure("Could not resolve wilaya: " + order.getWilaya());
            }

            String districtTerritoryId = resolveDistrictTerritoryId(cityTerritoryId, order.getCity());
            if (districtTerritoryId == null) {
                LOG.error("Could not resolve district territory ID for city: {} in wilaya: {} for order: {}",
                    order.getCity(), order.getWilaya(), order.getUniqueId());
                return ShippingResult.failure("Could not resolve commune: " + order.getCity());
            }

            // 2. Find or create customer (uses User info if available, falls back to Order)
            String customerId = findOrCreateCustomer(order, cityTerritoryId, districtTerritoryId);
            if (customerId == null) {
                LOG.error("Could not find or create customer for order: {}", order.getUniqueId());
                return ShippingResult.failure("Could not create customer in ZR Express");
            }

            // 3. Build parcel request
            ZrExpressParcelRequest request = buildParcelRequest(order, customerId, cityTerritoryId, districtTerritoryId);
            LOG.debug("Creating ZR Express parcel for order {}", order.getUniqueId());

            // Log the request for debugging
            try {
                String requestJson = objectMapper.writeValueAsString(request);
                LOG.debug("ZR Express parcel request: {}", requestJson);
            } catch (Exception e) {
                LOG.warn("Could not serialize request for logging", e);
            }

            // 4. POST to /parcels
            HttpHeaders headers = createHeaders();
            HttpEntity<ZrExpressParcelRequest> entity = new HttpEntity<>(request, headers);

            String url = shippingProperties.getZrExpress().getBaseUrl() + "/parcels";

            ResponseEntity<ZrExpressParcelResponse> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                ZrExpressParcelResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String parcelId = response.getBody().id();
                LOG.info("ZR Express parcel created for order {}: parcelId={}", order.getUniqueId(), parcelId);

                // 5. GET /parcels/{id} to fetch tracking number
                String trackingNumber = fetchTrackingNumber(parcelId);
                if (trackingNumber != null) {
                    LOG.info("ZR Express tracking number retrieved for order {}: tracking={}",
                        order.getUniqueId(), trackingNumber);
                    // ZR Express doesn't return label URL in create response - can be fetched separately
                    return ShippingResult.success(trackingNumber, null);
                } else {
                    // Parcel created but couldn't get tracking - still successful
                    LOG.warn("ZR Express parcel created but could not retrieve tracking number for order {}", order.getUniqueId());
                    return ShippingResult.success(parcelId, null);
                }
            } else {
                LOG.error("ZR Express API returned unexpected response for order {}: status={}",
                    order.getUniqueId(), response.getStatusCode());
                return ShippingResult.failure("ZR Express API returned unexpected response: " + response.getStatusCode());
            }

        } catch (HttpClientErrorException e) {
            LOG.error("ZR Express API client error for order {}: {} - {}",
                order.getUniqueId(), e.getStatusCode(), e.getResponseBodyAsString());
            return ShippingResult.failure("ZR Express API error: " + e.getResponseBodyAsString());
        } catch (HttpServerErrorException e) {
            LOG.error("ZR Express API server error for order {}: {} - {}",
                order.getUniqueId(), e.getStatusCode(), e.getResponseBodyAsString());
            return ShippingResult.failure("ZR Express server error: " + e.getStatusCode());
        } catch (Exception e) {
            LOG.error("Unexpected error creating ZR Express parcel for order {}", order.getUniqueId(), e);
            return ShippingResult.failure("Unexpected error: " + e.getMessage());
        }
    }

    @Override
    public boolean validateWebhook(YalidineWebhookPayload payload, String secret) {
        // ZR Express webhooks deferred to later phase
        return true;
    }

    @Override
    public OrderStatus mapProviderStatus(String statusCode, String event) {
        // ZR Express status mapping deferred to later phase
        return null;
    }

    /**
     * Get hubs (relay points/pickup points) from ZR Express.
     *
     * @param wilayaName Optional wilaya name to filter by (e.g., "Bejaia", "Alger")
     * @return List of relay points
     */
    public List<RelayPointDTO> getHubs(String wilayaName) {
        if (!shippingProperties.getZrExpress().isEnabled()) {
            LOG.warn("ZR Express integration is disabled ");
            return Collections.emptyList();
        }

        try {
            HttpHeaders headers = createHeaders();

            // Build search request with filter for pickup points only
            Map<String, Object> filters = new HashMap<>();
            filters.put("isPickupPoint", true);

            ZrExpressSearchRequest searchRequest = ZrExpressSearchRequest.builder()
                .filters(filters)
                .pageSize(100)
                .build();

            HttpEntity<ZrExpressSearchRequest> entity = new HttpEntity<>(searchRequest, headers);

            String url = shippingProperties.getZrExpress().getBaseUrl() + "/hubs/search";

            LOG.debug("Fetching ZR Express hubs from: {}", url);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                // ZR Express API returns data in "items" field
                Object itemsObj = body.get("items");
                if (itemsObj != null) {
                    List<ZrExpressHubResponse> hubs = objectMapper.convertValue(
                        itemsObj,
                        new TypeReference<List<ZrExpressHubResponse>>() {}
                    );
                    LOG.info("Retrieved {} hubs from ZR Express", hubs.size());

                    // Filter by wilaya name client-side if provided
                    return hubs.stream()
                        .map(ZrExpressHubResponse::toRelayPointDTO)
                        .filter(hub -> wilayaName == null || wilayaName.isBlank() ||
                            matchesWilayaName(hub.getWilayaName(), wilayaName))
                        .collect(Collectors.toList());
                }
            }

            LOG.warn("Empty or unsuccessful response from ZR Express hubs API");
            return Collections.emptyList();

        } catch (HttpClientErrorException e) {
            LOG.error("ZR Express API client error fetching hubs: {} - {}",
                e.getStatusCode(), e.getResponseBodyAsString());
            return Collections.emptyList();
        } catch (HttpServerErrorException e) {
            LOG.error("ZR Express API server error fetching hubs: {} - {}",
                e.getStatusCode(), e.getResponseBodyAsString());
            return Collections.emptyList();
        } catch (Exception e) {
            LOG.error("Unexpected error fetching ZR Express hubs", e);
            return Collections.emptyList();
        }
    }

    /**
     * Check if hub's wilaya matches the requested wilaya name.
     * Handles variations like "Bejaia" vs "Béjaia", partial matches, etc.
     */
    private boolean matchesWilayaName(String hubWilaya, String requestedWilaya) {
        if (hubWilaya == null || requestedWilaya == null) {
            return false;
        }
        String normalizedHub = normalizeLocationName(hubWilaya);
        String normalizedRequest = normalizeLocationName(requestedWilaya);
        return normalizedHub.contains(normalizedRequest) || normalizedRequest.contains(normalizedHub);
    }

    /**
     * Get a specific hub by ID.
     *
     * @param hubId The hub UUID
     * @return The relay point, or null if not found
     */
    public RelayPointDTO getHubById(String hubId) {
        if (!shippingProperties.getZrExpress().isEnabled()) {
            LOG.warn("ZR Express integration is disabled");
            return null;
        }

        if (hubId == null || hubId.isBlank()) {
            return null;
        }

        try {
            HttpHeaders headers = createHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            String url = shippingProperties.getZrExpress().getBaseUrl() + "/hubs/" + hubId;

            LOG.debug("Fetching ZR Express hub by ID: {}", hubId);

            ResponseEntity<ZrExpressHubResponse> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                ZrExpressHubResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody().toRelayPointDTO();
            }

            LOG.warn("Hub not found with ID: {}", hubId);
            return null;

        } catch (HttpClientErrorException e) {
            LOG.error("ZR Express API client error fetching hub {}: {} - {}",
                hubId, e.getStatusCode(), e.getResponseBodyAsString());
            return null;
        } catch (HttpServerErrorException e) {
            LOG.error("ZR Express API server error fetching hub {}: {} - {}",
                hubId, e.getStatusCode(), e.getResponseBodyAsString());
            return null;
        } catch (Exception e) {
            LOG.error("Unexpected error fetching ZR Express hub {}", hubId, e);
            return null;
        }
    }

    /**
     * Search hubs by query string.
     *
     * @param query Search query (searches in name)
     * @param wilayaName Optional wilaya name to filter by
     * @return List of matching relay points
     */
    public List<RelayPointDTO> searchHubs(String query, String wilayaName) {
        // Get hubs (optionally filtered by wilaya)
        List<RelayPointDTO> hubs = getHubs(wilayaName);

        // If no query, return all
        if (query == null || query.isBlank()) {
            return hubs;
        }

        // Filter by query (search in name, address, commune name)
        String lowerQuery = query.toLowerCase();
        return hubs.stream()
            .filter(h -> {
                boolean matches = false;
                if (h.getName() != null) {
                    matches = h.getName().toLowerCase().contains(lowerQuery);
                }
                if (!matches && h.getAddress() != null) {
                    matches = h.getAddress().toLowerCase().contains(lowerQuery);
                }
                if (!matches && h.getCommuneName() != null) {
                    matches = h.getCommuneName().toLowerCase().contains(lowerQuery);
                }
                return matches;
            })
            .collect(Collectors.toList());
    }

    /**
     * Resolve city territory UUID from wilaya name.
     * Uses cached data from hubs to resolve territories.
     */
    private String resolveCityTerritoryId(String wilayaName) {
        if (wilayaName == null || wilayaName.isBlank()) {
            return null;
        }

        // Check cache first
        String normalizedName = normalizeLocationName(wilayaName);
        if (cityTerritoryCache.containsKey(normalizedName)) {
            return cityTerritoryCache.get(normalizedName);
        }

        // Build cache from hubs if empty
        if (cityTerritoryCache.isEmpty()) {
            buildTerritoryCacheFromHubs();
        }

        // Try again after building cache
        if (cityTerritoryCache.containsKey(normalizedName)) {
            return cityTerritoryCache.get(normalizedName);
        }

        // Try partial match
        for (Map.Entry<String, String> entry : cityTerritoryCache.entrySet()) {
            if (entry.getKey().contains(normalizedName) || normalizedName.contains(entry.getKey())) {
                LOG.debug("Resolved wilaya '{}' to territory ID (partial match): {}", wilayaName, entry.getValue());
                return entry.getValue();
            }
        }

        LOG.warn("Could not resolve city territory for wilaya: {}", wilayaName);
        return null;
    }

    /**
     * Build territory cache from hub data.
     * Hubs contain cityTerritoryId, city, districtTerritoryId, district fields.
     */
    private void buildTerritoryCacheFromHubs() {
        LOG.debug("Building territory cache from hubs...");
        try {
            List<ZrExpressHubResponse> hubs = fetchAllHubsRaw();
            for (ZrExpressHubResponse hub : hubs) {
                if (hub.getAddress() != null) {
                    // Cache city (wilaya) territory
                    if (hub.getAddress().getCity() != null && hub.getAddress().getCityTerritoryId() != null) {
                        String normalizedCity = normalizeLocationName(hub.getAddress().getCity());
                        cityTerritoryCache.putIfAbsent(normalizedCity, hub.getAddress().getCityTerritoryId());
                    }

                    // Cache district (commune) territory
                    if (hub.getAddress().getCityTerritoryId() != null &&
                        hub.getAddress().getDistrict() != null &&
                        hub.getAddress().getDistrictTerritoryId() != null) {

                        String cityTerritoryId = hub.getAddress().getCityTerritoryId();
                        String normalizedDistrict = normalizeLocationName(hub.getAddress().getDistrict());

                        Map<String, String> cityDistrictCache = districtTerritoryCache.computeIfAbsent(
                            cityTerritoryId,
                            k -> new ConcurrentHashMap<>()
                        );
                        cityDistrictCache.putIfAbsent(normalizedDistrict, hub.getAddress().getDistrictTerritoryId());
                    }
                }
            }
            LOG.info("Built territory cache: {} cities, {} districts across cities",
                cityTerritoryCache.size(), districtTerritoryCache.values().stream().mapToInt(Map::size).sum());
        } catch (Exception e) {
            LOG.error("Error building territory cache from hubs", e);
        }
    }

    /**
     * Fetch all hubs raw (without filtering/conversion).
     */
    private List<ZrExpressHubResponse> fetchAllHubsRaw() {
        try {
            HttpHeaders headers = createHeaders();

            ZrExpressSearchRequest searchRequest = ZrExpressSearchRequest.builder()
                .pageSize(200)
                .build();

            HttpEntity<ZrExpressSearchRequest> entity = new HttpEntity<>(searchRequest, headers);
            String url = shippingProperties.getZrExpress().getBaseUrl() + "/hubs/search";

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Object itemsObj = response.getBody().get("items");
                if (itemsObj != null) {
                    return objectMapper.convertValue(itemsObj, new TypeReference<List<ZrExpressHubResponse>>() {});
                }
            }
        } catch (Exception e) {
            LOG.error("Error fetching hubs for territory cache", e);
        }
        return Collections.emptyList();
    }

    /**
     * Resolve district territory UUID from commune name within a city.
     * Uses cached data from hubs to resolve territories.
     */
    private String resolveDistrictTerritoryId(String cityTerritoryId, String communeName) {
        if (cityTerritoryId == null || communeName == null || communeName.isBlank()) {
            return null;
        }

        // Build cache from hubs if empty
        if (cityTerritoryCache.isEmpty()) {
            buildTerritoryCacheFromHubs();
        }

        // Check cache
        String normalizedName = normalizeLocationName(communeName);
        Map<String, String> cityDistrictCache = districtTerritoryCache.get(cityTerritoryId);

        if (cityDistrictCache != null) {
            // Exact match
            if (cityDistrictCache.containsKey(normalizedName)) {
                return cityDistrictCache.get(normalizedName);
            }

            // Try partial match within this city's districts
            for (Map.Entry<String, String> entry : cityDistrictCache.entrySet()) {
                if (entry.getKey().contains(normalizedName) || normalizedName.contains(entry.getKey())) {
                    LOG.debug("Resolved commune '{}' to territory ID (partial match): {}", communeName, entry.getValue());
                    return entry.getValue();
                }
            }
        }

        // If not found in specific city, try all districts (commune might be mismatched with wilaya)
        for (Map<String, String> districtCache : districtTerritoryCache.values()) {
            if (districtCache.containsKey(normalizedName)) {
                LOG.debug("Resolved commune '{}' to territory ID (cross-city match): {}", communeName, districtCache.get(normalizedName));
                return districtCache.get(normalizedName);
            }
            for (Map.Entry<String, String> entry : districtCache.entrySet()) {
                if (entry.getKey().contains(normalizedName) || normalizedName.contains(entry.getKey())) {
                    LOG.debug("Resolved commune '{}' to territory ID (cross-city partial match): {}", communeName, entry.getValue());
                    return entry.getValue();
                }
            }
        }

        LOG.warn("Could not resolve district territory for commune: {} in city: {}", communeName, cityTerritoryId);
        return null;
    }

    /**
     * Fetch tracking number for a parcel by ID.
     */
    private String fetchTrackingNumber(String parcelId) {
        try {
            HttpHeaders headers = createHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            String url = shippingProperties.getZrExpress().getBaseUrl() + "/parcels/" + parcelId;

            ResponseEntity<ZrExpressGetParcelResponse> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                ZrExpressGetParcelResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody().getTrackingNumber();
            }

            return null;

        } catch (Exception e) {
            LOG.error("Error fetching tracking number for parcel {}", parcelId, e);
            return null;
        }
    }

    /**
     * Create HTTP headers with ZR Express authentication.
     */
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Tenant", shippingProperties.getZrExpress().getTenantId());
        headers.set("X-Api-Key", shippingProperties.getZrExpress().getApiKey());
        return headers;
    }

    /**
     * Build parcel request from order.
     * Uses User info if available, falls back to Order info.
     */
    private ZrExpressParcelRequest buildParcelRequest(Order order, String customerId, String cityTerritoryId, String districtTerritoryId) {
        boolean isPickupPoint = Boolean.TRUE.equals(order.getIsStopDesk());
        String deliveryType = isPickupPoint ? DELIVERY_TYPE_PICKUP_POINT : DELIVERY_TYPE_HOME;

        // Get customer info (prefers User, falls back to Order)
        String customerName = getCustomerName(order);
        String customerPhone = formatPhoneForZrExpress(getCustomerPhone(order));
        String streetAddress = getCustomerStreetAddress(order);

        // Build ordered products list from order items
        List<ZrExpressParcelRequest.ZrOrderedProduct> orderedProducts = buildOrderedProducts(order);

        ZrExpressParcelRequest.Builder builder = ZrExpressParcelRequest.builder()
            .customer(customerId, customerName, customerPhone)
            .deliveryAddress(cityTerritoryId, districtTerritoryId, streetAddress != null ? streetAddress : "N/A")
            .deliveryType(deliveryType)
            .amount(order.getTotalAmount() != null ? order.getTotalAmount().doubleValue() : 0.0)
            .weight(1.0) // Default weight for books
            .externalId(order.getUniqueId())
            .description(buildProductList(order))
            .orderedProducts(orderedProducts);

        // If pickup point delivery, include hub ID
        if (isPickupPoint && order.getStopDeskId() != null) {
            builder.hubId(order.getStopDeskId());
        }

        return builder.build();
    }

    /**
     * Build ordered products list from order items.
     * StockType "none" means we ship our own stock (not using ZR warehouse).
     */
    private List<ZrExpressParcelRequest.ZrOrderedProduct> buildOrderedProducts(Order order) {
        if (order.getOrderItems() == null || order.getOrderItems().isEmpty()) {
            // Return at least one product to satisfy API requirement
            return List.of(new ZrExpressParcelRequest.ZrOrderedProduct("Books", 1,
                order.getTotalAmount() != null ? order.getTotalAmount().doubleValue() : 0.0, "none"));
        }

        return order.getOrderItems().stream()
            .map(item -> {
                String title;
                if (item.getItemType() == OrderItemType.BOOK && item.getBook() != null) {
                    title = item.getBook().getTitle();
                } else if (item.getItemType() == OrderItemType.PACK && item.getBookPack() != null) {
                    title = item.getBookPack().getTitle() + " (Pack)";
                } else {
                    title = "Item";
                }
                int quantity = item.getQuantity() != null ? item.getQuantity() : 1;
                double unitPrice = item.getUnitPrice() != null ? item.getUnitPrice().doubleValue() : 0.0;
                return new ZrExpressParcelRequest.ZrOrderedProduct(title, quantity, unitPrice, "none");
            })
            .collect(Collectors.toList());
    }

    /**
     * Format phone number for ZR Express API.
     * ZR Express expects international format: +213XXXXXXXXX
     * Our system stores as: +213XXXXXXXXX (E.164) - keep as-is
     */
    private String formatPhoneForZrExpress(String phone) {
        if (phone == null) {
            return null;
        }
        // Keep international format for ZR Express
        // If stored without +, add it
        if (phone.startsWith("213") && !phone.startsWith("+")) {
            return "+" + phone;
        }
        // If local format (0XXX), convert to international
        if (phone.startsWith("0") && phone.length() == 10) {
            return "+213" + phone.substring(1);
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
     * Find or create a customer in ZR Express.
     * First searches by phone number, creates new customer if not found.
     * Uses User info if available, falls back to Order info.
     *
     * @param order The order containing customer info
     * @param cityTerritoryId Resolved city territory UUID
     * @param districtTerritoryId Resolved district territory UUID
     * @return Customer UUID, or null if failed
     */
    private String findOrCreateCustomer(Order order, String cityTerritoryId, String districtTerritoryId) {
        String phone = formatPhoneForZrExpress(getCustomerPhone(order));

        // First, try to find existing customer by phone
        String existingCustomerId = searchCustomerByPhone(phone);
        if (existingCustomerId != null) {
            LOG.debug("Found existing ZR Express customer: {}", existingCustomerId);
            return existingCustomerId;
        }

        // Customer not found, create new one with full info
        LOG.debug("Customer not found, creating new customer with phone: {}", phone);
        return createCustomer(order, cityTerritoryId, districtTerritoryId);
    }

    /**
     * Get customer name - prefer User info, fallback to Order.
     */
    private String getCustomerName(Order order) {
        if (order.getUser() != null) {
            String firstName = order.getUser().getFirstName();
            String lastName = order.getUser().getLastName();
            if (firstName != null || lastName != null) {
                String fullName = ((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "")).trim();
                if (!fullName.isEmpty()) {
                    return fullName;
                }
            }
        }
        return order.getFullName();
    }

    /**
     * Get customer phone - prefer User info, fallback to Order.
     */
    private String getCustomerPhone(Order order) {
        if (order.getUser() != null && order.getUser().getPhone() != null && !order.getUser().getPhone().isEmpty()) {
            return order.getUser().getPhone();
        }
        return order.getPhone();
    }

    /**
     * Get customer wilaya - prefer User info, fallback to Order.
     */
    private String getCustomerWilaya(Order order) {
        if (order.getUser() != null && order.getUser().getWilaya() != null && !order.getUser().getWilaya().isEmpty()) {
            return order.getUser().getWilaya();
        }
        return order.getWilaya();
    }

    /**
     * Get customer city/commune - prefer User info, fallback to Order.
     */
    private String getCustomerCity(Order order) {
        if (order.getUser() != null && order.getUser().getCity() != null && !order.getUser().getCity().isEmpty()) {
            return order.getUser().getCity();
        }
        return order.getCity();
    }

    /**
     * Get customer street address - prefer User info, fallback to Order.
     */
    private String getCustomerStreetAddress(Order order) {
        if (order.getUser() != null && order.getUser().getStreetAddress() != null && !order.getUser().getStreetAddress().isEmpty()) {
            return order.getUser().getStreetAddress();
        }
        return order.getStreetAddress();
    }

    /**
     * Get customer postal code - prefer User info, fallback to Order.
     */
    private String getCustomerPostalCode(Order order) {
        if (order.getUser() != null && order.getUser().getPostalCode() != null && !order.getUser().getPostalCode().isEmpty()) {
            return order.getUser().getPostalCode();
        }
        return order.getPostalCode();
    }

    /**
     * Get delivery preference for ZR Express.
     * Maps User's defaultShippingMethod if available, falls back to order's isStopDesk.
     * SHIPPING_PROVIDER → "pickup-point", HOME_DELIVERY → "home"
     */
    private String getDeliveryPreference(Order order, boolean isPickupPointOrder) {
        if (order.getUser() != null && order.getUser().getDefaultShippingMethod() != null) {
            return switch (order.getUser().getDefaultShippingMethod()) {
                case SHIPPING_PROVIDER -> "pickup-point";
                case HOME_DELIVERY -> "home";
            };
        }
        // Fallback to order type
        return isPickupPointOrder ? "pickup-point" : "home";
    }

    /**
     * Search for customer by phone number.
     *
     * @param phone Phone number to search
     * @return Customer UUID if found, null otherwise
     */
    private String searchCustomerByPhone(String phone) {
        try {
            HttpHeaders headers = createHeaders();

            Map<String, Object> searchRequest = new HashMap<>();
            searchRequest.put("keyword", phone);
            searchRequest.put("pageSize", 10);
            searchRequest.put("pageNumber", 1);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(searchRequest, headers);
            String url = shippingProperties.getZrExpress().getBaseUrl() + "/customers/search";

            LOG.debug("Searching ZR Express customer by phone: {}", phone);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Object itemsObj = response.getBody().get("items");
                if (itemsObj != null) {
                    List<Map<String, Object>> items = objectMapper.convertValue(
                        itemsObj,
                        new TypeReference<List<Map<String, Object>>>() {}
                    );
                    if (!items.isEmpty()) {
                        // Return first matching customer's ID
                        Object id = items.get(0).get("id");
                        if (id != null) {
                            return id.toString();
                        }
                    }
                }
            }

            LOG.debug("No customer found with phone: {}", phone);
            return null;

        } catch (Exception e) {
            LOG.error("Error searching for customer by phone: {}", phone, e);
            return null;
        }
    }

    /**
     * Create a new customer in ZR Express with full info.
     * Uses User info if available, falls back to Order info.
     *
     * @param order The order containing customer info
     * @param cityTerritoryId Resolved city territory UUID
     * @param districtTerritoryId Resolved district territory UUID
     * @return Customer UUID if created, null otherwise
     */
    private String createCustomer(Order order, String cityTerritoryId, String districtTerritoryId) {
        try {
            HttpHeaders headers = createHeaders();

            String name = getCustomerName(order);
            String phone = formatPhoneForZrExpress(getCustomerPhone(order));
            String wilaya = getCustomerWilaya(order);
            String city = getCustomerCity(order);
            String streetAddress = getCustomerStreetAddress(order);
            String postalCode = getCustomerPostalCode(order);
            boolean isPickupPoint = Boolean.TRUE.equals(order.getIsStopDesk());

            Map<String, Object> customerRequest = new HashMap<>();
            customerRequest.put("name", name);

            // Phone
            Map<String, String> phoneDto = new HashMap<>();
            phoneDto.put("number1", phone);
            customerRequest.put("phone", phoneDto);

            // Delivery preference - prefer User's default, fallback to order type
            String deliveryPreference = getDeliveryPreference(order, isPickupPoint);
            customerRequest.put("deliveryPreference", deliveryPreference);

            // Build address
            Map<String, Object> address = new HashMap<>();
            address.put("street", streetAddress != null ? streetAddress : "N/A");
            address.put("city", wilaya);
            address.put("cityTerritoryId", cityTerritoryId);
            address.put("district", city);
            address.put("districtTerritoryId", districtTerritoryId);
            if (postalCode != null && !postalCode.isEmpty()) {
                address.put("postalCode", postalCode);
            }
            address.put("country", "Algeria");
            address.put("isPrimary", true);

            // Add address to customer
            customerRequest.put("addresses", List.of(address));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(customerRequest, headers);
            String url = shippingProperties.getZrExpress().getBaseUrl() + "/customers/individual";

            LOG.debug("Creating ZR Express customer: name={}, phone={}, wilaya={}, city={}",
                name, phone, wilaya, city);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Object id = response.getBody().get("id");
                if (id != null) {
                    LOG.info("Created ZR Express customer: {}", id);
                    return id.toString();
                }
            }

            LOG.error("Failed to create ZR Express customer - no ID in response");
            return null;

        } catch (HttpClientErrorException e) {
            LOG.error("ZR Express API error creating customer: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            return null;
        } catch (Exception e) {
            LOG.error("Error creating ZR Express customer", e);
            return null;
        }
    }

    /**
     * Normalize location name for caching.
     * Converts accented characters to ASCII equivalents (é→e, ï→i, etc.)
     */
    private String normalizeLocationName(String name) {
        if (name == null) {
            return "";
        }
        // Normalize accented characters to ASCII (é→e, ï→i, etc.)
        String normalized = java.text.Normalizer.normalize(name, java.text.Normalizer.Form.NFD)
            .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return normalized.toLowerCase().trim()
            .replaceAll("[^a-z0-9\\s]", "")
            .replaceAll("\\s+", " ");
    }

    /**
     * Check if two location names match (case-insensitive, accent-insensitive).
     */
    private boolean matchesLocationName(String name1, String name2) {
        if (name1 == null || name2 == null) {
            return false;
        }
        return normalizeLocationName(name1).equals(normalizeLocationName(name2));
    }
}
