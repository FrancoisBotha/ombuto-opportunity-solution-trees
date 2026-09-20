package com.opportunity.tree.service.mcp;

import com.opportunity.tree.service.TeamAccessService;
import org.springframework.stereotype.Service;

/**
 * Notes-visibility policy for the {@code list_interviews} MCP tool.
 *
 * <p>Mirrors the web UI's rule (Epic 7 NFR-014): interview notes may contain customer
 * personal data and are only returned to members of the owning team. Non-members — including
 * future title-only readers such as {@code ROLE_OVERVIEW} from Epic 9 — receive interview
 * titles and metadata without notes.
 *
 * <p>Kept in its own bean so it can be re-pointed by later access-control epics without
 * touching {@link TeamAccessService}, and so tests can spy on the single decision.
 */
@Service
public class McpInterviewNotesPolicy {

    private final TeamAccessService teamAccessService;

    public McpInterviewNotesPolicy(TeamAccessService teamAccessService) {
        this.teamAccessService = teamAccessService;
    }

    /** True when the current authenticated user may read interview notes for the given team. */
    public boolean canReadNotes(Long teamId) {
        if (teamId == null) {
            return false;
        }
        return teamAccessService.canReadTeam(teamId);
    }
}
