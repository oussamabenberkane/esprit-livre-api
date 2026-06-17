package com.oussamabenberkane.espritlivre.service.specs;

import com.oussamabenberkane.espritlivre.domain.Conversation;
import com.oussamabenberkane.espritlivre.domain.enumeration.Channel;
import com.oussamabenberkane.espritlivre.domain.enumeration.ConversationStatus;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public class ConversationSpecifications {

    /**
     * Specification to filter only active (non-soft-deleted) conversations.
     */
    public static Specification<Conversation> activeOnly() {
        return (root, query, builder) -> builder.equal(root.get("active"), true);
    }

    public static Specification<Conversation> hasChannel(Channel channel) {
        return (root, query, builder) -> {
            if (channel == null) {
                return builder.conjunction();
            }
            return builder.equal(root.get("channel"), channel);
        };
    }

    public static Specification<Conversation> hasStatus(ConversationStatus status) {
        return (root, query, builder) -> {
            if (status == null) {
                return builder.conjunction();
            }
            return builder.equal(root.get("status"), status);
        };
    }

    /**
     * Case-insensitive partial search across customer name, phone, sender id and the last snippet.
     */
    public static Specification<Conversation> searchByText(String searchTerm) {
        return (root, query, builder) -> {
            if (!StringUtils.hasText(searchTerm)) {
                return builder.conjunction();
            }
            String pattern = "%" + searchTerm.trim().toLowerCase() + "%";
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.like(builder.lower(root.get("customerName")), pattern));
            predicates.add(builder.like(builder.lower(root.get("customerPhone")), pattern));
            predicates.add(builder.like(builder.lower(root.get("senderId")), pattern));
            predicates.add(builder.like(builder.lower(root.get("lastMessageSnippet")), pattern));
            return builder.or(predicates.toArray(new Predicate[0]));
        };
    }
}
