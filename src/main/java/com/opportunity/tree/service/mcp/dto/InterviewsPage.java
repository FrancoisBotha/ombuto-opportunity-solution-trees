package com.opportunity.tree.service.mcp.dto;

import java.util.List;

/**
 * Bounded page of interviews returned by the {@code list_interviews} MCP tool.
 * {@code notesIncluded} tells the agent whether the {@code notes} field on each summary
 * was populated for this caller; when {@code false}, notes were withheld by the same
 * privacy rule as the web UI (Epic 7 NFR-014).
 */
public record InterviewsPage(
    Long teamId,
    Long productId,
    int page,
    int size,
    int limit,
    long totalMatching,
    boolean notesIncluded,
    List<InterviewSummary> interviews
) {}
