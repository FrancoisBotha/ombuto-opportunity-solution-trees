package com.opportunity.tree.service.broadcast;

import com.opportunity.tree.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Assigns each tree change a per-team monotonic {@code seq}, stamps it with the acting user login,
 * the current request id and the app-start epoch, then hands it to Spring so
 * {@link TreeChangeBroadcaster} can send it after commit.
 *
 * <p>Callers must invoke {@link #publish} while still inside the write transaction and holding the
 * team's {@link com.opportunity.tree.service.TreeStructureLock}. Doing so guarantees that {@code seq}
 * assignments for one team are strictly ordered — even though the AFTER_COMMIT dispatch itself
 * happens after the lock is released.
 */
@Service
public class TreeChangePublisher {

    /** Request header carrying the optional client-supplied request id, echoed back in the event. */
    public static final String REQUEST_ID_HEADER = "X-Client-Request-Id";

    private static final Logger LOG = LoggerFactory.getLogger(TreeChangePublisher.class);

    private final ApplicationEventPublisher applicationEventPublisher;
    private final ConcurrentMap<Long, AtomicLong> teamSeq = new ConcurrentHashMap<>();
    private final String epoch = UUID.randomUUID().toString();

    public TreeChangePublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    /** The epoch stamped into every event of this application instance. */
    public String epoch() {
        return epoch;
    }

    /**
     * Assigns a {@code seq} to a change in team {@code teamId} and enqueues the event for AFTER_COMMIT
     * broadcast. Must be called under the team's structure lock so per-team ordering is preserved.
     */
    public void publish(TreeChangeType type, Long teamId, Object payload) {
        if (teamId == null) {
            return;
        }
        long seq = teamSeq.computeIfAbsent(teamId, k -> new AtomicLong(0)).incrementAndGet();
        String login = SecurityUtils.getCurrentUserLogin().orElse(null);
        String requestId = currentRequestId();
        TreeChangeEvent event = new TreeChangeEvent(type, teamId, login, Instant.now(), seq, epoch, requestId, payload);
        LOG.debug("Staging tree change {} for team {} seq {}", type, teamId, seq);
        applicationEventPublisher.publishEvent(event);
    }

    private static String currentRequestId() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes servlet) {
            HttpServletRequest request = servlet.getRequest();
            String id = request.getHeader(REQUEST_ID_HEADER);
            if (id != null && !id.isBlank()) {
                return id.trim();
            }
        }
        return null;
    }
}
