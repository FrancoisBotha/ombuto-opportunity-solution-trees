package com.opportunity.tree.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.opportunity.tree.service.TeamAccessService;
import com.opportunity.tree.service.broadcast.TreeTopicRevocationRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;

/**
 * Unit tests for {@link TreeTopicChannelInterceptor} (RTC-003, FR-034).
 *
 * <p>Covers: a member's SUBSCRIBE to their team's tree passes through, a viewer's SUBSCRIBE passes
 * through (viewers get events like anyone else), a non-member's SUBSCRIBE is refused, any client
 * SEND to the topic namespace is refused, and — the security fix — that every destination under
 * {@code /topic} that is not an exact {@code /topic/teams/{digits}/tree} is refused by default,
 * including wildcard subscriptions that the broker's AntPathMatcher would otherwise expand across
 * every team.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
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
        TreeTopicChannelInterceptor interceptor = new TreeTopicChannelInterceptor(teamAccessService, new TreeTopicRevocationRegistry());

        Message<byte[]> message = subscribeMessage(TREE_TOPIC);
        Message<?> result = interceptor.preSend(message, channel);

        assertThat(result).isSameAs(message);
    }

    // AC 3 — viewer subscribes successfully (mirrors member: same TeamAccessService.canReadTeam path)
    @Test
    void viewerSubscribeIsAllowed() {
        when(teamAccessService.canReadTeam(eq(TEAM_ID))).thenReturn(true);
        TreeTopicChannelInterceptor interceptor = new TreeTopicChannelInterceptor(teamAccessService, new TreeTopicRevocationRegistry());

        Message<?> result = interceptor.preSend(subscribeMessage(TREE_TOPIC), channel);

        assertThat(result).isNotNull();
    }

    // AC 2 — non-member's SUBSCRIBE is refused with an AccessDeniedException, which Spring's
    // StompSubProtocolHandler turns into a STOMP ERROR frame, so the client is told rather than
    // left believing it is subscribed.
    @Test
    void nonMemberSubscribeIsRefused() {
        when(teamAccessService.canReadTeam(eq(TEAM_ID))).thenReturn(false);
        TreeTopicChannelInterceptor interceptor = new TreeTopicChannelInterceptor(teamAccessService, new TreeTopicRevocationRegistry());

        assertThatThrownBy(() -> interceptor.preSend(subscribeMessage(TREE_TOPIC), channel)).isInstanceOf(AccessDeniedException.class);
    }

    // AC 4 — a client SEND to a tree topic is rejected (belt and braces with the AuthorizationManager)
    @Test
    void clientSendToTreeTopicIsRejected() {
        TreeTopicChannelInterceptor interceptor = new TreeTopicChannelInterceptor(teamAccessService, new TreeTopicRevocationRegistry());

        assertThatThrownBy(() -> interceptor.preSend(sendMessage(TREE_TOPIC), channel)).isInstanceOf(AccessDeniedException.class);
    }

    // Any client SEND under /topic is rejected, not just a tree topic — /topic is server-to-client only.
    @Test
    void clientSendToAnyTopicIsRejected() {
        TreeTopicChannelInterceptor interceptor = new TreeTopicChannelInterceptor(teamAccessService, new TreeTopicRevocationRegistry());

        assertThatThrownBy(() -> interceptor.preSend(sendMessage("/topic/whatever"), channel)).isInstanceOf(AccessDeniedException.class);
    }

    /**
     * The security fix. Every one of these destinations was previously passed straight through
     * (the old regex simply did not match them, and the fall-through returned the message). The
     * wildcard forms are the dangerous ones: the simple broker resolves subscription destinations
     * with an AntPathMatcher, so {@code /topic/teams/*}{@code /tree} and {@code /topic/}{@code **}
     * both match every team's real destination and would deliver every team's events to a
     * non-member. Team access is never even consulted for these — they are refused outright.
     */
    @ParameterizedTest
    @ValueSource(
        strings = {
            "/topic/teams/*/tree", // wildcard segment — matches every team
            "/topic/**", // wildcard everything — matches every destination the broker has
            "/topic/*", //
            "/topic/teams/1/tree/../2/tree", // path traversal
            "/topic/teams/01/tree", // non-canonical (zero-padded) id
            "/topic/teams/1/tree/extra", // trailing segment
            "/topic/teams/1/tree ", // trailing whitespace
            "/topic/teams/+1/tree", // signed id
            "/topic/teams/-1/tree", // negative id
            "/topic/teams/%31/tree", // percent-encoded digit
            "/topic/teams/1%2ftree", // percent-encoded separator
            "/topic/teams/not-a-number/tree", // malformed id
            "/topic/teams/1/tree/", // trailing slash
            "/topic/whatever", // unknown topic destination
            "/topic/teams", //
            "/topic", //
            "/topicteams/1/tree", // prefix-adjacent destination
        }
    )
    void everyOtherTopicSubscribeIsRefused(String destination) {
        TreeTopicChannelInterceptor interceptor = new TreeTopicChannelInterceptor(teamAccessService, new TreeTopicRevocationRegistry());

        assertThatThrownBy(() -> interceptor.preSend(subscribeMessage(destination), channel)).isInstanceOf(AccessDeniedException.class);
        verify(teamAccessService, never()).canReadTeam(any());
    }

    // A destination outside the guarded broker namespace is left to the message-level
    // AuthorizationManager (which denies everything that is not /topic/** anyway).
    @Test
    void subscribeOutsideTheTopicNamespaceIsPassedThrough() {
        TreeTopicChannelInterceptor interceptor = new TreeTopicChannelInterceptor(teamAccessService, new TreeTopicRevocationRegistry());

        Message<?> result = interceptor.preSend(subscribeMessage("/user/queue/errors"), channel);

        assertThat(result).isNotNull();
    }

    // A frame that is neither SUBSCRIBE nor SEND (CONNECT, DISCONNECT, UNSUBSCRIBE, …) is untouched.
    @Test
    void nonSubscribeCommandIsPassedThrough() {
        TreeTopicChannelInterceptor interceptor = new TreeTopicChannelInterceptor(teamAccessService, new TreeTopicRevocationRegistry());

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.DISCONNECT);
        accessor.setLeaveMutable(true);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThat(interceptor.preSend(message, channel)).isSameAs(message);
    }

    private static Message<byte[]> subscribeMessage(String destination) {
        return frame(StompCommand.SUBSCRIBE, destination);
    }

    private static Message<byte[]> sendMessage(String destination) {
        return frame(StompCommand.SEND, destination);
    }

    private static Message<byte[]> frame(StompCommand command, String destination) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setDestination(destination);
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
