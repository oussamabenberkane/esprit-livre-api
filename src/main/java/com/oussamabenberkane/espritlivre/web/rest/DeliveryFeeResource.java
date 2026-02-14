package com.oussamabenberkane.espritlivre.web.rest;

import com.oussamabenberkane.espritlivre.service.DeliveryFeeCalculationService;
import com.oussamabenberkane.espritlivre.service.dto.DeliveryFeeCalculationRequest;
import com.oussamabenberkane.espritlivre.service.dto.DeliveryFeeCalculationResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for calculating delivery fees.
 * This is a public endpoint (no authentication required) to support guest checkout.
 */
@RestController
@RequestMapping("/api/delivery-fee")
public class DeliveryFeeResource {

    private static final Logger LOG = LoggerFactory.getLogger(DeliveryFeeResource.class);

    private final DeliveryFeeCalculationService deliveryFeeCalculationService;

    public DeliveryFeeResource(DeliveryFeeCalculationService deliveryFeeCalculationService) {
        this.deliveryFeeCalculationService = deliveryFeeCalculationService;
    }

    /**
     * POST /api/delivery-fee/calculate : Calculate delivery fee for a cart.
     *
     * Calculates the delivery fee based on:
     * - The shipping provider (YALIDINE or ZR)
     * - The destination wilaya and city
     * - Whether it's a stop desk (relay point) delivery
     * - The items in the cart (each can have fixed or automatic delivery fee)
     *
     * When items have different delivery fee settings:
     * - For automatic fees: calculates via the provider's API
     * - For fixed fees: uses the product's configured delivery fee
     * - Returns the MINIMUM fee among all items
     *
     * @param request the calculation request with cart items and destination
     * @return the calculated delivery fee with method and provider info
     */
    @PostMapping("/calculate")
    public ResponseEntity<DeliveryFeeCalculationResponse> calculateDeliveryFee(
        @Valid @RequestBody DeliveryFeeCalculationRequest request
    ) {
        LOG.debug("REST request to calculate delivery fee: provider={}, wilaya={}, city={}, isStopDesk={}, items={}",
            request.getShippingProvider(),
            request.getWilaya(),
            request.getCity(),
            request.getIsStopDesk(),
            request.getItems() != null ? request.getItems().size() : 0
        );

        DeliveryFeeCalculationResponse response = deliveryFeeCalculationService.calculateDeliveryFee(request);

        if (response.isSuccess()) {
            LOG.info("Delivery fee calculated: {} DA, method={}, provider={}",
                response.getFee(), response.getMethod(), response.getProvider());
            return ResponseEntity.ok(response);
        } else {
            LOG.warn("Delivery fee calculation failed: {}", response.getErrorMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
