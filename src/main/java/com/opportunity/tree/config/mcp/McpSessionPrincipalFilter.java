package com.opportunity.tree.config.mcp;

import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Binds every MCP SSE session to the principal that opened it and refuses any JSON-RPC message
 * posted to that session by a different principal.
 *
 * <h3>Why</h3>
 * The Spring AI MCP starter owns the transport ({@code McpServerSseWebMvcAutoConfiguration} /
 * {@code WebMvcSseServerTransportProvider}) and routes a POST to {@code /mcp/message} by its
 * {@code sessionId} query parameter alone. The tool then runs under the <em>poster's</em> identity
 * while its result is written into the stream of the user who opened that session. An authenticated
 * user could therefore push data from their own teams — teams the stream's owner is denied — into
 * another user's MCP stream. The starter is not forked: this filter sits in the dedicated MCP
 * security chain, after authentication, and wraps the two endpoints from the outside.
 *
 * <h3>How</h3>
 * <ul>
 *   <li><b>GET {@code /mcp}</b> — the session id is minted inside the transport and only ever
 *   appears in the {@code endpoint} SSE event it writes. The response is therefore wrapped and the
 *   outgoing bytes are scanned for {@code sessionId=<uuid>}; the binding is recorded <em>before</em>
 *   those bytes are handed to the container, so the id can never be usable before it is bound. The
 *   binding is released when the connection completes, times out or errors.</li>
 *   <li><b>POST {@code /mcp/message}</b> — the {@code sessionId} must be bound to the caller.
 *   Anything else is refused.</li>
 * </ul>
 *
 * <h3>Status code</h3>
 * A refusal is <b>404 Not Found</b> with an empty body, deliberately identical to the transport's
 * own answer for a session id that does not exist ({@code WebMvcSseServerTransportProvider}
 * answers {@code 404 "Session not found"}). A 403 would confirm to the caller that the id they
 * guessed or stole is a live session belonging to someone else; 404 leaks nothing at all, and
 * expiry and hijacking are indistinguishable from the client's point of view.
 */
