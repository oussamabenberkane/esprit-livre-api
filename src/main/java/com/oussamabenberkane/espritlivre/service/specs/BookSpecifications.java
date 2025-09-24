package com.oussamabenberkane.espritlivre.service.specs;

import com.oussamabenberkane.espritlivre.domain.Book;
import com.oussamabenberkane.espritlivre.domain.Tag;
import com.oussamabenberkane.espritlivre.domain.enumeration.TagType;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class BookSpecifications {

    public static Specification<Book> hasAuthor(String author) {
        return (root, query, builder) -> {
            if (author == null || author.trim().isEmpty()) {
                return builder.conjunction();
            }
            return builder.like(builder.lower(root.get("author")),
                "%" + author.toLowerCase().trim() + "%");
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
}
