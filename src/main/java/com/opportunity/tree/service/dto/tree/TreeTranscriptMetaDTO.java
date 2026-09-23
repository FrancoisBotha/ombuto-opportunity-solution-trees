package com.opportunity.tree.service.dto.tree;

import com.opportunity.tree.domain.enumeration.MeetingTranscriptSource;
import com.opportunity.tree.domain.enumeration.TreeNodeType;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Metadata view of a meeting transcript — everything the list and detail-panel row need, without
 * the body. The full body is only returned by the single-transcript read endpoint (NFR-024,
 * NFR-022).
 */
public record TreeTranscriptMetaDTO(
    Long id,
    String title,
    LocalDate meetingDate,
    String attendees,
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
