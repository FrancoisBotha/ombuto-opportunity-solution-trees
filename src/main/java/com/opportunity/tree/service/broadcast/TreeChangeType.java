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
}
