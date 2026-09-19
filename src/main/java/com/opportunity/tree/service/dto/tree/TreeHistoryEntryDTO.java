package com.opportunity.tree.service.dto.tree;

import com.opportunity.tree.domain.enumeration.HistoryEventType;
import java.io.Serializable;
import java.time.Instant;

/** One entry of a node's changelog. Author fields are null when the author is unknown. */
public record TreeHistoryEntryDTO(
    Long id,
    HistoryEventType eventType,
    String summary,
    String authorLogin,
    String authorInitials,
    Instant createdDate
) implements Serializable {}
