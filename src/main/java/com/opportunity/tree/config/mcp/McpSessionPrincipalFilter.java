package com.opportunity.tree.config.mcp;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Binds every MCP session to the principal that opened it and refuses any request against a
 * session id that belongs to somebody else.
 *
 * <h3>Why</h3>
 * The Spring AI MCP Streamable HTTP starter ({@code McpServerStreamableHttpWebMvcAutoConfiguration}
 * / {@code WebMvcStreamableServerTransportProvider}) owns the transport and routes each incoming
 * request by the {@code Mcp-Session-Id} header — not by who is calling. A tool call therefore runs
 * under the <em>poster's</em> identity while its result is written into the stream of whoever
 * initialised that session. An authenticated user could push data from their own teams — teams
 * the session's owner is denied — into another user's stream (the MCPSRV-002 hijack, in
 * Streamable HTTP form). This filter sits after authentication in the dedicated MCP security
 * chain and wraps every request against {@code /mcp} from the outside.
 *
 * <h3>How</h3>
 * <ul>
 *   <li><b>POST /mcp with no session header</b> — the JSON-RPC initialize call. The transport
 *   mints a new session id and returns it in the {@code Mcp-Session-Id} response header. The
 *   response is wrapped so the id is bound to the current caller <em>before</em> the header is
 *   flushed to the client, closing the "id exists but is unbound" window.</li>
 *   <li><b>Any request that carries an {@code Mcp-Session-Id} header</b> — POST tool call, GET
 *   listening stream or DELETE — is refused unless the header value is bound to the caller.
 *   Because the check keys off a request header (not a URL substring), no path-spelling variant
 *   (percent-encoding, matrix parameters, trailing slash) can bypass it.</li>
 *   <li><b>DELETE /mcp</b> is the Streamable HTTP session-termination request; after the
 *   transport has processed it the id will no longer be routed, so the filter releases the
 *   binding once the chain completes. The transport itself never notifies the registry, so
 *   without this call the registry would leak an entry per terminated session.</li>
 * </ul>
 *
 * <h3>Status code</h3>
 * A refusal is <b>404 Not Found</b> with an empty body, deliberately identical to the transport's
 * own answer for a session id it does not know. A 403 would confirm to the caller that the id
 * they guessed or stole belongs to a live session; 404 leaks nothing at all, and expiry and
 * hijacking are indistinguishable from the client's point of view.
 */
public class McpSessionPrincipalFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(McpSessionPrincipalFilter.class);

    /**
     * Header name for the session id in the MCP Streamable HTTP transport (spec-defined, so the
     * casing is fixed). We match case-insensitively when reading incoming request headers because
     * {@link HttpServletRequest#getHeader(String)} is case-insensitive by contract.
     */
    static final String SESSION_ID_HEADER = "Mcp-Session-Id";

    /** RFC-4122 uuid, the shape the Spring AI transport mints for session ids. */
    private static final Pattern SESSION_ID_VALUE = Pattern.compile(
        "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"
    );

    private final McpSessionRegistry registry;

    public McpSessionPrincipalFilter(McpSessionRegistry registry) {
        this.registry = registry;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {
        String principal = currentPrincipal();
        String sessionId = request.getHeader(SESSION_ID_HEADER);

        if (sessionId != null && !sessionId.isEmpty()) {
            // Any request that carries a session id must belong to the caller. The check is
            // header-driven, not path-driven, so a percent-encoded path variant (e.g. /mc%70)
            // cannot bypass it.
            if (!registry.isOwnedBy(sessionId, principal)) {
                // Same answer as an unknown session: never confirm that the id belongs to someone else.
                LOG.warn("Refused MCP request for a session the caller did not open (principal={})", principal);
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.setContentLength(0);
                response.flushBuffer();
                return;
            }
            try {
                chain.doFilter(request, response);
            } finally {
                // DELETE /mcp is the Streamable HTTP session-termination request: once the
                // transport has processed it the id will no longer be routed, so release the
                // binding here (the transport never notifies us on its own). Anything other
                // than a client/transport error means the session is gone as far as the
                // server is concerned.
                if ("DELETE".equalsIgnoreCase(request.getMethod()) && response.getStatus() < 500) {
                    registry.unbind(sessionId);
                }
            }
            return;
        }

        // No session header on the way in: this is the initialize call (or an unrelated GET).
        // Wrap the response so a session id the transport announces via the response header is
        // bound to the caller before that header reaches the client.
        if (principal != null) {
            SessionBindingResponseWrapper wrapper = new SessionBindingResponseWrapper(response, principal);
            chain.doFilter(request, wrapper);
            return;
        }

        chain.doFilter(request, response);
    }

    private static String currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return authentication.getName();
    }

    /**
     * Wraps the response so any {@code Mcp-Session-Id} header the transport writes is bound to
     * the request's principal before the header is committed. Only the first setHeader/addHeader
     * that carries a session-id-shaped value is honoured; subsequent writes with the same value
     * are a no-op (the transport writes it once, but we defend against any future double-write).
     */
    private final class SessionBindingResponseWrapper extends HttpServletResponseWrapper {

        private final String principal;
        private volatile String boundSessionId;

        private SessionBindingResponseWrapper(HttpServletResponse delegate, String principal) {
            super(delegate);
            this.principal = principal;
        }

        @Override
        public void setHeader(String name, String value) {
            maybeBind(name, value);
            super.setHeader(name, value);
        }

        @Override
        public void addHeader(String name, String value) {
            maybeBind(name, value);
            super.addHeader(name, value);
        }

        private void maybeBind(String name, String value) {
            if (name == null || value == null || value.isEmpty()) {
                return;
            }
            if (!SESSION_ID_HEADER.equalsIgnoreCase(name)) {
                return;
            }
            if (boundSessionId != null) {
                return;
            }
            if (!SESSION_ID_VALUE.matcher(value).matches()) {
                return;
            }
            boundSessionId = value;
            registry.bind(value, principal);
        }
    }
}
