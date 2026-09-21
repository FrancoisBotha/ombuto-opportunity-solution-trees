package com.opportunity.tree.config.mcp;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Remembers which principal opened each MCP session.
 *
 * <p>The Streamable HTTP transport routes every JSON-RPC request by its {@code Mcp-Session-Id}
 * header: whoever posts a message with that id has their tool call executed under <em>their own</em>
 * identity but the result delivered into the response (or listening stream) of whoever initialised
 * the session. Without an owner check any authenticated user who learns (or guesses) another
 * user's session id can push data from their own teams into that user's stream. This registry is
 * the server-side binding {@link McpSessionPrincipalFilter} enforces.
 *
 * <p>Entries are added the first time the transport announces a session id in the
 * {@code Mcp-Session-Id} response header (on initialize) and are bounded by {@link #MAX_SESSIONS}
 * as a safety valve if the transport ever forgets to notify us of a closed session.
 */
@Component
public class McpSessionRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(McpSessionRegistry.class);

    /** Safety valve: a leaked entry must never grow the map without bound. */
    static final int MAX_SESSIONS = 10_000;

    private final ConcurrentMap<String, String> owners = new ConcurrentHashMap<>();

    /** Binds a freshly opened session to the principal that opened it. */
    public void bind(String sessionId, String principal) {
        if (sessionId == null || principal == null) {
            return;
        }
        if (owners.size() >= MAX_SESSIONS && !owners.containsKey(sessionId)) {
            LOG.warn("MCP session registry is full ({} entries); refusing to bind another session", owners.size());
            return;
        }
        owners.put(sessionId, principal);
        LOG.debug("Bound MCP session {} to its opening principal", sessionId);
    }

    /** Forgets a session whose Streamable HTTP {@code DELETE /mcp} has terminated it. */
    public void unbind(String sessionId) {
        if (sessionId != null && owners.remove(sessionId) != null) {
            LOG.debug("Released MCP session {}", sessionId);
        }
    }

    /**
     * True only when {@code sessionId} is a known session that {@code principal} opened. An unknown
     * session (never opened, or already closed) is <em>not</em> owned by anybody.
     */
    public boolean isOwnedBy(String sessionId, String principal) {
        if (sessionId == null || principal == null) {
            return false;
        }
        return principal.equals(owners.get(sessionId));
    }

    /** Number of live bindings — for tests and diagnostics. */
    int size() {
        return owners.size();
    }
}
