package com.opportunity.tree.service;

import com.opportunity.tree.domain.Assumption;
import com.opportunity.tree.domain.Evidence;
import com.opportunity.tree.domain.Opportunity;
import com.opportunity.tree.domain.Outcome;
import com.opportunity.tree.domain.Product;
import com.opportunity.tree.domain.Solution;
import com.opportunity.tree.domain.enumeration.HistoryEventType;
import com.opportunity.tree.domain.enumeration.TreeNodeType;
import com.opportunity.tree.repository.AssumptionRepository;
import com.opportunity.tree.repository.EvidenceRepository;
import com.opportunity.tree.repository.OpportunityRepository;
import com.opportunity.tree.repository.OutcomeRepository;
import com.opportunity.tree.repository.ProductRepository;
import com.opportunity.tree.repository.SolutionRepository;
import com.opportunity.tree.service.broadcast.TreeChangePublisher;
import com.opportunity.tree.service.broadcast.TreeChangeType;
import com.opportunity.tree.service.dto.tree.MoveTreeNodeRequest;
import com.opportunity.tree.service.dto.tree.MoveTreeNodeResponse;
import com.opportunity.tree.service.dto.tree.SiblingOrderDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Re-parents and reorders tree nodes for {@code POST /api/tree/nodes/move} (ported from
 * REARR-001 and extended to assumptions, evidence and product reorder).
 *
 * <p>Rules:
 * <ul>
 *   <li>Products are reordered within their team and take no parent.</li>
 *   <li>Every other node needs a parent of a legal type ({@link TreeNodeRules}): outcome → product,
 *       opportunity → outcome | opportunity, solution → opportunity, assumption → solution,
 *       evidence → opportunity | assumption.</li>
 *   <li>The caller must be OWNER/EDITOR of the node's team and of the new parent's team (else
 *       403, same for missing ids); node and parent must be in the same team (else 400
 *       {@code crossteam}). The move then waits for the team's {@link TreeStructureLock}; a node or
 *       parent deleted by a concurrent request meanwhile is a 409 {@code error.concurrencyFailure}.</li>
 *   <li>An opportunity cannot move under itself or one of its descendants (400 {@code cycle}).
 *       Moving an opportunity to another outcome carries its whole nested subtree along.</li>
 *   <li>Siblings of the moved node's type are renumbered densely (0..n-1) under the new parent
 *       and, when the parent changed, under the old parent. {@code position} omitted = append.</li>
 *   <li>A MOVED history entry ("Moved under “X”") is written only when the parent changes; a
 *       pure reorder within the same parent writes no history (matches the prototype, which only
 *       logs a parent change).</li>
 * </ul>
 *
 * <p>Everything runs in one transaction, so a failure at any step rolls back the whole move.
 */
@Service
@Transactional
public class TreeNodeMoveService {

    private static final Logger LOG = LoggerFactory.getLogger(TreeNodeMoveService.class);

    private final TeamAccessService teamAccessService;
    private final NodeHistoryRecorder historyRecorder;
    private final TreeNodeDtoAssembler dtoAssembler;
    private final ProductRepository productRepository;
    private final OutcomeRepository outcomeRepository;
    private final OpportunityRepository opportunityRepository;
    private final SolutionRepository solutionRepository;
    private final AssumptionRepository assumptionRepository;
    private final EvidenceRepository evidenceRepository;
    private final TreeStructureLock structureLock;
    private final TreeChangePublisher changePublisher;

    @PersistenceContext
    private EntityManager em;

    public TreeNodeMoveService(
        TeamAccessService teamAccessService,
        NodeHistoryRecorder historyRecorder,
        TreeNodeDtoAssembler dtoAssembler,
        ProductRepository productRepository,
        OutcomeRepository outcomeRepository,
        OpportunityRepository opportunityRepository,
        SolutionRepository solutionRepository,
        AssumptionRepository assumptionRepository,
        EvidenceRepository evidenceRepository,
        TreeStructureLock structureLock,
        TreeChangePublisher changePublisher
    ) {
        this.teamAccessService = teamAccessService;
        this.historyRecorder = historyRecorder;
        this.dtoAssembler = dtoAssembler;
        this.productRepository = productRepository;
        this.outcomeRepository = outcomeRepository;
        this.opportunityRepository = opportunityRepository;
        this.solutionRepository = solutionRepository;
        this.assumptionRepository = assumptionRepository;
        this.evidenceRepository = evidenceRepository;
        this.structureLock = structureLock;
        this.changePublisher = changePublisher;
    }

