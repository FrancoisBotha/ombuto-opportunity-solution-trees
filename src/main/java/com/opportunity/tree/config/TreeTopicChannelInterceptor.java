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
import org.springframework.stereotype.Component;

/**
 * Authorises STOMP SUBSCRIBE frames on {@code /topic/teams/{teamId}/tree} by resolving the
 * {@code teamId} from the destination and delegating to {@link TeamAccessService#canReadTeam(Long)}
 * (FR-034). A non-member's SUBSCRIBE is dropped by returning {@code null} from
 * {@link #preSend(Message, MessageChannel)}, so no messages ever reach that session on that topic.
 *
 * <p>The interceptor also drops any client SEND (STOMP {@code SEND}) to a tree topic as belt and
 * braces alongside the deny rule in {@link WebsocketSecurityConfiguration}: the tree topic is
 * server-to-client only.
 *
 * <p>Runs after Spring Security's {@code SecurityContextChannelInterceptor}, so the current
 * user's authentication is already installed on the thread when {@code TeamAccessService} looks it
 * up.
 */
@Component
public class TreeTopicChannelInterceptor implements ChannelInterceptor {

    private static final Logger LOG = LoggerFactory.getLogger(TreeTopicChannelInterceptor.class);

    /** Matches {@code /topic/teams/{teamId}/tree}. */
    static final Pattern TREE_TOPIC = Pattern.compile("^/topic/teams/(\\d+)/tree$");

    private final TeamAccessService teamAccessService;

    public TreeTopicChannelInterceptor(TeamAccessService teamAccessService) {
        this.teamAccessService = teamAccessService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        StompCommand command = accessor.getCommand();
        if (command == null) {
            return message;
        }
        String destination = accessor.getDestination();
        if (destination == null) {
            return message;
        }
        Matcher m = TREE_TOPIC.matcher(destination);
        if (!m.matches()) {
            return message;
        }
        if (command == StompCommand.SEND) {
            // Tree topic is server-to-client only.
            LOG.debug("Rejecting client SEND to tree topic {}", destination);
            return null;
        }
        if (command != StompCommand.SUBSCRIBE) {
            return message;
        }
        Long teamId;
        try {
            teamId = Long.valueOf(m.group(1));
        } catch (NumberFormatException e) {
            return null;
        }
        if (!teamAccessService.canReadTeam(teamId)) {
            LOG.debug("Refusing SUBSCRIBE to {} — user is not a member of team {}", destination, teamId);
            return null;
        }
        return message;
    }
}
