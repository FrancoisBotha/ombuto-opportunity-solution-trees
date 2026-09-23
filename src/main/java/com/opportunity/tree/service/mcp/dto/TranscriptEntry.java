package com.opportunity.tree.service.mcp.dto;

import com.opportunity.tree.domain.enumeration.MeetingTranscriptSource;
import com.opportunity.tree.domain.enumeration.TreeNodeType;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Metadata-only view of a meeting transcript for the {@code list_transcripts} MCP tool.
 * The body is deliberately omitted — see NFR-022 / NFR-024. Agents call {@code get_transcript}
 * with the id when they need the full text.
 */
public record TranscriptEntry(
    Long id,
    String title,
    LocalDate meetingDate,
    String attendees,
    MeetingTranscriptSource source,
    TreeNodeType nodeType,
    Long nodeId,
    String authorLogin,
    Instant createdDate,
    Instant editedDate
) {}
