package com.opportunity.tree.service.broadcast;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Broadcasts a {@link TreeChangeEvent} to {@code /topic/teams/{teamId}/tree} strictly after the
 * originating transaction commits ({@link TransactionPhase#AFTER_COMMIT}). Nothing is broadcast when
 * the transaction rolls back — the listener is simply not invoked.
 *
 * <p>A failure to send is caught and logged; it never propagates to the caller and never rolls back
 * the write that produced the event (NFR-013). This covers {@link Error}s as well as runtime
 * exceptions — a broadcast-time {@code LinkageError} or {@code AssertionError} would otherwise take
 * down an already-committed write — with the exception of {@link VirtualMachineError}, which is
 * rethrown because the JVM itself is no longer usable.
 */
@Component
public class TreeChangeBroadcaster {

    private static final Logger LOG = LoggerFactory.getLogger(TreeChangeBroadcaster.class);

    private final SimpMessageSendingOperations messagingTemplate;

    private final TreeTopicRevocationRegistry revocations;

    public TreeChangeBroadcaster(SimpMessageSendingOperations messagingTemplate, TreeTopicRevocationRegistry revocations) {
        this.messagingTemplate = messagingTemplate;
        this.revocations = revocations;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTreeChange(TreeChangeEvent event) {
        String destination = "/topic/teams/" + event.teamId() + "/tree";
        Map<String, Object> headers = applyMembershipChange(event);
        try {
            if (headers == null) {
                messagingTemplate.convertAndSend(destination, event);
            } else {
                messagingTemplate.convertAndSend(destination, event, headers);
            }
        } catch (VirtualMachineError e) {
            // OutOfMemoryError, StackOverflowError and friends: the JVM is not in a state we can
            // recover from, so never swallow these.
            throw e;
        } catch (Throwable t) {
            // Anything else — including a LinkageError raised while serialising the payload — must
            // not escape: the write has already committed and broadcasting is best effort (NFR-013).
            LOG.warn("Failed to broadcast tree change {} seq {} to {}", event.type(), event.seq(), destination, t);
        }
    }

    /**
     * Keeps the subscription revocation registry in step with membership (FR-034, NFR-011).
     *
     * <p>A removal is recorded <em>before</em> the frame is sent, not after, so there is no window
     * in which a removed member's open subscription still receives tree events: the outbound
     * channel is asynchronous, so "send then revoke" would be a race. The removal notice itself is
     * stamped with {@link TreeTopicRevocationRegistry#REVOKED_LOGIN_HEADER} so it is the one frame
     * that still reaches the session it just cut off — the client needs it to update
     * {@code canEdit} and unsubscribe (FR-037).
     *
     * @return the headers the frame must carry, or {@code null} when it needs none.
     */
    private Map<String, Object> applyMembershipChange(TreeChangeEvent event) {
        if (event.type() != TreeChangeType.MEMBERSHIP_CHANGED || !(event.payload() instanceof MembershipChangedPayload membership)) {
            return null;
        }
        if (!membership.removed()) {
            revocations.restore(event.teamId(), membership.login());
            return null;
        }
        revocations.revoke(event.teamId(), membership.login());
        return Map.of(TreeTopicRevocationRegistry.REVOKED_LOGIN_HEADER, membership.login());
    }
}
