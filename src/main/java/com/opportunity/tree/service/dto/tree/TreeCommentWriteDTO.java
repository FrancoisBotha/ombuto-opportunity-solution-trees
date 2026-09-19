package com.opportunity.tree.service.dto.tree;

import java.io.Serializable;

/** Request body for posting or editing a chat message. */
public record TreeCommentWriteDTO(String body) implements Serializable {}
