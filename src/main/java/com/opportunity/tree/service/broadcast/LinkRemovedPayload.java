package com.opportunity.tree.service.broadcast;

/**
 * Payload of a {@link TreeChangeType#LINK_REMOVED} event: the owning tree node's client key
 * ({@code "opportunity-12"}) and the id of the removed link.
 */
public record LinkRemovedPayload(String nodeKey, Long linkId) {}
