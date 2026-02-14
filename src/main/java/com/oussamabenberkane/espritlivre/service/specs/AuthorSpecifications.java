package com.oussamabenberkane.espritlivre.service.specs;

import com.oussamabenberkane.espritlivre.domain.Author;
import com.oussamabenberkane.espritlivre.service.util.TextNormalizationUtils;
import jakarta.persistence.criteria.Expression;
import org.springframework.data.jpa.domain.Specification;

public class AuthorSpecifications {

    /**
     * Specification to filter only active (non-soft-deleted) authors.
     * This should be composed with all queries to exclude soft-deleted entities.
     */
    public static Specification<Author> activeOnly() {
        return (root, query, builder) -> builder.equal(root.get("active"), true);
    }

    /**
     * Search authors by name with accent-insensitive matching.
     * Uses PostgreSQL unaccent() function for database-level accent normalization.
     *
     * @param search the search term
     * @return the specification
     */
    public static Specification<Author> searchByName(String search) {
        return (root, query, criteriaBuilder) -> {
            if (search == null || search.trim().isEmpty()) {
                return null;
            }
            // Normalize search term to remove accents
            String normalizedSearch = TextNormalizationUtils.normalizeForSearch(search);
            String searchPattern = "%" + normalizedSearch + "%";
            // Use unaccent(lower(name)) for accent-insensitive matching
            Expression<String> nameUnaccent = criteriaBuilder.function("unaccent", String.class, criteriaBuilder.lower(root.get("name")));
            return criteriaBuilder.like(nameUnaccent, searchPattern);
        };
    }
}
