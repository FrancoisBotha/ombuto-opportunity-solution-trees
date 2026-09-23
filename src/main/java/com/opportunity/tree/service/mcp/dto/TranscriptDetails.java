package com.opportunity.tree.service.mcp.dto;

import com.opportunity.tree.domain.enumeration.MeetingTranscriptSource;
import com.opportunity.tree.domain.enumeration.TreeNodeType;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Full transcript record — including body — returned by the {@code get_transcript} MCP tool.
 * This is the only MCP shape that carries the body (NFR-022 / NFR-024).
 */
public record TranscriptDetails(
    Long id,
    String title,
    LocalDate meetingDate,
    String attendees,
    String body,
    MeetingTranscriptSource source,
    TreeNodeType nodeType,
    Long nodeId,
    String authorLogin,
    Instant createdDate,
    Instant editedDate
) {}
