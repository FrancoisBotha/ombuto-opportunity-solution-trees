package com.opportunity.tree.service.mcp.dto;

/**
 * One open question (checklist item) on an opportunity, in the shape the {@code get_node} MCP
 * tool returns.
 *
 * <p>{@code text} is the question as the team wrote it; {@code resolved} maps to the
 * {@code done} flag on the underlying entity — {@code true} means the team considers the
 * question answered, {@code false} means it is still open.
 */
public record OpenQuestionDetails(Long id, String text, boolean resolved) {}
