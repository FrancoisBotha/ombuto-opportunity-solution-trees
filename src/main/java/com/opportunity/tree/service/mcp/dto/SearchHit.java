package com.opportunity.tree.service.mcp.dto;

/**
 * One hit returned by the {@code search_nodes} MCP tool: enough for an agent to answer or
 * follow up with {@code get_node} / {@code get_tree}. {@code status} is null for node types
 * that have no status field (product, outcome, evidence).
 */
public record SearchHit(
    String type,
    Long id,
    String title,
    String description,
    String status,
    Long teamId,
    String teamName,
    String parentType,
    Long parentId
) {
    public SearchHit(String type, Long id, String title, String description, String status, Long teamId, String teamName) {
        this(type, id, title, description, status, teamId, teamName, null, null);
    }
}
