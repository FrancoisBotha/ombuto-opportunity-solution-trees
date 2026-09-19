package com.opportunity.tree.service.broadcast;

/**
 * Type of a tree change event broadcast to {@code /topic/teams/{teamId}/tree} after commit.
 * See {@link TreeChangeEvent}.
 */
public enum TreeChangeType {
    NODE_CREATED,
    NODE_UPDATED,
    NODE_MOVED,
    NODE_DELETED,
    LINK_ADDED,
    LINK_UPDATED,
    LINK_REMOVED,
    QUESTION_ADDED,
    QUESTION_UPDATED,
    QUESTION_REMOVED,
    COMMENT_ADDED,
    COMMENT_UPDATED,
    COMMENT_DELETED,
    MEMBERSHIP_CHANGED,
}