public class McpSessionPrincipalFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(McpSessionPrincipalFilter.class);

    static final String SESSION_ID_PARAM = "sessionId";

    /** The endpoint event carries {@code …?sessionId=<uuid>}; the transport mints a random UUID. */
    private static final Pattern SESSION_ID_IN_STREAM = Pattern.compile(
        "sessionId=([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})"
    );

    /** Enough to hold the endpoint event; the scan stops as soon as an id is found. */
    private static final int MAX_SNIFF_CHARS = 8192;

    private final McpSessionRegistry registry;
    private final String ssePath;

    public McpSessionPrincipalFilter(McpSessionRegistry registry, String ssePath, String messagePath) {
        // messagePath is accepted for symmetry with the configured endpoints but is deliberately
        // NOT used to detect a message: the transport routes a JSON-RPC message by its sessionId
        // query parameter alone, so we key the owner check off that parameter (see below) rather
        // than off an exact path string. Matching a decoded literal against the raw request URI
        // let a percent-encoded path (e.g. /mcp/messag%65) that the dispatcher still resolves to
        // /mcp/message slip past the check entirely.
        this.registry = registry;
        this.ssePath = ssePath;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {
        String path = pathWithinApplication(request);
        String principal = currentPrincipal();

        // Any request that carries a sessionId query parameter is a JSON-RPC message the transport
        // will route by that id, no matter how the path is spelled (percent-encoding, matrix
        // parameters, trailing slash). It must belong to the caller. The SSE open (GET /mcp) never
        // carries a sessionId — the id is minted inside the transport — so this branch cannot
        // swallow it.
        String sessionId = request.getParameter(SESSION_ID_PARAM);
        if (sessionId != null) {
            if (!registry.isOwnedBy(sessionId, principal)) {
                // Same answer as an unknown session: never confirm that the id belongs to someone else.
                LOG.warn("Refused MCP message for a session the caller did not open (principal={})", principal);
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.setContentLength(0);
                response.flushBuffer();
                return;
            }
            chain.doFilter(request, response);
            return;
        }

        if (ssePath.equals(path) && "GET".equalsIgnoreCase(request.getMethod()) && principal != null) {
            SessionIdBindingResponse wrapper = new SessionIdBindingResponse(response, principal);
            try {
                chain.doFilter(request, wrapper);
            } finally {
                releaseWhenConnectionEnds(request, wrapper);
            }
            return;
        }

        chain.doFilter(request, response);
    }

    /** Frees the binding once the SSE connection is over (or immediately, if it never went async). */
    private void releaseWhenConnectionEnds(HttpServletRequest request, SessionIdBindingResponse wrapper) {
        if (!request.isAsyncStarted()) {
            registry.unbind(wrapper.sessionId());
            return;
        }
        try {
            request
                .getAsyncContext()
                .addListener(
                    new AsyncListener() {
                        @Override
                        public void onComplete(AsyncEvent event) {
                            registry.unbind(wrapper.sessionId());
                        }

                        @Override
                        public void onTimeout(AsyncEvent event) {
                            registry.unbind(wrapper.sessionId());
                        }

                        @Override
                        public void onError(AsyncEvent event) {
                            registry.unbind(wrapper.sessionId());
                        }

                        @Override
                        public void onStartAsync(AsyncEvent event) {
                            // nothing to do
                        }
                    }
                );
        } catch (IllegalStateException e) {
            // The connection already finished — release straight away.
            registry.unbind(wrapper.sessionId());
        }
    }

    private static String currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return authentication.getName();
    }

    private static String pathWithinApplication(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)) {
            uri = uri.substring(contextPath.length());
        }
        return uri;
    }

    /**
     * Response wrapper that watches the outgoing SSE bytes for the transport's {@code endpoint}
     * event and binds the session id it announces to the principal of this request, before those
     * bytes reach the container.
     */
    private final class SessionIdBindingResponse extends HttpServletResponseWrapper {

        private final String principal;
        private final StringBuilder sniffed = new StringBuilder();

        private volatile String sessionId;
        private ServletOutputStream outputStream;
        private PrintWriter writer;

        private SessionIdBindingResponse(HttpServletResponse delegate, String principal) {
            super(delegate);
            this.principal = principal;
        }

        String sessionId() {
            return sessionId;
        }

        @Override
        public ServletOutputStream getOutputStream() throws IOException {
            if (outputStream == null) {
                ServletOutputStream delegate = super.getOutputStream();
                outputStream = new ServletOutputStream() {
                    @Override
                    public boolean isReady() {
                        return delegate.isReady();
                    }

                    @Override
                    public void setWriteListener(WriteListener listener) {
                        delegate.setWriteListener(listener);
                    }

                    @Override
                    public void write(int b) throws IOException {
                        observe(new byte[] { (byte) b }, 0, 1);
                        delegate.write(b);
                    }

                    @Override
                    public void write(byte[] b, int off, int len) throws IOException {
                        observe(b, off, len);
                        delegate.write(b, off, len);
                    }

                    @Override
                    public void write(byte[] b) throws IOException {
                        write(b, 0, b.length);
                    }

                    @Override
                    public void flush() throws IOException {
                        delegate.flush();
                    }

                    @Override
                    public void close() throws IOException {
                        delegate.close();
                    }
                };
            }
            return outputStream;
        }

        @Override
        public PrintWriter getWriter() throws IOException {
            if (writer == null) {
                String encoding = getCharacterEncoding();
                Charset charset = encoding == null ? StandardCharsets.UTF_8 : Charset.forName(encoding);
                writer = new PrintWriter(new OutputStreamWriter(getOutputStream(), charset), false);
            }
            return writer;
        }

        /**
         * Scans bytes on their way out. The binding is recorded before the completing bytes are
         * written through, so a client can never use an id the server has not bound yet.
         */
        private void observe(byte[] bytes, int off, int len) {
            if (sessionId != null || len <= 0) {
                return;
            }
            synchronized (sniffed) {
                if (sessionId != null) {
                    return;
                }
                sniffed.append(new String(bytes, off, len, StandardCharsets.UTF_8));
                Matcher matcher = SESSION_ID_IN_STREAM.matcher(sniffed);
                if (matcher.find()) {
                    sessionId = matcher.group(1);
                    registry.bind(sessionId, principal);
                    sniffed.setLength(0);
                } else if (sniffed.length() > MAX_SNIFF_CHARS) {
                    // Keep only a tail long enough to hold an id split across writes.
                    sniffed.delete(0, sniffed.length() - 128);
                }
            }
        }
    }
}
