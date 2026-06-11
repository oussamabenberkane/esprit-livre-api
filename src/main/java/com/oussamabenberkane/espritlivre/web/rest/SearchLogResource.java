package com.oussamabenberkane.espritlivre.web.rest;

import com.oussamabenberkane.espritlivre.service.SearchLogService;
import com.oussamabenberkane.espritlivre.service.dto.SearchLogDTO;
import com.oussamabenberkane.espritlivre.service.dto.SearchLogStatsDTO;
import com.oussamabenberkane.espritlivre.service.dto.SearchTermStatDTO;
import com.oussamabenberkane.espritlivre.service.dto.SearchTrendPointDTO;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Search analytics for the admin dashboard.
 * Admin-only: /api/admin/** is restricted to ROLE_ADMIN by SecurityConfiguration.
 */
@RestController
@RequestMapping("/api/admin/search-logs")
@Validated
public class SearchLogResource {

    private final SearchLogService searchLogService;

    public SearchLogResource(SearchLogService searchLogService) {
        this.searchLogService = searchLogService;
    }

    @GetMapping("/stats")
    public ResponseEntity<SearchLogStatsDTO> getStats(
        @RequestParam(value = "days", defaultValue = "30") @Min(1) @Max(365) int days
    ) {
        return ResponseEntity.ok(searchLogService.getStats(days));
    }

    @GetMapping("/top-terms")
    public ResponseEntity<List<SearchTermStatDTO>> getTopTerms(
        @RequestParam(value = "days", defaultValue = "30") @Min(1) @Max(365) int days,
        @RequestParam(value = "limit", defaultValue = "20") @Min(1) @Max(100) int limit
    ) {
        return ResponseEntity.ok(searchLogService.getTopTerms(days, limit));
    }

    @GetMapping("/zero-results")
    public ResponseEntity<List<SearchTermStatDTO>> getZeroResultTerms(
        @RequestParam(value = "days", defaultValue = "30") @Min(1) @Max(365) int days,
        @RequestParam(value = "limit", defaultValue = "20") @Min(1) @Max(100) int limit
    ) {
        return ResponseEntity.ok(searchLogService.getTopZeroResultTerms(days, limit));
    }

    @GetMapping("/trend")
    public ResponseEntity<List<SearchTrendPointDTO>> getTrend(
        @RequestParam(value = "days", defaultValue = "30") @Min(1) @Max(365) int days
    ) {
        return ResponseEntity.ok(searchLogService.getDailyTrend(days));
    }

    @GetMapping("")
    public ResponseEntity<Page<SearchLogDTO>> getRecentSearches(@ParameterObject Pageable pageable) {
        return ResponseEntity.ok(searchLogService.getRecentSearches(pageable));
    }
}
