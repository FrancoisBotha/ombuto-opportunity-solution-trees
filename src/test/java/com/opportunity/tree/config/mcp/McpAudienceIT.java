package com.opportunity.tree.config.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.opportunity.tree.IntegrationTest;
import com.opportunity.tree.security.AuthoritiesConstants;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * MCPSRV-007: the MCP filter chain refuses a valid realm token whose {@code aud} claim does not
 * include the MCP audience (for example, the browser {@code web_app} session token that only
 * carries {@code account}). Fails on the pre-ticket code, which delegated the audience check to
 * the shared {@link com.opportunity.tree.security.oauth2.AudienceValidator} configured with
 * {@code account} + {@code api://default} — a list that accepts essentially every realm token.
 *
 * <p>Drives both the Streamable HTTP handshake ({@code GET /mcp}) and a JSON-RPC POST
 * ({@code POST /mcp}) so the check holds on every entrypoint the transport exposes.
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
        "spring.ai.mcp.server.protocol=STREAMABLE",
        "spring.ai.mcp.server.streamable-http.mcp-endpoint=/mcp",
        "spring.ai.mcp.server.capabilities.tool=true",
        "spring.ai.mcp.server.capabilities.resource=false",
        "spring.ai.mcp.server.capabilities.prompt=false",
        "spring.ai.mcp.server.capabilities.completion=false",
    }
)
class McpAudienceIT {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void resetDecoderMock() {
        reset(jwtDecoder);
    }

    /** A valid realm token issued to `web_app`: signed, unexpired, correct issuer, `aud: [account]`. */
    private Jwt webAppSessionTokenWithoutMcpAudience() {
        return Jwt.withTokenValue("valid.web-app.session.token")
            .header("alg", "RS256")
            .claim("sub", "user-uuid")
            .claim("preferred_username", "alice")
            .claim("roles", List.of(AuthoritiesConstants.USER))
            // `account` is what almost every realm token carries; it is NOT the MCP audience.
            .audience(List.of("account"))
            .issuedAt(Instant.now().minusSeconds(60))
            .expiresAt(Instant.now().plusSeconds(300))
            .build();
    }

    @Test
    void getMcp_withRealmTokenLackingMcpAudience_isRefusedWith401() throws Exception {
        when(jwtDecoder.decode(anyString())).thenReturn(webAppSessionTokenWithoutMcpAudience());

        MvcResult result = mvc
            .perform(get("/mcp").header("Authorization", "Bearer valid.web-app.session.token").header("Accept", "text/event-stream"))
            .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(401);
    }

    @Test
    void postMcp_withRealmTokenLackingMcpAudience_isRefusedWith401() throws Exception {
        when(jwtDecoder.decode(anyString())).thenReturn(webAppSessionTokenWithoutMcpAudience());

        MvcResult result = mvc
            .perform(
                post("/mcp")
                    .header("Authorization", "Bearer valid.web-app.session.token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\"," +
                            "\"params\":{\"protocolVersion\":\"2024-11-05\"," +
                            "\"capabilities\":{},\"clientInfo\":{\"name\":\"test\",\"version\":\"1\"}}}"
                    )
            )
            .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(401);
    }

    @Test
    void getMcp_withTokenCarryingMcpAudience_isNotRefusedByTheSecurityLayer() throws Exception {
        Jwt jwt = Jwt.withTokenValue("valid.mcp.client.token")
            .header("alg", "RS256")
            .claim("sub", "user-uuid")
            .claim("preferred_username", "alice")
            .claim("roles", List.of(AuthoritiesConstants.USER))
            .audience(List.of("mcp-server"))
            .issuedAt(Instant.now().minusSeconds(60))
            .expiresAt(Instant.now().plusSeconds(300))
            .build();
        when(jwtDecoder.decode(anyString())).thenReturn(jwt);

        MvcResult result = mvc
            .perform(get("/mcp").header("Authorization", "Bearer valid.mcp.client.token").header("Accept", "text/event-stream"))
            .andReturn();

        // Whatever the transport does downstream, the security layer must not refuse a token that
        // carries the MCP audience.
        assertThat(result.getResponse().getStatus()).isNotEqualTo(401);
    }
}
