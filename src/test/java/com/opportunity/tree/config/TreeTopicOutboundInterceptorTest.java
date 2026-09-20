package com.opportunity.tree.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.opportunity.tree.service.broadcast.TreeTopicRevocationRegistry;
import java.security.Principal;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.support.MessageBuilder;

/**
 * Unit tests for {@link TreeTopicOutboundInterceptor} (S2 — FR-034, NFR-011).
 *
 * <p>The hole these pin: a SUBSCRIBE is authorised once, when the frame arrives, and the
 * subscription then lives as long as the socket. Removing a member from a team did not touch their
 * open subscription, so the removed user's browser kept receiving that team's full
 * {@code TreeNodeDTO} payloads — node titles, notes and all — indefinitely. Only the frame that
 * tells them they were removed still gets through, because the client needs it to update
 * {@code canEdit} and unsubscribe (FR-037).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TreeTopicOutboundInterceptorTest {

    private static final String TREE_TOPIC = "/topic/teams/42/tree";
    private static final Long TEAM_ID = 42L;
    private static final String REMOVED = "gone-user";
    private static final String STILL_A_MEMBER = "staying-user";

    @Mock
    private MessageChannel channel;

    private TreeTopicRevocationRegistry revocations;
    private TreeTopicOutboundInterceptor interceptor;

    @BeforeEach
    void setUp() {
        revocations = new TreeTopicRevocationRegistry();
        interceptor = new TreeTopicOutboundInterceptor(revocations);
    }

    @Test
    void aMemberStillReceivesTreeFrames() {
        Message<?> message = treeFrame(STILL_A_MEMBER, null);

        assertThat(interceptor.preSend(message, channel)).isSameAs(message);
    }

    @Test
    void aRemovedMemberReceivesNothingFurtherOnThatTeamsTopic() {
        revocations.revoke(TEAM_ID, REMOVED);

        assertThat(interceptor.preSend(treeFrame(REMOVED, null), channel)).isNull();
    }

    @Test
    void aRemovedMemberStillReceivesTheNoticeThatRemovedThem() {
        revocations.revoke(TEAM_ID, REMOVED);

        Message<?> notice = treeFrame(REMOVED, REMOVED);

        assertThat(interceptor.preSend(notice, channel)).isSameAs(notice);
    }

    /** The removal notice is broadcast to the whole team; it must not unblock anyone else. */
    @Test
    void anotherUsersRemovalNoticeDoesNotUnblockARevokedSession() {
        revocations.revoke(TEAM_ID, REMOVED);

        assertThat(interceptor.preSend(treeFrame(REMOVED, "someone-else"), channel)).isNull();
    }

    /** Revocation is per team: losing one team does not silence the user's other teams. */
    @Test
    void revocationIsScopedToTheTeamTheUserLost() {
        revocations.revoke(99L, REMOVED);

        Message<?> message = treeFrame(REMOVED, null);

        assertThat(interceptor.preSend(message, channel)).isSameAs(message);
    }

    @Test
    void beingAddedBackRestoresDelivery() {
        revocations.revoke(TEAM_ID, REMOVED);
        revocations.restore(TEAM_ID, REMOVED);

        Message<?> message = treeFrame(REMOVED, null);

        assertThat(interceptor.preSend(message, channel)).isSameAs(message);
    }

    /** Frames that are not team tree topics are none of this interceptor's business. */
    @Test
    void otherDestinationsPassThroughUntouched() {
        revocations.revoke(TEAM_ID, REMOVED);

        Message<?> message = frame("/topic/something/else", REMOVED, null);

        assertThat(interceptor.preSend(message, channel)).isSameAs(message);
    }

    private Message<?> treeFrame(String login, String revokedLogin) {
        return frame(TREE_TOPIC, login, revokedLogin);
    }

    /**
     * Builds the frame the broker actually hands to the outbound channel. The removal stamp goes in
     * {@code nativeHeaders}, which is where {@code SimpMessagingTemplate.convertAndSend(dest,
     * payload, headers)} puts it — asserting against a plain header here would pass while the real
     * frame was being dropped.
     */
    private Message<?> frame(String destination, String login, String revokedLogin) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
        accessor.setDestination(destination);
        accessor.setSessionId("session-" + login);
        accessor.setUser((Principal) () -> login);
        if (revokedLogin != null) {
            accessor.setNativeHeader(TreeTopicRevocationRegistry.REVOKED_LOGIN_HEADER, revokedLogin);
        }
        accessor.setLeaveMutable(true);
        Map<String, Object> headers = accessor.toMap();
        return MessageBuilder.withPayload("{}".getBytes()).copyHeaders(headers).build();
    }
}
