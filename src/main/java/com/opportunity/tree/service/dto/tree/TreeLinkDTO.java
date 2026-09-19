package com.opportunity.tree.service.dto.tree;

import java.io.Serializable;

/** A link as returned by the link write endpoints. */
public record TreeLinkDTO(Long id, String name, String url, Integer sortOrder) implements Serializable {}
