package com.opportunity.tree.service.mcp;

import com.opportunity.tree.domain.MeetingTranscript;
import com.opportunity.tree.domain.User;
import com.opportunity.tree.domain.enumeration.TreeNodeType;
import com.opportunity.tree.repository.MeetingTranscriptRepository;
import com.opportunity.tree.service.TeamAccessService;
import com.opportunity.tree.service.mcp.dto.TranscriptEntry;
import com.opportunity.tree.service.mcp.dto.TranscriptsPage;
import java.util.List;
import java.util.Locale;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-only MCP tool that lists meeting transcript metadata for a team or a single node.
 *
 * <p>Never returns the transcript body — that is only ever handed out by
 * {@link GetTranscriptTool}. Access is enforced through {@link TeamAccessService}: a cross-team
 * or non-existent id is refused with the same {@code TeamAccessDeniedException} shape so the
 * caller cannot use the error to probe existence (NFR-002, Epic 12 §5 NFR-022).
 */
@Service
@Transactional(readOnly = true)
public class ListTranscriptsTool {

    /** Hard cap on the number of transcripts returned per call, regardless of {@code size}. */
    public static final int MAX_PAGE_SIZE = 50;

    /** Default page size when the caller does not pass one. */
    public static final int DEFAULT_PAGE_SIZE = 20;

    private final TeamAccessService teamAccessService;
    private final MeetingTranscriptRepository transcriptRepository;

    public ListTranscriptsTool(TeamAccessService teamAccessService, MeetingTranscriptRepository transcriptRepository) {
        this.teamAccessService = teamAccessService;
        this.transcriptRepository = transcriptRepository;
    }

    @Tool(
        name = "list_transcripts",
        description = "List meeting transcripts attached to nodes in a team, or attached to a " +
            "single node. Metadata only — title, meeting date, attendees, source, author, created " +
            "and edited timestamps, plus the owning node reference. The transcript body is never " +
            "returned here; call get_transcript with the id to read one. Pass either teamId for " +
            "every transcript in a team, or both nodeType and nodeId for one node's transcripts; " +
            "exactly one of the two scopes must be given. Supported node types: PRODUCT, OUTCOME, " +
            "OPPORTUNITY, SOLUTION, ASSUMPTION, EVIDENCE. Results are ordered newest first (created " +
            "date descending, then id descending) and returned as a bounded page (default 20, capped " +
            "at " +
            MAX_PAGE_SIZE +
            " per call — use the 'page' parameter for further pages). Cross-team " +
            "or invalid ids are refused with the same error whether the record exists or not."
    )
    public TranscriptsPage listTranscripts(
        @ToolParam(
            required = false,
            description = "Id of the team whose transcripts to list. Provide when you want every " +
                "transcript on every node in the team. Do not combine with nodeType / nodeId."
        ) Long teamId,
        @ToolParam(
            required = false,
            description = "Node type. One of PRODUCT, OUTCOME, OPPORTUNITY, SOLUTION, ASSUMPTION, " +
                "EVIDENCE. Required together with nodeId when narrowing to a single node."
        ) String nodeType,
        @ToolParam(
            required = false,
            description = "Numeric id of the node. Required together with nodeType when narrowing " + "to a single node."
        ) Long nodeId,
        @ToolParam(required = false, description = "Zero-based page index, default 0.") Integer page,
        @ToolParam(
            required = false,
            description = "Page size, default " + DEFAULT_PAGE_SIZE + ", capped at " + MAX_PAGE_SIZE + "."
        ) Integer size
    ) {
        boolean teamScope = teamId != null;
        boolean nodeScope = nodeType != null || nodeId != null;
        if (teamScope == nodeScope) {
            throw new IllegalArgumentException("Provide either teamId, or both nodeType and nodeId — not both scopes, not neither.");
        }
        if (nodeScope && (nodeType == null || nodeType.isBlank() || nodeId == null)) {
            throw new IllegalArgumentException("nodeType and nodeId must be provided together.");
        }

        int effectivePage = page == null || page < 0 ? 0 : page;
        int requestedSize = size == null || size <= 0 ? DEFAULT_PAGE_SIZE : size;
        int effectiveSize = Math.min(requestedSize, MAX_PAGE_SIZE);
        PageRequest pageable = PageRequest.of(effectivePage, effectiveSize, Sort.by(Sort.Order.desc("createdDate"), Sort.Order.desc("id")));

        Page<MeetingTranscript> found;
        Long resolvedTeamId;
        String resolvedNodeType;
        Long resolvedNodeId;
        if (teamScope) {
            teamAccessService.requireReadTeam(teamId);
            resolvedTeamId = teamId;
            resolvedNodeType = null;
            resolvedNodeId = null;
            found = transcriptRepository.findAllByTeamId(teamId, pageable);
        } else {
            TreeNodeType type = parseNodeType(nodeType);
            resolvedTeamId = teamAccessService.requireReadNode(type, nodeId);
            resolvedNodeType = type.name();
            resolvedNodeId = nodeId;
            found = listByNode(type, nodeId, pageable);
        }

        List<TranscriptEntry> entries = found.getContent().stream().map(ListTranscriptsTool::toEntry).toList();
        return new TranscriptsPage(
            resolvedTeamId,
            resolvedNodeType,
            resolvedNodeId,
            effectivePage,
            effectiveSize,
            MAX_PAGE_SIZE,
            found.getTotalElements(),
            entries
        );
    }