    public MoveTreeNodeResponse move(MoveTreeNodeRequest request) {
        if (request == null) {
            throw new NodeWriteRuleException("Request body is required", TreeNodeRules.ENTITY_NAME, "unknowntype");
        }
        TreeNodeType nodeType = TreeNodeRules.parseType(request.nodeType(), "nodeType");
        Long nodeId = TreeNodeRules.wholeLong(request.nodeId(), "nodeId", "nodemissing");
        if (nodeId == null) {
            throw new NodeWriteRuleException("nodeId is required", TreeNodeRules.ENTITY_NAME, "nodemissing");
        }
        Integer position = TreeNodeRules.wholeInt(request.position(), "position", "invalidposition");
        if (position != null && position < 0) {
            throw new NodeWriteRuleException("position must be zero or greater", TreeNodeRules.ENTITY_NAME, "invalidposition");
        }
        LOG.debug("Move {} {} under {} {} at {}", nodeType, nodeId, request.parentType(), request.parentId(), position);

        if (nodeType == TreeNodeType.PRODUCT) {
            return moveProduct(request, nodeId, position);
        }

        TreeNodeType parentType = TreeNodeRules.parseType(request.parentType(), "parentType");
        Long parentId = TreeNodeRules.wholeLong(request.parentId(), "parentId", "parentmissing");
        if (parentId == null) {
            throw new NodeWriteRuleException("parentId is required", TreeNodeRules.ENTITY_NAME, "parentmissing");
        }
        TreeNodeRules.requireAllowed(parentType, nodeType);
        TreeNodeRef newParent = new TreeNodeRef(parentType, parentId);

        Long nodeTeamId = teamAccessService.requireEditNode(nodeType, nodeId);
        Long parentTeamId = teamAccessService.requireEditNode(parentType, newParent.id());
        if (!nodeTeamId.equals(parentTeamId)) {
            throw new NodeWriteRuleException("A node can only move within its own team", TreeNodeRules.ENTITY_NAME, "crossteam");
        }
        // Serialise with other structural writes in this team before reading any parent/sibling state
        // (concurrent moves between the same parents would otherwise deadlock on the renumbering).
        structureLock.lockTeam(nodeTeamId);
        // Access was checked before the wait: the previous lock holder may have deleted either one.
        structureLock.requireNode(nodeType, nodeId, nodeTeamId);
        structureLock.requireNode(parentType, newParent.id(), nodeTeamId);
        if (nodeType == TreeNodeType.OPPORTUNITY && parentType == TreeNodeType.OPPORTUNITY && wouldFormCycle(nodeId, newParent.id())) {
            throw new NodeWriteRuleException(
                "Cannot move an opportunity under itself or one of its descendants",
                TreeNodeRules.ENTITY_NAME,
                "cycle"
            );
        }

        Object node = em.find(entityClass(nodeType), nodeId);
        TreeNodeRef oldParent = parentOf(node);
        boolean sameParent = oldParent.equals(newParent);
        if (!sameParent) {
            reparent(node, newParent);
        }
        em.flush();

        List<SiblingOrderDTO> siblings = new ArrayList<>(renumber(nodeType, newParent, nodeId, position));
        if (!sameParent) {
            siblings.addAll(renumber(nodeType, oldParent, null, null));
            setLastModified(node, Instant.now());
            historyRecorder.record(nodeType, nodeId, HistoryEventType.MOVED, "Moved under “" + titleOf(newParent) + "”");
        }
        em.flush();
        MoveTreeNodeResponse response = new MoveTreeNodeResponse(dtoAssembler.toDto(nodeType, nodeId), siblings);
        changePublisher.publish(TreeChangeType.NODE_MOVED, nodeTeamId, response);
        return response;
    }

    // ---------------------------------------------------------------------
    // Product — reorder within the team
    // ---------------------------------------------------------------------

    private MoveTreeNodeResponse moveProduct(MoveTreeNodeRequest request, Long productId, Integer position) {
        if (request.parentType() != null || request.parentId() != null) {
            throw new NodeWriteRuleException(
                "A product has no parent; it can only be reordered",
                TreeNodeRules.ENTITY_NAME,
                "invalidparent"
            );
        }
        Long teamId = teamAccessService.requireEditNode(TreeNodeType.PRODUCT, productId);
        structureLock.lockTeam(teamId);
        structureLock.requireNode(TreeNodeType.PRODUCT, productId, teamId);
        List<SiblingOrderDTO> siblings = renumber(TreeNodeType.PRODUCT, new TreeNodeRef(null, teamId), productId, position);
        em.flush();
        MoveTreeNodeResponse response = new MoveTreeNodeResponse(dtoAssembler.toDto(TreeNodeType.PRODUCT, productId), siblings);
        changePublisher.publish(TreeChangeType.NODE_MOVED, teamId, response);
        return response;
    }

    // ---------------------------------------------------------------------
    // Re-parenting
    // ---------------------------------------------------------------------

