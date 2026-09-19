package com.opportunity.tree.service;

import com.opportunity.tree.domain.Team;
import com.opportunity.tree.domain.TeamMember;
import com.opportunity.tree.domain.enumeration.TeamRole;
import com.opportunity.tree.domain.enumeration.TreeNodeType;
import com.opportunity.tree.repository.TeamMemberRepository;
import com.opportunity.tree.repository.TreeAccessLookupRepository;
import com.opportunity.tree.security.SecurityUtils;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Central access-control service for team-scoped resources.
 *
 * <p>Every service method that touches team-owned data must go through this
 * service (FR-007). The current user is resolved from the Spring Security
 * context — never from a client-supplied parameter — and role semantics are:
 *
 * <ul>
 *   <li>OWNER  — read + edit + owner</li>
 *   <li>EDITOR — read + edit</li>
 *   <li>VIEWER — read only</li>
 *   <li>non-member — nothing</li>
 * </ul>
 *
 * <p>Enforcing variants ({@code requireRead}, {@code requireEdit},
 * {@code requireOwner}) throw {@link TeamAccessDeniedException}, which Spring
 * Security maps to HTTP 403. The same exception is thrown whether the resource
 * exists or not, so callers cannot use the response to infer resource existence
 * (NFR-002).
 */
@Service
@Transactional(readOnly = true)
public class TeamAccessService {

    private final TeamMemberRepository teamMemberRepository;
    private final TreeAccessLookupRepository treeAccessLookupRepository;

    public TeamAccessService(TeamMemberRepository teamMemberRepository, TreeAccessLookupRepository treeAccessLookupRepository) {
        this.teamMemberRepository = teamMemberRepository;
        this.treeAccessLookupRepository = treeAccessLookupRepository;
    }

    // ---------------------------------------------------------------------
    // Membership lookup
    // ---------------------------------------------------------------------

