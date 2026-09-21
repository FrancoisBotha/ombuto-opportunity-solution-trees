package com.opportunity.tree.config.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.opportunity.tree.IntegrationTest;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * MCPSRV-008: an MCP client discovers the authorization server without any hand-minted token by
 * following the {@code WWW-Authenticate} challenge on a 401 to the OAuth 2.0 protected-resource
 * metadata document, per RFC 9728 / the MCP authorization specification.
 *
 * <p>This IT covers the two contracts the client relies on:
 * <ul>
 *   <li>{@code GET /.well-known/oauth-protected-resource} returns 200 with a JSON body that names
 *       the MCP resource URL and the Keycloak authorization-server issuer;</li>
 *   <li>{@code POST /mcp} without an {@code Authorization} header returns 401 with a
 *       {@code WWW-Authenticate: Bearer resource_metadata="..."} header that points at that
 *       metadata endpoint.</li>
 * </ul>
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
class McpProtectedResourceMetadataIT {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void resetDecoderMock() {
        reset(jwtDecoder);
    }

    @Test
    void protectedResourceMetadata_isPubliclyReadableAndNamesTheAuthorizationServer() throws Exception {
        mvc
            .perform(get("/.well-known/oauth-protected-resource").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.resource").value(org.hamcrest.Matchers.endsWith("/mcp")))
            .andExpect(jsonPath("$.authorization_servers[0]").value("http://DO_NOT_CALL:9080/realms/jhipster"))
            .andExpect(jsonPath("$.bearer_methods_supported[0]").value("header"))
            .andExpect(jsonPath("$.scopes_supported").isArray());
    }

    @Test
    void pathDerivedProtectedResourceMetadata_isPubliclyReadableForClaudeCodeDiscovery() throws Exception {
        mvc
            .perform(get("/.well-known/oauth-protected-resource/mcp").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.resource").value(org.hamcrest.Matchers.endsWith("/mcp")))
            .andExpect(jsonPath("$.authorization_servers[0]").value("http://DO_NOT_CALL:9080/realms/jhipster"));
    }

    @Test
    void mcpEndpoint_missingAuthorization_returns401WithResourceMetadataChallenge() throws Exception {
        MvcResult result = mvc.perform(post("/mcp")).andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(401);
        String challenge = result.getResponse().getHeader(HttpHeaders.WWW_AUTHENTICATE);
        assertThat(challenge).as("MCP authorization spec requires a WWW-Authenticate challenge on 401").isNotBlank();
        assertThat(challenge).isEqualTo("Bearer resource_metadata=\"http://localhost/.well-known/oauth-protected-resource\"");
    }

    /**
     * The refresh-across-expiry contract: the MCP client keeps its Streamable HTTP session past the
     * access-token lifetime by retrying against the same 401 + {@code WWW-Authenticate} challenge
     * once its refresh-token exchange produces a fresh access token. That retry only works if the
     * server answers a request bearing an <em>expired</em> token with the same challenge shape a
     * missing-token request gets — same {@code Bearer}, same {@code resource_metadata=...} pointing
     * at the discovery document, plus the OAuth2 {@code invalid_token} / expired hint the client
     * uses to distinguish "refresh me" from other failure modes. This test pins that contract.
     */
    @Test
    void mcpEndpoint_afterAccessTokenExpiry_returns401WithSameResourceMetadataChallenge_soClientRefreshes() throws Exception {
        when(jwtDecoder.decode(anyString())).thenThrow(
            new JwtValidationException(
                "Jwt expired",
                List.of(new OAuth2Error("invalid_token", "Jwt expired at 2026-09-21T09:00:00Z", null))
            )
        );

        MvcResult result = mvc.perform(post("/mcp").header("Authorization", "Bearer expired.token.value")).andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(401);
        String challenge = result.getResponse().getHeader(HttpHeaders.WWW_AUTHENTICATE);
        assertThat(challenge).as("expiry must produce the same challenge shape as a missing token").isNotBlank();
        assertThat(challenge).startsWith("Bearer ");
        assertThat(challenge).contains("resource_metadata=\"");
        assertThat(challenge).contains("/.well-known/oauth-protected-resource");
        assertThat(challenge).contains("error=\"invalid_token\"");
        assertThat(challenge).containsIgnoringCase("expired");
    }
}
