package com.opportunity.tree.config.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.opportunity.tree.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
 *   <li>{@code GET /mcp} without an {@code Authorization} header returns 401 with a
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
        "spring.ai.mcp.server.sse-endpoint=/mcp",
        "spring.ai.mcp.server.sse-message-endpoint=/mcp/message",
        "spring.ai.mcp.server.capabilities.tool=true",
        "spring.ai.mcp.server.capabilities.resource=false",
        "spring.ai.mcp.server.capabilities.prompt=false",
        "spring.ai.mcp.server.capabilities.completion=false",
    }
)
class McpProtectedResourceMetadataIT {

    @Autowired
    private MockMvc mvc;

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
    void mcpEndpoint_missingAuthorization_returns401WithResourceMetadataChallenge() throws Exception {
        MvcResult result = mvc.perform(get("/mcp")).andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(401);
        String challenge = result.getResponse().getHeader(HttpHeaders.WWW_AUTHENTICATE);
        assertThat(challenge).as("MCP authorization spec requires a WWW-Authenticate challenge on 401").isNotBlank();
        assertThat(challenge).startsWith("Bearer ");
        assertThat(challenge).contains("resource_metadata=\"");
        assertThat(challenge).contains("/.well-known/oauth-protected-resource");
    }
}
