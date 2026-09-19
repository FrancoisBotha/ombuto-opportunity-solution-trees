package com.opportunity.tree.service.dto.tree;

import java.io.Serializable;

/** Request body for adding (both required) or editing (both optional) a link. */
public record TreeLinkWriteDTO(String name, String url) implements Serializable {}
