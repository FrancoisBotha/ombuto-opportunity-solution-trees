package com.opportunity.tree.service.mcp;

import com.opportunity.tree.domain.enumeration.TreeNodeType;
import com.opportunity.tree.repository.McpNodeReadRepository;
import com.opportunity.tree.service.TeamAccessDeniedException;
import com.opportunity.tree.service.TeamAccessService;
import com.opportunity.tree.service.mcp.dto.NodeDetails;
import com.opportunity.tree.service.mcp.dto.NodeRef;
import java.util.List;
import java.util.Objects;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-only MCP tool that returns one node by type + id.
 *
 * <p>Use this instead of {@code get_tree} when the agent already knows a specific node and
 * needs its details, parent and immediate children — {@code get_tree} returns the whole tree,
 * which is far larger. Access is enforced through {@link TeamAccessService#requireReadNode},
 * so cross-team ids return the same {@code TeamAccessDeniedException} whether the record
 * exists or not.
 */
@Service
@Transactional(readOnly = true)
public class GetNodeTool {

    private final TeamAccessService teamAccessService;
    private final McpNodeReadRepository readRepository;

    public GetNodeTool(TeamAccessService teamAccessService, McpNodeReadRepository readRepository) {
        this.teamAccessService = teamAccessService;
        this.readRepository = readRepository;
    }

    @Tool(
        name = "get_node",
        description = "Return one tree node by type and id. Prefer this over get_tree when you " +
            "already know the node — it returns only that node's title, description, status, its " +
            "parent, a summary of its direct children (type, id, title) and counts of related " +
            "features (links, evidence, comments). The 'parent' field is absent for a PRODUCT " +
            "(top of a team's tree); 'status' is absent for PRODUCT, OUTCOME and EVIDENCE " +
            "which have no status field; 'evidenceCount' is only populated for OPPORTUNITY and " +
            "ASSUMPTION (the two node types evidence can attach to); 'commentCount' is only " +
            "populated for OUTCOME, OPPORTUNITY, SOLUTION, ASSUMPTION and EVIDENCE (a product " +
            "has no comments). Missing sections are returned as null rather than failing the " +
            "call. Refuses with an access-denied error if the node belongs to a team the caller " +
            "is not a member of, with the same error whether the id exists or not."
    )
    public NodeDetails getNode(
        @ToolParam(
            description = "Node type. One of PRODUCT, OUTCOME, OPPORTUNITY, SOLUTION, ASSUMPTION, EVIDENCE. " +
                "Must be the actual type of the referenced id; a type/id mismatch is refused as a validation error."
        ) String type,
        @ToolParam(description = "Numeric id of the node.") Long id
    ) {
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("type is required");
        }
        if (id == null) {
            throw new IllegalArgumentException("id is required");
        }
        TreeNodeType nodeType;
        try {
            nodeType = TreeNodeType.valueOf(type.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                "Unknown node type '" + type + "'. Expected one of PRODUCT, OUTCOME, OPPORTUNITY, SOLUTION, ASSUMPTION, EVIDENCE."
            );
        }

        // Access check first: same TeamAccessDeniedException whether the record exists or belongs
        // to another team, so an agent cannot use the error to probe existence.
        try {
            teamAccessService.requireReadNode(nodeType, id);
        } catch (TeamAccessDeniedException accessDenied) {
            // A caller who owns the id but under a different type gets a clear validation
            // error instead of the generic access-denied. Cross-team ids stay access-denied.
            TreeNodeType actualType = actualTypeIfInCallersTeams(id).orElse(null);
            if (actualType != null && actualType != nodeType) {
                throw new IllegalArgumentException(
                    "Node id " + id + " is a " + actualType + ", not a " + nodeType + ". Call get_node with the correct type."
                );
            }
            throw accessDenied;
        }

        List<Object[]> basicsRows = basicsFor(nodeType, id);
        if (basicsRows.isEmpty()) {
            // Access check passed (the id resolved to a team the caller is in) but the record
            // has disappeared between the two queries. Treat as access-denied to keep the error
            // shape consistent with the cross-team case.
            throw new TeamAccessDeniedException();
        }
        Object[] basics = basicsRows.get(0);
        Long resolvedId = ((Number) basics[0]).longValue();
        String title = (String) basics[1];
        String description = (String) basics[2];
        String status = (String) basics[3];

        NodeRef parent = parentFor(nodeType, id);
        List<NodeRef> children = childrenFor(nodeType, id);

        Long linkCount = linkCountFor(nodeType, id);
        Long evidenceCount = evidenceCountFor(nodeType, id);
        Long commentCount = commentCountFor(nodeType, id);

        return new NodeDetails(
            nodeType.name(),
            resolvedId,
            title,
            description,
            status,
            parent,
            children,
            linkCount,
            evidenceCount,
            commentCount
        );
    }

    private java.util.Optional<TreeNodeType> actualTypeIfInCallersTeams(Long id) {
        for (TreeNodeType candidate : TreeNodeType.values()) {
            Long teamId = teamAccessService.teamIdForNode(candidate, id).orElse(null);
            if (teamId != null && teamAccessService.canReadTeam(teamId)) {
                return java.util.Optional.of(candidate);
            }
        }
        return java.util.Optional.empty();
    }

    private List<Object[]> basicsFor(TreeNodeType type, Long id) {
        return switch (type) {
            case PRODUCT -> readRepository.findProductBasics(id);
            case OUTCOME -> readRepository.findOutcomeBasics(id);
            case OPPORTUNITY -> readRepository.findOpportunityBasics(id);
            case SOLUTION -> readRepository.findSolutionBasics(id);
            case ASSUMPTION -> readRepository.findAssumptionBasics(id);
            case EVIDENCE -> readRepository.findEvidenceBasics(id);
        };
    }

    private NodeRef parentFor(TreeNodeType type, Long id) {
        List<Object[]> rows = switch (type) {
            case PRODUCT -> List.of();
            case OUTCOME -> readRepository.findOutcomeParent(id);
            case OPPORTUNITY -> readRepository.findOpportunityParent(id);
            case SOLUTION -> readRepository.findSolutionParent(id);
            case ASSUMPTION -> readRepository.findAssumptionParent(id);
            case EVIDENCE -> readRepository.findEvidenceParent(id);
        };
        if (rows.isEmpty()) {
            return null;
        }
        Object[] row = rows.get(0);
        if (row[0] == null || row[1] == null) {
            return null;
        }
        return new NodeRef((String) row[0], ((Number) row[1]).longValue(), (String) row[2]);
    }

    private List<NodeRef> childrenFor(TreeNodeType type, Long id) {
        return switch (type) {
            case PRODUCT -> toRefs(readRepository.findProductChildren(id));
            case OUTCOME -> toRefs(readRepository.findOutcomeChildren(id));
            case OPPORTUNITY -> {
                List<NodeRef> merged = new java.util.ArrayList<>();
                merged.addAll(toRefs(readRepository.findOpportunityChildOpportunities(id)));
                merged.addAll(toRefs(readRepository.findOpportunityChildSolutions(id)));
                merged.addAll(toRefs(readRepository.findOpportunityChildEvidence(id)));
                yield List.copyOf(merged);
            }
            case SOLUTION -> toRefs(readRepository.findSolutionChildren(id));
            case ASSUMPTION -> toRefs(readRepository.findAssumptionChildren(id));
            case EVIDENCE -> List.of();
        };
    }

    private static List<NodeRef> toRefs(List<Object[]> rows) {
        return rows
            .stream()
            .filter(Objects::nonNull)
            .map(r -> new NodeRef((String) r[0], ((Number) r[1]).longValue(), (String) r[2]))
            .toList();
    }

    private Long linkCountFor(TreeNodeType type, Long id) {
        return switch (type) {
            case PRODUCT -> readRepository.countLinksForProduct(id);
            case OUTCOME -> readRepository.countLinksForOutcome(id);
            case OPPORTUNITY -> readRepository.countLinksForOpportunity(id);
            case SOLUTION -> readRepository.countLinksForSolution(id);
            case ASSUMPTION -> readRepository.countLinksForAssumption(id);
            case EVIDENCE -> readRepository.countLinksForEvidence(id);
        };
    }

    private Long evidenceCountFor(TreeNodeType type, Long id) {
        return switch (type) {
            case OPPORTUNITY -> readRepository.countEvidenceForOpportunity(id);
            case ASSUMPTION -> readRepository.countEvidenceForAssumption(id);
            default -> null;
        };
    }

    private Long commentCountFor(TreeNodeType type, Long id) {
        return switch (type) {
            case PRODUCT -> null;
            case OUTCOME -> readRepository.countCommentsForOutcome(id);
            case OPPORTUNITY -> readRepository.countCommentsForOpportunity(id);
            case SOLUTION -> readRepository.countCommentsForSolution(id);
            case ASSUMPTION -> readRepository.countCommentsForAssumption(id);
            case EVIDENCE -> readRepository.countCommentsForEvidence(id);
        };
    }
}
