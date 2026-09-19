package com.opportunity.tree.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.opportunity.tree.IntegrationTest;
import com.opportunity.tree.domain.Team;
import com.opportunity.tree.domain.TeamMember;
import com.opportunity.tree.domain.User;
import com.opportunity.tree.domain.enumeration.TeamRole;
import com.opportunity.tree.repository.TeamMemberRepository;
import com.opportunity.tree.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
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
 * non-delivery when a SUBSCRIBE or SEND is dropped.
 *
 * <p>Seeds a real {@link Team} with an OWNER, a VIEWER and one user that is not
 * a member, then verifies:
 *
 * <ul>
 *   <li>the OWNER's SUBSCRIBE to {@code /topic/teams/{id}/tree} reaches the
 *       downstream broker handlers,</li>
 *   <li>the VIEWER's SUBSCRIBE reaches them too — viewers get events like any
 *       other member,</li>
 *   <li>the non-member's SUBSCRIBE is dropped and nothing is delivered on that
 *       destination for that session,</li>
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

    // AC 8 — a non-member's SUBSCRIBE is dropped by the channel interceptor
    // (preSend returns null so AbstractMessageChannel.send returns false), which
    // means no handler downstream ever sees the frame and no event will ever be
    // delivered to that session on the team's tree topic.
    @Test
    @Transactional
    void nonMemberSubscribeIsRefused() {
        authenticateAs(OUTSIDER_LOGIN);
        Message<byte[]> subscribe = subscribeFrame(treeTopic());

        boolean sent = clientInboundChannel.send(subscribe);

        assertThat(sent).isFalse();
    }

    // AC 8 — a client SEND to a tree topic is rejected. Either the message-level
    // AuthorizationManager throws AccessDeniedException (denyAll rule) or the
    // interceptor drops the frame; either way the send does not reach downstream
    // handlers.
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

    private String treeTopic() {
        return "/topic/teams/" + team.getId() + "/tree";
    }

    private Message<byte[]> subscribeFrame(String destination) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination(destination);
        accessor.setSubscriptionId("sub-" + UUID.randomUUID());
        accessor.setSessionId("session-" + UUID.randomUUID());
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
