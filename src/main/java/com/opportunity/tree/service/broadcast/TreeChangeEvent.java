package com.opportunity.tree.service.broadcast;

import java.time.Instant;

/**
 * Event broadcast to {@code /topic/teams/{teamId}/tree} strictly after the write transaction commits.
 *
 * <p>{@code seq} is a per-team monotonic counter, assigned under the team's structure lock so events
 * for one team can never be numbered out of order. {@code epoch} is generated once per application
 * start; a change in {@code epoch} tells clients to reload the tree instead of replaying events.
 * {@code requestId} echoes the optional client request id ({@code X-Client-Request-Id} header) that
 * originated the write, so a client can suppress its own echo. {@code payload} shape:
 *
 * <ul>
 *   <li>{@link TreeChangeType#NODE_CREATED} / {@link TreeChangeType#NODE_UPDATED} — the full
 *       {@link com.opportunity.tree.service.dto.tree.TreeNodeDTO}</li>
 *   <li>{@link TreeChangeType#NODE_MOVED} — a
 *       {@link com.opportunity.tree.service.dto.tree.MoveTreeNodeResponse}</li>
 *   <li>{@link TreeChangeType#NODE_DELETED} — a {@link NodeDeletedPayload}</li>
 *   <li>{@link TreeChangeType#LINK_ADDED} / {@link TreeChangeType#LINK_UPDATED} — a
 *       {@link LinkChangedPayload}</li>
 *   <li>{@link TreeChangeType#LINK_REMOVED} — a {@link LinkRemovedPayload}</li>
 *   <li>{@link TreeChangeType#QUESTION_ADDED} / {@link TreeChangeType#QUESTION_UPDATED} — a
 *       {@link QuestionChangedPayload}</li>
 *   <li>{@link TreeChangeType#QUESTION_REMOVED} — a {@link QuestionRemovedPayload}</li>
 *   <li>{@link TreeChangeType#COMMENT_ADDED} / {@link TreeChangeType#COMMENT_UPDATED} — a
 *       {@link CommentChangedPayload}</li>
 *   <li>{@link TreeChangeType#COMMENT_DELETED} — a {@link CommentDeletedPayload}</li>
 *   <li>{@link TreeChangeType#MEMBERSHIP_CHANGED} — a {@link MembershipChangedPayload}</li>
 * </ul>
 */
public record TreeChangeEvent(
    TreeChangeType type,
    Long teamId,
    String actingUserLogin,
    Instant at,
    long seq,
    String epoch,
    String requestId,
    Object payload
) {}
