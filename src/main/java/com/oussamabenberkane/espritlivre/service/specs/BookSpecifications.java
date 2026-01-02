package com.oussamabenberkane.espritlivre.service.specs;

import com.oussamabenberkane.espritlivre.domain.Author;
import com.oussamabenberkane.espritlivre.domain.Book;
import com.oussamabenberkane.espritlivre.domain.Tag;
import com.oussamabenberkane.espritlivre.domain.enumeration.Language;
import com.oussamabenberkane.espritlivre.domain.enumeration.TagType;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BookSpecifications {

    /**
     * Specification to filter only active (non-soft-deleted) books.
     * This should be composed with all queries to exclude soft-deleted entities.
     */
    public static Specification<Book> activeOnly() {
        return (root, query, builder) -> builder.equal(root.get("active"), true);
    }

    public static Specification<Book> hasAuthor(List<Long> authorIds) {
        return (root, query, builder) -> {
            if (authorIds == null || authorIds.isEmpty()) {
                return builder.conjunction();
            }
            Join<Book, Author> authorJoin = root.join("author", JoinType.INNER);
            return authorJoin.get("id").in(authorIds);
        };
    }

    public static Specification<Book> hasPriceBetween(BigDecimal minPrice, BigDecimal maxPrice) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (minPrice != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("price"), minPrice));
            }

            if (maxPrice != null) {
                predicates.add(builder.lessThanOrEqualTo(root.get("price"), maxPrice));
            }

            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Book> hasTagOfType(TagType tagType, Long tagId) {
        return (root, query, builder) -> {
            if (tagId == null) {
                return builder.conjunction();
            }

            // Add distinct to avoid duplicates when joining
            query.distinct(true);

            Join<Book, Tag> tagJoin = root.join("tags", JoinType.INNER);
            return builder.and(
                builder.equal(tagJoin.get("type"), tagType),
                builder.equal(tagJoin.get("id"), tagId)
            );
        };
    }

    public static Specification<Book> hasCategory(Long categoryId) {
        return hasTagOfType(TagType.CATEGORY, categoryId);
    }

    public static Specification<Book> hasMainDisplay(Long mainDisplayId) {
        return hasTagOfType(TagType.MAIN_DISPLAY, mainDisplayId);
    }

    public static Specification<Book> searchByText(String searchTerm) {
        return (root, query, builder) -> {
            if (searchTerm == null || searchTerm.trim().isEmpty()) {
                return builder.conjunction();
            }

            String searchPattern = "%" + searchTerm.toLowerCase().trim() + "%";
            query.distinct(true);

            // Search in title and author
            Predicate titleMatch = builder.like(builder.lower(root.get("title")), searchPattern);

            // Search in author name
            Join<Book, Author> authorJoin = root.join("author", JoinType.LEFT);
            Predicate authorMatch = builder.like(builder.lower(authorJoin.get("name")), searchPattern);

            // Search in tag names (both English and French)
            Join<Book, Tag> tagJoin = root.join("tags", JoinType.LEFT);
            Predicate tagNameEnMatch = builder.like(builder.lower(tagJoin.get("nameEn")), searchPattern);
            Predicate tagNameFrMatch = builder.like(builder.lower(tagJoin.get("nameFr")), searchPattern);

            return builder.or(titleMatch, authorMatch, tagNameEnMatch, tagNameFrMatch);
        };
    }

    public static Specification<Book> isLikedByCurrentUser() {
        return (root, query, builder) -> {
            query.distinct(true);
            return builder.isNotNull(root.get("id"));
        };
    }

    public static Specification<Book> hasLanguage(List<String> languages) {
        return (root, query, builder) -> {
            if (languages == null || languages.isEmpty()) {
                return builder.conjunction();
            }

            // Convert string list to Language enum list
            List<Language> languageEnums = languages.stream()
                .map(lang -> {
                    try {
                        return Language.valueOf(lang.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        return null;
                    }
                })
                .filter(lang -> lang != null)
                .collect(Collectors.toList());

            if (languageEnums.isEmpty()) {
                return builder.conjunction();
            }

            return root.get("language").in(languageEnums);
        };
    }
}
