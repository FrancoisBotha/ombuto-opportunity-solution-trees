package com.opportunity.tree.config.mcp;

import com.opportunity.tree.config.ApplicationProperties;
import org.springframework.ai.mcp.server.common.autoconfigure.properties.McpServerStreamableHttpProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;

/**
 * A stateless, CSRF-exempt filter chain isolated to the MCP endpoint path that authenticates
 * callers by Keycloak bearer token (OAuth2 resource server, JWT).
 *
 * <p>The MCP transport is Streamable HTTP: every JSON-RPC message is a POST/GET/DELETE against
 * a single URL ({@code /mcp}), and per-session identity is carried in the {@code Mcp-Session-Id}
 * HTTP header (both request and response) rather than in a path segment or query parameter.
 * This removes the URL-spelling attack surface that the previous {@code /mcp/message} path had:
 * the session id can no longer be smuggled through a percent-encoded path variant because it is
 * not in the path at all.
 *
 * <p>Registered with {@link Order} ahead of the session-based {@code SecurityConfiguration#filterChain}
 * and restricted with {@link HttpSecurity#securityMatcher} to {@code /mcp} so that the existing
 * web login, CSRF ({@code CookieCsrfTokenRepository} + {@code SpaCsrfTokenRequestHandler}) and
 * REST API rules stay untouched for every other path.
 *
 * <p>The JWT decoder ({@code SecurityConfiguration#jwtDecoder}) shared with the session chain
 * validates the token's signature against the Keycloak JWK set and enforces issuer and expiry;
 * the shared {@code Converter<Jwt, AbstractAuthenticationToken>} bean maps its claims to the same
 * principal and authorities the session login produces, so downstream services such as
 * {@code TeamAccessService} see the same {@code SecurityContext} whether the caller arrived
 * through the web UI or an MCP client.
 *
 * <p>The audience check on this chain is <em>not</em> the shared session validator: that list
 * ({@code jhipster.security.oauth2.audience}) has to accept audiences almost every realm token
 * carries (Keycloak stamps {@code account} on session tokens too), so on its own it lets a
 * browser-app token pass at {@code /mcp}. Instead this chain installs {@link McpAudienceFilter}
 * with {@code application.mcp.audience}, kept separate so widening one cannot silently widen the
 * other. Only tokens issued to a client that carries an MCP audience (Keycloak's {@code mcp_client}
 * with its {@code oidc-audience-mapper} for {@code mcp-server}) are accepted here.
 */
@Configuration
public class McpSecurityConfiguration {

    static final String MCP_DEFAULT_ENDPOINT = "/mcp";

    /**
     * A stateless, CSRF-exempt filter chain isolated to the OAuth 2.0 protected-resource metadata
     * document (RFC 9728 / MCP authorization spec). The MCP client fetches this document, without a
     * token, to discover the authorization server(s) after a 401 challenge — gating it on a token
     * would defeat the discovery step. Registered ahead of the MCP chain so its
     * {@link HttpSecurity#securityMatcher} wins for the metadata path only.
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE + 5)
    public SecurityFilterChain mcpMetadataSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher(McpProtectedResourceMetadataResource.METADATA_PATH, McpProtectedResourceMetadataResource.MCP_METADATA_PATH)
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz.anyRequest().permitAll());
        return http.build();
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE + 10)
    public SecurityFilterChain mcpSecurityFilterChain(
        HttpSecurity http,
        Converter<Jwt, AbstractAuthenticationToken> authenticationConverter,
        McpSessionRegistry mcpSessionRegistry,
        ObjectProvider<McpServerStreamableHttpProperties> streamableProperties,
        ApplicationProperties applicationProperties
    ) throws Exception {
        McpAuthenticationEntryPoint entryPoint = new McpAuthenticationEntryPoint();
        McpServerStreamableHttpProperties properties = streamableProperties.getIfAvailable();
        String endpoint = properties != null && properties.getMcpEndpoint() != null ? properties.getMcpEndpoint() : MCP_DEFAULT_ENDPOINT;
        McpAudienceFilter audienceFilter = new McpAudienceFilter(applicationProperties.getMcp().getAudience(), entryPoint);
        http
            .securityMatcher(endpoint)
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz.anyRequest().authenticated())
            .exceptionHandling(eh -> eh.authenticationEntryPoint(entryPoint))
            .oauth2ResourceServer(oauth2 ->
                oauth2.authenticationEntryPoint(entryPoint).jwt(jwt -> jwt.jwtAuthenticationConverter(authenticationConverter))
            )
            // Before AuthorizationFilter: reject a token that is not scoped to the MCP server as
            // 401 (invalid_token), matching every other bearer-token failure on this chain.
            .addFilterBefore(audienceFilter, AuthorizationFilter.class)
            // After AuthorizationFilter: the caller is authenticated, so the MCP session id
            // (Mcp-Session-Id header) can be bound to them and any request that carries somebody
            // else's session id can be refused.
            .addFilterAfter(new McpSessionPrincipalFilter(mcpSessionRegistry), AuthorizationFilter.class);
        return http.build();
    }
}
