package com.opportunity.tree.service.dto.tree;

import java.io.Serializable;
import java.time.Instant;

/**
 * A chat message on a tree node. Ownership is not carried here: it is derived per viewer from
 * {@code authorLogin} against the current user's login. Broadcasting a per-viewer flag on a team
 * topic would attribute someone else's message to every recipient (CHAT-001).
 */
public record TreeCommentDTO(
    Long id,
    String body,
    String authorLogin,
    String authorInitials,
    String authorName,
    Instant createdDate,
    Instant editedDate
) implements Serializable {}
