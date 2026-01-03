package com.oussamabenberkane.espritlivre.service;

import com.oussamabenberkane.espritlivre.repository.DashboardRepository;
import com.oussamabenberkane.espritlivre.service.dto.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for dashboard statistics and metrics.
 * Handles aggregation, time-based filtering, and growth calculations.
 */
@Service
@Transactional(readOnly = true)
public class DashboardService {

    private static final Logger LOG = LoggerFactory.getLogger(DashboardService.class);
    private static final ZoneId ALGERIA_ZONE = ZoneId.of("Africa/Algiers");

    private final DashboardRepository dashboardRepository;

    public DashboardService(DashboardRepository dashboardRepository) {
        this.dashboardRepository = dashboardRepository;
    }

    /**
     * Get comprehensive dashboard statistics for a specific time range.
     *
     * @param timeRange the time range filter (TODAY, THIS_WEEK, THIS_MONTH)
     * @return dashboard statistics DTO
     */
    public DashboardStatsDTO getDashboardStats(String timeRange) {
        LOG.debug("Getting dashboard stats for time range: {}", timeRange);

        ZonedDateTime now = ZonedDateTime.now(ALGERIA_ZONE);
        TimeRangeBounds currentBounds = getTimeRangeBounds(timeRange, now);
        TimeRangeBounds previousBounds = getPreviousTimeRangeBounds(timeRange, now);

        // Get best selling book
        BestSellingBookDTO bestSellingBook = getBestSellingBook(currentBounds.start, currentBounds.end);

        // Get new users stats
        NewUsersDTO newUsers = getNewUsersStats(now);

        // Get total orders for the time range
        Long totalOrders = dashboardRepository.countOrdersByDateRange(currentBounds.start, currentBounds.end);

        // Get monthly sales (always current month regardless of timeRange)
        TimeRangeBounds monthBounds = getTimeRangeBounds("THIS_MONTH", now);
        BigDecimal monthlySales = dashboardRepository.sumSalesByDateRange(monthBounds.start, monthBounds.end);

        // Calculate growth metrics
        GrowthMetricsDTO growth = calculateGrowthMetrics(currentBounds, previousBounds, now);

        return new DashboardStatsDTO(bestSellingBook, newUsers, totalOrders, monthlySales, growth);
    }

    /**
     * Get sales chart data for different time periods.
     *
     * @param period the period type (TODAY, THIS_WEEK, MONTH, YEAR)
     * @param year   the year for YEAR or MONTH period (optional)
     * @param month  the month for MONTH period (optional, 1-12)
     * @return list of sales data points
     */
    public List<SalesDataPointDTO> getSalesChartData(String period, Integer year, Integer month) {
        LOG.debug("Getting sales chart data for period: {}, year: {}, month: {}", period, year, month);

        ZonedDateTime now = ZonedDateTime.now(ALGERIA_ZONE);

        switch (period.toUpperCase()) {
            case "TODAY":
            case "AUJOURD'HUI":
                return getHourlySalesData(now);
            case "THIS_WEEK":
            case "CETTE SEMAINE":
                return getDailySalesData(now);
            case "MONTH":
            case "MOIS":
                int targetYear = year != null ? year : now.getYear();
                int targetMonth = month != null ? month : now.getMonthValue();
                return getWeeklySalesData(targetYear, targetMonth);
            case "YEAR":
            case "ANNÉE":
                int targetYearForYear = year != null ? year : now.getYear();
                return getMonthlySalesData(targetYearForYear);
            default:
                LOG.warn("Unknown period: {}, defaulting to monthly", period);
                return getWeeklySalesData(now.getYear(), now.getMonthValue());
        }
    }

    // Private helper methods

    /**
     * Get best selling book for a time range.
     */
    private BestSellingBookDTO getBestSellingBook(ZonedDateTime startDate, ZonedDateTime endDate) {
        DashboardRepository.BestSellingBookProjection projection = dashboardRepository.findBestSellingBook(startDate, endDate);

        if (projection == null || projection.getBookId() == null) {
            return new BestSellingBookDTO(null, "Aucun livre", 0L, BigDecimal.ZERO);
        }

        return new BestSellingBookDTO(projection.getBookId(), projection.getBookTitle(), projection.getTotalQuantity(), projection.getBookPrice());
    }