    private static Class<?> entityClass(TreeNodeType type) {
        return switch (type) {
            case PRODUCT -> Product.class;
            case OUTCOME -> Outcome.class;
            case OPPORTUNITY -> Opportunity.class;
            case SOLUTION -> Solution.class;
            case ASSUMPTION -> Assumption.class;
            case EVIDENCE -> Evidence.class;
        };
    }

    private static TreeNodeRef parentOf(Object node) {
        return switch (node) {
            case Outcome o -> new TreeNodeRef(TreeNodeType.PRODUCT, o.getProduct().getId());
            case Opportunity o -> o.getParent() != null
                ? new TreeNodeRef(TreeNodeType.OPPORTUNITY, o.getParent().getId())
                : new TreeNodeRef(TreeNodeType.OUTCOME, o.getOutcome().getId());
            case Solution s -> new TreeNodeRef(TreeNodeType.OPPORTUNITY, s.getOpportunity().getId());
            case Assumption a -> new TreeNodeRef(TreeNodeType.SOLUTION, a.getSolution().getId());
            case Evidence e -> e.getOpportunity() != null
                ? new TreeNodeRef(TreeNodeType.OPPORTUNITY, e.getOpportunity().getId())
                : new TreeNodeRef(TreeNodeType.ASSUMPTION, e.getAssumption().getId());
            default -> throw new IllegalArgumentException("Not a movable node: " + node);
        };
    }

    private void reparent(Object node, TreeNodeRef parent) {
        switch (node) {
            case Outcome o -> o.setProduct(em.find(Product.class, parent.id()));
            case Opportunity o -> reparentOpportunity(o, parent);
            case Solution s -> s.setOpportunity(em.find(Opportunity.class, parent.id()));
            case Assumption a -> a.setSolution(em.find(Solution.class, parent.id()));
            case Evidence e -> {
                if (parent.type() == TreeNodeType.OPPORTUNITY) {
                    e.setOpportunity(em.find(Opportunity.class, parent.id()));
                    e.setAssumption(null);
                } else {
                    e.setAssumption(em.find(Assumption.class, parent.id()));
                    e.setOpportunity(null);
                }
            }
            default -> throw new IllegalArgumentException("Not a movable node: " + node);
        }
    }

    /**
     * Nested opportunities carry a denormalised outcome reference; when the effective outcome
     * changes, every descendant opportunity is switched to the new outcome as well.
     */
    private void reparentOpportunity(Opportunity o, TreeNodeRef parent) {
        Opportunity parentOpp = null;
        Outcome outcome;
        if (parent.type() == TreeNodeType.OPPORTUNITY) {
            parentOpp = em.find(Opportunity.class, parent.id());
            outcome = parentOpp.getOutcome();
        } else {
            outcome = em.find(Outcome.class, parent.id());
        }
        Long oldOutcomeId = o.getOutcome().getId();
        o.setParent(parentOpp);
        o.setOutcome(outcome);
        if (!oldOutcomeId.equals(outcome.getId())) {
            List<Long> descendants = opportunityDescendants(o.getId());
            if (!descendants.isEmpty()) {
                em.flush();
                em
                    .createQuery("update Opportunity o set o.outcome = :outcome where o.id in :ids")
                    .setParameter("outcome", outcome)
                    .setParameter("ids", descendants)
                    .executeUpdate();
            }
        }
    }

    /** True when {@code candidateParentId} is {@code nodeId} itself or one of its descendants. */
    private boolean wouldFormCycle(Long nodeId, Long candidateParentId) {
        Long cursor = candidateParentId;
        Set<Long> seen = new HashSet<>();
        while (cursor != null) {
            if (cursor.equals(nodeId) || !seen.add(cursor)) {
                return true;
            }
            List<Long> parentIds = em
                .createQuery("select o.parent.id from Opportunity o where o.id = :id", Long.class)
                .setParameter("id", cursor)
                .getResultList();
            cursor = parentIds.isEmpty() ? null : parentIds.get(0);
        }
        return false;
    }

    private List<Long> opportunityDescendants(Long rootId) {
        List<Long> all = new ArrayList<>();
        Deque<Long> frontier = new ArrayDeque<>();
        frontier.add(rootId);
        Set<Long> visited = new HashSet<>();
        visited.add(rootId);
        while (!frontier.isEmpty()) {
            Long current = frontier.poll();
            for (Long child : em
                .createQuery("select o.id from Opportunity o where o.parent.id = :pid", Long.class)
                .setParameter("pid", current)
                .getResultList()) {
                if (visited.add(child)) {
                    all.add(child);
                    frontier.add(child);
                }
            }
        }
        return all;
    }

    // ---------------------------------------------------------------------
    // Renumbering — dense 0..n-1 among the parent's children of one type
    // ---------------------------------------------------------------------

