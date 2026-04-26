package com.oussamabenberkane.espritlivre.service.shipping;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oussamabenberkane.espritlivre.config.ShippingProperties;
import com.oussamabenberkane.espritlivre.domain.Order;
import com.oussamabenberkane.espritlivre.domain.OrderItem;
import com.oussamabenberkane.espritlivre.domain.enumeration.OrderItemType;
import com.oussamabenberkane.espritlivre.domain.enumeration.OrderStatus;
import com.oussamabenberkane.espritlivre.domain.enumeration.ShippingProvider;
import com.oussamabenberkane.espritlivre.service.dto.shipping.DeliveryFeeResult;
import com.oussamabenberkane.espritlivre.service.dto.shipping.RelayPointDTO;
import com.oussamabenberkane.espritlivre.service.dto.shipping.ShippingResult;
import com.oussamabenberkane.espritlivre.service.dto.shipping.YalidineWebhookPayload;
import com.oussamabenberkane.espritlivre.service.dto.shipping.ZrExpressGetParcelResponse;
import com.oussamabenberkane.espritlivre.service.dto.shipping.ZrExpressHubResponse;
import com.oussamabenberkane.espritlivre.service.dto.shipping.ZrExpressParcelRequest;
import com.oussamabenberkane.espritlivre.service.dto.shipping.ZrExpressParcelResponse;
import com.oussamabenberkane.espritlivre.service.dto.shipping.ZrExpressRateResponse;
import com.oussamabenberkane.espritlivre.service.dto.shipping.ZrExpressSearchRequest;
import com.oussamabenberkane.espritlivre.service.util.TextNormalizationUtils;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

            boolean isPickupPoint = Boolean.TRUE.equals(order.getIsStopDesk());
            String districtTerritoryId = null;
            if (!isPickupPoint) {
                districtTerritoryId = resolveDistrictTerritoryId(cityTerritoryId, order.getCity());
                if (districtTerritoryId == null) {
                    LOG.error("Could not resolve district territory ID for city: {} in wilaya: {} for order: {}",
                        order.getCity(), order.getWilaya(), order.getUniqueId());
                    return ShippingResult.failure("Could not resolve commune: " + order.getCity());
                }
            } else {
                LOG.warn("Stop desk order {}: skipping district territory resolution (commune: {})", order.getUniqueId(), order.getCity());
            }

            // 2. Resolve or create customer
            String customerPhone = formatPhoneForZrExpress(getCustomerPhone(order));
            String customerName = getCustomerName(order);
            String customerId = resolveOrCreateCustomer(customerName, customerPhone);
            if (customerId == null) {
                LOG.error("Could not resolve or create ZR Express customer for order: {}", order.getUniqueId());
                return ShippingResult.failure("Could not resolve or create ZR Express customer");
            }

            // 3. Build parcel request with customer ID + inline info
            ZrExpressParcelRequest request = buildParcelRequest(order, cityTerritoryId, districtTerritoryId, customerId, customerName, customerPhone);
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
        if (statusCode == null && event == null) {
            return null;
        }

        // Handle by status name (state.name from API)
        String statusToCheck = statusCode != null ? statusCode : event;
        return mapZrExpressStateName(statusToCheck);
    }

    @Override
    public Optional<OrderStatus> fetchOrderStatus(Order order) {
        if (order.getTrackingNumber() == null || order.getTrackingNumber().isBlank()) {
            return Optional.empty();
        }

        if (!shippingProperties.getZrExpress().isEnabled()) {
            LOG.debug("ZR Express integration is disabled, skipping status fetch for order: {}", order.getUniqueId());
            return Optional.empty();
        }

        try {
            HttpHeaders headers = createHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            String url = shippingProperties.getZrExpress().getBaseUrl() + "/parcels/" + order.getTrackingNumber();
            LOG.debug("Fetching ZR Express parcel status for tracking: {}", order.getTrackingNumber());

            ResponseEntity<ZrExpressGetParcelResponse> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                ZrExpressGetParcelResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                ZrExpressGetParcelResponse parcel = response.getBody();
                if (parcel.getState() != null && parcel.getState().getName() != null) {
                    String stateName = parcel.getState().getName();
                    OrderStatus mappedStatus = mapZrExpressStateName(stateName);
                    if (mappedStatus != null) {
                        LOG.debug("ZR Express status for order {}: {} -> {}",
                            order.getUniqueId(), stateName, mappedStatus);
                        return Optional.of(mappedStatus);
                    }
                }
            }

            return Optional.empty();

        } catch (HttpClientErrorException e) {
            LOG.debug("ZR Express API error fetching status for order {}: {} - {}",
                order.getUniqueId(), e.getStatusCode(), e.getResponseBodyAsString());
            return Optional.empty();
        } catch (HttpServerErrorException e) {
            LOG.debug("ZR Express server error fetching status for order {}: {}",
                order.getUniqueId(), e.getStatusCode());
            return Optional.empty();
        } catch (Exception e) {
            LOG.debug("Error fetching ZR Express status for order {}: {}", order.getUniqueId(), e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Map ZR Express state.name to internal OrderStatus.
     * Uses loose contains matching to handle various status formats.
     * Order of checks matters: more specific matches first.
     */
    private OrderStatus mapZrExpressStateName(String stateName) {
        if (stateName == null || stateName.isBlank()) {
            return null;
        }

        // Normalize: lowercase, trim, remove accents, replace underscores with spaces
        String normalized = normalizeLocationName(stateName).replace("_", " ");

        // DELIVERED status (check first - final state)
        if (normalized.contains("livre") ||
            normalized.contains("deliver") ||
            normalized.contains("recouvert")) {
            return OrderStatus.DELIVERED;
        }

        // CANCELLED/RETURNED statuses
        if (normalized.contains("annul") ||
            normalized.contains("retour") ||
            normalized.contains("echec") ||
            normalized.contains("cancel") ||
            normalized.contains("return") ||
            normalized.contains("fail") ||
            normalized.contains("refuse")) {
            return OrderStatus.CANCELLED;
        }

        // CONFIRMED statuses (preparation phase) - check BEFORE shipped
        // "pret_a_expedier" = ready to ship, not yet shipped
        if (normalized.contains("pret") ||
            normalized.contains("ready") ||
            normalized.contains("prepara") ||
            normalized.contains("pending") ||
            normalized.contains("created") ||
            normalized.contains("attente") ||
            normalized.contains("commande") ||
            normalized.contains("recu") ||
            normalized.contains("nouveau") ||
            normalized.contains("new")) {
            return OrderStatus.CONFIRMED;
        }

        // SHIPPED statuses (in transit) - checked after CONFIRMED
        if (normalized.contains("transit") ||
            normalized.contains("ramass") ||
            normalized.contains("exped") ||
            normalized.contains("picked") ||
            normalized.contains("dispatch") ||
            normalized.contains("shipping") ||
            normalized.contains("centre") ||
            normalized.contains("wilaya") ||
            normalized.contains("hub") ||
            normalized.contains("transfert") ||
            normalized.contains("sorti")) {
            return OrderStatus.SHIPPED;
        }

        LOG.debug("Unknown ZR Express status: {}", stateName);
        return null;
    }

    @Override
    public Map<String, OrderStatus> fetchOrderStatuses(List<Order> orders) {
        Map<String, OrderStatus> results = new HashMap<>();

        if (orders == null || orders.isEmpty()) {
            return results;
        }

        if (!shippingProperties.getZrExpress().isEnabled()) {
            LOG.debug("ZR Express integration is disabled, skipping batch status fetch");
            return results;
        }

        LOG.debug("Fetching ZR Express status for {} parcels", orders.size());

        // ZR Express doesn't support batch filtering by tracking number,
        // so we fetch each parcel individually using the single-parcel endpoint
        for (Order order : orders) {
            Optional<OrderStatus> status = fetchOrderStatus(order);
            status.ifPresent(s -> results.put(order.getTrackingNumber(), s));
        }

        LOG.debug("ZR Express fetch completed: {}/{} statuses retrieved", results.size(), orders.size());
        return results;
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
     * Fetches all hubs and filters by ID since ZR Express doesn't support direct GET by ID.
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

        LOG.debug("Fetching ZR Express hub by ID: {}", hubId);

        try {
            // Fetch all hubs and filter by ID
            List<ZrExpressHubResponse> hubs = fetchAllHubsRaw();

            return hubs.stream()
                .filter(hub -> hubId.equals(hub.getId()))
                .findFirst()
                .map(ZrExpressHubResponse::toRelayPointDTO)
                .orElseGet(() -> {
                    LOG.warn("Hub not found with ID: {}", hubId);
                    return null;
                });

        } catch (Exception e) {
            LOG.error("Unexpected error fetching ZR Express hub {}", hubId, e);
            return null;
        }
    }

    /**
     * Search hubs by query string with accent-insensitive matching.
     * Supports searching "bejaia" to find "Béjaia", "tizi" to find "Tizi-Ouzou", etc.
     *
     * @param query Search query (searches in name, address, commune name)
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

        // Filter by query with accent-insensitive matching
        String normalizedQuery = TextNormalizationUtils.normalizeForSearch(query);
        return hubs.stream()
            .filter(h -> {
                boolean matches = false;
                if (h.getName() != null) {
                    matches = TextNormalizationUtils.normalizeForSearch(h.getName()).contains(normalizedQuery);
                }
                if (!matches && h.getAddress() != null) {
                    matches = TextNormalizationUtils.normalizeForSearch(h.getAddress()).contains(normalizedQuery);
                }
                if (!matches && h.getCommuneName() != null) {
                    matches = TextNormalizationUtils.normalizeForSearch(h.getCommuneName()).contains(normalizedQuery);
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
     * Search for an existing ZR Express customer by phone, or create one if not found.
     * Returns the customer UUID.
     */
    private String resolveOrCreateCustomer(String name, String phone) {
        // 1. Search by phone keyword
        String customerId = searchCustomerByPhone(phone);
        if (customerId != null) {
            LOG.debug("Found existing ZR Express customer {} for phone {}", customerId, phone);
            return customerId;
        }

        // 2. Create a new individual customer
        return createCustomer(name, phone);
    }

    @SuppressWarnings("unchecked")
    private String searchCustomerByPhone(String phone) {
        try {
            HttpHeaders headers = createHeaders();

            Map<String, Object> searchRequest = new HashMap<>();
            searchRequest.put("keyword", phone);
            searchRequest.put("pageSize", 1);
            searchRequest.put("pageNumber", 1);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(searchRequest, headers);
            String url = shippingProperties.getZrExpress().getBaseUrl() + "/customers/search";

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Object itemsObj = response.getBody().get("items");
                if (itemsObj != null) {
                    List<Map<String, Object>> items = objectMapper.convertValue(itemsObj, new TypeReference<List<Map<String, Object>>>() {});
                    if (!items.isEmpty()) {
                        Object id = items.get(0).get("id");
                        return id != null ? id.toString() : null;
                    }
                }
            }
        } catch (Exception e) {
            LOG.warn("Error searching ZR Express customer by phone {}: {}", phone, e.getMessage());
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private String createCustomer(String name, String phone) {
        try {
            HttpHeaders headers = createHeaders();

            Map<String, Object> phoneDto = new HashMap<>();
            phoneDto.put("number1", phone);

            Map<String, Object> customerRequest = new HashMap<>();
            customerRequest.put("name", name);
            customerRequest.put("phone", phoneDto);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(customerRequest, headers);
            String url = shippingProperties.getZrExpress().getBaseUrl() + "/customers/individual";

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Object id = response.getBody().get("id");
                if (id != null) {
                    LOG.info("Created ZR Express customer {} for {}", id, name);
                    return id.toString();
                }
            }
        } catch (Exception e) {
            LOG.error("Error creating ZR Express customer for {}: {}", name, e.getMessage());
        }
        return null;
    }

    /**
     * Build parcel request from order.
     * Uses the pre-resolved customerId for the ZR Express API.
     */
    private ZrExpressParcelRequest buildParcelRequest(Order order, String cityTerritoryId, String districtTerritoryId,
                                                       String customerId, String customerName, String customerPhone) {
        boolean isPickupPoint = Boolean.TRUE.equals(order.getIsStopDesk());
        String deliveryType = isPickupPoint ? DELIVERY_TYPE_PICKUP_POINT : DELIVERY_TYPE_HOME;

        String streetAddress = getCustomerStreetAddress(order);

        // Build ordered products list from order items
        List<ZrExpressParcelRequest.ZrOrderedProduct> orderedProducts = buildOrderedProducts(order);

        ZrExpressParcelRequest.Builder builder = ZrExpressParcelRequest.builder()
            .customer(customerName, customerPhone)
            .customerId(customerId)
            .deliveryAddress(cityTerritoryId, districtTerritoryId, streetAddress != null ? streetAddress : "N/A")
            .deliveryType(deliveryType)
            .amount(order.getTotalAmount() != null ? order.getTotalAmount().doubleValue() : 0.0)
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
     * Get customer name from order info.
     * Always uses order.fullName as it contains the actual recipient info.
     */
    private String getCustomerName(Order order) {
        return order.getFullName();
    }

    /**
     * Get customer phone from order info.
     * Always uses order.phone as it contains the actual recipient info.
     */
    private String getCustomerPhone(Order order) {
        return order.getPhone();
    }

    /**
     * Get customer street address from order info.
     * Always uses order.streetAddress as it contains the actual delivery address.
     */
    private String getCustomerStreetAddress(Order order) {
        return order.getStreetAddress();
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

    /**
     * Check if the destination wilaya is the supplier's origin wilaya.
     * ZR Express API only allows commune-level rate queries for the origin wilaya.
     */
    private boolean isDestinationInOriginWilaya(String destinationWilaya) {
        String originWilaya = shippingProperties.getOriginWilaya();
        if (originWilaya == null || destinationWilaya == null) {
            return false;
        }
        return matchesLocationName(originWilaya, destinationWilaya);
    }

    /**
     * Get delivery fee for a specific destination territory.
     *
     * @param wilayaName Destination wilaya name (e.g., "Bejaia", "Alger")
     * @param communeName Commune/district name within the wilaya
     * @param isStopDesk Whether this is a pickup point (stop desk) delivery
     * @return DeliveryFeeResult with the calculated fee or error
     */
    public DeliveryFeeResult getDeliveryFee(String wilayaName, String communeName, boolean isStopDesk) {
        if (!shippingProperties.getZrExpress().isEnabled()) {
            LOG.warn("ZR Express integration is disabled");
            return DeliveryFeeResult.failure("ZR Express integration is disabled");
        }

        if (wilayaName == null || wilayaName.isBlank()) {
            return DeliveryFeeResult.failure("Destination wilaya name is required");
        }

        try {
            // First resolve the territory ID from wilaya/commune name
            String cityTerritoryId = resolveCityTerritoryId(wilayaName);
            if (cityTerritoryId == null) {
                return DeliveryFeeResult.failure("Could not resolve wilaya: " + wilayaName);
            }

            // ZR Express API only allows commune-level rates for the supplier's origin wilaya.
            // For other wilayas, we must use the wilaya-level territory ID.
            String territoryId = cityTerritoryId;
            boolean isOriginWilaya = isDestinationInOriginWilaya(wilayaName);
            String territoryLevel = "wilaya";

            if (isOriginWilaya && communeName != null && !communeName.isBlank()) {
                // Only try district-level for origin wilaya
                String districtTerritoryId = resolveDistrictTerritoryId(cityTerritoryId, communeName);
                if (districtTerritoryId != null) {
                    territoryId = districtTerritoryId;
                    territoryLevel = "commune";
                }
            }

            LOG.debug("ZR Express rate lookup: wilaya={}, commune={}, isOriginWilaya={}, territoryLevel={}, territoryId={}",
                wilayaName, communeName, isOriginWilaya, territoryLevel, territoryId);

            // Now fetch the rate for this territory
            HttpHeaders headers = createHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            String url = shippingProperties.getZrExpress().getBaseUrl() +
                "/delivery-pricing/rates/" + territoryId;

            LOG.debug("Fetching ZR Express rate from: {}", url);

            ResponseEntity<ZrExpressRateResponse> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                ZrExpressRateResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                ZrExpressRateResponse rateResponse = response.getBody();

                BigDecimal fee = rateResponse.getDeliveryFee(isStopDesk);
                if (fee != null) {
                    LOG.debug("ZR Express delivery fee for territory {}: {} DA (stopDesk={})",
                        territoryId, fee, isStopDesk);
                    return DeliveryFeeResult.automatic(fee, ShippingProvider.ZR);
                } else {
                    return DeliveryFeeResult.failure("No delivery prices found for territory: " + territoryId);
                }
            } else {
                LOG.error("ZR Express rate API returned unexpected response: status={}", response.getStatusCode());
                return DeliveryFeeResult.failure("ZR Express API returned unexpected response");
            }

        } catch (HttpClientErrorException e) {
            LOG.error("ZR Express rate API client error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            return DeliveryFeeResult.failure("ZR Express API error: " + e.getResponseBodyAsString());
        } catch (HttpServerErrorException e) {
            LOG.error("ZR Express rate API server error: {}", e.getStatusCode());
            return DeliveryFeeResult.failure("ZR Express server error");
        } catch (Exception e) {
            LOG.error("Error fetching ZR Express delivery fee", e);
            return DeliveryFeeResult.failure("Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Get delivery fee using territory ID directly.
     *
     * @param territoryId The ZR Express territory UUID
     * @param isStopDesk Whether this is a pickup point delivery
     * @return DeliveryFeeResult with the calculated fee or error
     */
    public DeliveryFeeResult getDeliveryFeeByTerritoryId(String territoryId, boolean isStopDesk) {
        if (!shippingProperties.getZrExpress().isEnabled()) {
            LOG.warn("ZR Express integration is disabled");
            return DeliveryFeeResult.failure("ZR Express integration is disabled");
        }

        if (territoryId == null || territoryId.isBlank()) {
            return DeliveryFeeResult.failure("Territory ID is required");
        }

        try {
            HttpHeaders headers = createHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            String url = shippingProperties.getZrExpress().getBaseUrl() +
                "/delivery-pricing/rates/" + territoryId;

            LOG.debug("Fetching ZR Express rate from: {}", url);

            ResponseEntity<ZrExpressRateResponse> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                ZrExpressRateResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                ZrExpressRateResponse rateResponse = response.getBody();

                BigDecimal fee = rateResponse.getDeliveryFee(isStopDesk);
                if (fee != null) {
                    LOG.debug("ZR Express delivery fee for territory {}: {} DA (stopDesk={})",
                        territoryId, fee, isStopDesk);
                    return DeliveryFeeResult.automatic(fee, ShippingProvider.ZR);
                } else {
                    return DeliveryFeeResult.failure("No delivery prices found for territory: " + territoryId);
                }
            } else {
                LOG.error("ZR Express rate API returned unexpected response: status={}", response.getStatusCode());
                return DeliveryFeeResult.failure("ZR Express API returned unexpected response");
            }

        } catch (HttpClientErrorException e) {
            LOG.error("ZR Express rate API client error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            return DeliveryFeeResult.failure("ZR Express API error: " + e.getResponseBodyAsString());
        } catch (HttpServerErrorException e) {
            LOG.error("ZR Express rate API server error: {}", e.getStatusCode());
            return DeliveryFeeResult.failure("ZR Express server error");
        } catch (Exception e) {
            LOG.error("Error fetching ZR Express delivery fee", e);
            return DeliveryFeeResult.failure("Unexpected error: " + e.getMessage());
        }
    }
}
