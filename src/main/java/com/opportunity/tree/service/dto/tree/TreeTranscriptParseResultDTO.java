package com.opportunity.tree.service.dto.tree;

/**
 * Result of a parse-only transcript upload (Epic 12 / MTRANS-003). Carries the cleaned plain
 * text for the client to preview; nothing is persisted server-side by the parse endpoint.
 */
public record TreeTranscriptParseResultDTO(String text) {}
