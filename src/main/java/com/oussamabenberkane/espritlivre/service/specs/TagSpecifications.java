package com.oussamabenberkane.espritlivre.service.specs;

import com.oussamabenberkane.espritlivre.domain.Tag;
import com.oussamabenberkane.espritlivre.domain.enumeration.TagType;
import com.oussamabenberkane.espritlivre.service.util.TextNormalizationUtils;
import jakarta.persistence.criteria.Expression;
import org.springframework.data.jpa.domain.Specification;

public class TagSpecifications {

    /**
     * Specification to filter only active (non-soft-deleted) tags.
     * This should be composed with all queries to exclude soft-deleted entities.
     */
    public static Specification<Tag> activeOnly() {
        return (root, query, builder) -> builder.equal(root.get("active"), true);
    }

    /**
     * @deprecated Use {@link #activeOnly()} instead for consistency across all specifications
     */
    @Deprecated
    public static Specification<Tag> isActive() {
        return activeOnly();
    }

    public static Specification<Tag> hasType(TagType type) {
        return (root, query, criteriaBuilder) ->
            type == null ? null : criteriaBuilder.equal(root.get("type"), type);
    }

    /**
     * Search tags by name with accent-insensitive matching.
     * Uses PostgreSQL unaccent() function for database-level accent normalization.
     * Searches in both English and French name fields.
     *
     * @param search the search term
     * @return the specification
     */
    public static Specification<Tag> searchByName(String search) {
        return (root, query, criteriaBuilder) -> {
            if (search == null || search.trim().isEmpty()) {
                return null;
            }
            // Normalize search term to remove accents
            String normalizedSearch = TextNormalizationUtils.normalizeForSearch(search);
            String searchPattern = "%" + normalizedSearch + "%";
            // Use unaccent(lower(name)) for accent-insensitive matching
            Expression<String> nameEnUnaccent = criteriaBuilder.function("unaccent", String.class, criteriaBuilder.lower(root.get("nameEn")));
            Expression<String> nameFrUnaccent = criteriaBuilder.function("unaccent", String.class, criteriaBuilder.lower(root.get("nameFr")));
            return criteriaBuilder.or(
                criteriaBuilder.like(nameEnUnaccent, searchPattern),
                criteriaBuilder.like(nameFrUnaccent, searchPattern)
            );
        };
    }
}