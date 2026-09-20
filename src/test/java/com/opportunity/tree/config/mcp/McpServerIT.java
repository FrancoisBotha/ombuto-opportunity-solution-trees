package com.opportunity.tree.config.mcp;

import static org.assertj.core.api.Assertions.assertThat;

import com.opportunity.tree.IntegrationTest;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.mcp.server.common.autoconfigure.properties.McpServerProperties;
import org.springframework.ai.mcp.server.common.autoconfigure.properties.McpServerSseProperties;
import org.springframework.ai.mcp.server.webmvc.transport.WebMvcSseServerTransportProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * MCPSRV-001 AC 5: the Spring context starts with the MCP server starter on the classpath, the
 * MCP endpoint is bound to the dedicated {@code /mcp} path, and the read-only probe tool reaches
 * the MCP server's tool registry (not just the {@code ToolCallbackProvider} bean this ticket
 * declares — that would prove nothing about the hand-off through
 * {@code ToolCallbackConverterAutoConfiguration}).
 *
 * The MCP block from {@code src/main/resources/config/application.yml} is repeated here as
 * {@code @TestPropertySource} because the JHipster test profile replaces the main
 * {@code application.yml} rather than overlaying it. The values MUST stay in sync with the
 * production configuration.
 */
@IntegrationTest
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
class McpServerIT {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private McpServerProperties mcpServerProperties;

    @Autowired
    private McpServerSseProperties mcpServerSseProperties;

    @Autowired
    private WebMvcSseServerTransportProvider webMvcSseServerTransportProvider;

    @Autowired
    @Qualifier("webMvcSseServerRouterFunction")
    private RouterFunction<ServerResponse> webMvcSseServerRouterFunction;

    @Autowired
    private McpSyncServer mcpSyncServer;

    @Autowired
    @Qualifier("syncTools")
    private List<McpServerFeatures.SyncToolSpecification> syncToolSpecifications;

    @Test
    void mcpEndpointIsMappedOnTheDedicatedPath() {
        // AC 3 + AC 5: the transport is actually mapped on /mcp — the WebMvc SSE transport
        // provider is present, its bound endpoint is /mcp, and the RouterFunction the starter
        // registers to serve it is a bean in the context.
        assertThat(mcpServerProperties.isEnabled()).isTrue();
        assertThat(mcpServerProperties.isStdio()).isFalse();
        assertThat(mcpServerSseProperties.getSseEndpoint()).isEqualTo("/mcp");
        assertThat(mcpServerSseProperties.getSseMessageEndpoint()).isEqualTo("/mcp/message");

        assertThat(context.getBeanNamesForType(WebMvcSseServerTransportProvider.class)).isNotEmpty();
        assertThat(webMvcSseServerTransportProvider).isNotNull();
        assertThat(webMvcSseServerRouterFunction).isNotNull();
    }

    @Test
    void mcpServerIsConfiguredReadOnlyToolsOnly() {
        // AC 6: no MCP resources, prompts, completions or writes are enabled beyond tools.
        McpServerProperties.Capabilities capabilities = mcpServerProperties.getCapabilities();
        assertThat(capabilities.isTool()).isTrue();
        assertThat(capabilities.isResource()).isFalse();
        assertThat(capabilities.isPrompt()).isFalse();
        assertThat(capabilities.isCompletion()).isFalse();
    }

    @Test
    void probeToolIsListedByServerToolRegistry() {
        // AC 4 + AC 5: the probe tool reaches the MCP server's own tool registry via the
        // ToolCallbackProvider -> ToolCallbackConverterAutoConfiguration -> McpSyncServer link.
        // Reading back the ToolCallbackProvider this ticket declares would not prove that link.
        assertThat(mcpSyncServer).isNotNull();

        assertThat(syncToolSpecifications)
            .as("MCP server sync tool registry")
            .isNotEmpty()
            .anySatisfy(spec -> assertThat(spec.tool().name()).isEqualTo("ping"));
    }
}
