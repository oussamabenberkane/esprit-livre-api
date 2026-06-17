package com.oussamabenberkane.espritlivre.service.mapper;

import com.oussamabenberkane.espritlivre.domain.Conversation;
import com.oussamabenberkane.espritlivre.service.dto.ConversationDTO;
import java.time.ZonedDateTime;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * Mapper for the entity {@link Conversation} and its DTO {@link ConversationDTO}.
 */
@Mapper(componentModel = "spring")
public interface ConversationMapper extends EntityMapper<ConversationDTO, Conversation> {
    /** WhatsApp free-text replies are only allowed within 24h of the customer's last message. */
    int CARE_WINDOW_HOURS = 24;

    // Computed fields are populated in @AfterMapping, not mapped from the entity.
    @Mapping(target = "withinCustomerCareWindow", ignore = true)
    @Mapping(target = "customerCareWindowExpiresAt", ignore = true)
    ConversationDTO toDto(Conversation entity);

    @Mapping(target = "active", ignore = true)
    Conversation toEntity(ConversationDTO dto);

    @AfterMapping
    default void computeCareWindow(Conversation entity, @MappingTarget ConversationDTO dto) {
        ZonedDateTime lastInbound = entity.getLastInboundAt();
        if (lastInbound == null) {
            dto.setWithinCustomerCareWindow(Boolean.FALSE);
            dto.setCustomerCareWindowExpiresAt(null);
            return;
        }
        ZonedDateTime expiresAt = lastInbound.plusHours(CARE_WINDOW_HOURS);
        dto.setCustomerCareWindowExpiresAt(expiresAt);
        dto.setWithinCustomerCareWindow(expiresAt.isAfter(ZonedDateTime.now()));
    }
}
