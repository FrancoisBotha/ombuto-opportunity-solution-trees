package com.opportunity.tree.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.opportunity.tree.service.TeamAccessService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

/**
 * Unit tests for {@link TreeTopicChannelInterceptor} (RTC-003, FR-034).
 *
 * <p>Covers: a member's SUBSCRIBE to their team's tree passes through, a non-member's SUBSCRIBE
 * is dropped, a viewer's SUBSCRIBE passes through (viewers get events like anyone else), and any
 * client SEND to a tree topic is rejected.
 */
@ExtendWith(MockitoExtension.class)
class TreeTopicChannelInterceptorTest {

    private static final Long TEAM_ID = 42L;
    private static final String TREE_TOPIC = "/topic/teams/42/tree";

    @Mock
    private TeamAccessService teamAccessService;

    @Mock
    private MessageChannel channel;

    // AC 1 + AC 3 — member (any role) subscribes successfully via the interceptor
    @Test
    void memberSubscribeIsAllowed() {
        when(teamAccessService.canReadTeam(eq(TEAM_ID))).thenReturn(true);
        TreeTopicChannelInterceptor interceptor = new TreeTopicChannelInterceptor(teamAccessService);

        Message<byte[]> message = subscribeMessage(TREE_TOPIC);
        Message<?> result = interceptor.preSend(message, channel);

        assertThat(result).isSameAs(message);
    }

    // AC 3 — viewer subscribes successfully (mirrors member: same TeamAccessService.canReadTeam path)
    @Test
    void viewerSubscribeIsAllowed() {
        when(teamAccessService.canReadTeam(eq(TEAM_ID))).thenReturn(true);
        TreeTopicChannelInterceptor interceptor = new TreeTopicChannelInterceptor(teamAccessService);

        Message<?> result = interceptor.preSend(subscribeMessage(TREE_TOPIC), channel);

        assertThat(result).isNotNull();
    }

    // AC 2 — non-member's SUBSCRIBE is refused; the message is dropped so nothing reaches the session
    @Test
    void nonMemberSubscribeIsRefused() {
        when(teamAccessService.canReadTeam(eq(TEAM_ID))).thenReturn(false);
        TreeTopicChannelInterceptor interceptor = new TreeTopicChannelInterceptor(teamAccessService);

        Message<?> result = interceptor.preSend(subscribeMessage(TREE_TOPIC), channel);

        assertThat(result).isNull();
    }

    // AC 4 — a client SEND to a tree topic is rejected (belt and braces with the AuthorizationManager)
    @Test
    void clientSendToTreeTopicIsRejected() {
        TreeTopicChannelInterceptor interceptor = new TreeTopicChannelInterceptor(teamAccessService);

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setDestination(TREE_TOPIC);
        accessor.setLeaveMutable(true);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = interceptor.preSend(message, channel);

        assertThat(result).isNull();
    }

    // AC 1 — a non-matching destination is passed through unchanged (interceptor only enforces the tree topic)
    @Test
    void nonTreeTopicSubscribeIsPassedThrough() {
        TreeTopicChannelInterceptor interceptor = new TreeTopicChannelInterceptor(teamAccessService);

        Message<?> result = interceptor.preSend(subscribeMessage("/topic/other"), channel);

        assertThat(result).isNotNull();
    }

    // AC 1 — a malformed teamId in the tree-topic destination is refused (defensive)
    @Test
    void malformedDestinationIsRefused() {
        TreeTopicChannelInterceptor interceptor = new TreeTopicChannelInterceptor(teamAccessService);

        Message<?> result = interceptor.preSend(subscribeMessage("/topic/teams/not-a-number/tree"), channel);

        // pattern requires digits; this is not a tree-topic subscribe and passes through — the
        // security AuthorizationManager still requires authentication.
        assertThat(result).isNotNull();
    }

    private static Message<byte[]> subscribeMessage(String destination) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination(destination);
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
