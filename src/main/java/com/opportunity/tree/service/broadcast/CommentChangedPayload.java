package com.opportunity.tree.service.broadcast;

import com.opportunity.tree.service.dto.tree.TreeCommentDTO;

/**
 * Payload of a {@link TreeChangeType#COMMENT_ADDED} or {@link TreeChangeType#COMMENT_UPDATED}
 * event: the owning tree node's client key ({@code "opportunity-12"}), the comment DTO and the
 * node's new {@code commentCount}, so clients can update the panel badge without a tree reload.
 */
public record CommentChangedPayload(String nodeKey, TreeCommentDTO comment, long commentCount) {}
