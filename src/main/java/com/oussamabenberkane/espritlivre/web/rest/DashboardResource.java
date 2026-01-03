package com.oussamabenberkane.espritlivre.web.rest;

import com.oussamabenberkane.espritlivre.service.DashboardService;
import com.oussamabenberkane.espritlivre.service.dto.DashboardStatsDTO;
import com.oussamabenberkane.espritlivre.service.dto.SalesDataPointDTO;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for dashboard statistics and metrics.
 * Provides endpoints for KPI cards and sales chart data.
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardResource {

    private static final Logger LOG = LoggerFactory.getLogger(DashboardResource.class);

    private final DashboardService dashboardService;

    public DashboardResource(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /**
     * GET  /api/dashboard/stats : Get dashboard statistics.
     * Returns KPI metrics including best selling book, new users, orders, and sales.
     *
     * @param timeRange the time range filter (optional, defaults to THIS_MONTH)
     *                  Valid values: TODAY, THIS_WEEK, THIS_MONTH
     * @return the ResponseEntity with status 200 (OK) and dashboard stats in body
     */
    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<DashboardStatsDTO> getDashboardStats(@RequestParam(defaultValue = "THIS_MONTH") String timeRange) {
        LOG.debug("REST request to get dashboard stats for time range: {}", timeRange);
        DashboardStatsDTO stats = dashboardService.getDashboardStats(timeRange);
        return ResponseEntity.ok(stats);
    }

    /**
     * GET  /api/dashboard/sales : Get sales chart data.
     * Returns time-series sales data for different periods.
     *
     * @param period the period type (required)
     *               Valid values: TODAY, THIS_WEEK, MONTH, YEAR
     * @param year   the year for YEAR or MONTH period (optional)
     * @param month  the month for MONTH period (optional, 1-12)
     * @return the ResponseEntity with status 200 (OK) and sales data points in body
     */
    @GetMapping("/sales")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<SalesDataPointDTO>> getSalesChartData(
        @RequestParam String period,
        @RequestParam(required = false) Integer year,
        @RequestParam(required = false) Integer month
    ) {
        LOG.debug("REST request to get sales chart data for period: {}, year: {}, month: {}", period, year, month);
        List<SalesDataPointDTO> salesData = dashboardService.getSalesChartData(period, year, month);
        return ResponseEntity.ok(salesData);
    }
}
