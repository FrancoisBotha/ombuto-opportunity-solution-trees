package com.opportunity.tree.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Unit tests for {@link WebsocketSecurityConfiguration#messageAuthorizationManager()} (RTC-003
 * AC 4): a client SEND to any {@code /topic/**} destination — including a tree topic — is denied
 * regardless of the user's authentication.
 */
class WebsocketSecurityConfigurationTest {

    private final AuthorizationManager<Message<?>> manager = new WebsocketSecurityConfiguration().messageAuthorizationManager();

    // AC 4 — client SEND (MESSAGE) to a tree topic is denied even for an authenticated user
    @Test
    void authenticatedClientSendToTreeTopicIsDenied() {
        AuthorizationResult result = manager.authorize(authenticated(), sendTo("/topic/teams/1/tree"));

        assertThat(result).isNotNull();
        assertThat(result.isGranted()).isFalse();
    }

    // AC 4 — a client SEND to any /topic/** destination is denied (server-to-client only)
    @Test
    void authenticatedClientSendToOtherTopicIsDenied() {
        AuthorizationResult result = manager.authorize(authenticated(), sendTo("/topic/anything"));

        assertThat(result).isNotNull();
        assertThat(result.isGranted()).isFalse();
    }

    // AC 5 (auth path) — an unauthenticated SUBSCRIBE to /topic/** is denied
    @Test
    void unauthenticatedSubscribeIsDenied() {
        AuthorizationResult result = manager.authorize(anonymous(), subscribeTo("/topic/teams/1/tree"));

        assertThat(result).isNotNull();
        assertThat(result.isGranted()).isFalse();
    }

    // AC 1 — an authenticated SUBSCRIBE to /topic/** is allowed at the security layer; the per-team
    // check is enforced by TreeTopicChannelInterceptor.
    @Test
    void authenticatedSubscribeIsAllowedAtSecurityLayer() {
        AuthorizationResult result = manager.authorize(authenticated(), subscribeTo("/topic/teams/1/tree"));

        assertThat(result).isNotNull();
        assertThat(result.isGranted()).isTrue();
    }

    private static Supplier<Authentication> authenticated() {
        return () -> new UsernamePasswordAuthenticationToken("alice", "n/a", List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    private static Supplier<Authentication> anonymous() {
        return () -> new AnonymousAuthenticationToken("key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
    }

    private static Message<byte[]> sendTo(String destination) {
        return message(SimpMessageType.MESSAGE, destination);
    }

    private static Message<byte[]> subscribeTo(String destination) {
        return message(SimpMessageType.SUBSCRIBE, destination);
    }

    private static Message<byte[]> message(SimpMessageType type, String destination) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(type);
        accessor.setDestination(destination);
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
