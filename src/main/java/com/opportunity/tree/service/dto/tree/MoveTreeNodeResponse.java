package com.opportunity.tree.service.dto.tree;

import java.util.List;

/**
 * Result of a move: the moved node (same shape as the tree read) and the new {@code sortOrder} of
 * every sibling that was renumbered — the new parent's children of the moved node's type and, when
 * the parent changed, the old parent's remaining children of that type.
 */
public record MoveTreeNodeResponse(TreeNodeDTO node, List<SiblingOrderDTO> siblings) {}
