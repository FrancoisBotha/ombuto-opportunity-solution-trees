package com.opportunity.tree.service.dto.tree;

import java.io.Serializable;
import java.time.Instant;

/** A chat message on a tree node; {@code mine} is true when the caller wrote it. */
public record TreeCommentDTO(
    Long id,
    String body,
    String authorLogin,
    String authorInitials,
    String authorName,
    Instant createdDate,
    Instant editedDate,
    boolean mine
) implements Serializable {}
