package com.oussamabenberkane.espritlivre.service.specs;

import com.oussamabenberkane.espritlivre.domain.Order;
import com.oussamabenberkane.espritlivre.domain.enumeration.OrderStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
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
}
