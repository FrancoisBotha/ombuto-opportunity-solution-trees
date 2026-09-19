package com.opportunity.tree.service.broadcast;

import java.util.List;

/**
 * Payload of a {@link TreeChangeType#NODE_DELETED} event: the key of the deleted node and
 * every cascaded descendant key, so clients can drop the whole subtree in one pass.
 * {@code cascadedKeys} does not repeat {@code key}.
 */
public record NodeDeletedPayload(String key, List<String> cascadedKeys) {}
