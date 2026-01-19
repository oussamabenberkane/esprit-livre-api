package com.oussamabenberkane.espritlivre.service;

import com.oussamabenberkane.espritlivre.domain.Order;
import com.oussamabenberkane.espritlivre.domain.enumeration.OrderStatus;
import com.oussamabenberkane.espritlivre.domain.enumeration.ShippingProvider;
import com.oussamabenberkane.espritlivre.repository.OrderRepository;
import com.oussamabenberkane.espritlivre.service.dto.OrderDTO;
import com.oussamabenberkane.espritlivre.service.dto.OrderPageResponse;
import com.oussamabenberkane.espritlivre.service.dto.OrderPageResponse.StatusRefreshInfo;
import com.oussamabenberkane.espritlivre.service.shipping.ShippingProviderFactory;
import com.oussamabenberkane.espritlivre.service.shipping.ShippingProviderService;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for enriching orders with live status from shipping providers.
 * Uses batch API requests to minimize API calls and respect rate limits.
 * Fetches status from provider APIs and persists changes to the database.
 */
@Service
public class OrderStatusEnrichmentService {

    private static final Logger LOG = LoggerFactory.getLogger(OrderStatusEnrichmentService.class);

    private final OrderRepository orderRepository;
    private final ShippingProviderFactory shippingProviderFactory;

    public OrderStatusEnrichmentService(
        OrderRepository orderRepository,
        ShippingProviderFactory shippingProviderFactory
    ) {
        this.orderRepository = orderRepository;
        this.shippingProviderFactory = shippingProviderFactory;
    }

    /**
     * Enrich orders with live status from shipping providers using batch requests.
     * Groups orders by provider and makes one API call per provider.
     * Updates both the DTOs and persists changes to the database.
     *
     * Uses REQUIRES_NEW to ensure changes are persisted in a separate transaction,
     * since this may be called from a read-only transaction context.
     *
     * @param orders the page of orders to enrich
     * @return OrderPageResponse containing the enriched page and refresh counts per provider
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public OrderPageResponse enrichWithLiveStatus(Page<OrderDTO> orders) {
        if (orders.isEmpty()) {
            return OrderPageResponse.of(orders);
        }

        // Find orders eligible for status polling
        List<OrderDTO> eligibleOrders = orders.getContent().stream()
            .filter(this::isEligibleForPolling)
            .toList();

        if (eligibleOrders.isEmpty()) {
            LOG.debug("No orders eligible for status polling");
            return OrderPageResponse.of(orders);
        }

        LOG.debug("Fetching live status for {} orders using batch requests", eligibleOrders.size());

        // Group orders by shipping provider
        Map<ShippingProvider, List<OrderDTO>> ordersByProvider = eligibleOrders.stream()
            .collect(Collectors.groupingBy(OrderDTO::getShippingProvider));

        // Fetch statuses in batch for each provider (one API call per provider)
        Map<String, OrderStatus> allStatuses = new HashMap<>();
        for (Map.Entry<ShippingProvider, List<OrderDTO>> entry : ordersByProvider.entrySet()) {
            ShippingProvider provider = entry.getKey();
            List<OrderDTO> providerOrders = entry.getValue();

            Optional<ShippingProviderService> serviceOpt = shippingProviderFactory.getService(provider);
            if (serviceOpt.isEmpty()) {
                LOG.debug("No service available for provider: {}", provider);
                continue;
            }

            // Convert DTOs to minimal Order objects for the service call
            List<Order> orderEntities = providerOrders.stream()
                .map(this::toMinimalOrder)
                .toList();

            // Batch fetch statuses (single API call)
            Map<String, OrderStatus> providerStatuses = serviceOpt.get().fetchOrderStatuses(orderEntities);
            allStatuses.putAll(providerStatuses);

            LOG.debug("Provider {} batch fetch: {}/{} statuses retrieved",
                provider, providerStatuses.size(), providerOrders.size());
        }

        // Track refresh counts per provider
        int yalidineRefreshed = 0;
        int zrExpressRefreshed = 0;

        // Update DTOs and persist changes
        for (OrderDTO orderDto : eligibleOrders) {
            OrderStatus newStatus = allStatuses.get(orderDto.getTrackingNumber());
            if (newStatus != null && newStatus != orderDto.getStatus()) {
                OrderStatus oldStatus = orderDto.getStatus();
                ShippingProvider provider = orderDto.getShippingProvider();

                // Update the DTO
                orderDto.setStatus(newStatus);

                // Persist to database
                persistStatusChange(orderDto.getId(), newStatus);

                // Track count per provider
                if (provider == ShippingProvider.YALIDINE) {
                    yalidineRefreshed++;
                } else if (provider == ShippingProvider.ZR) {
                    zrExpressRefreshed++;
                }

                LOG.info("Order {} status updated: {} -> {} ({})",
                    orderDto.getId(), oldStatus, newStatus, provider);
            }
        }

        int totalRefreshed = yalidineRefreshed + zrExpressRefreshed;
        LOG.debug("Status enrichment complete: {} orders updated (Yalidine: {}, ZR Express: {})",
            totalRefreshed, yalidineRefreshed, zrExpressRefreshed);

        // Return response with refresh info only if there were refreshes
        if (totalRefreshed > 0) {
            StatusRefreshInfo refreshInfo = new StatusRefreshInfo(yalidineRefreshed, zrExpressRefreshed);
            return OrderPageResponse.of(orders, refreshInfo);
        }

        return OrderPageResponse.of(orders);
    }

    /**
     * Check if an order is eligible for status polling.
     * Criteria: has tracking number, has shipping provider, status is not PENDING.
     */
    private boolean isEligibleForPolling(OrderDTO order) {
        return order.getTrackingNumber() != null &&
            !order.getTrackingNumber().isBlank() &&
            order.getShippingProvider() != null &&
            order.getStatus() != OrderStatus.PENDING;
    }

    /**
     * Convert OrderDTO to minimal Order entity for service call.
     */
    private Order toMinimalOrder(OrderDTO dto) {
        Order order = new Order();
        order.setId(dto.getId());
        order.setUniqueId(dto.getUniqueId());
        order.setTrackingNumber(dto.getTrackingNumber());
        order.setShippingProvider(dto.getShippingProvider());
        return order;
    }

    /**
     * Persist status change to the database.
     */
    private void persistStatusChange(Long orderId, OrderStatus newStatus) {
        try {
            orderRepository.findById(orderId).ifPresent(order -> {
                order.setStatus(newStatus);
                order.setUpdatedAt(ZonedDateTime.now());
                orderRepository.save(order);
            });
        } catch (Exception e) {
            LOG.error("Failed to persist status change for order {}: {}", orderId, e.getMessage());
        }
    }
}
