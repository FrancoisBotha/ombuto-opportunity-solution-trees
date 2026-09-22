package com.opportunity.tree.config.mcp;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.web.AuthenticationEntryPoint;

/**
 * Writes the 401 for every failure on the MCP filter chain — expired token, wrong issuer, wrong
 * audience, malformed token, missing header — and puts a WWW-Authenticate challenge that names the
 * MCP protected-resource metadata URL on the response, so an MCP client can discover the
 * authorization server and run authorization-code + PKCE without a hand-minted token.
 *
 * <p>The challenge is built explicitly (rather than delegating to
 * {@code BearerTokenAuthenticationEntryPoint}) so the {@code resource_metadata} parameter required
 * by the MCP authorization specification is always present, and the OAuth2 error code and
 * description are propagated when the resource server has one to give.
 *
 * <p>Each failure is logged at WARN with the OAuth2 error code and description; the bearer token
 * itself (the {@code Authorization} header value) is never logged.
 */
class McpAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final Logger LOG = LoggerFactory.getLogger(McpAuthenticationEntryPoint.class);

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
        throws IOException, ServletException {
        logReason(authException);
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, buildChallenge(request, authException));
    }

    private String buildChallenge(HttpServletRequest request, AuthenticationException authException) {
        Map<String, String> params = new LinkedHashMap<>();
        String metadataUrl = McpProtectedResourceMetadataResource.baseUrl(request) + McpProtectedResourceMetadataResource.METADATA_PATH;
        params.put("resource_metadata", metadataUrl);
        if (authException instanceof OAuth2AuthenticationException oauth2Ex) {
            OAuth2Error error = oauth2Ex.getError();
            if (error != null) {
                if (error.getErrorCode() != null && !error.getErrorCode().isBlank()) {
                    params.put("error", error.getErrorCode());
                }
                if (error.getDescription() != null && !error.getDescription().isBlank()) {
                    params.put("error_description", error.getDescription());
                }
                if (error.getUri() != null && !error.getUri().isBlank()) {
                    params.put("error_uri", error.getUri());
                }
            }
        }
        String parameters = params
            .entrySet()
            .stream()
            .map(e -> e.getKey() + "=\"" + quote(e.getValue()) + "\"")
            .collect(Collectors.joining(", "));
        return "Bearer " + parameters;
    }

    private static String quote(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
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
