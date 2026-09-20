package com.opportunity.tree.config;

import com.opportunity.tree.service.broadcast.TreeTopicRevocationRegistry;
import java.security.Principal;
import java.util.regex.Matcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

/**
 * Re-checks authorisation on the way out (FR-034, NFR-011).
 *
 * <p>A SUBSCRIBE is authorised once. This interceptor sits on the {@code clientOutboundChannel} and
 * drops any {@code /topic/teams/{teamId}/tree} frame addressed to a session whose principal has
 * since lost read access to that team, so a removed member's still-open subscription goes silent
 * immediately instead of streaming the team's tree until they happen to reload.
 *
 * <p>The one frame that still gets through is the {@code MEMBERSHIP_CHANGED} event that revoked
 * them: {@link com.opportunity.tree.service.broadcast.TreeChangeBroadcaster} stamps it with
 * {@link TreeTopicRevocationRegistry#REVOKED_LOGIN_HEADER}, and a session whose login matches receives it. Without that
 * exception the removed client would be cut off before being told why, and FR-037's live
 * {@code canEdit} update would never arrive.
 */
@Component
public class TreeTopicOutboundInterceptor implements ChannelInterceptor {

    private static final Logger LOG = LoggerFactory.getLogger(TreeTopicOutboundInterceptor.class);

    private final TreeTopicRevocationRegistry revocations;

    public TreeTopicOutboundInterceptor(TreeTopicRevocationRegistry revocations) {
        this.revocations = revocations;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(message);
        String destination = accessor.getDestination();
        if (destination == null) {
            return message;
        }
        Matcher m = TreeTopicChannelInterceptor.TREE_TOPIC.matcher(destination);
        if (!m.matches()) {
            return message;
        }
        // The broker's own outbound frames do not always carry the principal, so the session id is
        // the reliable handle: the SUBSCRIBE that opened it recorded which login it belongs to.
        Principal user = accessor.getUser();
        String login = user != null ? user.getName() : revocations.loginOf(accessor.getSessionId());
        if (login == null) {
            return message;
        }
        Long teamId;
        try {
            teamId = Long.valueOf(m.group(1));
        } catch (NumberFormatException e) {
            return message;
        }
        if (!revocations.isRevoked(teamId, login)) {
            return message;
        }
        if (login.equals(revokedLoginOf(accessor, message))) {
            // The removal notice itself: the client needs it to update canEdit and stop subscribing.
            return message;
        }
        LOG.debug("Dropping a team {} tree frame for {} — no longer a member", teamId, login);
        return null;
    }

    /**
     * The login a frame's removal stamp names. {@code SimpMessagingTemplate.convertAndSend} with
     * extra headers puts them in {@code nativeHeaders} (they are STOMP headers on the wire), so the
     * native form is what actually arrives; the plain header is read as well so the stamp cannot be
     * lost to a future change in how the broadcast is sent.
     */
    private static String revokedLoginOf(SimpMessageHeaderAccessor accessor, Message<?> message) {
        String native_ = accessor.getFirstNativeHeader(TreeTopicRevocationRegistry.REVOKED_LOGIN_HEADER);
        if (native_ != null) {
            return native_;
        }
        Object plain = message.getHeaders().get(TreeTopicRevocationRegistry.REVOKED_LOGIN_HEADER);
        return plain instanceof String value ? value : null;
    }
}
