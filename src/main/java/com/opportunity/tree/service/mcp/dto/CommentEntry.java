package com.opportunity.tree.service.mcp.dto;

import java.time.Instant;

/**
 * One comment in a {@link CommentsPage}. Body, author login and created / edited times as they
 * appear in the web UI's discussion panel.
 */
public record CommentEntry(Long id, String body, String author, Instant createdDate, Instant editedDate) {}
