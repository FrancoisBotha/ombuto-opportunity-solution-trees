package com.opportunity.tree.service.mcp.dto;

/**
 * Reference to an opportunity linked to an interview: id, title and status. The status is
 * included so an agent can answer in the user's vocabulary without a second call.
 */
public record OpportunityRef(Long id, String title, String status) {}
