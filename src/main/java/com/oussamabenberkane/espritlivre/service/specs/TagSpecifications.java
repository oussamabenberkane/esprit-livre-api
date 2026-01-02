package com.oussamabenberkane.espritlivre.service.specs;

import com.oussamabenberkane.espritlivre.domain.Tag;
import com.oussamabenberkane.espritlivre.domain.enumeration.TagType;
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

    public static Specification<Tag> searchByName(String search) {
        return (root, query, criteriaBuilder) -> {
            if (search == null || search.trim().isEmpty()) {
                return null;
            }
            String searchPattern = "%" + search.toLowerCase() + "%";
            return criteriaBuilder.or(
                criteriaBuilder.like(criteriaBuilder.lower(root.get("nameEn")), searchPattern),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("nameFr")), searchPattern)
            );
        };
    }
}