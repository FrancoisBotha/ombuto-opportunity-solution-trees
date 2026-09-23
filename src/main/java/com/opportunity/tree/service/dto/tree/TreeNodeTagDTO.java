package com.opportunity.tree.service.dto.tree;

import java.io.Serializable;

/**
 * LABEL-001: a tag attached to an opportunity or a solution as it appears in the flat tree read
 * and in the panel patch response. Only id and name — the colour field on the Tag entity is
 * deliberately left unset for LABEL-001 (see the epic decision on 2026-09-21).
 */
public record TreeNodeTagDTO(Long id, String name) implements Serializable {}
