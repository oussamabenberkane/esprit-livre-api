package com.oussamabenberkane.espritlivre.web.rest;

import com.oussamabenberkane.espritlivre.service.MetaConversionsApiService;
import com.oussamabenberkane.espritlivre.service.dto.PixelEventSummaryDTO;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for Meta Pixel event log (admin only).
 * Security enforced at the filter level: /api/admin/** requires ROLE_ADMIN.
 */
@RestController
@RequestMapping("/api/admin/pixel")
public class PixelEventResource {

    private final MetaConversionsApiService metaConversionsApiService;

    public PixelEventResource(MetaConversionsApiService metaConversionsApiService) {
        this.metaConversionsApiService = metaConversionsApiService;
    }

    /**
     * GET /api/admin/pixel/events
     * Returns a summary of CAPI-dispatched events per type for the last 24h.
     */
    @GetMapping("/events")
    public ResponseEntity<List<PixelEventSummaryDTO>> getPixelEvents() {
        return ResponseEntity.ok(metaConversionsApiService.getRecentEventSummaries());
    }
}