    private Page<MeetingTranscript> listByNode(TreeNodeType type, Long id, PageRequest pageable) {
        return switch (type) {
            case PRODUCT -> transcriptRepository.findAllByProductId(id, pageable);
            case OUTCOME -> transcriptRepository.findAllByOutcomeId(id, pageable);
            case OPPORTUNITY -> transcriptRepository.findAllByOpportunityId(id, pageable);
            case SOLUTION -> transcriptRepository.findAllBySolutionId(id, pageable);
            case ASSUMPTION -> transcriptRepository.findAllByAssumptionId(id, pageable);
            case EVIDENCE -> transcriptRepository.findAllByEvidenceId(id, pageable);
        };
    }

    private static TreeNodeType parseNodeType(String raw) {
        try {
            return TreeNodeType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                "Unknown node type '" + raw + "'. Expected one of PRODUCT, OUTCOME, OPPORTUNITY, SOLUTION, ASSUMPTION, EVIDENCE."
            );
        }
    }

    static TranscriptEntry toEntry(MeetingTranscript t) {
        User author = t.getAuthor();
        NodeRef node = nodeOf(t);
        return new TranscriptEntry(
            t.getId(),
            t.getTitle(),
            t.getMeetingDate(),
            t.getAttendees(),
            t.getSource(),
            node == null ? null : node.type(),
            node == null ? null : node.id(),
            author == null ? null : author.getLogin(),
            t.getCreatedDate(),
            t.getEditedDate()
        );
    }

    static NodeRef nodeOf(MeetingTranscript t) {
        if (t.getProduct() != null) return new NodeRef(TreeNodeType.PRODUCT, t.getProduct().getId());
        if (t.getOutcome() != null) return new NodeRef(TreeNodeType.OUTCOME, t.getOutcome().getId());
        if (t.getOpportunity() != null) return new NodeRef(TreeNodeType.OPPORTUNITY, t.getOpportunity().getId());
        if (t.getSolution() != null) return new NodeRef(TreeNodeType.SOLUTION, t.getSolution().getId());
        if (t.getAssumption() != null) return new NodeRef(TreeNodeType.ASSUMPTION, t.getAssumption().getId());
        if (t.getEvidence() != null) return new NodeRef(TreeNodeType.EVIDENCE, t.getEvidence().getId());
        return null;
    }

    record NodeRef(TreeNodeType type, Long id) {}
}
