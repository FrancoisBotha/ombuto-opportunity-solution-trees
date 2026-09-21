package com.opportunity.tree.config.mcp;

import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.ai.mcp.server.common.autoconfigure.properties.McpServerStreamableHttpProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Serves the OAuth 2.0 Protected Resource Metadata document required by the MCP authorization
 * specification (RFC 9728). MCP clients that receive a 401 with a
 * {@code WWW-Authenticate: Bearer resource_metadata="..."} challenge fetch this document to
 * discover the authorization server(s), then run authorization-code + PKCE against them without
 * anyone having to paste a token by hand.
 *
 * <p>The document is derived from the running configuration so it is correct under localhost, a
 * preview host, or the internal production deployment:
 * <ul>
 *   <li>{@code resource} — the MCP endpoint URL built from the request's scheme, host and port;</li>
 *   <li>{@code authorization_servers} — the Keycloak realm issuer from
 *       {@code spring.security.oauth2.client.provider.oidc.issuer-uri};</li>
 *   <li>{@code bearer_methods_supported} — {@code header} (the MCP chain only reads
 *       {@code Authorization: Bearer});</li>
 *   <li>{@code scopes_supported} — the OpenID Connect scopes the MCP client should request.</li>
 * </ul>
 *
 * <p>The endpoint is unauthenticated by design (metadata discovery cannot be gated on a token the
 * client is trying to obtain). It reveals only the issuer URL a caller could already read from any
 * OIDC sign-in redirect, so there is no additional exposure.
 */
@RestController
public class McpProtectedResourceMetadataResource {

    static final String METADATA_PATH = "/.well-known/oauth-protected-resource";
    static final String MCP_METADATA_PATH = METADATA_PATH + McpSecurityConfiguration.MCP_DEFAULT_ENDPOINT;

    private final String issuerUri;
    private final String mcpEndpoint;

    public McpProtectedResourceMetadataResource(
        @Value("${spring.security.oauth2.client.provider.oidc.issuer-uri}") String issuerUri,
        ObjectProvider<McpServerStreamableHttpProperties> streamableProperties
    ) {
        this.issuerUri = issuerUri;
        McpServerStreamableHttpProperties properties = streamableProperties.getIfAvailable();
        this.mcpEndpoint =
            properties != null && properties.getMcpEndpoint() != null
                ? properties.getMcpEndpoint()
                : McpSecurityConfiguration.MCP_DEFAULT_ENDPOINT;
    }

    @GetMapping(path = { METADATA_PATH, MCP_METADATA_PATH }, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> metadata(HttpServletRequest request) {
        String resourceUrl = baseUrl(request) + mcpEndpoint;
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("resource", resourceUrl);
        body.put("authorization_servers", List.of(issuerUri));
        body.put("bearer_methods_supported", List.of("header"));
        body.put("scopes_supported", List.of("openid", "profile", "email", "roles"));
        return body;
    }

    static String baseUrl(HttpServletRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append(request.getScheme()).append("://").append(request.getServerName());
        int port = request.getServerPort();
        boolean defaultPort = ("http".equals(request.getScheme()) && port == 80) || ("https".equals(request.getScheme()) && port == 443);
        if (!defaultPort) {
            sb.append(':').append(port);
        }
        return sb.toString();
    }
}
