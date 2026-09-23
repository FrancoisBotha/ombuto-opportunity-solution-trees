package com.opportunity.tree.service.mcp.dto;

import java.util.List;

/**
 * Bounded page of transcript metadata returned by the {@code list_transcripts} MCP tool.
 * Either {@code teamId} or ({@code nodeType}, {@code nodeId}) identifies the scope; the other
 * pair is null. Transcript bodies never appear here — see NFR-022 / NFR-024.
 */
public record TranscriptsPage(
    Long teamId,
    String nodeType,
    Long nodeId,
    int page,
    int size,
    int limit,
    long totalMatching,
    List<TranscriptEntry> transcripts
) {}
