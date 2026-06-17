package com.oussamabenberkane.espritlivre.repository;

import com.oussamabenberkane.espritlivre.domain.Message;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the {@link Message} entity.
 */
@Repository
public interface MessageRepository extends JpaRepository<Message, Long>, JpaSpecificationExecutor<Message> {
    /**
     * Ordered thread for a conversation (oldest first).
     */
    Page<Message> findByConversationIdOrderByIdAsc(Long conversationId, Pageable pageable);

    /**
     * Incremental thread fetch for polling — only messages after the given id.
     */
    Page<Message> findByConversationIdAndIdGreaterThanOrderByIdAsc(Long conversationId, Long afterId, Pageable pageable);

    Optional<Message> findByExternalMessageId(String externalMessageId);

    boolean existsByExternalMessageId(String externalMessageId);
}
