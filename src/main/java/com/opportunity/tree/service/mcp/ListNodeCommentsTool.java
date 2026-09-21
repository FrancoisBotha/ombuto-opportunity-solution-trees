package com.opportunity.tree.service.mcp;

import com.opportunity.tree.domain.enumeration.TreeNodeType;
import com.opportunity.tree.repository.McpNodeReadRepository;
import com.opportunity.tree.service.TeamAccessDeniedException;
import com.opportunity.tree.service.TeamAccessService;
import com.opportunity.tree.service.mcp.dto.CommentEntry;
import com.opportunity.tree.service.mcp.dto.CommentsPage;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-only MCP tool that returns the comments on one node with body, author and times.
 *
 * <p>Visibility policy (MCPSRV-011): comment bodies are only returned to callers who can read
 * the owning team. This is a deliberate <em>stricter</em> policy than
 * {@link McpInterviewNotesPolicy}: for interviews we can hand a non-member the title and linked
 * opportunities without the notes, so a title-only reader still gets a useful record. A comment
 * without its body is just a count, which {@code get_node} already returns as
 * {@code commentCount} — there is no useful "metadata-only comment". So a caller who cannot read
 * the team is refused with the same {@link TeamAccessDeniedException} shape as {@code get_node},
 * with no distinction between "no such node" and "cross-team node" (NFR-002).
 *
 * <p>Enforcement goes through {@link TeamAccessService#requireReadNode} exactly as every other
 * scoped tool does.
 */
@Service
@Transactional(readOnly = true)
public class ListNodeCommentsTool {

    /** Hard cap on the number of comments returned per call, regardless of {@code size}. */
    public static final int MAX_PAGE_SIZE = 50;

    /** Default page size when the caller does not pass one. */
    public static final int DEFAULT_PAGE_SIZE = 20;

    private static final Set<TreeNodeType> DISCUSSABLE_TYPES = EnumSet.of(
        TreeNodeType.OUTCOME,
        TreeNodeType.OPPORTUNITY,
        TreeNodeType.SOLUTION,
        TreeNodeType.ASSUMPTION,
        TreeNodeType.EVIDENCE
    );

    private final TeamAccessService teamAccessService;
    private final McpNodeReadRepository readRepository;

    public ListNodeCommentsTool(TeamAccessService teamAccessService, McpNodeReadRepository readRepository) {
        this.teamAccessService = teamAccessService;
        this.readRepository = readRepository;
    }

    @Tool(
        name = "list_node_comments",
        description = "Return the discussion thread on one tree node — body, author and " +
            "created / edited times — oldest first. Use this after get_node reports a non-zero " +
            "commentCount to read what the team actually said. Supported node types: OUTCOME, " +
            "OPPORTUNITY, SOLUTION, ASSUMPTION, EVIDENCE (PRODUCT has no comments). Results are " +
            "returned as a bounded page (default 20, capped at " +
            MAX_PAGE_SIZE +
            " per call — use the 'page' parameter for further pages) with the same shape as " +
            "list_interviews. 'totalMatching' is the total message count. Refuses with an " +
            "access-denied error if the node belongs to a team the caller is not a member of, " +
            "with the same error whether the id exists or not."
    )
    public CommentsPage listNodeComments(
        @ToolParam(
            description = "Node type. One of OUTCOME, OPPORTUNITY, SOLUTION, ASSUMPTION, EVIDENCE. " +
                "PRODUCT is not supported (products have no discussion)."
        ) String type,
        @ToolParam(description = "Numeric id of the node.") Long id,
        @ToolParam(required = false, description = "Zero-based page index, default 0.") Integer page,
        @ToolParam(
            required = false,
            description = "Page size, default " + DEFAULT_PAGE_SIZE + ", capped at " + MAX_PAGE_SIZE + "."
        ) Integer size
    ) {
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("type is required");
        }
        if (id == null) {
            throw new IllegalArgumentException("id is required");
        }
        TreeNodeType nodeType;
        try {
            nodeType = TreeNodeType.valueOf(type.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                "Unknown node type '" + type + "'. Expected one of OUTCOME, OPPORTUNITY, SOLUTION, ASSUMPTION, EVIDENCE."
            );
        }
        if (!DISCUSSABLE_TYPES.contains(nodeType)) {
            throw new IllegalArgumentException(
                "Node type " + nodeType + " has no discussion. Supported types: OUTCOME, OPPORTUNITY, SOLUTION, ASSUMPTION, EVIDENCE."
            );
        }

        // Access check first — same TeamAccessDeniedException whether the record exists or
        // belongs to another team, so a caller cannot use the error to probe existence.
        teamAccessService.requireReadNode(nodeType, id);

        int effectivePage = page == null || page < 0 ? 0 : page;
        int requestedSize = size == null || size <= 0 ? DEFAULT_PAGE_SIZE : size;
        int effectiveSize = Math.min(requestedSize, MAX_PAGE_SIZE);

        List<Object[]> allRows = commentsFor(nodeType, id);
        long total = allRows.size();
        int from = Math.min(effectivePage * effectiveSize, allRows.size());
        int to = Math.min(from + effectiveSize, allRows.size());
        List<CommentEntry> entries = allRows.subList(from, to).stream().map(ListNodeCommentsTool::toEntry).toList();

        return new CommentsPage(nodeType.name(), id, effectivePage, effectiveSize, MAX_PAGE_SIZE, total, entries);
    }

    private List<Object[]> commentsFor(TreeNodeType type, Long id) {
        return switch (type) {
            case OUTCOME -> readRepository.findCommentsForOutcome(id);
            case OPPORTUNITY -> readRepository.findCommentsForOpportunity(id);
            case SOLUTION -> readRepository.findCommentsForSolution(id);
            case ASSUMPTION -> readRepository.findCommentsForAssumption(id);
            case EVIDENCE -> readRepository.findCommentsForEvidence(id);
            default -> throw new IllegalStateException("Unsupported: " + type);
        };
    }

    private static CommentEntry toEntry(Object[] row) {
        Long id = ((Number) row[0]).longValue();
        String body = (String) row[1];
        String author = (String) row[2];
        Instant createdDate = (Instant) row[3];
        Instant editedDate = (Instant) row[4];
        return new CommentEntry(id, body, author, createdDate, editedDate);
    }
}
