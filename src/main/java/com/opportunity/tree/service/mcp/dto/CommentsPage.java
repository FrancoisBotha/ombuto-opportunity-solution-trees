package com.opportunity.tree.service.mcp.dto;

import java.util.List;

/**
 * Bounded page of comments returned by the {@code list_node_comments} MCP tool. Same shape as
 * {@link InterviewsPage} so an agent can page through both the same way.
 */
public record CommentsPage(String nodeType, Long nodeId, int page, int size, int limit, long totalMatching, List<CommentEntry> comments) {}