    /**
     * Get new users statistics with breakdown by time periods.
     */
    private NewUsersDTO getNewUsersStats(ZonedDateTime now) {
        // Today
        ZonedDateTime startOfToday = now.truncatedTo(ChronoUnit.DAYS);
        ZonedDateTime endOfToday = startOfToday.plusDays(1);
        Long today = dashboardRepository.countUsersByCreatedAtRange(startOfToday, endOfToday);

        // This week
        ZonedDateTime startOfWeek = now.with(DayOfWeek.MONDAY).truncatedTo(ChronoUnit.DAYS);
        ZonedDateTime endOfWeek = startOfWeek.plusWeeks(1);
        Long thisWeek = dashboardRepository.countUsersByCreatedAtRange(startOfWeek, endOfWeek);

        // This month
        ZonedDateTime startOfMonth = now.with(TemporalAdjusters.firstDayOfMonth()).truncatedTo(ChronoUnit.DAYS);
        ZonedDateTime endOfMonth = startOfMonth.plusMonths(1);
        Long thisMonth = dashboardRepository.countUsersByCreatedAtRange(startOfMonth, endOfMonth);

        // Total is the same as the selected time range filter in the parent method
        // For the card, we use thisMonth as total
        return new NewUsersDTO(thisMonth, today, thisWeek, thisMonth);
    }

    /**
     * Calculate growth metrics by comparing current and previous periods.
     */
    private GrowthMetricsDTO calculateGrowthMetrics(TimeRangeBounds currentBounds, TimeRangeBounds previousBounds, ZonedDateTime now) {
        // Best selling book growth
        DashboardRepository.BestSellingBookProjection currentBestBook = dashboardRepository.findBestSellingBook(
            currentBounds.start,
            currentBounds.end
        );
        DashboardRepository.BestSellingBookProjection previousBestBook = dashboardRepository.findBestSellingBook(
            previousBounds.start,
            previousBounds.end
        );
        GrowthMetricDTO bestSellingBookGrowth = calculateGrowth(
            currentBestBook != null ? currentBestBook.getTotalQuantity() : 0L,
            previousBestBook != null ? previousBestBook.getTotalQuantity() : 0L
        );

        // New users growth (compare this month vs last month)
        ZonedDateTime startOfThisMonth = now.with(TemporalAdjusters.firstDayOfMonth()).truncatedTo(ChronoUnit.DAYS);
        ZonedDateTime endOfThisMonth = startOfThisMonth.plusMonths(1);
        ZonedDateTime startOfLastMonth = startOfThisMonth.minusMonths(1);
        ZonedDateTime endOfLastMonth = startOfThisMonth;

        Long thisMonthUsers = dashboardRepository.countUsersByCreatedAtRange(startOfThisMonth, endOfThisMonth);
        Long lastMonthUsers = dashboardRepository.countUsersByCreatedAtRange(startOfLastMonth, endOfLastMonth);
        GrowthMetricDTO newUsersGrowth = calculateGrowth(thisMonthUsers, lastMonthUsers);

        // Orders growth
        Long currentOrders = dashboardRepository.countOrdersByDateRange(currentBounds.start, currentBounds.end);
        Long previousOrders = dashboardRepository.countOrdersByDateRange(previousBounds.start, previousBounds.end);
        GrowthMetricDTO ordersGrowth = calculateGrowth(currentOrders, previousOrders);

        // Sales growth (compare this month vs last month for consistency)
        BigDecimal thisMonthSales = dashboardRepository.sumSalesByDateRange(startOfThisMonth, endOfThisMonth);
        BigDecimal lastMonthSales = dashboardRepository.sumSalesByDateRange(startOfLastMonth, endOfLastMonth);
        GrowthMetricDTO salesGrowth = calculateGrowthFromBigDecimal(thisMonthSales, lastMonthSales);

        return new GrowthMetricsDTO(bestSellingBookGrowth, newUsersGrowth, ordersGrowth, salesGrowth);
    }

