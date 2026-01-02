package com.oussamabenberkane.espritlivre.service.specs;

import com.oussamabenberkane.espritlivre.domain.Author;
import com.oussamabenberkane.espritlivre.domain.Book;
import com.oussamabenberkane.espritlivre.domain.BookPack;
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
        return hasTagOfType(TagType.MAIN_DISPLAY, mainDisplayId);
    }

    public static Specification<BookPack> searchByText(String searchTerm) {
        return (root, query, builder) -> {
            if (searchTerm == null || searchTerm.trim().isEmpty()) {
                return builder.conjunction();
            }

            String searchPattern = "%" + searchTerm.toLowerCase().trim() + "%";
            query.distinct(true);

            // Search in pack title and description
            Predicate titleMatch = builder.like(builder.lower(root.get("title")), searchPattern);
            Predicate descriptionMatch = builder.like(builder.lower(root.get("description")), searchPattern);

            // Search in book titles
            Join<BookPack, Book> bookJoin = root.join("books", JoinType.LEFT);
            Predicate bookTitleMatch = builder.like(builder.lower(bookJoin.get("title")), searchPattern);

            // Search in author names
            Join<Book, Author> authorJoin = bookJoin.join("author", JoinType.LEFT);
            Predicate authorMatch = builder.like(builder.lower(authorJoin.get("name")), searchPattern);

            // Search in tag names
            Join<Book, Tag> tagJoin = bookJoin.join("tags", JoinType.LEFT);
            Predicate tagNameEnMatch = builder.like(builder.lower(tagJoin.get("nameEn")), searchPattern);
            Predicate tagNameFrMatch = builder.like(builder.lower(tagJoin.get("nameFr")), searchPattern);

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
