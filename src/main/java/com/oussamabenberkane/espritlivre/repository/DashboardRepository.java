package com.oussamabenberkane.espritlivre.repository;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Custom repository for dashboard-specific queries.
 * Optimized for performance with aggregated data and specific time ranges.
 */
@Repository
public interface DashboardRepository {
    /**
     * Data projection for best selling book query result.
     */
    public interface BestSellingBookProjection {
        Long getBookId();
        String getBookTitle();
        Long getTotalQuantity();
        BigDecimal getBookPrice();
    }

    /**
     * Data projection for sales time series query result.
     */
    public interface SalesDataProjection {
        String getTimePeriod();
        BigDecimal getTotalSales();
    }

    /**
     * Find the best selling book within a time range.
     *
     * @param startDate start of the time range
     * @param endDate   end of the time range
     * @return best selling book data
     */
    BestSellingBookProjection findBestSellingBook(@Param("startDate") ZonedDateTime startDate, @Param("endDate") ZonedDateTime endDate);

    /**
     * Count total orders within a time range.
     *
     * @param startDate start of the time range
     * @param endDate   end of the time range
     * @return total order count
     */
    Long countOrdersByDateRange(@Param("startDate") ZonedDateTime startDate, @Param("endDate") ZonedDateTime endDate);

    /**
     * Calculate total sales amount within a time range.
     *
     * @param startDate start of the time range
     * @param endDate   end of the time range
     * @return total sales amount
     */
    BigDecimal sumSalesByDateRange(@Param("startDate") ZonedDateTime startDate, @Param("endDate") ZonedDateTime endDate);

    /**
     * Count new users registered within a time range.
     *
     * @param startDate start of the time range
     * @param endDate   end of the time range
     * @return count of new users
     */
    Long countUsersByCreatedAtRange(@Param("startDate") ZonedDateTime startDate, @Param("endDate") ZonedDateTime endDate);

    /**
     * Get hourly sales data for today.
     *
     * @param startOfDay start of the day
     * @param endOfDay   end of the day
     * @return list of hourly sales data
     */
    List<SalesDataProjection> getHourlySales(@Param("startOfDay") ZonedDateTime startOfDay, @Param("endOfDay") ZonedDateTime endOfDay);

    /**
     * Get daily sales data for a week.
     *
     * @param startOfWeek start of the week
     * @param endOfWeek   end of the week
     * @return list of daily sales data
     */
    List<SalesDataProjection> getDailySales(@Param("startOfWeek") ZonedDateTime startOfWeek, @Param("endOfWeek") ZonedDateTime endOfWeek);

    /**
     * Get weekly sales data for a month.
     *
     * @param startOfMonth start of the month
     * @param endOfMonth   end of the month
     * @return list of weekly sales data
     */
    List<SalesDataProjection> getWeeklySales(
        @Param("startOfMonth") ZonedDateTime startOfMonth,
        @Param("endOfMonth") ZonedDateTime endOfMonth
    );

    /**
     * Get monthly sales data for a year.
     *
     * @param startOfYear start of the year
     * @param endOfYear   end of the year
     * @return list of monthly sales data
     */
    List<SalesDataProjection> getMonthlySales(@Param("startOfYear") ZonedDateTime startOfYear, @Param("endOfYear") ZonedDateTime endOfYear);
}
