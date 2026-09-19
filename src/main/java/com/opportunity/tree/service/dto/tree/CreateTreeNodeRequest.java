package com.opportunity.tree.service.dto.tree;

/**
 * Body of {@code POST /api/tree/nodes}. {@code type} and {@code parentType} are node type names,
 * case-insensitive ({@code "opportunity"} or {@code "OPPORTUNITY"}). {@code title} is optional; when
 * absent or blank the type's default title is used ({@code "New opportunity"}, {@code "New snippet"}
 * for evidence, …).
 *
 * <p>{@code parentId} is bound as {@link Number} so that a fractional value reaches the service
 * instead of being truncated by Jackson; the service accepts whole numbers only (400
 * {@code parentmissing}).
 */
public record CreateTreeNodeRequest(String type, String parentType, Number parentId, String title) {}
