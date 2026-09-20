package com.opportunity.tree.service;

import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.stereotype.Component;

/**
 * Serialises backup restores across the whole application (BKRST fix C6).
 *
 * <p>A restore deletes every application row and re-inserts the archive's. Two of them running at
 * the same time would interleave deletes and inserts and leave a mixture of both archives behind,
 * so the second one is refused outright rather than queued: the caller is an administrator who can
 * simply wait and retry, and a queued restore would silently undo the one before it.
 *
 * <p>The lock is held for the whole request, released in a {@code finally} by the REST layer once
 * the restore transaction has committed or rolled back. It is process-wide only, which matches the
 * single-node deployment this application targets; the pessimistic locks the restore takes on the
 * team rows are what stop a concurrent <em>tree write</em> from racing it.
 */
@Component
public class RestoreMutex {

    /**
     * Deliberately not a {@code ReentrantLock}: "a restore is already running" has to be true even
     * for a second attempt on the same thread, which is what a test — and a request the container
     * happens to serve on a recycled thread — would otherwise slip through.
     */
    private final AtomicBoolean running = new AtomicBoolean();

    /** @return {@code true} when the caller now holds the restore lock, {@code false} if another restore does. */
    public boolean tryAcquire() {
        return running.compareAndSet(false, true);
    }

    /** Releases a lock taken by {@link #tryAcquire()}; only the caller that took it may call this. */
    public void release() {
        running.set(false);
    }

    /** Whether a restore currently holds the lock. */
    public boolean isHeld() {
        return running.get();
    }
}
