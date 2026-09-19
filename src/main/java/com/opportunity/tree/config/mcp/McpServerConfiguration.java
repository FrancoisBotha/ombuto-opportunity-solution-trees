package com.opportunity.tree.config.mcp;

import com.opportunity.tree.service.mcp.ProbeTool;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the read-only MCP tool set. The MCP server itself is auto-configured by the
 * spring-ai-starter-mcp-server-webmvc starter and served on the path configured in
 * application.yml (spring.ai.mcp.server.sse-endpoint = /mcp).
 */
@Configuration
public class McpServerConfiguration {

    @Bean
    public ToolCallbackProvider probeToolCallbackProvider(ProbeTool probeTool) {
        return MethodToolCallbackProvider.builder().toolObjects(probeTool).build();
    }
}
