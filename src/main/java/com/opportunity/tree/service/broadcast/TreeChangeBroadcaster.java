package com.opportunity.tree.service.broadcast;

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

    public TreeChangeBroadcaster(SimpMessageSendingOperations messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTreeChange(TreeChangeEvent event) {
        String destination = "/topic/teams/" + event.teamId() + "/tree";
        try {
            messagingTemplate.convertAndSend(destination, event);
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
}
