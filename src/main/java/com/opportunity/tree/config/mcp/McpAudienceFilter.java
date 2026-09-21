package com.opportunity.tree.config.mcp;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationEntryPointFailureHandler;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Refuses any request that reached the MCP filter chain with a JWT whose {@code aud} claim does
 * not include one of the configured MCP audiences. The session chain's audience list
 * ({@code jhipster.security.oauth2.audience}) has to be permissive enough to cover the browser
 * app's own tokens (Keycloak stamps {@code account} on almost everything in the realm), so on its
 * own it distinguishes nothing at {@code /mcp}. This filter is the MCP-only audience check;
 * without it, any valid realm token — including a web-app session token — would pass.
 *
 * <p>The filter runs after the resource server has authenticated the caller, so it can read the
 * decoded {@link Jwt} out of the {@link JwtAuthenticationToken} and hand the same
 * {@link OAuth2AuthenticationException} shape the resource server itself would raise to
 * {@link McpAuthenticationEntryPoint}, keeping the 401 response body and WARN log identical to
 * every other bearer-token failure on the chain.
 */
public class McpAudienceFilter extends OncePerRequestFilter {

    private final List<String> allowedAudience;
    private final AuthenticationFailureHandler failureHandler;

    public McpAudienceFilter(List<String> allowedAudience, McpAuthenticationEntryPoint entryPoint) {
        if (allowedAudience == null || allowedAudience.isEmpty()) {
            throw new IllegalArgumentException("MCP allowed audience must not be empty");
        }
        this.allowedAudience = List.copyOf(allowedAudience);
        this.failureHandler = new AuthenticationEntryPointFailureHandler(entryPoint);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            List<String> audience = jwt.getAudience();
            boolean allowed = audience != null && audience.stream().anyMatch(allowedAudience::contains);
            if (!allowed) {
                SecurityContextHolder.clearContext();
                OAuth2Error error = new OAuth2Error(
                    "invalid_token",
                    "The token is not scoped to the MCP server (missing required audience)",
                    null
                );
                failureHandler.onAuthenticationFailure(request, response, new OAuth2AuthenticationException(error));
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
