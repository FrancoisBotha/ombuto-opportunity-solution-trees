package com.opportunity.tree.config.mcp;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * A stateless, CSRF-exempt filter chain isolated to the MCP endpoint paths. Left permit-all in this
 * ticket so the probe tool is reachable over the transport; MCPSRV-002 replaces this with a Keycloak
 * bearer-token resource-server chain. Keeping this chain in a separate class (higher precedence
 * than {@code SecurityConfiguration}) preserves the existing session login, CSRF protection and
 * REST API rules verbatim for every non-MCP path.
 */
@Configuration
public class McpSecurityConfiguration {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE + 10)
    public SecurityFilterChain mcpSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/mcp", "/mcp/**")
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz.anyRequest().permitAll());
        return http.build();
    }
}
