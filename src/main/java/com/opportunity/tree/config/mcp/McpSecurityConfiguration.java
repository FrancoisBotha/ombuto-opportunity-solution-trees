package com.opportunity.tree.config.mcp;

import com.opportunity.tree.config.ApplicationProperties;
import org.springframework.ai.mcp.server.common.autoconfigure.properties.McpServerSseProperties;
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
 * A stateless, CSRF-exempt filter chain isolated to the MCP endpoint paths that authenticates
 * callers by Keycloak bearer token (OAuth2 resource server, JWT).
 *
 * <p>Registered with {@link Order} ahead of the session-based {@code SecurityConfiguration#filterChain}
 * and restricted with {@link HttpSecurity#securityMatcher} to {@code /mcp} and {@code /mcp/**} so
 * that the existing web login, CSRF ({@code CookieCsrfTokenRepository} +
 * {@code SpaCsrfTokenRequestHandler}) and REST API rules stay untouched for every other path.
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

    static final String MCP_SSE_PATH = "/mcp";
    static final String MCP_MESSAGE_PATH_PATTERN = "/mcp/**";
    static final String MCP_DEFAULT_MESSAGE_PATH = "/mcp/message";

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE + 10)
    public SecurityFilterChain mcpSecurityFilterChain(
        HttpSecurity http,
        Converter<Jwt, AbstractAuthenticationToken> authenticationConverter,
        McpSessionRegistry mcpSessionRegistry,
        ObjectProvider<McpServerSseProperties> sseProperties,
        ApplicationProperties applicationProperties
    ) throws Exception {
        McpAuthenticationEntryPoint entryPoint = new McpAuthenticationEntryPoint();
        McpServerSseProperties properties = sseProperties.getIfAvailable();
        String ssePath = properties != null ? properties.getSseEndpoint() : MCP_SSE_PATH;
        String messagePath = properties != null ? properties.getSseMessageEndpoint() : MCP_DEFAULT_MESSAGE_PATH;
        McpAudienceFilter audienceFilter = new McpAudienceFilter(applicationProperties.getMcp().getAudience(), entryPoint);
        http
            .securityMatcher(MCP_SSE_PATH, MCP_MESSAGE_PATH_PATTERN)
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
            // After AuthorizationFilter: the caller is authenticated, so the SSE session can be
            // bound to them and a message posted to somebody else's session can be refused.
            .addFilterAfter(new McpSessionPrincipalFilter(mcpSessionRegistry, ssePath, messagePath), AuthorizationFilter.class);
        return http.build();
    }
}
