package com.opportunity.tree.service.mcp.dto;

import java.util.List;

/**
 * Output of the {@code get_node} MCP tool: the identified node's basic fields, its parent
 * (absent for a product), its children summary, and counts of links / evidence / comments.
 *
 * <p>Fields whose underlying feature does not apply to the node type (for example
 * {@code evidenceCount} for a solution) are returned as {@code null} so the tool
 * response degrades gracefully rather than fabricating a zero.
 */
public record NodeDetails(
    String type,
    Long id,
    String title,
    String description,
    String status,
    NodeRef parent,
    List<NodeRef> children,
    Long linkCount,
    Long evidenceCount,
    Long commentCount
) {}
