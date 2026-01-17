package com.oussamabenberkane.espritlivre.web.rest;

import com.oussamabenberkane.espritlivre.domain.enumeration.ShippingProvider;
import com.oussamabenberkane.espritlivre.service.dto.shipping.RelayPointDTO;
import com.oussamabenberkane.espritlivre.service.shipping.YalidineService;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for managing relay points (stopdesks/centers).
 * Provides endpoints to fetch relay points from shipping providers (Yalidine, ZR Express).
 */
@RestController
@RequestMapping("/api/relay-points")
public class RelayPointResource {

    private static final Logger LOG = LoggerFactory.getLogger(RelayPointResource.class);

    private final YalidineService yalidineService;

    public RelayPointResource(YalidineService yalidineService) {
        this.yalidineService = yalidineService;
    }

    /**
     * GET /api/relay-points : Get relay points with optional filters.
     *
     * @param provider the shipping provider (YALIDINE or ZR)
     * @param wilayaId optional wilaya ID to filter by
     * @return list of relay points
     */
    @GetMapping
    public ResponseEntity<List<RelayPointDTO>> getRelayPoints(
        @RequestParam(required = false) String provider,
        @RequestParam(required = false) Integer wilayaId
    ) {
        LOG.debug("REST request to get relay points: provider={}, wilayaId={}", provider, wilayaId);

        ShippingProvider shippingProvider = parseProvider(provider);

        if (shippingProvider == ShippingProvider.YALIDINE) {
            List<RelayPointDTO> relayPoints = yalidineService.getCenters(wilayaId);
            return ResponseEntity.ok(relayPoints);
        } else if (shippingProvider == ShippingProvider.ZR) {
            // ZR Express not yet implemented
            LOG.warn("ZR Express relay points not yet implemented");
            return ResponseEntity.ok(Collections.emptyList());
        }

        // If no provider specified, return Yalidine by default
        List<RelayPointDTO> relayPoints = yalidineService.getCenters(wilayaId);
        return ResponseEntity.ok(relayPoints);
    }

    /**
     * GET /api/relay-points/search : Search relay points.
     *
     * @param provider the shipping provider (YALIDINE or ZR)
     * @param search optional search query
     * @param wilayaId optional wilaya ID to filter by
     * @return list of matching relay points
     */
    @GetMapping("/search")
    public ResponseEntity<List<RelayPointDTO>> searchRelayPoints(
        @RequestParam(required = false) String provider,
        @RequestParam(required = false) String search,
        @RequestParam(required = false) Integer wilayaId
    ) {
        LOG.debug("REST request to search relay points: provider={}, search={}, wilayaId={}",
            provider, search, wilayaId);

        ShippingProvider shippingProvider = parseProvider(provider);

        if (shippingProvider == ShippingProvider.YALIDINE) {
            List<RelayPointDTO> relayPoints = yalidineService.searchCenters(search, wilayaId);
            return ResponseEntity.ok(relayPoints);
        } else if (shippingProvider == ShippingProvider.ZR) {
            // ZR Express not yet implemented
            LOG.warn("ZR Express relay points search not yet implemented");
            return ResponseEntity.ok(Collections.emptyList());
        }

        // If no provider specified, search Yalidine by default
        List<RelayPointDTO> relayPoints = yalidineService.searchCenters(search, wilayaId);
        return ResponseEntity.ok(relayPoints);
    }

    /**
     * GET /api/relay-points/{id} : Get a specific relay point by ID.
     *
     * @param id the relay point ID
     * @return the relay point, or 404 if not found
     */
    @GetMapping("/{id}")
    public ResponseEntity<RelayPointDTO> getRelayPointById(@PathVariable String id) {
        LOG.debug("REST request to get relay point by ID: {}", id);

        try {
            Integer centerId = Integer.parseInt(id);
            RelayPointDTO relayPoint = yalidineService.getCenterById(centerId);

            if (relayPoint != null) {
                return ResponseEntity.ok(relayPoint);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (NumberFormatException e) {
            LOG.warn("Invalid relay point ID format: {}", id);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Parse the provider string to ShippingProvider enum.
     */
    private ShippingProvider parseProvider(String provider) {
        if (provider == null || provider.isBlank()) {
            return null;
        }

        try {
            return ShippingProvider.valueOf(provider.toUpperCase());
        } catch (IllegalArgumentException e) {
            LOG.warn("Unknown shipping provider: {}", provider);
            return null;
        }
    }
}
