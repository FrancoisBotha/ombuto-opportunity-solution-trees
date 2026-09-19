package com.opportunity.tree.service.dto.tree;

/**
 * Body of {@code POST /api/tree/nodes/move}. Node types are case-insensitive. Products are
 * reordered within their team and take no parent ({@code parentType}/{@code parentId} null); every
 * other node needs a parent. {@code position} is the 0-based index among the new parent's children
 * of the same type (clamped to the end); omitted means append.
 */
public record MoveTreeNodeRequest(String nodeType, Long nodeId, String parentType, Long parentId, Integer position) {}
