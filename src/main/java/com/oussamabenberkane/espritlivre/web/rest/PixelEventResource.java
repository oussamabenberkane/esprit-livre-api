package com.oussamabenberkane.espritlivre.web.rest;

import com.oussamabenberkane.espritlivre.service.MetaConversionsApiService;
import com.oussamabenberkane.espritlivre.service.dto.PixelAddToCartRequestDTO;
import com.oussamabenberkane.espritlivre.service.dto.PixelCompleteRegistrationRequestDTO;
import com.oussamabenberkane.espritlivre.service.dto.PixelContactRequestDTO;
import com.oussamabenberkane.espritlivre.service.dto.PixelEventSummaryDTO;
import com.oussamabenberkane.espritlivre.service.dto.PixelInitiateCheckoutRequestDTO;
import com.oussamabenberkane.espritlivre.service.dto.PixelPageViewRequestDTO;
import com.oussamabenberkane.espritlivre.service.dto.PixelSearchRequestDTO;
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
            body.eventSourceUrl(),
            body.fbc(),
            body.fbp()
        );
        return ResponseEntity.ok().build();
    }

    @PostMapping("/api/pixel/page-view")
    public ResponseEntity<Void> trackPageView(@RequestBody PixelPageViewRequestDTO body) {
        if (!StringUtils.hasText(body.eventId())) {
            return ResponseEntity.badRequest().build();
        }
        metaConversionsApiService.sendPageViewEvent(body.eventId(), body.eventSourceUrl(), body.fbc(), body.fbp());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/api/pixel/search")
    public ResponseEntity<Void> trackSearch(@RequestBody PixelSearchRequestDTO body) {
        if (!StringUtils.hasText(body.eventId()) || !StringUtils.hasText(body.searchString())) {
            return ResponseEntity.badRequest().build();
        }
        metaConversionsApiService.sendSearchEvent(body.eventId(), body.searchString(), body.eventSourceUrl(), body.fbc(), body.fbp());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/api/pixel/add-to-cart")
    public ResponseEntity<Void> trackAddToCart(@RequestBody PixelAddToCartRequestDTO body) {
        if (!StringUtils.hasText(body.eventId()) || !StringUtils.hasText(body.contentId())) {
            return ResponseEntity.badRequest().build();
        }
        metaConversionsApiService.sendAddToCartEvent(
            body.eventId(), body.contentId(), body.contentType(), body.value(), body.numItems(), body.eventSourceUrl(), body.fbc(), body.fbp()
        );
        return ResponseEntity.ok().build();
    }

    @PostMapping("/api/pixel/initiate-checkout")
    public ResponseEntity<Void> trackInitiateCheckout(@RequestBody PixelInitiateCheckoutRequestDTO body) {
        if (!StringUtils.hasText(body.eventId())) {
            return ResponseEntity.badRequest().build();
        }
        metaConversionsApiService.sendInitiateCheckoutEvent(
            body.eventId(), body.value(), body.numItems(), body.contentIds(), body.eventSourceUrl(), body.fbc(), body.fbp()
        );
        return ResponseEntity.ok().build();
    }

    @PostMapping("/api/pixel/complete-registration")
    public ResponseEntity<Void> trackCompleteRegistration(@RequestBody PixelCompleteRegistrationRequestDTO body) {
        if (!StringUtils.hasText(body.eventId())) {
            return ResponseEntity.badRequest().build();
        }
        metaConversionsApiService.sendCompleteRegistrationEvent(body.eventId(), body.eventSourceUrl(), body.fbc(), body.fbp());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/api/pixel/contact")
    public ResponseEntity<Void> trackContact(@RequestBody PixelContactRequestDTO body) {
        if (!StringUtils.hasText(body.eventId())) {
            return ResponseEntity.badRequest().build();
        }
        metaConversionsApiService.sendContactEvent(body.eventId(), body.eventSourceUrl(), body.fbc(), body.fbp());
        return ResponseEntity.ok().build();
    }
}
