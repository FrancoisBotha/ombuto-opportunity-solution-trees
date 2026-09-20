package com.opportunity.tree.config.mcp;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.AuthenticationEntryPoint;

/**
 * Wraps {@link BearerTokenAuthenticationEntryPoint} so that every 401 on the MCP chain
 * — expired token, wrong issuer, wrong audience, malformed token, missing header —
 * emits a WARN log line naming the OAuth2 error code and description. The bearer token
 * itself (the {@code Authorization} header value) is never logged.
 */
class McpAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final Logger LOG = LoggerFactory.getLogger(McpAuthenticationEntryPoint.class);

    private final BearerTokenAuthenticationEntryPoint delegate = new BearerTokenAuthenticationEntryPoint();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
        throws IOException, ServletException {
        logReason(authException);
        delegate.commence(request, response, authException);
    }

    private void logReason(AuthenticationException authException) {
        if (authException instanceof OAuth2AuthenticationException oauth2Ex) {
            OAuth2Error error = oauth2Ex.getError();
            String code = error != null ? error.getErrorCode() : "unknown";
            String description = error != null ? error.getDescription() : oauth2Ex.getMessage();
            LOG.warn("MCP bearer authentication rejected: code={} description={}", code, description);
        } else {
            LOG.warn("MCP bearer authentication rejected: {}", authException.getMessage());
        }
    }
}
