package com.oussamabenberkane.espritlivre.service.specs;

import com.oussamabenberkane.espritlivre.domain.User;
import com.oussamabenberkane.espritlivre.service.util.TextNormalizationUtils;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

/**
 * JPA Specifications for dynamic {@link User} queries (admin user listing).
 * Mirrors the conventions used by {@code OrderSpecifications} / {@code AuthorSpecifications}.
 */
public class UserSpecifications {

    private UserSpecifications() {
        // Utility class, prevent instantiation
    }

    /**
     * Exclude users that hold the given authority (e.g. {@code ROLE_ADMIN}).
     * Equivalent to the JPQL {@code NOT EXISTS (SELECT a FROM u.authorities a WHERE a.name = :authority)}.
     *
     * @param authorityName the authority to exclude
     * @return the specification
     */
    public static Specification<User> withoutAuthority(String authorityName) {
        return (root, query, builder) -> {
            Subquery<String> subquery = query.subquery(String.class);
            var subRoot = subquery.from(User.class);
            subquery.select(subRoot.get("id"));
            subquery.where(builder.equal(subRoot.join("authorities").get("name"), authorityName));
            return builder.not(root.get("id").in(subquery));
        };
    }

    /**
     * Filter by activation status. Returns all users when {@code activated} is null.
     *
     * @param activated the activation status to match, or null for no filter
     * @return the specification
     */
    public static Specification<User> activatedEquals(Boolean activated) {
        return (root, query, builder) -> activated == null ? builder.conjunction() : builder.equal(root.get("activated"), activated);
    }

    /**
     * Free-text search across login, name, email and phone.
     * <ul>
     *   <li>firstName / lastName: accent-insensitive partial match (PostgreSQL {@code unaccent}).</li>
     *   <li>login / email: case-insensitive partial match (kept raw so identifiers such as the
     *       {@code ?login=} deep-link match exactly, without hyphen/whitespace normalization).</li>
     *   <li>phone: matched on normalized digits so "0549..." finds stored "+213549...".</li>
     * </ul>
     * Returns a no-op (matches everything) when the search term is blank.
     *
     * @param search the search term
     * @return the specification
     */
    public static Specification<User> searchByText(String search) {
        return (root, query, builder) -> {
            if (!StringUtils.hasText(search)) {
                return builder.conjunction();
            }

            List<Predicate> predicates = new ArrayList<>();

            // Accent-insensitive match on names (e.g. "said" finds "Saïd")
            String accentPattern = "%" + TextNormalizationUtils.normalizeForSearch(search) + "%";
            for (String field : List.of("firstName", "lastName")) {
                Expression<String> unaccented = builder.function("unaccent", String.class, builder.lower(root.get(field)));
                predicates.add(builder.like(unaccented, accentPattern));
            }

            // Plain case-insensitive match on identifiers (login/email)
            String rawPattern = "%" + search.trim().toLowerCase() + "%";
            predicates.add(builder.like(builder.lower(root.get("login")), rawPattern));
            predicates.add(builder.like(builder.lower(root.get("email")), rawPattern));

            // Normalized phone match (handles country code / leading zero)
            String normalizedPhone = TextNormalizationUtils.normalizePhoneForSearch(search.trim());
            if (normalizedPhone != null && !normalizedPhone.isEmpty()) {
                predicates.add(builder.like(root.get("phone"), "%" + normalizedPhone + "%"));
            }

            return builder.or(predicates.toArray(new Predicate[0]));
        };
    }
}
