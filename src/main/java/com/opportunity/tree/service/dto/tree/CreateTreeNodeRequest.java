package com.opportunity.tree.service.dto.tree;

/**
 * Body of {@code POST /api/tree/nodes}. {@code type} and {@code parentType} are node type names,
 * case-insensitive ({@code "opportunity"} or {@code "OPPORTUNITY"}). {@code title} is optional; when
 * absent or blank the type's default title is used ({@code "New opportunity"}, {@code "New snippet"}
 * for evidence, …).
 */
public record CreateTreeNodeRequest(String type, String parentType, Long parentId, String title) {}
