package com.oussamabenberkane.espritlivre.service.shipping;

import com.oussamabenberkane.espritlivre.domain.Order;
import com.oussamabenberkane.espritlivre.domain.enumeration.OrderStatus;
import com.oussamabenberkane.espritlivre.domain.enumeration.ShippingProvider;
import com.oussamabenberkane.espritlivre.service.dto.shipping.ShippingResult;
import com.oussamabenberkane.espritlivre.service.dto.shipping.YalidineWebhookPayload;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Interface defining operations for shipping providers (Yalidine, ZR Express).
 */
public interface ShippingProviderService {

    /**
     * Get the shipping provider this service handles.
     *
     * @return the shipping provider enum value
     */
    ShippingProvider getProvider();

    /**
     * Create a parcel/shipment with the shipping provider.
     *
     * @param order the order to create a parcel for
     * @return the result containing tracking number and label URL on success, or error message on failure
     */
    ShippingResult createParcel(Order order);

    /**
     * Validate a webhook payload from the shipping provider.
     *
     * @param payload the webhook payload to validate
     * @param secret the webhook secret to validate against
     * @return true if the webhook is valid, false otherwise
     */
    boolean validateWebhook(YalidineWebhookPayload payload, String secret);

    /**
     * Map a provider-specific status code and event to an internal OrderStatus.
     *
     * @param statusCode the provider's status code
     * @param event the webhook event type
     * @return the corresponding OrderStatus, or null if no mapping applies
     */
    OrderStatus mapProviderStatus(String statusCode, String event);

    /**
     * Fetch the current status of an order from the provider's API.
     *
     * @param order the order to fetch status for (must have tracking number)
     * @return Optional containing the mapped OrderStatus, or empty if unavailable
     */
    Optional<OrderStatus> fetchOrderStatus(Order order);

    /**
     * Fetch the current status of multiple orders from the provider's API in a single batch request.
     * This is more efficient than calling fetchOrderStatus for each order individually.
     *
     * @param orders the list of orders to fetch status for (must have tracking numbers)
     * @return Map of tracking number to OrderStatus for orders where status was successfully fetched
     */
    Map<String, OrderStatus> fetchOrderStatuses(List<Order> orders);
}
