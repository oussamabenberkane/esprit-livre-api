package com.oussamabenberkane.espritlivre.repository;

import com.oussamabenberkane.espritlivre.domain.Conversation;
import com.oussamabenberkane.espritlivre.domain.enumeration.Channel;
import com.oussamabenberkane.espritlivre.service.specs.ConversationSpecifications;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the {@link Conversation} entity.
 */
@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long>, JpaSpecificationExecutor<Conversation> {
    /**
     * Override findAll() to only return active (non-soft-deleted) conversations.
     */
    @Override
    default List<Conversation> findAll() {
        return findAll(ConversationSpecifications.activeOnly());
    }

    /**
     * Override findAll(Pageable) to only return active (non-soft-deleted) conversations.
     */
    @Override
    default Page<Conversation> findAll(Pageable pageable) {
        return findAll(ConversationSpecifications.activeOnly(), pageable);
    }

    /**
     * Override findById() to only return active (non-soft-deleted) conversations.
     */
    @Override
    default Optional<Conversation> findById(Long id) {
        return findOne(ConversationSpecifications.activeOnly().and((root, query, builder) -> builder.equal(root.get("id"), id)));
    }

    /**
     * Look up a conversation by its channel + channel-specific sender id (the upsert key).
     */
    Optional<Conversation> findByChannelAndSenderId(Channel channel, String senderId);

    /**
     * Total unread inbound messages across all active conversations (nav badge).
     */
    @Query("select coalesce(sum(c.unreadCount), 0) from Conversation c where c.active = true")
    long sumUnreadCount();
}
