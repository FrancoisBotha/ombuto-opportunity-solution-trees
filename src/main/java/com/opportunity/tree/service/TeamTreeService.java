package com.opportunity.tree.service;

import com.opportunity.tree.domain.Assumption;
import com.opportunity.tree.domain.Evidence;
import com.opportunity.tree.domain.NodeLink;
import com.opportunity.tree.domain.OpenQuestion;
import com.opportunity.tree.domain.Opportunity;
import com.opportunity.tree.domain.Outcome;
import com.opportunity.tree.domain.Product;
import com.opportunity.tree.domain.Solution;
import com.opportunity.tree.domain.Team;
import com.opportunity.tree.domain.TeamMember;
import com.opportunity.tree.domain.User;
import com.opportunity.tree.domain.enumeration.TeamRole;
import com.opportunity.tree.domain.enumeration.TreeNodeType;
import com.opportunity.tree.repository.TeamRepository;
import com.opportunity.tree.repository.TeamTreeRepository;
import com.opportunity.tree.security.SecurityUtils;
import com.opportunity.tree.service.dto.tree.TeamTreeDTO;
import com.opportunity.tree.service.dto.tree.TeamTreeMemberDTO;
import com.opportunity.tree.service.dto.tree.TreeActivityDTO;
import com.opportunity.tree.service.dto.tree.TreeNodeDTO;
import com.opportunity.tree.service.dto.tree.TreeNodeLinkDTO;
import com.opportunity.tree.service.dto.tree.TreeOpenQuestionDTO;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Assembles the flat whole-team tree returned by {@code GET /api/teams/{teamId}/tree}.
 *
 * <p>Any member (owner, editor, viewer) may read; non-members — including
 * ROLE_ADMIN users who are not members — get {@link TeamAccessDeniedException}
 * (HTTP 403) whether or not the team exists.
 *
 * <p>Nodes are returned in pre-order: products by sortOrder then id; below each
 * node its children grouped outcomes → opportunities → solutions → assumptions
 * → evidence, each group by sortOrder then id. Every node carries its
 * {@code parentKey}, so the client can rebuild the hierarchy from array order
 * alone.
 *
 * <p>The tree is assembled with a fixed number of queries (members, one per node
 * type, comment counts, links, open questions, latest history), independent of
 * the tree's size — see {@link TeamTreeRepository}.
 */
@Service
@Transactional(readOnly = true)
public class TeamTreeService {

    private static final Logger LOG = LoggerFactory.getLogger(TeamTreeService.class);

    /** Sentinel for id lists that would otherwise be empty (an empty IN list is not portable). */
    private static final List<Long> NO_IDS = List.of(-1L);

    private static final Map<TreeNodeType, Integer> SIBLING_RANK = new EnumMap<>(TreeNodeType.class);

    static {
        SIBLING_RANK.put(TreeNodeType.PRODUCT, 0);
        SIBLING_RANK.put(TreeNodeType.OUTCOME, 1);
        SIBLING_RANK.put(TreeNodeType.OPPORTUNITY, 2);
        SIBLING_RANK.put(TreeNodeType.SOLUTION, 3);
        SIBLING_RANK.put(TreeNodeType.ASSUMPTION, 4);
        SIBLING_RANK.put(TreeNodeType.EVIDENCE, 5);
    }

    private static final Comparator<TreeNodeDTO> SIBLING_ORDER = Comparator.<TreeNodeDTO>comparingInt(n -> SIBLING_RANK.get(n.getType()))
        .thenComparing(TreeNodeDTO::getSortOrder, Comparator.nullsLast(Comparator.naturalOrder()))
        .thenComparing(TreeNodeDTO::getId, Comparator.nullsLast(Comparator.naturalOrder()));

    private final TeamRepository teamRepository;
    private final TeamTreeRepository teamTreeRepository;
    private final TeamAccessService teamAccessService;
    private final Clock clock;

