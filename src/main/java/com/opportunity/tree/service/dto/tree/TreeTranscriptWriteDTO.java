package com.opportunity.tree.service.dto.tree;

import com.opportunity.tree.domain.enumeration.MeetingTranscriptSource;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * Request body for creating or editing a meeting transcript on a tree node (Epic 12).
 *
 * <p>On create the six node relationship ids define which node the transcript hangs off — exactly
 * one must be set and it must belong to the caller's team. On update the node relationships are
 * ignored: a transcript is bound to its node for life. {@code source} defaults to {@code PASTED}
 * on create and is ignored on update.
 */
public record TreeTranscriptWriteDTO(
    String title,
    LocalDate meetingDate,
    String attendees,
    String body,
    MeetingTranscriptSource source,
    Long productId,
    Long outcomeId,
    Long opportunityId,
    Long solutionId,
    Long assumptionId,
    Long evidenceId
) implements Serializable {}
