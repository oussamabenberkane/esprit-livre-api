package com.oussamabenberkane.espritlivre.service.mapper;

import com.oussamabenberkane.espritlivre.domain.Message;
import com.oussamabenberkane.espritlivre.service.dto.MessageDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper for the entity {@link Message} and its DTO {@link MessageDTO}.
 */
@Mapper(componentModel = "spring")
public interface MessageMapper extends EntityMapper<MessageDTO, Message> {
    @Mapping(target = "conversationId", source = "conversation.id")
    MessageDTO toDto(Message s);

    @Mapping(target = "conversation", ignore = true)
    Message toEntity(MessageDTO dto);
}
