package com.opportunity.tree.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.opportunity.tree.IntegrationTest;
import com.opportunity.tree.domain.Team;
import com.opportunity.tree.domain.TeamMember;
import com.opportunity.tree.domain.User;
import com.opportunity.tree.domain.enumeration.TeamRole;
import com.opportunity.tree.repository.TeamMemberRepository;
import com.opportunity.tree.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.broker.SimpleBrokerMessageHandler;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.AbstractSubscribableChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration test for RTC-003 acceptance criterion 8. Boots the real Spring
 * context and drives the registered {@code clientInboundChannel}, so it exercises
 * interceptor registration and ordering (our {@link TreeTopicChannelInterceptor}
 * running after Spring Security's context interceptor), the message-level
 * {@link WebsocketSecurityConfiguration} deny rules, and end-to-end message
 * non-delivery when a SUBSCRIBE or SEND is refused.
 *
 * <p>Seeds a real {@link Team} with an OWNER, a VIEWER and one user that is not
 * a member, then verifies:
 *
 * <ul>
 *   <li>the OWNER's SUBSCRIBE to {@code /topic/teams/{id}/tree} reaches the
 *       downstream broker handlers,</li>
 *   <li>the VIEWER's SUBSCRIBE reaches them too — viewers get events like any
 *       other member,</li>
 *   <li>the non-member's SUBSCRIBE is refused and nothing is delivered on that
 *       destination for that session,</li>
 *   <li>a non-member's <em>wildcard</em> SUBSCRIBE is refused and that session
 *       receives nothing when the team is really broadcast to,</li>
 *   <li>a client SEND to the tree topic is refused (server-to-client only).</li>
 * </ul>
 */
@IntegrationTest
class TreeTopicChannelInterceptorIT {

    private static final String OWNER_LOGIN = "rtc003-owner";
    private static final String VIEWER_LOGIN = "rtc003-viewer";
    private static final String OUTSIDER_LOGIN = "rtc003-outsider";

    @Autowired
    private EntityManager em;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TeamMemberRepository teamMemberRepository;

    @Autowired
    @Qualifier("clientInboundChannel")
    private MessageChannel clientInboundChannel;

    @Autowired
    @Qualifier("clientOutboundChannel")
    private AbstractSubscribableChannel clientOutboundChannel;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private SimpleBrokerMessageHandler broker;

    private Team team;
    private User ownerUser;
    private User viewerUser;
    private User outsiderUser;

    @BeforeEach
    void seed() {
        team = persistTeam();
        ownerUser = persistUser(OWNER_LOGIN);
        viewerUser = persistUser(VIEWER_LOGIN);
        outsiderUser = persistUser(OUTSIDER_LOGIN);
        persistMembership(team, ownerUser, TeamRole.OWNER);
        persistMembership(team, viewerUser, TeamRole.VIEWER);
    }

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
        teamMemberRepository.deleteAll();
        userRepository.delete(ownerUser);
        userRepository.delete(viewerUser);
        userRepository.delete(outsiderUser);
    }

    // AC 8 — a real member (OWNER) subscribes successfully. The channel's
    // preSend chain runs the message all the way through (returning true from
    // send()), so the frame is dispatched to the broker and this session would
    // receive any subsequent server-to-client message on the topic.
    @Test
    @Transactional
    void memberSubscribeIsAllowed() {
        authenticateAs(OWNER_LOGIN);
        Message<byte[]> subscribe = subscribeFrame(treeTopic());

        boolean sent = clientInboundChannel.send(subscribe);

        assertThat(sent).isTrue();
    }

    // AC 8 — a VIEWER (read-only role) subscribes successfully. Distinct from the
    // OWNER case because the seed puts this user in with TeamRole.VIEWER, so if
    // the access check ever narrows to OWNER/EDITOR this test flips.
    @Test
    @Transactional
    void viewerSubscribeIsAllowed() {
        authenticateAs(VIEWER_LOGIN);
        Message<byte[]> subscribe = subscribeFrame(treeTopic());

        boolean sent = clientInboundChannel.send(subscribe);

        assertThat(sent).isTrue();
    }

    // AC 8 — a non-member's SUBSCRIBE is refused by the channel interceptor. The
    // refusal is an AccessDeniedException, which Spring's StompSubProtocolHandler
    // renders as a STOMP ERROR frame back to the client, so the client is told
    // instead of being left believing it is subscribed. No handler downstream
    // ever sees the frame, so no event reaches that session.
    @Test
    @Transactional
    void nonMemberSubscribeIsRefused() {
        authenticateAs(OUTSIDER_LOGIN);
        Message<byte[]> subscribe = subscribeFrame(treeTopic());

        assertThatThrownBy(() -> clientInboundChannel.send(subscribe)).isInstanceOfAny(
            AccessDeniedException.class,
            MessagingException.class
        );
    }

    /**
     * Security regression guard for the wildcard-subscription hole. Spring's simple broker resolves
     * subscription destinations with an {@code AntPathMatcher}, so {@code /topic/teams/*}{@code /tree}
     * and {@code /topic/}{@code **} both match every team's real destination. Before the default-deny
     * fix these SUBSCRIBEs did not match the exact-tree-topic regex, fell through the interceptor
     * untouched, and the subscriber was served every team's events. Here a non-member connects and
     * tries a wildcard (plus a couple of other non-canonical forms), a real member connects and
     * subscribes normally as a positive control, the team's topic is broadcast to — and only the
     * member's session is served.
     */
    @ParameterizedTest
    @ValueSource(strings = { "/topic/teams/*/tree", "/topic/**", "/topic/teams/1/tree/../2/tree", "/topic/teams/01/tree" })
    @Transactional
    void wildcardSubscribeByNonMemberReceivesNothing(String wildcard) throws Exception {
        String outsiderSession = "session-outsider-" + UUID.randomUUID();
        String memberSession = "session-member-" + UUID.randomUUID();
        List<Message<?>> delivered = new CopyOnWriteArrayList<>();
        ChannelInterceptor capture = new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                delivered.add(message);
                return message;
            }
        };
        clientOutboundChannel.addInterceptor(capture);
        try {
            // Both sessions CONNECT first: the simple broker only delivers to a session it has seen
            // connect, so without this the "receives nothing" assertion would be vacuous. The
            // CONNECT is handed to the broker directly — going through clientInboundChannel would
            // drag in the STOMP CSRF interceptor, which has nothing to do with what is under test
            // here. The SUBSCRIBE frames below still travel the real channel.
            authenticateAs(OUTSIDER_LOGIN);
            broker.handleMessage(connectFrame(outsiderSession));
            authenticateAs(OWNER_LOGIN);
            broker.handleMessage(connectFrame(memberSession));

            // The outsider's wildcard SUBSCRIBE is refused outright.
            authenticateAs(OUTSIDER_LOGIN);
            Message<byte[]> wild = subscribeFrame(wildcard, outsiderSession);
            assertThatThrownBy(() -> clientInboundChannel.send(wild)).isInstanceOfAny(
                AccessDeniedException.class,
                MessagingException.class
            );

            // A real member subscribes the normal way — the positive control that proves the
            // broadcast below really did go out.
            authenticateAs(OWNER_LOGIN);
            assertThat(clientInboundChannel.send(subscribeFrame(treeTopic(), memberSession))).isTrue();

            assertThat(broadcastUntilDelivered(delivered, memberSession))
                .as("the member's session receives the broadcast (positive control)")
                .isTrue();
            assertThat(sessionsOf(delivered))
                .as("nothing is delivered to the wildcard subscriber's session")
                .doesNotContain(outsiderSession);
        } finally {
            clientOutboundChannel.removeInterceptor(capture);
            disconnectQuietly(outsiderSession);
            disconnectQuietly(memberSession);
        }
    }

    // AC 8 — a client SEND to a tree topic is rejected. Either the message-level
    // AuthorizationManager throws AccessDeniedException (denyAll rule) or the
    // interceptor refuses the frame; either way the send does not reach
    // downstream handlers.
    @Test
    @Transactional
    void clientSendToTreeTopicIsRejected() {
        authenticateAs(OWNER_LOGIN);
        Message<byte[]> send = sendFrame(treeTopic());

        boolean sent;
        try {
            sent = clientInboundChannel.send(send);
        } catch (AccessDeniedException | MessagingException expected) {
            // Deny rule fired — this is the intended outcome.
            return;
        }

        assertThat(sent).isFalse();
    }

    // -----------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------

    /**
     * CONNECT and SUBSCRIBE are dispatched to the broker asynchronously, so the broadcast is
     * repeated until the member's session is served (or the deadline passes). Once it is, a short
     * grace period gives any wrongly-delivered copy time to show up before the assertions run.
     */
    private boolean broadcastUntilDelivered(List<Message<?>> delivered, String sessionId) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 15_000;
        while (System.currentTimeMillis() < deadline) {
            messagingTemplate.convertAndSend(treeTopic(), "tree-event-for-members-only");
            for (int i = 0; i < 10; i++) {
                if (sessionsOf(delivered).contains(sessionId)) {
                    Thread.sleep(300);
                    return true;
                }
                Thread.sleep(25);
            }
        }
        return false;
    }

    private void disconnectQuietly(String sessionId) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.DISCONNECT);
        accessor.setSessionId(sessionId);
        accessor.setLeaveMutable(true);
        try {
            broker.handleMessage(MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders()));
        } catch (RuntimeException ignored) {
            // Best-effort teardown of the broker's session bookkeeping.
        }
    }

    /**
     * The sessions that were served an actual tree event. Broker housekeeping frames (CONNECT_ACK
     * and friends) also travel the outbound channel and carry a session id, so they are filtered
     * out — only a {@code MESSAGE} on the team's tree destination counts as "received an event".
     */
    private List<String> sessionsOf(List<Message<?>> delivered) {
        return delivered
            .stream()
            .map(StompHeaderAccessor::wrap)
            .filter(a -> SimpMessageType.MESSAGE.equals(a.getMessageType()))
            .filter(a -> treeTopic().equals(a.getDestination()))
            .map(StompHeaderAccessor::getSessionId)
            .filter(Objects::nonNull)
            .toList();
    }

    private String treeTopic() {
        return "/topic/teams/" + team.getId() + "/tree";
    }

    private Message<byte[]> connectFrame(String sessionId) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setSessionId(sessionId);
        accessor.setUser(currentAuthentication());
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private Message<byte[]> subscribeFrame(String destination) {
        return subscribeFrame(destination, "session-" + UUID.randomUUID());
    }

    private Message<byte[]> subscribeFrame(String destination, String sessionId) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination(destination);
        accessor.setSubscriptionId("sub-" + UUID.randomUUID());
        accessor.setSessionId(sessionId);
        accessor.setUser(currentAuthentication());
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private Message<byte[]> sendFrame(String destination) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setDestination(destination);
        accessor.setSessionId("session-" + UUID.randomUUID());
        accessor.setUser(currentAuthentication());
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage("intruder".getBytes(), accessor.getMessageHeaders());
    }

    private static Authentication currentAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    private static void authenticateAs(String login) {
        // Populate both the thread SecurityContext and the message-level user
        // header. Spring Security's SecurityContextChannelInterceptor uses
        // simpUser to (re-)install the SecurityContext for each message, so it
        // is what our TreeTopicChannelInterceptor (and TeamAccessService) see.
        Authentication auth = new TestingAuthenticationToken(login, "n/a", "ROLE_USER");
        auth.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private Team persistTeam() {
        Team t = new Team().name("rtc003-team-" + UUID.randomUUID()).description("d").createdDate(Instant.now());
        em.persist(t);
        em.flush();
        return t;
    }

    private User persistUser(String login) {
        return userRepository
            .findOneByLogin(login)
            .orElseGet(() -> {
                User u = new User();
                u.setId(UUID.randomUUID().toString());
                u.setLogin(login);
                u.setActivated(true);
                u.setEmail(login + "@example.com");
                u.setFirstName(login);
                u.setLastName("test");
                u.setLangKey("en");
                em.persist(u);
                em.flush();
                return u;
            });
    }

    private void persistMembership(Team t, User u, TeamRole role) {
        TeamMember tm = new TeamMember().role(role).joinedDate(Instant.now());
        tm.setTeam(t);
        tm.setUser(u);
        em.persist(tm);
        em.flush();
    }
}
