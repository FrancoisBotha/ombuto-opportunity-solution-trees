package com.opportunity.tree.service.dto.tree;

import com.opportunity.tree.domain.enumeration.MeetingTranscriptSource;
import com.opportunity.tree.domain.enumeration.TreeNodeType;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;

/**
 * A single transcript with its full body, returned only by the single-transcript read endpoint.
 * Everywhere else (list by node, list by team, WebSocket events) the metadata-only
 * {@link TreeTranscriptMetaDTO} is used instead — see NFR-022 / NFR-024.
 */
public record TreeTranscriptDTO(
    Long id,
    String title,
    LocalDate meetingDate,
    String attendees,
    String body,
    MeetingTranscriptSource source,
    TreeNodeType nodeType,
    Long nodeId,
    String nodeKey,
    String authorLogin,
    String authorInitials,
    String authorName,
    Instant createdDate,
    Instant editedDate
) implements Serializable {}
