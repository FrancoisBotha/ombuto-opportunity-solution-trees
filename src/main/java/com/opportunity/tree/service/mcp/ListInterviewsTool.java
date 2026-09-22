package com.opportunity.tree.service.mcp;

import com.opportunity.tree.domain.Interview;
import com.opportunity.tree.domain.Opportunity;
import com.opportunity.tree.domain.enumeration.TreeNodeType;
import com.opportunity.tree.repository.McpNodeReadRepository;
import com.opportunity.tree.service.TeamAccessService;
import com.opportunity.tree.service.mcp.dto.InterviewSummary;
import com.opportunity.tree.service.mcp.dto.InterviewsPage;
import com.opportunity.tree.service.mcp.dto.OpportunityRef;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-only MCP tool that lists interviews for a product or team.
 *
 * <p>Access is enforced through {@link TeamAccessService} before any data is returned; a
 * cross-team id raises {@code TeamAccessDeniedException} with the same shape whether the
 * record exists or not. The notes-visibility rule from Epic 7 (NFR-014) is applied via
 * {@link McpInterviewNotesPolicy}: callers who cannot read the team's interview notes still
 * receive titles, dates, participants, interviewers and linked opportunities, but with
 * {@code notes} set to null.
 */
@Service
@Transactional(readOnly = true)
public class ListInterviewsTool {

    /** Hard cap on the number of interviews returned per call, regardless of {@code size}. */
    public static final int MAX_PAGE_SIZE = 50;

    /** Default page size when the caller does not pass one. */
    public static final int DEFAULT_PAGE_SIZE = 20;

    private final TeamAccessService teamAccessService;
    private final McpInterviewNotesPolicy notesPolicy;
    private final McpNodeReadRepository readRepository;

    public ListInterviewsTool(
        TeamAccessService teamAccessService,
        McpInterviewNotesPolicy notesPolicy,
        McpNodeReadRepository readRepository
    ) {
        this.teamAccessService = teamAccessService;
        this.notesPolicy = notesPolicy;
        this.readRepository = readRepository;
    }

    @Tool(
        name = "list_interviews",
        description = "List a product's or team's interviews with their date, title, " +
            "participant / interviewee label, interviewer and the opportunities each is linked " +
            "to (id, title, status). Pass a productId when you know which product you are " +
            "asking about — the result is narrower and easier to reason about; only fall back to " +
            "teamId when you specifically want every product of the team; pass opportunityId for " +
            "interviews directly linked to that opportunity (including nested opportunities). Exactly " +
            "one of productId, teamId and opportunityId must be given. Results are ordered by interview date " +
            "descending, then id descending, and returned as a bounded page (default 20, " +
            "capped at " +
            MAX_PAGE_SIZE +
            " per call — use the 'page' parameter for further " +
            "pages). Interview notes are returned only to team members (Epic 7 privacy rule); " +
            "when notes are withheld the response still includes every title and every linked " +
            "opportunity, and 'notesIncluded' is false. Cross-team ids are refused with the " +
            "same error whether the record exists or not."
    )
    public InterviewsPage listInterviews(
        @ToolParam(
            required = false,
            description = "Id of the product whose interviews to list. Prefer this parameter when the agent already " +
                "knows the product; leave null only when the agent wants every interview of the team."
        ) Long productId,
        @ToolParam(
            required = false,
            description = "Id of the team whose interviews to list. Use only when productId is not known; a " +
                "product-scoped call is narrower and cheaper."
        ) Long teamId,
        @ToolParam(required = false, description = "Zero-based page index, default 0.") Integer page,
        @ToolParam(
            required = false,
            description = "Page size, default " + DEFAULT_PAGE_SIZE + ", capped at " + MAX_PAGE_SIZE + "."
        ) Integer size,
        @ToolParam(
            required = false,
            description = "Optional opportunity id. When set, return only interviews linked to this opportunity; do not also pass productId or teamId. Nested opportunities are supported."
        ) Long opportunityId
    ) {
        int scopes = (productId == null ? 0 : 1) + (teamId == null ? 0 : 1) + (opportunityId == null ? 0 : 1);
        if (scopes != 1) {
            throw new IllegalArgumentException("Provide exactly one of productId, teamId or opportunityId.");
        }

        int effectivePage = page == null || page < 0 ? 0 : page;
        int requestedSize = size == null || size <= 0 ? DEFAULT_PAGE_SIZE : size;
        int effectiveSize = Math.min(requestedSize, MAX_PAGE_SIZE);

        Long resolvedTeamId;
        Long resolvedProductId;
        Page<Interview> interviews;
        PageRequest pageable = PageRequest.of(effectivePage, effectiveSize);

        if (opportunityId != null) {
            resolvedTeamId = teamAccessService.requireReadNode(TreeNodeType.OPPORTUNITY, opportunityId);
            resolvedProductId = null;
            interviews = readRepository.findInterviewsByOpportunityId(opportunityId, resolvedTeamId, pageable);
        } else if (productId != null) {
            resolvedTeamId = teamAccessService.requireReadNode(TreeNodeType.PRODUCT, productId);
            resolvedProductId = productId;
            interviews = readRepository.findInterviewsByProductId(productId, pageable);
        } else {
            teamAccessService.requireReadTeam(teamId);
            resolvedTeamId = teamId;
            resolvedProductId = null;
            interviews = readRepository.findInterviewsByTeamId(teamId, pageable);
        }

        boolean notesIncluded = notesPolicy.canReadNotes(resolvedTeamId);

        long total = interviews.getTotalElements();
        List<Interview> pageContent = interviews.getContent();

        List<InterviewSummary> summaries = pageContent
            .stream()
            .map(i -> toSummary(i, notesIncluded))
            .toList();

        return new InterviewsPage(
            resolvedTeamId,
            resolvedProductId,
            effectivePage,
            effectiveSize,
            MAX_PAGE_SIZE,
            total,
            notesIncluded,
            summaries,
            opportunityId
        );
    }

    /** Java-call compatibility for existing four-argument callers; the MCP schema uses five arguments. */
    public InterviewsPage listInterviews(Long productId, Long teamId, Integer page, Integer size) {
        return listInterviews(productId, teamId, page, size, null);
    }

    private InterviewSummary toSummary(Interview interview, boolean notesIncluded) {
        // The lazy many-to-many is fetched here inside the read-only transaction. For small
        // per-page sizes (default 20, capped at 50) the N+1 cost is acceptable and keeps the
        // pipeline of the ordered projection query simple.
        List<OpportunityRef> opps = interview
            .getOpportunities()
            .stream()
            .filter(op -> interview.getProduct().getTeam().getId().equals(op.getOutcome().getProduct().getTeam().getId()))
            .map(this::toOpportunityRef)
            .sorted(java.util.Comparator.comparing(OpportunityRef::id))
            .collect(Collectors.toList());

        String interviewerLabel = interview.getInterviewer() == null ? null : interview.getInterviewer().getLogin();
        String notes = notesIncluded ? interview.getNotes() : null;

        return new InterviewSummary(
            interview.getId(),
            interview.getInterviewDate(),
            interview.getTitle(),
            interview.getParticipant(),
            interviewerLabel,
            notes,
            opps,
            interview.getProduct().getId(),
            interview.getCreatedDate()
        );
    }

    private OpportunityRef toOpportunityRef(Opportunity op) {
        String status = op.getStatus() == null ? null : op.getStatus().name();
        return new OpportunityRef(op.getId(), op.getTitle(), status);
    }
}
