package com.opportunity.tree.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.web.socket.EnableWebSocketSecurity;
import org.springframework.security.messaging.access.intercept.MessageMatcherDelegatingAuthorizationManager;

/**
 * STOMP message-level authorisation:
 *
 * <ul>
 *   <li>SUBSCRIBE to {@code /topic/**} requires an authenticated user; a per-team check is layered
 *       on top by {@link TreeTopicChannelInterceptor} (FR-034).</li>
 *   <li>Client SEND to {@code /topic/**} is denied outright — {@code /topic/**} is
 *       server-to-client only (FR-034).</li>
 *   <li>Every other message type is denied.</li>
 * </ul>
 */
@Configuration
@EnableWebSocketSecurity
public class WebsocketSecurityConfiguration {

    @Bean
    public AuthorizationManager<Message<?>> messageAuthorizationManager() {
        return MessageMatcherDelegatingAuthorizationManager.builder()
            .nullDestMatcher()
            .authenticated()
            // Reject any client SEND to a /topic destination — server-to-client only.
            .simpMessageDestMatchers("/topic/**")
            .denyAll()
            // SUBSCRIBE to /topic/** requires authentication; team-scoping is enforced by
            // TreeTopicChannelInterceptor.
            .simpSubscribeDestMatchers("/topic/**")
            .authenticated()
            // Anything else on MESSAGE or SUBSCRIBE is denied.
            .simpTypeMatchers(SimpMessageType.MESSAGE, SimpMessageType.SUBSCRIBE)
            .denyAll()
            .anyMessage()
            .denyAll()
            .build();
    }
}
