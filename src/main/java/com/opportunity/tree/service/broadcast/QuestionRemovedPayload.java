package com.opportunity.tree.service.broadcast;

/**
 * Payload of a {@link TreeChangeType#QUESTION_REMOVED} event: the owning opportunity's client key
 * and the id of the removed question.
 */
public record QuestionRemovedPayload(String nodeKey, Long questionId) {}
