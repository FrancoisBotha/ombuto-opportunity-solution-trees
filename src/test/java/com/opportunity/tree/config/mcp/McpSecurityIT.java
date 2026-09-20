package com.opportunity.tree.config.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.opportunity.tree.IntegrationTest;
import com.opportunity.tree.security.AuthoritiesConstants;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * MCPSRV-002: the stateless bearer-token filter chain refuses requests to {@code /mcp} without a
 * valid JWT and lets the existing authority-mapping converter produce authenticated principals
 * whose authorities line up with the web UI's session login.
 *
 * <p>The JWT decoder is mocked (see {@code TestSecurityConfiguration}) so signature / issuer /
 * audience / expiry checks are exercised in production by the real
 * {@code SecurityConfiguration#jwtDecoder} bean. Here we drive per-test decoder behaviour to
 * assert the filter chain reacts as required by the acceptance criteria.
 */
@IntegrationTest
@AutoConfigureMockMvc
@TestPropertySource(
    properties = {
        "spring.ai.mcp.server.enabled=true",
        "spring.ai.mcp.server.name=opportunity-solution-tree-mcp",
        "spring.ai.mcp.server.version=0.0.1",
        "spring.ai.mcp.server.type=SYNC",
        "spring.ai.mcp.server.stdio=false",
        "spring.ai.mcp.server.sse-endpoint=/mcp",
        "spring.ai.mcp.server.sse-message-endpoint=/mcp/message",
        "spring.ai.mcp.server.capabilities.tool=true",
        "spring.ai.mcp.server.capabilities.resource=false",
        "spring.ai.mcp.server.capabilities.prompt=false",
        "spring.ai.mcp.server.capabilities.completion=false",
    }
)
class McpSecurityIT {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Autowired
    private Converter<Jwt, AbstractAuthenticationToken> authenticationConverter;

    private ListAppender<ILoggingEvent> entryPointAppender;
    private Logger entryPointLogger;

    @BeforeEach
    void resetDecoderMock() {
        reset(jwtDecoder);
        entryPointLogger = (Logger) LoggerFactory.getLogger(McpAuthenticationEntryPoint.class);
        entryPointAppender = new ListAppender<>();
        entryPointAppender.start();
        entryPointLogger.addAppender(entryPointAppender);
        entryPointLogger.setLevel(Level.WARN);
    }

    @AfterEach
    void detachAppender() {
        if (entryPointLogger != null && entryPointAppender != null) {
            entryPointLogger.detachAppender(entryPointAppender);
        }
    }

    private List<ILoggingEvent> warnEvents() {
        return entryPointAppender.list
            .stream()
            .filter(e -> e.getLevel() == Level.WARN)
            .toList();
    }

    @Test
    void requestWithNoAuthorizationHeader_isRefusedWith401() throws Exception {
        MvcResult result = mvc.perform(get("/mcp")).andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(401);
    }

    @Test
    void requestWithExpiredToken_isRefusedWith401() throws Exception {
        when(jwtDecoder.decode(anyString())).thenThrow(
            new JwtValidationException("Jwt expired", List.of(new OAuth2Error("invalid_token", "Jwt expired at ...", null)))
        );

        MvcResult result = mvc.perform(get("/mcp").header("Authorization", "Bearer expired.token.value")).andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(401);
        List<ILoggingEvent> events = warnEvents();
        assertThat(events).isNotEmpty();
        String rendered = events
            .stream()
            .map(ILoggingEvent::getFormattedMessage)
            .reduce("", (a, b) -> a + "\n" + b);
        assertThat(rendered).contains("invalid_token").contains("expired");
        assertThat(rendered).doesNotContain("expired.token.value");
    }

    @Test
    void requestWithWrongAudienceToken_isRefusedWith401() throws Exception {
        when(jwtDecoder.decode(anyString())).thenThrow(
            new JwtValidationException(
                "Invalid audience",
                List.of(new OAuth2Error("invalid_token", "The required audience is missing", null))
            )
        );

        MvcResult result = mvc.perform(get("/mcp").header("Authorization", "Bearer wrong.aud.value")).andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(401);
    }

    @Test
    void requestWithWrongIssuerToken_isRefusedWith401() throws Exception {
        when(jwtDecoder.decode(anyString())).thenThrow(
            new JwtValidationException("Invalid issuer", List.of(new OAuth2Error("invalid_token", "The iss claim is not valid", null)))
        );

        MvcResult result = mvc.perform(get("/mcp").header("Authorization", "Bearer wrong.iss.value")).andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(401);
        List<ILoggingEvent> events = warnEvents();
        assertThat(events).isNotEmpty();
        String rendered = events
            .stream()
            .map(ILoggingEvent::getFormattedMessage)
            .reduce("", (a, b) -> a + "\n" + b);
        assertThat(rendered).contains("invalid_token");
        assertThat(rendered.toLowerCase()).contains("issuer");
        assertThat(rendered).doesNotContain("wrong.iss.value");
    }

    @Test
    void authenticationConverter_mapsValidJwtToPrincipalAndAuthorities() {
        // AC 6: the reused JwtAuthenticationConverter resolves the same principal (preferred_username)
        // and the same authorities as the session login by delegating to SecurityUtils.
        Jwt jwt = Jwt.withTokenValue("valid.token")
            .header("alg", "RS256")
            .claim("sub", "user-uuid")
            .claim("preferred_username", "alice")
            .claim("roles", List.of(AuthoritiesConstants.ADMIN, AuthoritiesConstants.USER))
            .issuedAt(Instant.now().minusSeconds(60))
            .expiresAt(Instant.now().plusSeconds(300))
            .build();

        AbstractAuthenticationToken authentication = authenticationConverter.convert(jwt);

        assertThat(authentication).isNotNull();
        assertThat(authentication.getName()).isEqualTo("alice");
        assertThat(authentication.getAuthorities())
            .extracting(a -> a.getAuthority())
            .contains(AuthoritiesConstants.ADMIN, AuthoritiesConstants.USER);
    }

    @Test
    void requestWithValidToken_passesTheSecurityFilterChain() throws Exception {
        Jwt jwt = Jwt.withTokenValue("valid.token")
            .header("alg", "RS256")
            .claim("sub", "user-uuid")
            .claim("preferred_username", "alice")
            .claim("roles", List.of(AuthoritiesConstants.USER))
            .issuedAt(Instant.now().minusSeconds(60))
            .expiresAt(Instant.now().plusSeconds(300))
            .build();
        when(jwtDecoder.decode(anyString())).thenReturn(jwt);

        MvcResult result = mvc
            .perform(get("/mcp").header("Authorization", "Bearer valid.token").header("Accept", "text/event-stream"))
            .andReturn();

        // A valid JWT must not be refused by the bearer-token filter chain. Whatever the
        // downstream MCP transport does with the request (200/406/404/…) it must not be a
        // 401 from the security layer.
        assertThat(result.getResponse().getStatus()).isNotEqualTo(401);
    }
}
