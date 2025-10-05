package com.oussamabenberkane.espritlivre.service;

import com.oussamabenberkane.espritlivre.web.rest.errors.BadRequestAlertException;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import org.springframework.stereotype.Service;

/**
 * Common validation service for reusable validation logic.
 */
@Service
public class ValidationService {

    /**
     * Validates that minPrice is not greater than maxPrice.
     *
     * @param minPrice the minimum price
     * @param maxPrice the maximum price
     * @param entityName the entity name for error reporting
     * @throws BadRequestAlertException if minPrice > maxPrice
     */
    public void validatePriceRange(BigDecimal minPrice, BigDecimal maxPrice, String entityName) {
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new BadRequestAlertException(
                "minPrice cannot be greater than maxPrice",
                entityName,
                "min_price_greater_than_max_price"
            );
        }
    }

    /**
     * Validates that minAmount is not greater than maxAmount.
     *
     * @param minAmount the minimum amount
     * @param maxAmount the maximum amount
     * @param entityName the entity name for error reporting
     * @throws BadRequestAlertException if minAmount > maxAmount
     */
    public void validateAmountRange(BigDecimal minAmount, BigDecimal maxAmount, String entityName) {
        if (minAmount != null && maxAmount != null && minAmount.compareTo(maxAmount) > 0) {
            throw new BadRequestAlertException(
                "minAmount cannot be greater than maxAmount",
                entityName,
                "min_amount_greater_than_max_amount"
            );
        }
    }

    /**
     * Validates that dateFrom is not after dateTo.
     *
     * @param dateFrom the start date
     * @param dateTo the end date
     * @param entityName the entity name for error reporting
     * @throws BadRequestAlertException if dateFrom is after dateTo
     */
    public void validateDateRange(ZonedDateTime dateFrom, ZonedDateTime dateTo, String entityName) {
        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            throw new BadRequestAlertException(
                "dateFrom cannot be after dateTo",
                entityName,
                "date_from_after_date_to"
            );
        }
    }
}
