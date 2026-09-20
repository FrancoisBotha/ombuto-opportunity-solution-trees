package com.opportunity.tree.service.mcp.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * One interview in a {@link InterviewsPage}: date, title, participant / interviewer label and
 * the opportunities it is linked to. {@code notes} is only populated when the caller may read
 * interview notes (privacy rule from Epic 7 NFR-014); otherwise it is {@code null}.
 */
public record InterviewSummary(
    Long id,
    LocalDate date,
    String title,
    String participant,
    String interviewer,
    String notes,
    List<OpportunityRef> opportunities
) {}
