package com.oussamabenberkane.espritlivre.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.oussamabenberkane.espritlivre.domain.Conversation;
import com.oussamabenberkane.espritlivre.domain.Message;
import com.oussamabenberkane.espritlivre.domain.enumeration.Channel;
import com.oussamabenberkane.espritlivre.domain.enumeration.ConversationStatus;
import com.oussamabenberkane.espritlivre.domain.enumeration.MessageDirection;
import com.oussamabenberkane.espritlivre.domain.enumeration.MessageSenderType;
import com.oussamabenberkane.espritlivre.repository.ConversationRepository;
import com.oussamabenberkane.espritlivre.repository.MessageRepository;
import com.oussamabenberkane.espritlivre.service.dto.AdminReplyRequest;
import com.oussamabenberkane.espritlivre.service.dto.ConversationDTO;
import com.oussamabenberkane.espritlivre.service.dto.EscalateRequest;
import com.oussamabenberkane.espritlivre.service.dto.InboundMessageRequest;
import com.oussamabenberkane.espritlivre.service.dto.MessageDTO;
import com.oussamabenberkane.espritlivre.service.dto.RecordInboundResponse;
import com.oussamabenberkane.espritlivre.service.mapper.ConversationMapper;
import com.oussamabenberkane.espritlivre.service.mapper.MessageMapper;
import java.time.ZonedDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Unit tests for the conversation handoff state machine, idempotent persistence and the
 * 24h customer-care window. Pure Mockito — no Spring context or database required.
 */
