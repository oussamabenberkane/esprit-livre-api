package com.oussamabenberkane.espritlivre.web.rest;

import com.oussamabenberkane.espritlivre.config.ShippingProperties;
import com.oussamabenberkane.espritlivre.domain.Order;
import com.oussamabenberkane.espritlivre.domain.enumeration.OrderStatus;
import com.oussamabenberkane.espritlivre.repository.OrderRepository;
import com.oussamabenberkane.espritlivre.service.OrderService;
import com.oussamabenberkane.espritlivre.service.dto.shipping.YalidineWebhookPayload;
import com.oussamabenberkane.espritlivre.service.shipping.YalidineService;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for handling shipping provider webhooks.
 */
@RestController
@RequestMapping("/api/webhooks")
public class ShippingWebhookResource {

    private static final Logger LOG = LoggerFactory.getLogger(ShippingWebhookResource.class);

    private final YalidineService yalidineService;
    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final ShippingProperties shippingProperties;

    public ShippingWebhookResource(
        YalidineService yalidineService,
        OrderRepository orderRepository,
        OrderService orderService,
        ShippingProperties shippingProperties
    ) {
        this.yalidineService = yalidineService;
        this.orderRepository = orderRepository;
        this.orderService = orderService;
        this.shippingProperties = shippingProperties;
    }

    /**
     * POST /api/webhooks/yalidine : Handle Yalidine webhook events.
     *
     * @param payload the webhook payload from Yalidine
     * @return 200 OK if processed successfully, 401 if invalid security token
     */
    @PostMapping("/yalidine")
    public ResponseEntity<Void> handleYalidineWebhook(@RequestBody YalidineWebhookPayload payload) {
        LOG.info("Received Yalidine webhook: event={}, tracking={}, orderId={}, status={}",
            payload.event(), payload.trackingNumber(), payload.orderId(), payload.status());

        // 1. Validate security token
        String webhookSecret = shippingProperties.getYalidine().getWebhookSecret();
        if (!yalidineService.validateWebhook(payload, webhookSecret)) {
            LOG.warn("Invalid webhook security token for tracking: {}", payload.trackingNumber());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // 2. Find order by uniqueId (order_id in payload)
        Optional<Order> orderOpt = orderRepository.findByUniqueIdAndActiveTrue(payload.orderId());
        if (orderOpt.isEmpty()) {
            LOG.warn("Order not found for webhook: orderId={}", payload.orderId());
            // Return 200 to acknowledge receipt even if order not found
            // (prevents Yalidine from retrying for deleted/unknown orders)
            return ResponseEntity.ok().build();
        }

        Order order = orderOpt.get();

        // 3. Map status and update order if status changed
        OrderStatus newStatus = yalidineService.mapProviderStatus(payload.statusCode(), payload.event());

        if (newStatus != null && newStatus != order.getStatus()) {
            LOG.info("Updating order {} status from {} to {} via Yalidine webhook",
                order.getUniqueId(), order.getStatus(), newStatus);
            orderService.updateStatus(order.getId(), newStatus);
        } else {
            LOG.debug("No status change needed for order {}: current={}, mapped={}",
                order.getUniqueId(), order.getStatus(), newStatus);
        }

        return ResponseEntity.ok().build();
    }
}
