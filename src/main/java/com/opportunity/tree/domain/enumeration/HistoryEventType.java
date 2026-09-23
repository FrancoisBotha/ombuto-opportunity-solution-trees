package com.opportunity.tree.domain.enumeration;

/**
 * The HistoryEventType enumeration.
 */
public enum HistoryEventType {
    CREATED,
    STATUS_CHANGED,
    CONFIDENCE_CHANGED,
    PRIORITY_CHANGED,
    VALUE_CHANGED,
    MOVED,
    COMMENT_ADDED,
    COMMENT_DELETED,
    LINK_ADDED,
    LINK_REMOVED,
    QUESTION_ADDED,
    TRANSCRIPT_ADDED,
    TRANSCRIPT_DELETED,
    /** LABEL-001: a tag was applied to or removed from a node. */
    TAGS_CHANGED,
}
