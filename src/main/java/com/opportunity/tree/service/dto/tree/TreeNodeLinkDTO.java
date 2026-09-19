package com.opportunity.tree.service.dto.tree;

import java.io.Serializable;

/** A name + URL link on a tree node. */
public record TreeNodeLinkDTO(Long id, String name, String url) implements Serializable {}
