package com.oussamabenberkane.espritlivre.service.specs;

import com.oussamabenberkane.espritlivre.domain.Author;
import com.oussamabenberkane.espritlivre.domain.Book;
import com.oussamabenberkane.espritlivre.domain.BookPack;
import com.oussamabenberkane.espritlivre.domain.Tag;
import com.oussamabenberkane.espritlivre.domain.enumeration.Language;
import com.oussamabenberkane.espritlivre.domain.enumeration.TagType;
import com.oussamabenberkane.espritlivre.service.util.TextNormalizationUtils;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BookPackSpecifications {

    /**
     * Specification to filter only active (non-soft-deleted) book packs.
     * This should be composed with all queries to exclude soft-deleted entities.
     */
    public static Specification<BookPack> activeOnly() {
        return (root, query, builder) -> builder.equal(root.get("active"), true);
    }

    public static Specification<BookPack> hasAuthor(List<Long> authorIds) {
        return (root, query, builder) -> {
            if (authorIds == null || authorIds.isEmpty()) {
                return builder.conjunction();
            }
            query.distinct(true);
            Join<BookPack, Book> bookJoin = root.join("books", JoinType.INNER);
            Join<Book, Author> authorJoin = bookJoin.join("author", JoinType.INNER);
            return authorJoin.get("id").in(authorIds);
        };
    }

    public static Specification<BookPack> hasPriceBetween(BigDecimal minPrice, BigDecimal maxPrice) {
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

    public static Specification<BookPack> hasTagOfType(TagType tagType, Long tagId) {
        return (root, query, builder) -> {
            if (tagId == null) {
                return builder.conjunction();
            }

            query.distinct(true);

            Join<BookPack, Book> bookJoin = root.join("books", JoinType.INNER);
            Join<Book, Tag> tagJoin = bookJoin.join("tags", JoinType.INNER);
            return builder.and(
                builder.equal(tagJoin.get("type"), tagType),
                builder.equal(tagJoin.get("id"), tagId)
            );
        };
    }

    public static Specification<BookPack> hasCategory(Long categoryId) {
        return hasTagOfType(TagType.CATEGORY, categoryId);
    }

    public static Specification<BookPack> hasMainDisplay(Long mainDisplayId) {
        return (root, query, builder) -> {
            if (mainDisplayId == null) {
                return builder.conjunction();
            }

            query.distinct(true);

            // Use the direct Tag <-> BookPack relationship (rel_tag__book_pack)
            Join<BookPack, Tag> tagJoin = root.join("tags", JoinType.INNER);
            return builder.and(
                builder.equal(tagJoin.get("type"), TagType.MAIN_DISPLAY),
                builder.equal(tagJoin.get("id"), mainDisplayId)
            );
        };
    }

    /**
     * Search book packs by text with accent-insensitive matching.
     * Uses PostgreSQL unaccent() function for database-level accent normalization.
     * Searches in: pack title, description, book titles, author names, tag names (EN and FR).
     *
     * @param searchTerm the search term
     * @return the specification
     */
    public static Specification<BookPack> searchByText(String searchTerm) {
        return (root, query, builder) -> {
            if (searchTerm == null || searchTerm.trim().isEmpty()) {
                return builder.conjunction();
            }

            // Normalize search term to remove accents for consistent matching
            String normalizedSearch = TextNormalizationUtils.normalizeForSearch(searchTerm);
            String searchPattern = "%" + normalizedSearch + "%";
            query.distinct(true);

            // Search in pack title and description with accent-insensitive matching
            Expression<String> titleUnaccent = builder.function("unaccent", String.class, builder.lower(root.get("title")));
            Expression<String> descriptionUnaccent = builder.function("unaccent", String.class, builder.lower(root.get("description")));
            Predicate titleMatch = builder.like(titleUnaccent, searchPattern);
            Predicate descriptionMatch = builder.like(descriptionUnaccent, searchPattern);

            // Search in book titles with accent-insensitive matching
            Join<BookPack, Book> bookJoin = root.join("books", JoinType.LEFT);
            Expression<String> bookTitleUnaccent = builder.function("unaccent", String.class, builder.lower(bookJoin.get("title")));
            Predicate bookTitleMatch = builder.like(bookTitleUnaccent, searchPattern);

            // Search in author names with accent-insensitive matching
            Join<Book, Author> authorJoin = bookJoin.join("author", JoinType.LEFT);
            Expression<String> authorUnaccent = builder.function("unaccent", String.class, builder.lower(authorJoin.get("name")));
            Predicate authorMatch = builder.like(authorUnaccent, searchPattern);

            // Search in tag names with accent-insensitive matching
            Join<Book, Tag> tagJoin = bookJoin.join("tags", JoinType.LEFT);
            Expression<String> tagNameEnUnaccent = builder.function("unaccent", String.class, builder.lower(tagJoin.get("nameEn")));
            Expression<String> tagNameFrUnaccent = builder.function("unaccent", String.class, builder.lower(tagJoin.get("nameFr")));
            Predicate tagNameEnMatch = builder.like(tagNameEnUnaccent, searchPattern);
            Predicate tagNameFrMatch = builder.like(tagNameFrUnaccent, searchPattern);

            return builder.or(titleMatch, descriptionMatch, bookTitleMatch, authorMatch, tagNameEnMatch, tagNameFrMatch);
        };
    }

    public static Specification<BookPack> hasLanguage(List<String> languages) {
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

            query.distinct(true);

            Join<BookPack, Book> bookJoin = root.join("books", JoinType.INNER);
            return bookJoin.get("language").in(languageEnums);
        };
    }
}
