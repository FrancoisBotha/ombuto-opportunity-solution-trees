package com.opportunity.tree.service.mcp;

import com.opportunity.tree.repository.McpNodeReadRepository;
import com.opportunity.tree.service.TeamAccessDeniedException;
import com.opportunity.tree.service.TeamAccessService;
import com.opportunity.tree.service.mcp.dto.SearchHit;
import com.opportunity.tree.service.mcp.dto.SearchPage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-only MCP tool that finds nodes matching a term across the caller's readable teams
 * without walking the whole tree.
 *
 * <p>Scoping is enforced through {@link TeamAccessService}: the search is restricted to teams
 * the caller belongs to (an unscoped call), or the single {@code teamId} the caller passed
 * after that team's membership is verified. A cross-team {@code teamId} is refused with
 * {@link TeamAccessDeniedException} — the same shape as an unknown id — so ids cannot be
 * probed for existence, and a hit from a team the caller cannot read never appears in the
 * response (the query is bound to a whitelist of team ids at the JPQL layer, not filtered
 * after the fact).
 *
 * <p>Results are paginated ({@code limit} up to {@link #MAX_LIMIT}, default
 * {@link #DEFAULT_LIMIT}); the response carries {@code total} and {@code hasMore} so an agent
 * can page or refine the term.
 */
@Service
@Transactional(readOnly = true)
public class SearchTool {

    /** Default page size when the caller does not pass one. */
    public static final int DEFAULT_LIMIT = 20;

    /** Hard cap on the page size, regardless of what the caller passes. */
    public static final int MAX_LIMIT = 50;

    /** Minimum length of the query after trimming. Shorter terms match too broadly. */
    static final int MIN_QUERY_LENGTH = 2;

    private final TeamAccessService teamAccessService;
    private final McpNodeReadRepository readRepository;

    public SearchTool(TeamAccessService teamAccessService, McpNodeReadRepository readRepository) {
        this.teamAccessService = teamAccessService;
        this.readRepository = readRepository;
    }

    @Tool(
        name = "search_nodes",
        description = "Find tree nodes whose title or description matches a term, without walking the tree. " +
            "Case-insensitive substring match. Search is restricted to the caller's readable teams — a hit " +
            "from a team the caller cannot read never appears in the response. Results are paginated " +
            "(default " +
            DEFAULT_LIMIT +
            " per page, capped at " +
            MAX_LIMIT +
            "); the response includes total, offset, limit and hasMore so an agent can page or refine. " +
            "Pass teamId to narrow the search to one team (refused if the caller is not a member — same " +
            "shape as an unknown id). Each hit carries type, id, title, description, status (where " +
            "applicable), teamId and teamName so the agent can answer or follow up with get_node / " +
            "get_tree. Read-only; performs no writes."
    )
    public SearchPage searchNodes(
        @ToolParam(
            description = "Case-insensitive substring to match against node title/statement and description. Minimum 2 characters after trim."
        ) String query,
        @ToolParam(
            required = false,
            description = "Optional team id. When set, the search is restricted to that team. Refused if the caller is not a member."
        ) Long teamId,
        @ToolParam(required = false, description = "Zero-based offset into the result set, default 0.") Integer offset,
        @ToolParam(
            required = false,
            description = "Maximum number of hits to return, default " + DEFAULT_LIMIT + ", capped at " + MAX_LIMIT + "."
        ) Integer limit
    ) {
        String rawQuery = query == null ? "" : query.trim();
        if (rawQuery.length() < MIN_QUERY_LENGTH) {
            throw new IllegalArgumentException("query is required and must be at least " + MIN_QUERY_LENGTH + " characters after trim.");
        }

        int effectiveOffset = offset == null || offset < 0 ? 0 : offset;
        int requestedLimit = limit == null || limit <= 0 ? DEFAULT_LIMIT : limit;
        int effectiveLimit = Math.min(requestedLimit, MAX_LIMIT);

        Set<Long> readable = teamAccessService.getCurrentUserTeamIds();
        Set<Long> scopeTeams;
        if (teamId != null) {
            if (!readable.contains(teamId)) {
                // Cross-team probe or unknown id — same error shape.
                throw new TeamAccessDeniedException();
            }
            scopeTeams = Set.of(teamId);
        } else {
            scopeTeams = readable;
        }

        if (scopeTeams.isEmpty()) {
            return new SearchPage(rawQuery, 0L, effectiveOffset, effectiveLimit, false, List.of());
        }

        String needle = "%" + rawQuery.toLowerCase(Locale.ROOT) + "%";
        List<SearchHit> merged = new ArrayList<>();
        merged.addAll(mapRows(readRepository.searchProducts(scopeTeams, needle)));
        merged.addAll(mapRows(readRepository.searchOutcomes(scopeTeams, needle)));
        merged.addAll(mapRows(readRepository.searchOpportunities(scopeTeams, needle)));
        merged.addAll(mapRows(readRepository.searchSolutions(scopeTeams, needle)));
        merged.addAll(mapRows(readRepository.searchAssumptions(scopeTeams, needle)));
        merged.addAll(mapRows(readRepository.searchEvidence(scopeTeams, needle)));

        // Stable order: title (case-insensitive), then type, then id.
        merged.sort(
            Comparator.<SearchHit, String>comparing(h -> h.title() == null ? "" : h.title(), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(SearchHit::type)
                .thenComparing(SearchHit::id)
        );

        long total = merged.size();
        int from = Math.min(effectiveOffset, merged.size());
        int to = Math.min(from + effectiveLimit, merged.size());
        List<SearchHit> page = List.copyOf(merged.subList(from, to));
        boolean hasMore = to < merged.size();
        return new SearchPage(rawQuery, total, effectiveOffset, effectiveLimit, hasMore, page);
    }

    private static List<SearchHit> mapRows(List<Object[]> rows) {
        List<SearchHit> out = new ArrayList<>(rows.size());
        for (Object[] r : rows) {
            String type = (String) r[0];
            Long id = ((Number) r[1]).longValue();
            String title = (String) r[2];
            String description = (String) r[3];
            String status = (String) r[4];
            Long teamId = r[5] == null ? null : ((Number) r[5]).longValue();
            String teamName = (String) r[6];
            out.add(new SearchHit(type, id, title, description, status, teamId, teamName));
        }
        return out;
    }
}
