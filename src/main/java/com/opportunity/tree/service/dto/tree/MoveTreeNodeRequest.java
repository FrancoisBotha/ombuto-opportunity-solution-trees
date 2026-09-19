package com.opportunity.tree.service.dto.tree;

/**
 * Body of {@code POST /api/tree/nodes/move}. Node types are case-insensitive. Products are
 * reordered within their team and take no parent ({@code parentType}/{@code parentId} null); every
 * other node needs a parent. {@code position} is the 0-based index among the new parent's children
 * of the same type (clamped to the end); omitted means append.
 *
 * <p>The numeric fields are bound as {@link Number} so that a fractional value ({@code 1.5}) reaches
 * the service as such instead of being truncated by Jackson; the service accepts whole numbers only
 * (400 {@code nodemissing} / {@code parentmissing} / {@code invalidposition}).
 */
public record MoveTreeNodeRequest(String nodeType, Number nodeId, String parentType, Number parentId, Number position) {}