    /**
     * Calculate growth percentage between current and previous values.
     */
    private GrowthMetricDTO calculateGrowth(Long current, Long previous) {
        if (previous == null || previous == 0) {
            return new GrowthMetricDTO(0.0, current != null && current > 0);
        }

        double growth = ((current - previous) * 100.0) / previous;
        return new GrowthMetricDTO(Math.round(growth * 100.0) / 100.0, growth >= 0);
    }

    /**
     * Calculate growth percentage from BigDecimal values.
     */
    private GrowthMetricDTO calculateGrowthFromBigDecimal(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            return new GrowthMetricDTO(0.0, current != null && current.compareTo(BigDecimal.ZERO) > 0);
        }

        BigDecimal growth = current.subtract(previous).multiply(new BigDecimal("100")).divide(previous, 2, RoundingMode.HALF_UP);
        return new GrowthMetricDTO(growth.doubleValue(), growth.compareTo(BigDecimal.ZERO) >= 0);
    }

    /**
     * Get hourly sales data for today.
     */
    private List<SalesDataPointDTO> getHourlySalesData(ZonedDateTime now) {
        ZonedDateTime startOfDay = now.truncatedTo(ChronoUnit.DAYS);
        ZonedDateTime endOfDay = startOfDay.plusDays(1);

        List<DashboardRepository.SalesDataProjection> data = dashboardRepository.getHourlySales(startOfDay, endOfDay);

        // Create map of existing data
        Map<String, BigDecimal> salesMap = new HashMap<>();
        for (DashboardRepository.SalesDataProjection projection : data) {
            salesMap.put(projection.getTimePeriod(), projection.getTotalSales());
        }

        // Generate all 24 hours
        List<SalesDataPointDTO> result = new ArrayList<>();
        for (int hour = 0; hour < 24; hour++) {
            String hourStr = String.format("%02d", hour);
            BigDecimal sales = salesMap.getOrDefault(hourStr, BigDecimal.ZERO);
            result.add(new SalesDataPointDTO(hour + "h", sales));
        }

        return result;
    }

    /**
     * Get daily sales data for this week.
     */
    private List<SalesDataPointDTO> getDailySalesData(ZonedDateTime now) {
        ZonedDateTime startOfWeek = now.with(DayOfWeek.MONDAY).truncatedTo(ChronoUnit.DAYS);
        ZonedDateTime endOfWeek = startOfWeek.plusWeeks(1);

        List<DashboardRepository.SalesDataProjection> data = dashboardRepository.getDailySales(startOfWeek, endOfWeek);

        // Create map of existing data (day of week 1-7)
        Map<String, BigDecimal> salesMap = new HashMap<>();
        for (DashboardRepository.SalesDataProjection projection : data) {
            salesMap.put(projection.getTimePeriod(), projection.getTotalSales());
        }

        // Generate all 7 days
        String[] dayNames = { "Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim" };
        List<SalesDataPointDTO> result = new ArrayList<>();
        for (int i = 1; i <= 7; i++) {
            String dayOfWeek = String.valueOf(i);
            BigDecimal sales = salesMap.getOrDefault(dayOfWeek, BigDecimal.ZERO);
            result.add(new SalesDataPointDTO(dayNames[i - 1], sales));
        }

        return result;
    }

    /**
     * Get weekly sales data for a specific month.
     */
    private List<SalesDataPointDTO> getWeeklySalesData(int year, int month) {
        ZonedDateTime startOfMonth = ZonedDateTime.of(year, month, 1, 0, 0, 0, 0, ALGERIA_ZONE);
        ZonedDateTime endOfMonth = startOfMonth.plusMonths(1);

        List<DashboardRepository.SalesDataProjection> data = dashboardRepository.getWeeklySales(startOfMonth, endOfMonth);

        // Create map of existing data
        Map<String, BigDecimal> salesMap = new HashMap<>();
        for (DashboardRepository.SalesDataProjection projection : data) {
            salesMap.put(projection.getTimePeriod(), projection.getTotalSales());
        }

        // Generate 4 weeks (simplified)
        List<SalesDataPointDTO> result = new ArrayList<>();
        for (int week = 1; week <= 4; week++) {
            // Get week number for that week in the month
            ZonedDateTime weekDate = startOfMonth.plusWeeks(week - 1);
            String weekNum = String.format("%02d", weekDate.get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR));
            BigDecimal sales = salesMap.getOrDefault(weekNum, BigDecimal.ZERO);
            result.add(new SalesDataPointDTO("Sem " + week, sales));
        }

        return result;
    }

    /**
     * Get monthly sales data for a specific year.
     */
    private List<SalesDataPointDTO> getMonthlySalesData(int year) {
        ZonedDateTime startOfYear = ZonedDateTime.of(year, 1, 1, 0, 0, 0, 0, ALGERIA_ZONE);
        ZonedDateTime endOfYear = startOfYear.plusYears(1);

        List<DashboardRepository.SalesDataProjection> data = dashboardRepository.getMonthlySales(startOfYear, endOfYear);

        // Create map of existing data
        Map<String, BigDecimal> salesMap = new HashMap<>();
        for (DashboardRepository.SalesDataProjection projection : data) {
            salesMap.put(projection.getTimePeriod(), projection.getTotalSales());
        }

        // Generate all 12 months
        String[] monthNames = { "Jan", "Fév", "Mar", "Avr", "Mai", "Jun", "Jul", "Aoû", "Sep", "Oct", "Nov", "Déc" };
        List<SalesDataPointDTO> result = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            String monthStr = String.format("%02d", m);
            BigDecimal sales = salesMap.getOrDefault(monthStr, BigDecimal.ZERO);
            result.add(new SalesDataPointDTO(monthNames[m - 1], sales));
        }

        return result;
    }

    /**
     * Get time range bounds based on the time range filter.
     */
    private TimeRangeBounds getTimeRangeBounds(String timeRange, ZonedDateTime now) {
        switch (timeRange.toUpperCase()) {
            case "TODAY":
            case "AUJOURD'HUI":
                ZonedDateTime startOfToday = now.truncatedTo(ChronoUnit.DAYS);
                return new TimeRangeBounds(startOfToday, startOfToday.plusDays(1));
            case "THIS_WEEK":
            case "CETTE SEMAINE":
                ZonedDateTime startOfWeek = now.with(DayOfWeek.MONDAY).truncatedTo(ChronoUnit.DAYS);
                return new TimeRangeBounds(startOfWeek, startOfWeek.plusWeeks(1));
            case "THIS_MONTH":
            case "CE MOIS-CI":
                ZonedDateTime startOfMonth = now.with(TemporalAdjusters.firstDayOfMonth()).truncatedTo(ChronoUnit.DAYS);
                return new TimeRangeBounds(startOfMonth, startOfMonth.plusMonths(1));
            default:
                LOG.warn("Unknown time range: {}, defaulting to THIS_MONTH", timeRange);
                ZonedDateTime defaultStart = now.with(TemporalAdjusters.firstDayOfMonth()).truncatedTo(ChronoUnit.DAYS);
                return new TimeRangeBounds(defaultStart, defaultStart.plusMonths(1));
        }
    }

    /**
     * Get previous time range bounds for growth comparison.
     */
    private TimeRangeBounds getPreviousTimeRangeBounds(String timeRange, ZonedDateTime now) {
        TimeRangeBounds currentBounds = getTimeRangeBounds(timeRange, now);
        long daysInPeriod = java.time.Duration.between(currentBounds.start, currentBounds.end).toDays();

        ZonedDateTime previousStart = currentBounds.start.minusDays(daysInPeriod);
        ZonedDateTime previousEnd = currentBounds.start;

        return new TimeRangeBounds(previousStart, previousEnd);
    }

    /**
     * Internal class to hold time range bounds.
     */
    private static class TimeRangeBounds {

        final ZonedDateTime start;
        final ZonedDateTime end;

        TimeRangeBounds(ZonedDateTime start, ZonedDateTime end) {
            this.start = start;
            this.end = end;
        }
    }
}
