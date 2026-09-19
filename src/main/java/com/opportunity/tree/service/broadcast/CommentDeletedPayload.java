package com.opportunity.tree.service.broadcast;

/**
 * Payload of a {@link TreeChangeType#COMMENT_DELETED} event: the owning tree node's client key,
 * the id of the deleted comment and the node's new {@code commentCount}.
 */
public record CommentDeletedPayload(String nodeKey, Long commentId, long commentCount) {}