    public TeamTreeService(TeamRepository teamRepository, TeamTreeRepository teamTreeRepository, TeamAccessService teamAccessService) {
        this.teamRepository = teamRepository;
        this.teamTreeRepository = teamTreeRepository;
        this.teamAccessService = teamAccessService;
        this.clock = Clock.systemUTC();
    }

    /**
     * Return the flat tree for the given team. The current user must be a member
     * of the team (any role); otherwise {@link TeamAccessDeniedException} is thrown.
     */
    public TeamTreeDTO getTreeForTeam(Long teamId) {
        LOG.debug("Assembling team tree for team {}", teamId);
        TeamRole role = teamAccessService.getCurrentUserRole(teamId).orElseThrow(TeamAccessDeniedException::new);
        Team team = teamRepository.findById(teamId).orElseThrow(TeamAccessDeniedException::new);

        TeamTreeDTO dto = new TeamTreeDTO();
        dto.setId(team.getId());
        dto.setName(team.getName());
        dto.setDescription(team.getDescription());
        dto.setCurrentUserLogin(SecurityUtils.getCurrentUserLogin().orElse(null));
        dto.setCurrentUserRole(role);
        dto.setCanEdit(role == TeamRole.OWNER || role == TeamRole.EDITOR);
        dto.setMembers(members(teamId));

        // Load order matters: each type's required (eager) parent association is
        // already in the persistence context when the child type is loaded.
        Map<String, TreeNodeDTO> byKey = new HashMap<>();
        Map<TreeNodeType, Set<Long>> idsByType = new EnumMap<>(TreeNodeType.class);
        for (TreeNodeType t : TreeNodeType.values()) {
            idsByType.put(t, new HashSet<>());
        }

        for (Product p : teamTreeRepository.findProducts(teamId)) {
            TreeNodeDTO n = node(TreeNodeType.PRODUCT, p.getId(), null, p.getName(), p.getDescription(), p.getSortOrder());
            n.setArchived(p.getArchived());
            n.setCreatedDate(p.getCreatedDate());
            add(n, byKey, idsByType);
        }
        for (Object[] row : teamTreeRepository.findOutcomesWithOwner(teamId)) {
            Outcome o = (Outcome) row[0];
            TreeNodeDTO n = node(
                TreeNodeType.OUTCOME,
                o.getId(),
                key(TreeNodeType.PRODUCT, o.getProduct().getId()),
                o.getTitle(),
                o.getDescription(),
                o.getSortOrder()
            );
            n.setOwnerLogin((String) row[1]);
            dates(n, o.getCreatedDate(), o.getLastModifiedDate());
            add(n, byKey, idsByType);
        }
        for (Object[] row : teamTreeRepository.findOpportunitiesWithOwner(teamId)) {
            Opportunity op = (Opportunity) row[0];
            String parentKey =
                op.getParent() != null
                    ? key(TreeNodeType.OPPORTUNITY, op.getParent().getId())
                    : key(TreeNodeType.OUTCOME, op.getOutcome().getId());
            TreeNodeDTO n = node(TreeNodeType.OPPORTUNITY, op.getId(), parentKey, op.getTitle(), op.getDescription(), op.getSortOrder());
            n.setStatus(op.getStatus() == null ? null : op.getStatus().name());
            n.setPriority(op.getPriority());
            n.setValueRating(op.getValuerating());
            n.setOwnerLogin((String) row[1]);
            dates(n, op.getCreatedDate(), op.getLastModifiedDate());
            add(n, byKey, idsByType);
        }
        for (Object[] row : teamTreeRepository.findSolutionsWithOwner(teamId)) {
            Solution s = (Solution) row[0];
            TreeNodeDTO n = node(
                TreeNodeType.SOLUTION,
                s.getId(),
                key(TreeNodeType.OPPORTUNITY, s.getOpportunity().getId()),
                s.getTitle(),
                s.getDescription(),
                s.getSortOrder()
            );
            n.setStatus(s.getStatus() == null ? null : s.getStatus().name());
            n.setOwnerLogin((String) row[1]);
            dates(n, s.getCreatedDate(), s.getLastModifiedDate());
            add(n, byKey, idsByType);
        }
        for (Object[] row : teamTreeRepository.findAssumptionsWithOwner(teamId)) {
            Assumption a = (Assumption) row[0];
            TreeNodeDTO n = node(
                TreeNodeType.ASSUMPTION,
                a.getId(),
                key(TreeNodeType.SOLUTION, a.getSolution().getId()),
                a.getStatement(),
                a.getDescription(),
                a.getSortOrder()
            );
            n.setStatus(a.getStatus() == null ? null : a.getStatus().name());
            n.setConfidence(a.getConfidence());
            n.setOwnerLogin((String) row[1]);
            dates(n, a.getCreatedDate(), a.getLastModifiedDate());
            add(n, byKey, idsByType);
        }
        for (Evidence e : teamTreeRepository.findEvidence(teamId)) {
            // getId() on a lazy association reads the foreign key — no extra query.
            String parentKey =
                e.getOpportunity() != null
                    ? key(TreeNodeType.OPPORTUNITY, e.getOpportunity().getId())
                    : e.getAssumption() != null
                        ? key(TreeNodeType.ASSUMPTION, e.getAssumption().getId())
                        : null;
            TreeNodeDTO n = node(TreeNodeType.EVIDENCE, e.getId(), parentKey, e.getTitle(), e.getDescription(), e.getSortOrder());
            dates(n, e.getCreatedDate(), e.getLastModifiedDate());
            add(n, byKey, idsByType);
        }

        if (!byKey.isEmpty()) {
            attachCommentCounts(byKey, idsByType);
            attachLinks(byKey, idsByType);
            attachQuestions(byKey, idsByType);
        }

        Map<String, String> productKeyOf = new HashMap<>();
        dto.setNodes(preOrder(byKey, productKeyOf));

        if (!byKey.isEmpty()) {
            attachLastActivity(byKey, idsByType, productKeyOf);
        }

        Instant monthStart = LocalDate.now(clock).withDayOfMonth(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        dto.setEvidenceThisMonth(
            dto
                .getNodes()
                .stream()
                .filter(n -> n.getType() == TreeNodeType.EVIDENCE && n.getCreatedDate() != null && !n.getCreatedDate().isBefore(monthStart))
                .count()
        );
        return dto;
    }

    // ---------------------------------------------------------------------
    // Assembly helpers
    // ---------------------------------------------------------------------

    private List<TeamTreeMemberDTO> members(Long teamId) {
        return teamTreeRepository
            .findMembersWithUser(teamId)
            .stream()
            .sorted(
                Comparator.comparing((TeamMember tm) -> tm.getRole() == null ? Integer.MAX_VALUE : tm.getRole().ordinal()).thenComparing(
                    tm -> tm.getUser().getLogin()
                )
            )
            .map(tm -> {
                User u = tm.getUser();
                return new TeamTreeMemberDTO(u.getLogin(), u.getFirstName(), u.getLastName(), initials(u), tm.getRole());
            })
            .toList();
    }

    static String initials(User u) {
        String first = u.getFirstName() == null ? "" : u.getFirstName().trim();
        String last = u.getLastName() == null ? "" : u.getLastName().trim();
        String result;
        if (!first.isEmpty() && !last.isEmpty()) {
            result = first.substring(0, 1) + last.substring(0, 1);
        } else if (!first.isEmpty() || !last.isEmpty()) {
            String name = first.isEmpty() ? last : first;
            result = name.substring(0, Math.min(2, name.length()));
        } else {
            String login = u.getLogin() == null ? "" : u.getLogin();
            result = login.substring(0, Math.min(2, login.length()));
        }
        return result.toUpperCase(Locale.ROOT);
    }

    private static TreeNodeDTO node(TreeNodeType type, Long id, String parentKey, String title, String notes, Integer sortOrder) {
        TreeNodeDTO n = new TreeNodeDTO();
        n.setKey(key(type, id));
        n.setType(type);
        n.setId(id);
        n.setParentKey(parentKey);
        n.setTitle(title);
        n.setNotes(notes);
        n.setSortOrder(sortOrder);
        return n;
    }

    private static void dates(TreeNodeDTO n, Instant created, Instant lastModified) {
        n.setCreatedDate(created);
        n.setLastModifiedDate(lastModified);
    }

    private static void add(TreeNodeDTO n, Map<String, TreeNodeDTO> byKey, Map<TreeNodeType, Set<Long>> idsByType) {
        byKey.put(n.getKey(), n);
        idsByType.get(n.getType()).add(n.getId());
    }

    private static String key(TreeNodeType type, Long id) {
        return TreeNodeRef.key(type, id);
    }

    private static Collection<Long> ids(Map<TreeNodeType, Set<Long>> idsByType, TreeNodeType type) {
        Set<Long> ids = idsByType.get(type);
        return ids.isEmpty() ? NO_IDS : ids;
    }

    private void attachCommentCounts(Map<String, TreeNodeDTO> byKey, Map<TreeNodeType, Set<Long>> idsByType) {
        TreeNodeType[] columns = {
            TreeNodeType.OUTCOME,
            TreeNodeType.OPPORTUNITY,
            TreeNodeType.SOLUTION,
            TreeNodeType.ASSUMPTION,
            TreeNodeType.EVIDENCE,
        };
        List<Object[]> rows = teamTreeRepository.countComments(
            ids(idsByType, TreeNodeType.OUTCOME),
            ids(idsByType, TreeNodeType.OPPORTUNITY),
            ids(idsByType, TreeNodeType.SOLUTION),
            ids(idsByType, TreeNodeType.ASSUMPTION),
            ids(idsByType, TreeNodeType.EVIDENCE)
        );
        for (Object[] row : rows) {
            long count = ((Number) row[columns.length]).longValue();
            for (int i = 0; i < columns.length; i++) {
                if (row[i] instanceof Number id) {
                    TreeNodeDTO n = byKey.get(key(columns[i], id.longValue()));
                    if (n != null) {
                        n.setCommentCount(n.getCommentCount() + count);
                    }
                    break;
                }
            }
        }
    }

    private void attachLinks(Map<String, TreeNodeDTO> byKey, Map<TreeNodeType, Set<Long>> idsByType) {
        List<NodeLink> links = teamTreeRepository.findLinks(
            ids(idsByType, TreeNodeType.PRODUCT),
            ids(idsByType, TreeNodeType.OUTCOME),
            ids(idsByType, TreeNodeType.OPPORTUNITY),
            ids(idsByType, TreeNodeType.SOLUTION),
            ids(idsByType, TreeNodeType.ASSUMPTION),
            ids(idsByType, TreeNodeType.EVIDENCE)
        );
        for (NodeLink l : links) {
            String nodeKey = linkNodeKey(l);
            TreeNodeDTO n = nodeKey == null ? null : byKey.get(nodeKey);
            if (n != null) {
                n.getLinks().add(new TreeNodeLinkDTO(l.getId(), l.getName(), l.getUrl()));
            }
        }
    }

    private static String linkNodeKey(NodeLink l) {
        if (l.getProduct() != null) {
            return key(TreeNodeType.PRODUCT, l.getProduct().getId());
        }
        if (l.getOutcome() != null) {
            return key(TreeNodeType.OUTCOME, l.getOutcome().getId());
        }
        if (l.getOpportunity() != null) {
            return key(TreeNodeType.OPPORTUNITY, l.getOpportunity().getId());
        }
        if (l.getSolution() != null) {
            return key(TreeNodeType.SOLUTION, l.getSolution().getId());
        }
        if (l.getAssumption() != null) {
            return key(TreeNodeType.ASSUMPTION, l.getAssumption().getId());
        }
        if (l.getEvidence() != null) {
            return key(TreeNodeType.EVIDENCE, l.getEvidence().getId());
        }
        return null;
    }

    private void attachQuestions(Map<String, TreeNodeDTO> byKey, Map<TreeNodeType, Set<Long>> idsByType) {
        if (idsByType.get(TreeNodeType.OPPORTUNITY).isEmpty()) {
            return;
        }
        for (OpenQuestion q : teamTreeRepository.findQuestions(idsByType.get(TreeNodeType.OPPORTUNITY))) {
            TreeNodeDTO n = byKey.get(key(TreeNodeType.OPPORTUNITY, q.getOpportunity().getId()));
            if (n != null) {
                n.getQuestions().add(new TreeOpenQuestionDTO(q.getId(), q.getQuestionText(), q.getDone()));
            }
        }
    }

    /** Sets each product's lastActivity to the newest history event anywhere in its branch. */
    private void attachLastActivity(
        Map<String, TreeNodeDTO> byKey,
        Map<TreeNodeType, Set<Long>> idsByType,
        Map<String, String> productKeyOf
    ) {
        List<Object[]> rows = teamTreeRepository.findLatestHistory(
            ids(idsByType, TreeNodeType.PRODUCT),
            ids(idsByType, TreeNodeType.OUTCOME),
            ids(idsByType, TreeNodeType.OPPORTUNITY),
            ids(idsByType, TreeNodeType.SOLUTION),
            ids(idsByType, TreeNodeType.ASSUMPTION),
            ids(idsByType, TreeNodeType.EVIDENCE)
        );
        for (Object[] row : rows) {
            String nodeKey = key((TreeNodeType) row[0], ((Number) row[1]).longValue());
            Instant at = (Instant) row[2];
            String productKey = productKeyOf.get(nodeKey);
            TreeNodeDTO product = productKey == null ? null : byKey.get(productKey);
            if (product == null || at == null) {
                continue;
            }
            TreeActivityDTO current = product.getLastActivity();
            if (current == null || at.isAfter(current.at())) {
                product.setLastActivity(new TreeActivityDTO(at, (String) row[3]));
            }
        }
    }

    /**
     * Orders the nodes depth-first from the products. Nodes whose parent is not
     * part of the team's tree are dropped. Fills {@code productKeyOf} with the
     * product key of every emitted node.
     */
    private static List<TreeNodeDTO> preOrder(Map<String, TreeNodeDTO> byKey, Map<String, String> productKeyOf) {
        Map<String, List<TreeNodeDTO>> children = new LinkedHashMap<>();
        List<TreeNodeDTO> roots = new ArrayList<>();
        for (TreeNodeDTO n : byKey.values()) {
            if (n.getType() == TreeNodeType.PRODUCT) {
                roots.add(n);
            } else if (n.getParentKey() != null) {
                children.computeIfAbsent(n.getParentKey(), k -> new ArrayList<>()).add(n);
            }
        }
        roots.sort(SIBLING_ORDER);
        children.values().forEach(list -> list.sort(SIBLING_ORDER));

        List<TreeNodeDTO> out = new ArrayList<>(byKey.size());
        Set<String> visited = new HashSet<>();
        for (TreeNodeDTO root : roots) {
            // Iterative DFS keeps deep opportunity nesting off the call stack.
            List<TreeNodeDTO> stack = new ArrayList<>();
            stack.add(root);
            while (!stack.isEmpty()) {
                TreeNodeDTO n = stack.remove(stack.size() - 1);
                if (!visited.add(n.getKey())) {
                    continue; // guards against a corrupt parent cycle
                }
                out.add(n);
                productKeyOf.put(n.getKey(), root.getKey());
                List<TreeNodeDTO> kids = children.getOrDefault(n.getKey(), List.of());
                for (int i = kids.size() - 1; i >= 0; i--) {
                    stack.add(kids.get(i));
                }
            }
        }
        return out;
    }
}