    /**
     * Renumbers the children of {@code type} under {@code parent} (for products: the team, with
     * the team id in {@code parent.id()}). When {@code moveId} is set that node is placed at
     * {@code position} (clamped; null = last).
     */
    private List<SiblingOrderDTO> renumber(TreeNodeType type, TreeNodeRef parent, Long moveId, Integer position) {
        List<Object> ordered = new ArrayList<>(siblings(type, parent));
        if (moveId != null) {
            Object moved = null;
            for (var it = ordered.iterator(); it.hasNext(); ) {
                Object n = it.next();
                if (moveId.equals(idOf(n))) {
                    moved = n;
                    it.remove();
                    break;
                }
            }
            if (moved != null) {
                int at = position == null ? ordered.size() : Math.min(position, ordered.size());
                ordered.add(at, moved);
            }
        }
        List<SiblingOrderDTO> result = new ArrayList<>(ordered.size());
        for (int i = 0; i < ordered.size(); i++) {
            setSortOrder(ordered.get(i), i);
            result.add(new SiblingOrderDTO(TreeNodeRef.key(type, idOf(ordered.get(i))), i));
        }
        saveAll(type, ordered);
        return result;
    }

    private List<?> siblings(TreeNodeType type, TreeNodeRef parent) {
        String where = switch (type) {
            case PRODUCT -> "x.team.id = :pid";
            case OUTCOME -> "x.product.id = :pid";
            case OPPORTUNITY -> parent.type() == TreeNodeType.OPPORTUNITY
                ? "x.parent.id = :pid"
                : "x.outcome.id = :pid and x.parent is null";
            case SOLUTION -> "x.opportunity.id = :pid";
            case ASSUMPTION -> "x.solution.id = :pid";
            case EVIDENCE -> parent.type() == TreeNodeType.OPPORTUNITY ? "x.opportunity.id = :pid" : "x.assumption.id = :pid";
        };
        return em
            .createQuery(
                "select x from " + entityClass(type).getSimpleName() + " x where " + where + " order by x.sortOrder asc, x.id asc",
                entityClass(type)
            )
            .setParameter("pid", parent.id())
            .getResultList();
    }

    /** Explicit save of the renumbered siblings through their repository. */
    @SuppressWarnings("unchecked")
    private void saveAll(TreeNodeType type, List<Object> entities) {
        switch (type) {
            case PRODUCT -> productRepository.saveAll((List<Product>) (List<?>) entities);
            case OUTCOME -> outcomeRepository.saveAll((List<Outcome>) (List<?>) entities);
            case OPPORTUNITY -> opportunityRepository.saveAll((List<Opportunity>) (List<?>) entities);
            case SOLUTION -> solutionRepository.saveAll((List<Solution>) (List<?>) entities);
            case ASSUMPTION -> assumptionRepository.saveAll((List<Assumption>) (List<?>) entities);
            case EVIDENCE -> evidenceRepository.saveAll((List<Evidence>) (List<?>) entities);
        }
    }

    private static Long idOf(Object node) {
        return switch (node) {
            case Product p -> p.getId();
            case Outcome o -> o.getId();
            case Opportunity o -> o.getId();
            case Solution s -> s.getId();
            case Assumption a -> a.getId();
            case Evidence e -> e.getId();
            default -> throw new IllegalArgumentException("Not a tree node: " + node);
        };
    }

    private static void setSortOrder(Object node, int sortOrder) {
        switch (node) {
            case Product p -> p.setSortOrder(sortOrder);
            case Outcome o -> o.setSortOrder(sortOrder);
            case Opportunity o -> o.setSortOrder(sortOrder);
            case Solution s -> s.setSortOrder(sortOrder);
            case Assumption a -> a.setSortOrder(sortOrder);
            case Evidence e -> e.setSortOrder(sortOrder);
            default -> throw new IllegalArgumentException("Not a tree node: " + node);
        }
    }

    private static void setLastModified(Object node, Instant at) {
        switch (node) {
            case Outcome o -> o.setLastModifiedDate(at);
            case Opportunity o -> o.setLastModifiedDate(at);
            case Solution s -> s.setLastModifiedDate(at);
            case Assumption a -> a.setLastModifiedDate(at);
            case Evidence e -> e.setLastModifiedDate(at);
            default -> {
                // Products have no lastModifiedDate.
            }
        }
    }

    private String titleOf(TreeNodeRef ref) {
        Object parent = em.find(entityClass(ref.type()), ref.id());
        return switch (parent) {
            case Product p -> p.getName();
            case Outcome o -> o.getTitle();
            case Opportunity o -> o.getTitle();
            case Solution s -> s.getTitle();
            case Assumption a -> a.getStatement();
            case Evidence e -> e.getTitle();
            default -> "—";
        };
    }
}
