package com.oussamabenberkane.espritlivre.web.rest;

import com.oussamabenberkane.espritlivre.service.MetaConversionsApiService;
import com.oussamabenberkane.espritlivre.service.dto.PixelEventSummaryDTO;
import com.oussamabenberkane.espritlivre.service.dto.PixelViewContentRequestDTO;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PixelEventResource {

    private final MetaConversionsApiService metaConversionsApiService;

    public PixelEventResource(MetaConversionsApiService metaConversionsApiService) {
        this.metaConversionsApiService = metaConversionsApiService;
    }

    /**
     * GET /api/admin/pixel/events — admin only (enforced by security filter).
     */
    @GetMapping("/api/admin/pixel/events")
    public ResponseEntity<List<PixelEventSummaryDTO>> getPixelEvents(
        @RequestParam(value = "period", defaultValue = "HOURS_24") String period
    ) {
        return ResponseEntity.ok(metaConversionsApiService.getRecentEventSummaries(period));
    }

    /**
     * POST /api/pixel/view-content — public, no auth required.
     * Receives a ViewContent event from the browser and forwards it to Meta CAPI.
     */
    @PostMapping("/api/pixel/view-content")
    public ResponseEntity<Void> trackViewContent(@RequestBody PixelViewContentRequestDTO body) {
        if (!StringUtils.hasText(body.eventId()) || !StringUtils.hasText(body.contentId())) {
            return ResponseEntity.badRequest().build();
        }
        metaConversionsApiService.sendViewContentEvent(
            body.eventId(),
            body.contentId(),
            body.contentType(),
            body.value(),
            body.eventSourceUrl()
        );
        return ResponseEntity.ok().build();
    }
}
