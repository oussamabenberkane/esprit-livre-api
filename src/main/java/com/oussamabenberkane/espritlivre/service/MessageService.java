package com.oussamabenberkane.espritlivre.service;

import com.oussamabenberkane.espritlivre.domain.Message;
import com.oussamabenberkane.espritlivre.repository.MessageRepository;
import com.oussamabenberkane.espritlivre.service.dto.MessageDTO;
import com.oussamabenberkane.espritlivre.service.mapper.MessageMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read service for a conversation's message thread.
 */
@Service
@Transactional(readOnly = true)
public class MessageService {

    private final MessageRepository messageRepository;
    private final MessageMapper messageMapper;

    public MessageService(MessageRepository messageRepository, MessageMapper messageMapper) {
        this.messageRepository = messageRepository;
        this.messageMapper = messageMapper;
    }

    /**
     * Ordered message thread for a conversation (oldest first). When {@code afterId} is given,
     * only messages after that id are returned (incremental polling).
     */
    public Page<MessageDTO> getThread(Long conversationId, Long afterId, Pageable pageable) {
        Page<Message> page = afterId != null
            ? messageRepository.findByConversationIdAndIdGreaterThanOrderByIdAsc(conversationId, afterId, pageable)
            : messageRepository.findByConversationIdOrderByIdAsc(conversationId, pageable);
        return page.map(messageMapper::toDto);
    }
}
