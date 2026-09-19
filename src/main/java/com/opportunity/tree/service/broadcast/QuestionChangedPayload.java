package com.opportunity.tree.service.broadcast;

import com.opportunity.tree.service.dto.tree.TreeQuestionDTO;

/**
 * Payload of a {@link TreeChangeType#QUESTION_ADDED} or {@link TreeChangeType#QUESTION_UPDATED}
 * event: the owning opportunity's client key ({@code "opportunity-12"}) and the full open-question
 * DTO.
 */
public record QuestionChangedPayload(String nodeKey, TreeQuestionDTO question) {}
