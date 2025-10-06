package com.oussamabenberkane.espritlivre.service.specs;

import com.oussamabenberkane.espritlivre.domain.Tag;
import com.oussamabenberkane.espritlivre.domain.enumeration.TagType;
import org.springframework.data.jpa.domain.Specification;

public class TagSpecifications {

    public static Specification<Tag> hasType(TagType type) {
        return (root, query, criteriaBuilder) ->
            type == null ? null : criteriaBuilder.equal(root.get("type"), type);
    }

    public static Specification<Tag> isActive() {
        return (root, query, criteriaBuilder) ->
            criteriaBuilder.equal(root.get("active"), true);
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