@ExtendWith(MockitoExtension.class)
class ConversationServiceTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ConversationMapper conversationMapper;

    @Mock
    private MessageMapper messageMapper;

    @Mock
    private BotClient botClient;

    @InjectMocks
    private ConversationService conversationService;

    private static final Channel WA = Channel.WHATSAPP;
    private static final String SENDER = "213555000111";

    @Test
    void recordInbound_newConversation_persistsAndAllowsBotReply() {
        when(conversationRepository.findByChannelAndSenderId(WA, SENDER)).thenReturn(Optional.empty());
        when(messageRepository.existsByExternalMessageId(anyString())).thenReturn(false);
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(inv -> inv.getArgument(0));

        RecordInboundResponse response = conversationService.recordInbound(
            new InboundMessageRequest(WA, SENDER, "Sam", "Bonjour", "wamid.1", null)
        );

        assertThat(response.status()).isEqualTo(ConversationStatus.BOT_HANDLING);
        assertThat(response.botShouldReply()).isTrue();

        ArgumentCaptor<Conversation> convoCaptor = ArgumentCaptor.forClass(Conversation.class);
        verify(conversationRepository).save(convoCaptor.capture());
        Conversation saved = convoCaptor.getValue();
        assertThat(saved.getCustomerName()).isEqualTo("Sam");
        assertThat(saved.getCustomerPhone()).isEqualTo(SENDER);
        assertThat(saved.getUnreadCount()).isEqualTo(1);
        assertThat(saved.getLastInboundAt()).isNotNull();

        ArgumentCaptor<Message> msgCaptor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).save(msgCaptor.capture());
        Message msg = msgCaptor.getValue();
        assertThat(msg.getDirection()).isEqualTo(MessageDirection.INBOUND);
        assertThat(msg.getSenderType()).isEqualTo(MessageSenderType.CUSTOMER);
        assertThat(msg.getContent()).isEqualTo("Bonjour");
    }

    @Test
    void recordInbound_pausedConversation_doesNotAllowBotReply() {
        Conversation paused = new Conversation().channel(WA).senderId(SENDER).status(ConversationStatus.HUMAN_HANDLING).unreadCount(2);
        when(conversationRepository.findByChannelAndSenderId(WA, SENDER)).thenReturn(Optional.of(paused));
        when(messageRepository.existsByExternalMessageId(anyString())).thenReturn(false);
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(inv -> inv.getArgument(0));

        RecordInboundResponse response = conversationService.recordInbound(
            new InboundMessageRequest(WA, SENDER, null, "Vous êtes là ?", "wamid.2", null)
        );

        assertThat(response.status()).isEqualTo(ConversationStatus.HUMAN_HANDLING);
        assertThat(response.botShouldReply()).isFalse();
    }

    @Test
    void recordInbound_resolvedConversation_reopensToBot() {
        Conversation resolved = new Conversation().channel(WA).senderId(SENDER).status(ConversationStatus.RESOLVED).unreadCount(0);
        when(conversationRepository.findByChannelAndSenderId(WA, SENDER)).thenReturn(Optional.of(resolved));
        when(messageRepository.existsByExternalMessageId(anyString())).thenReturn(false);
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(inv -> inv.getArgument(0));

        RecordInboundResponse response = conversationService.recordInbound(
            new InboundMessageRequest(WA, SENDER, null, "Encore une question", "wamid.3", null)
        );

        assertThat(response.status()).isEqualTo(ConversationStatus.BOT_HANDLING);
        assertThat(response.botShouldReply()).isTrue();
    }

    @Test
    void recordInbound_duplicateMessage_isIdempotent() {
        Conversation existing = new Conversation().channel(WA).senderId(SENDER).status(ConversationStatus.BOT_HANDLING).unreadCount(1);
        when(messageRepository.existsByExternalMessageId("wamid.dup")).thenReturn(true);
        when(conversationRepository.findByChannelAndSenderId(WA, SENDER)).thenReturn(Optional.of(existing));

        RecordInboundResponse response = conversationService.recordInbound(
            new InboundMessageRequest(WA, SENDER, null, "Bonjour", "wamid.dup", null)
        );

        assertThat(response.botShouldReply()).isTrue();
        verify(messageRepository, never()).save(any());
        verify(conversationRepository, never()).save(any());
    }

    @Test
    void adminReply_outsideCareWindow_throws422AndDoesNotSend() {
        Conversation convo = new Conversation()
            .id(1L)
            .channel(WA)
            .senderId(SENDER)
            .status(ConversationStatus.NEEDS_HUMAN)
            .lastInboundAt(ZonedDateTime.now().minusHours(25));
        when(conversationRepository.findById(1L)).thenReturn(Optional.of(convo));

        assertThatThrownBy(() -> conversationService.adminReply(1L, "Bonjour", "admin"))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));

        verify(botClient, never()).sendText(any(), anyString(), anyString());
    }

    @Test
    void adminReply_insideWindow_takesOverSendsAndPersists() {
        Conversation convo = new Conversation()
            .id(1L)
            .channel(WA)
            .senderId(SENDER)
            .status(ConversationStatus.BOT_HANDLING)
            .lastInboundAt(ZonedDateTime.now().minusHours(1));
        when(conversationRepository.findById(1L)).thenReturn(Optional.of(convo));
        when(botClient.sendText(WA, SENDER, "Bonjour")).thenReturn("wamid.out");
        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> inv.getArgument(0));
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(inv -> inv.getArgument(0));
        when(messageMapper.toDto(any(Message.class))).thenReturn(new MessageDTO());

        MessageDTO result = conversationService.adminReply(1L, "Bonjour", "admin");

        assertThat(result).isNotNull();
        assertThat(convo.getStatus()).isEqualTo(ConversationStatus.HUMAN_HANDLING);
        verify(botClient).sendText(WA, SENDER, "Bonjour");

        ArgumentCaptor<Message> msgCaptor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).save(msgCaptor.capture());
        Message msg = msgCaptor.getValue();
        assertThat(msg.getSenderType()).isEqualTo(MessageSenderType.ADMIN);
        assertThat(msg.getDirection()).isEqualTo(MessageDirection.OUTBOUND);
        assertThat(msg.getAdminLogin()).isEqualTo("admin");
        assertThat(msg.getExternalMessageId()).isEqualTo("wamid.out");
    }

    @Test
    void takeOverHandBackResolve_transitionStatus() {
        Conversation convo = new Conversation().id(1L).channel(WA).senderId(SENDER).status(ConversationStatus.BOT_HANDLING).escalationReason("frustré");
        when(conversationRepository.findById(1L)).thenReturn(Optional.of(convo));
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(inv -> inv.getArgument(0));
        when(conversationMapper.toDto(any(Conversation.class))).thenReturn(new ConversationDTO());

        conversationService.takeOver(1L);
        assertThat(convo.getStatus()).isEqualTo(ConversationStatus.HUMAN_HANDLING);

        conversationService.handBack(1L);
        assertThat(convo.getStatus()).isEqualTo(ConversationStatus.BOT_HANDLING);
        assertThat(convo.getEscalationReason()).isNull();

        conversationService.resolve(1L);
        assertThat(convo.getStatus()).isEqualTo(ConversationStatus.RESOLVED);
    }

    @Test
    void escalate_setsNeedsHumanWithReason() {
        Conversation convo = new Conversation().channel(WA).senderId(SENDER).status(ConversationStatus.BOT_HANDLING);
        when(conversationRepository.findByChannelAndSenderId(WA, SENDER)).thenReturn(Optional.of(convo));
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(inv -> inv.getArgument(0));

        conversationService.escalate(new EscalateRequest(WA, SENDER, "Le client demande un humain"));

        assertThat(convo.getStatus()).isEqualTo(ConversationStatus.NEEDS_HUMAN);
        assertThat(convo.getEscalationReason()).isEqualTo("Le client demande un humain");
    }

    @Test
    void adminReply_unknownConversation_throws404() {
        when(conversationRepository.findById(99L)).thenReturn(Optional.empty());
        AdminReplyRequest request = new AdminReplyRequest("Bonjour");

        assertThatThrownBy(() -> conversationService.adminReply(99L, request.content(), "admin"))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));

        verify(botClient, never()).sendText(any(), eq(SENDER), anyString());
    }
}
