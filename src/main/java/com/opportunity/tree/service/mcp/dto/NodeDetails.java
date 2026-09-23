package com.opportunity.tree.service.mcp.dto;

import java.time.Instant;
import java.util.List;

/**
 * Output of the {@code get_node} MCP tool: the identified node's basic fields, its parent
 * (absent for a product), its children summary, its links, and — for opportunities only —
 * its open questions.
 *
 * <p>Fields whose underlying feature does not apply to the node type (for example
 * {@code evidenceCount} for a solution, or {@code openQuestions} for anything that is not an
 * opportunity) are returned as {@code null} so the tool response degrades gracefully rather
 * than fabricating a zero or an empty list.
 *
 * <p>{@code linkCount} is retained alongside the {@code links} list so existing agents that
 * only look at the count keep working; {@code linkCount} matches {@code links.size()} when
 * {@code links} is non-null.
 */
public record NodeDetails(
    String type,
    Long id,
    String title,
    String description,
    String status,
    NodeRef parent,
    List<NodeRef> children,
    List<LinkDetails> links,
    Long linkCount,
    List<OpenQuestionDetails> openQuestions,
    Long evidenceCount,
    Long commentCount,
    Integer priority,
    Integer valueRating,
    Integer confidence,
    String ownerLogin,
    Instant createdDate,
    Instant lastModifiedDate,
    /** LABEL-001: only populated for OPPORTUNITY and SOLUTION; null for other node types. */
    List<String> labels
) {}
