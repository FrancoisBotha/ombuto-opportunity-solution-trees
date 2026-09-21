package com.opportunity.tree.service.mcp.dto;

/**
 * One link on a tree node, in the shape the {@code get_node} MCP tool returns.
 *
 * <p>{@code target} is the URL, {@code title} is the display name the team gave the link, and
 * {@code type} is a coarse classification derived from the URL — {@code JIRA} for Atlassian Jira
 * (paths matching {@code /browse/}), {@code CONFLUENCE} for Atlassian Confluence (paths under
 * {@code /wiki/}), otherwise {@code OTHER}. The classification is a hint for the agent; the
 * URL itself is authoritative.
 */
public record LinkDetails(String target, String type, String title) {}
