package com.opportunity.tree.config.mcp;

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
 * validates the token's signature against the Keycloak JWK set and enforces issuer, audience
 * (via {@code AudienceValidator}) and expiry; the shared {@code Converter<Jwt,
 * AbstractAuthenticationToken>} bean maps its claims to the same principal and authorities the
 * session login produces, so downstream services such as {@code TeamAccessService} see the same
 * {@code SecurityContext} whether the caller arrived through the web UI or an MCP client.
 */
@Configuration
public class McpSecurityConfiguration {

    static final String MCP_SSE_PATH = "/mcp";
    static final String MCP_MESSAGE_PATH_PATTERN = "/mcp/**";

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE + 10)
    public SecurityFilterChain mcpSecurityFilterChain(
        HttpSecurity http,
        Converter<Jwt, AbstractAuthenticationToken> authenticationConverter
    ) throws Exception {
        McpAuthenticationEntryPoint entryPoint = new McpAuthenticationEntryPoint();
        http
            .securityMatcher(MCP_SSE_PATH, MCP_MESSAGE_PATH_PATTERN)
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz.anyRequest().authenticated())
            .exceptionHandling(eh -> eh.authenticationEntryPoint(entryPoint))
            .oauth2ResourceServer(oauth2 ->
                oauth2.authenticationEntryPoint(entryPoint).jwt(jwt -> jwt.jwtAuthenticationConverter(authenticationConverter))
            );
        return http.build();
    }
}
