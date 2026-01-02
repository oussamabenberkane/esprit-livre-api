package com.oussamabenberkane.espritlivre.service.specs;

import com.oussamabenberkane.espritlivre.domain.Author;
import org.springframework.data.jpa.domain.Specification;

public class AuthorSpecifications {

    /**
     * Specification to filter only active (non-soft-deleted) authors.
     * This should be composed with all queries to exclude soft-deleted entities.
     */
    public static Specification<Author> activeOnly() {
        return (root, query, builder) -> builder.equal(root.get("active"), true);
    }

    public static Specification<Author> searchByName(String search) {
        return (root, query, criteriaBuilder) -> {
            if (search == null || search.trim().isEmpty()) {
                return null;
            }
            String searchPattern = "%" + search.toLowerCase() + "%";
            return criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), searchPattern);
        };
    }
}
