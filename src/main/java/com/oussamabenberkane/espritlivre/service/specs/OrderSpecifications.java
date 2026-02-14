package com.oussamabenberkane.espritlivre.service.specs;

import com.oussamabenberkane.espritlivre.domain.Order;
import com.oussamabenberkane.espritlivre.domain.enumeration.OrderStatus;
import com.oussamabenberkane.espritlivre.service.util.TextNormalizationUtils;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

public class OrderSpecifications {

    /**
     * Specification to filter only active (non-soft-deleted) orders.
     * This should be composed with all queries to exclude soft-deleted entities.
     */
    public static Specification<Order> activeOnly() {
        return (root, query, builder) -> builder.equal(root.get("active"), true);
    }

    public static Specification<Order> hasStatus(OrderStatus status) {
        return (root, query, builder) -> {
            if (status == null) {
                return builder.conjunction();
            }
            return builder.equal(root.get("status"), status);
        };
    }

    public static Specification<Order> createdBetween(ZonedDateTime dateFrom, ZonedDateTime dateTo) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (dateFrom != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("createdAt"), dateFrom));
            }

            if (dateTo != null) {
                predicates.add(builder.lessThanOrEqualTo(root.get("createdAt"), dateTo));
            }

            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Order> totalAmountBetween(BigDecimal minAmount, BigDecimal maxAmount) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (minAmount != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("totalAmount"), minAmount));
            }

            if (maxAmount != null) {
                predicates.add(builder.lessThanOrEqualTo(root.get("totalAmount"), maxAmount));
            }

            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Specification to search orders by text.
     * Searches in: order ID (numeric), customer name, email, phone number.
     * Uses case-insensitive partial matching (LIKE with LOWER).
     * Phone search is normalized to handle leading zeros and country codes
     * (e.g., searching "0549697533" will match "+213549697533").
     *
     * @param searchTerm the search term
     * @return the specification
     */
    public static Specification<Order> searchByText(String searchTerm) {
        return (root, query, builder) -> {
            if (!StringUtils.hasText(searchTerm)) {
                return builder.conjunction();
            }

            String trimmedSearch = searchTerm.trim();
            String searchPattern = "%" + trimmedSearch.toLowerCase() + "%";
            List<Predicate> predicates = new ArrayList<>();

            // Search in customer name (fullName)
            predicates.add(builder.like(builder.lower(root.get("fullName")), searchPattern));

            // Search in email
            predicates.add(builder.like(builder.lower(root.get("email")), searchPattern));

            // Search in phone number with normalization for flexible matching
            // This handles cases like searching "0549697533" to find "+213549697533"
            String normalizedPhone = TextNormalizationUtils.normalizePhoneForSearch(trimmedSearch);
            if (normalizedPhone != null && !normalizedPhone.isEmpty()) {
                // Match the normalized digits anywhere in the stored phone number
                predicates.add(builder.like(root.get("phone"), "%" + normalizedPhone + "%"));
            }

            // Search by order ID if the search term is numeric
            try {
                Long orderId = Long.parseLong(trimmedSearch);
                predicates.add(builder.equal(root.get("id"), orderId));
            } catch (NumberFormatException e) {
                // Not a number, skip ID search
            }

            return builder.or(predicates.toArray(new Predicate[0]));
        };
    }
}
