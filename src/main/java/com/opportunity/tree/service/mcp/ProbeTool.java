package com.opportunity.tree.service.mcp;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

/**
 * Trivial read-only probe tool used to prove MCP tool discovery and invocation over the transport.
 * Exposes no application data.
 */
@Service
public class ProbeTool {

    @Tool(
        name = "ping",
        description = "Read-only probe. Returns a static acknowledgement so agents can verify MCP transport and tool discovery."
    )
    public String ping() {
        return "pong";
    }
}
