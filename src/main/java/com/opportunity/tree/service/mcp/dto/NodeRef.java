package com.opportunity.tree.service.mcp.dto;

/**
 * Compact reference to a tree node: type (uppercase like {@code OPPORTUNITY}), id and title.
 * Used for parent and child summaries returned by the MCP tools.
 */
public record NodeRef(String type, Long id, String title) {}
