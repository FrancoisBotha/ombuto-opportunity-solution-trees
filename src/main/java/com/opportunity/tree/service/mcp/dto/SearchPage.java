package com.opportunity.tree.service.mcp.dto;

import java.util.List;

/**
 * Bounded page of search hits returned by the {@code search_nodes} MCP tool. {@code total} is
 * the number of matches across the caller's readable teams (respecting {@code teamId} /
 * {@code productId} scope); {@code hits} is the {@code offset}..{@code offset + limit} slice.
 * {@code hasMore} is {@code true} when further pages remain.
 */
public record SearchPage(String query, long total, int offset, int limit, boolean hasMore, List<SearchHit> hits) {}
