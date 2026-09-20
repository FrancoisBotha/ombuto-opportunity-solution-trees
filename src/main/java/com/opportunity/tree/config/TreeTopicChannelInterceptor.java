package com.opportunity.tree.config;

import com.opportunity.tree.service.TeamAccessService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * Default-deny authorisation for STOMP frames addressed to the broker's {@code /topic} namespace
 * (FR-034, NFR-011).
 *
 * <p><strong>Why default-deny.</strong> Spring's simple broker resolves a SUBSCRIBE destination with
 * an {@link org.springframework.util.AntPathMatcher}, so a subscription to a <em>pattern</em> such as
 * {@code /topic/teams/*}{@code /tree} or {@code /topic/}{@code **} matches every team's real
 * destination and would deliver every team's tree events to whoever subscribed. Matching only the
 * exact tree topic and letting everything else fall through therefore leaks the whole broker. This
 * interceptor instead refuses <em>any</em> SUBSCRIBE under {@code /topic} unless the destination is
 * literally {@code /topic/teams/{teamId}/tree} — a canonical, unsigned, non-zero-padded decimal id —
 * and {@link TeamAccessService#canReadTeam(Long)} grants the current principal read access to that
 * team. Wildcards, path traversal ({@code /topic/teams/1/tree/../2/tree}), percent-encoded forms,
 * zero-padded ids, trailing segments and unknown {@code /topic/...} destinations all fail the exact
 * match and are refused.
 *
 * <p><strong>How a refusal is reported.</strong> A refusal throws {@link AccessDeniedException}
 * rather than silently dropping the frame (which was the previous behaviour). Spring's
 * {@code StompSubProtocolHandler} turns an exception raised on the {@code clientInboundChannel} into
 * a STOMP {@code ERROR} frame, so the client is told the subscription was refused instead of being
 * left believing it is subscribed and merely never receiving anything. This matches how the
 * {@code denyAll} rules in {@link WebsocketSecurityConfiguration} already surface.
 *
 * <p>Viewers are allowed: the check is {@code canReadTeam}, so any team member — including the
 * read-only VIEWER role — may subscribe.
 *
 * <p>Any client SEND to the {@code /topic} namespace is refused as well, as belt and braces
 * alongside the deny rule in {@link WebsocketSecurityConfiguration}: {@code /topic} is
 * server-to-client only.
 *
 * <p>Runs after Spring Security's {@code SecurityContextChannelInterceptor}, so the current user's
 * authentication is already installed on the thread when {@code TeamAccessService} looks it up.
 */
@Component
public class TreeTopicChannelInterceptor implements ChannelInterceptor {

    private static final Logger LOG = LoggerFactory.getLogger(TreeTopicChannelInterceptor.class);

    /** The broker prefix this interceptor guards; mirrors {@code enableSimpleBroker("/topic")}. */
    static final String TOPIC_PREFIX = "/topic";

    /**
     * The one destination a client may subscribe to. The id is a canonical decimal — no sign, no
     * leading zeros (so {@code /topic/teams/01/tree} is refused), at most 18 digits so it always fits
     * a {@code long}.
     */
    static final Pattern TREE_TOPIC = Pattern.compile("^/topic/teams/(0|[1-9][0-9]{0,17})/tree$");

    private final TeamAccessService teamAccessService;

    public TreeTopicChannelInterceptor(TeamAccessService teamAccessService) {
        this.teamAccessService = teamAccessService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        StompCommand command = accessor.getCommand();
        if (command != StompCommand.SUBSCRIBE && command != StompCommand.SEND) {
            return message;
        }
        String destination = accessor.getDestination();
        if (destination == null || !destination.startsWith(TOPIC_PREFIX)) {
            // Not addressed to the guarded broker namespace; the message-level AuthorizationManager
            // in WebsocketSecurityConfiguration still applies.
            return message;
        }
        if (command == StompCommand.SEND) {
            LOG.debug("Refusing client SEND to {} — the topic namespace is server-to-client only", destination);
            throw new AccessDeniedException("Client SEND to a topic destination is not allowed");
        }
        Matcher m = TREE_TOPIC.matcher(destination);
        if (!m.matches()) {
            LOG.debug("Refusing SUBSCRIBE to {} — not an exact team tree topic", destination);
            throw new AccessDeniedException("Subscription destination is not allowed");
        }
        Long teamId;
        try {
            teamId = Long.valueOf(m.group(1));
        } catch (NumberFormatException e) {
            LOG.debug("Refusing SUBSCRIBE to {} — team id is out of range", destination);
            throw new AccessDeniedException("Subscription destination is not allowed");
        }
        if (!teamAccessService.canReadTeam(teamId)) {
            LOG.debug("Refusing SUBSCRIBE to {} — user is not a member of team {}", destination, teamId);
            throw new AccessDeniedException("Not a member of team " + teamId);
        }
        return message;
    }
}
