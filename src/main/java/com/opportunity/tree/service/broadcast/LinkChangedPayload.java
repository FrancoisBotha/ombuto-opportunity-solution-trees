package com.opportunity.tree.service.broadcast;

import com.opportunity.tree.service.dto.tree.TreeLinkDTO;

/**
 * Payload of a {@link TreeChangeType#LINK_ADDED} or {@link TreeChangeType#LINK_UPDATED} event:
 * the owning tree node's client key ({@code "opportunity-12"}) and the full link DTO. Clients
 * apply the link into the node's link list without a tree reload.
 */
public record LinkChangedPayload(String nodeKey, TreeLinkDTO link) {}