    /**
     * Returns the ids of every team the current user belongs to, regardless of
     * role. Intended for team-filtered list queries. Returns an empty set when
     * no user is authenticated.
     */
    public Set<Long> getCurrentUserTeamIds() {
        return currentUserMemberships()
            .stream()
            .map(TeamMember::getTeam)
            .filter(t -> t != null && t.getId() != null)
            .map(Team::getId)
            .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Returns the current user's role in the given team, or empty when the user
     * is not authenticated or not a member.
     */
    public Optional<TeamRole> getCurrentUserRole(Long teamId) {
        if (teamId == null) {
            return Optional.empty();
        }
        return currentUserMemberships()
            .stream()
            .filter(tm -> tm.getTeam() != null && teamId.equals(tm.getTeam().getId()))
            .map(TeamMember::getRole)
            .findFirst();
    }

    // ---------------------------------------------------------------------
    // Team-level checks
    // ---------------------------------------------------------------------

    public boolean canReadTeam(Long teamId) {
        return getCurrentUserRole(teamId).isPresent();
    }

    public boolean canEditTeam(Long teamId) {
        return getCurrentUserRole(teamId)
            .map(r -> r == TeamRole.OWNER || r == TeamRole.EDITOR)
            .orElse(false);
    }

    public boolean isTeamOwner(Long teamId) {
        return getCurrentUserRole(teamId)
            .map(r -> r == TeamRole.OWNER)
            .orElse(false);
    }

    public void requireReadTeam(Long teamId) {
        if (!canReadTeam(teamId)) {
            throw new TeamAccessDeniedException();
        }
    }

    public void requireEditTeam(Long teamId) {
        if (!canEditTeam(teamId)) {
            throw new TeamAccessDeniedException();
        }
    }

    public void requireOwnerTeam(Long teamId) {
        if (!isTeamOwner(teamId)) {
            throw new TeamAccessDeniedException();
        }
    }

    // ---------------------------------------------------------------------
    // Generic node checks (all six tree node types) — OST tree builder.
    //
    // Read = any member; edit (including chat, links, questions) = OWNER or
    // EDITOR. A missing id and a non-member get the same TeamAccessDeniedException
    // (no existence leak). ROLE_ADMIN grants no implicit access.
    // ---------------------------------------------------------------------

    /** Resolves a node of any type to its team id; empty when the node does not exist. */
    public Optional<Long> teamIdForNode(TreeNodeType type, Long id) {
        if (type == null || id == null) {
            return Optional.empty();
        }
        return switch (type) {
            case PRODUCT -> treeAccessLookupRepository.findTeamIdOfProduct(id);
            case OUTCOME -> treeAccessLookupRepository.findTeamIdOfOutcome(id);
            case OPPORTUNITY -> treeAccessLookupRepository.findTeamIdOfOpportunity(id);
            case SOLUTION -> treeAccessLookupRepository.findTeamIdOfSolution(id);
            case ASSUMPTION -> treeAccessLookupRepository.findTeamIdOfAssumption(id);
            case EVIDENCE -> treeAccessLookupRepository.findTeamIdOfEvidence(id);
        };
    }

    public boolean canReadNode(TreeNodeType type, Long id) {
        return teamIdForNode(type, id).map(this::canReadTeam).orElse(false);
    }

    public boolean canEditNode(TreeNodeType type, Long id) {
        return teamIdForNode(type, id).map(this::canEditTeam).orElse(false);
    }

    /** Requires read access (any member) to the node; returns the node's team id. */
    public Long requireReadNode(TreeNodeType type, Long id) {
        Long teamId = teamIdForNode(type, id).orElseThrow(TeamAccessDeniedException::new);
        requireReadTeam(teamId);
        return teamId;
    }

    /** Requires edit access (OWNER or EDITOR) to the node; returns the node's team id. */
    public Long requireEditNode(TreeNodeType type, Long id) {
        Long teamId = teamIdForNode(type, id).orElseThrow(TeamAccessDeniedException::new);
        requireEditTeam(teamId);
        return teamId;
    }

    /** The node a {@code NodeLink} hangs off; empty when the link does not exist. */
    public Optional<TreeNodeRef> nodeOfLink(Long linkId) {
        if (linkId == null) {
            return Optional.empty();
        }
        return firstNonNull(
            treeAccessLookupRepository.findNodeIdsOfLink(linkId),
            TreeNodeType.PRODUCT,
            TreeNodeType.OUTCOME,
            TreeNodeType.OPPORTUNITY,
            TreeNodeType.SOLUTION,
            TreeNodeType.ASSUMPTION,
            TreeNodeType.EVIDENCE
        );
    }

    /** The opportunity an {@code OpenQuestion} belongs to; empty when the question does not exist. */
    public Optional<TreeNodeRef> nodeOfQuestion(Long questionId) {
        if (questionId == null) {
            return Optional.empty();
        }
        return treeAccessLookupRepository.findOpportunityIdOfQuestion(questionId).map(id -> new TreeNodeRef(TreeNodeType.OPPORTUNITY, id));
    }

    /** The node a {@code Comment} is attached to; empty when the comment does not exist. */
    public Optional<TreeNodeRef> nodeOfComment(Long commentId) {
        if (commentId == null) {
            return Optional.empty();
        }
        return firstNonNull(
            treeAccessLookupRepository.findNodeIdsOfComment(commentId),
            TreeNodeType.OUTCOME,
            TreeNodeType.OPPORTUNITY,
            TreeNodeType.SOLUTION,
            TreeNodeType.ASSUMPTION,
            TreeNodeType.EVIDENCE
        );
    }

    /** Requires read access to the link's node; returns that node. */
    public TreeNodeRef requireReadLink(Long linkId) {
        return requireReadOn(nodeOfLink(linkId));
    }

    /** Requires edit access to the link's node; returns that node. */
    public TreeNodeRef requireEditLink(Long linkId) {
        return requireEditOn(nodeOfLink(linkId));
    }

    /** Requires read access to the question's opportunity; returns that node. */
    public TreeNodeRef requireReadQuestion(Long questionId) {
        return requireReadOn(nodeOfQuestion(questionId));
    }

    /** Requires edit access to the question's opportunity; returns that node. */
    public TreeNodeRef requireEditQuestion(Long questionId) {
        return requireEditOn(nodeOfQuestion(questionId));
    }

    /** Requires read access to the comment's node; returns that node. */
    public TreeNodeRef requireReadComment(Long commentId) {
        return requireReadOn(nodeOfComment(commentId));
    }

    /**
     * Requires edit access (OWNER or EDITOR) to the comment's node; returns that
     * node. Callers must additionally check that the current user authored the
     * comment before editing or deleting it.
     */
    public TreeNodeRef requireEditComment(Long commentId) {
        return requireEditOn(nodeOfComment(commentId));
    }

    private TreeNodeRef requireReadOn(Optional<TreeNodeRef> node) {
        TreeNodeRef ref = node.orElseThrow(TeamAccessDeniedException::new);
        requireReadNode(ref.type(), ref.id());
        return ref;
    }

    private TreeNodeRef requireEditOn(Optional<TreeNodeRef> node) {
        TreeNodeRef ref = node.orElseThrow(TeamAccessDeniedException::new);
        requireEditNode(ref.type(), ref.id());
        return ref;
    }

    private static Optional<TreeNodeRef> firstNonNull(List<Object[]> rows, TreeNodeType... columnTypes) {
        if (rows == null || rows.isEmpty()) {
            return Optional.empty();
        }
        Object[] row = rows.get(0);
        for (int i = 0; i < columnTypes.length && i < row.length; i++) {
            if (row[i] instanceof Number n) {
                return Optional.of(new TreeNodeRef(columnTypes[i], n.longValue()));
            }
        }
        return Optional.empty();
    }

    // ---------------------------------------------------------------------
    // Internal helpers
    // ---------------------------------------------------------------------

    private List<TeamMember> currentUserMemberships() {
        return SecurityUtils.getCurrentUserLogin().map(teamMemberRepository::findAllByUserLogin).orElse(Collections.emptyList());
    }
}
