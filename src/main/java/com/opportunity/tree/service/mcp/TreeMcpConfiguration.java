package com.opportunity.tree.service.mcp;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the read-only tree/product MCP tools ({@link TreeTool}) with the Spring AI MCP
 * server. The MCP server auto-configuration aggregates every {@link ToolCallbackProvider}
 * bean in the context (see {@code McpServerConfiguration} for the probe tool), so this
 * separate provider bean is added alongside without touching that ticket's registration.
 */
@Configuration
public class TreeMcpConfiguration {

    @Bean
    public ToolCallbackProvider treeToolCallbackProvider(TreeTool treeTool) {
        return MethodToolCallbackProvider.builder().toolObjects(treeTool).build();
    }

    @Bean
    public ToolCallbackProvider searchToolCallbackProvider(SearchTool searchTool) {
        return MethodToolCallbackProvider.builder().toolObjects(searchTool).build();
    }
}
