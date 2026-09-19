package com.opportunity.tree.service;

import com.opportunity.tree.domain.enumeration.TreeNodeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cascade delete for all six tree node types (Product, Outcome, Opportunity,
 * Solution, Assumption, Evidence) — see TREE-003 / FR-016. Every descendant
 * node is removed with it, together with the node links, open questions,
 * comments and NodeHistory rows of every deleted node.
 *
 * <p>Lives in its own class (rather than in the JHipster-generated
 * {@code *ServiceImpl} classes) so the logic survives entity regeneration and
 * keeps the cascade rules in one place.
 *
 * <p>Every entry point authorises the caller through {@link TeamAccessService}
 * (owners and editors succeed; viewers and non-members are rejected with
 * {@link TeamAccessDeniedException} which Spring maps to HTTP 403). The whole
 * delete runs in a single transaction, so a failure at any step rolls back the
 * entire operation and leaves the tree untouched.
 */
@Service
@Transactional
public class TreeNodeCascadeService {

    private static final Logger LOG = LoggerFactory.getLogger(TreeNodeCascadeService.class);

    private final TeamAccessService teamAccessService;

    @PersistenceContext
    private EntityManager em;

    public TreeNodeCascadeService(TeamAccessService teamAccessService) {
        this.teamAccessService = teamAccessService;
    }

    public void deleteProduct(Long productId) {
        LOG.debug("Cascade delete Product {}", productId);
        teamAccessService.requireEditNode(TreeNodeType.PRODUCT, productId);
        List<Long> outcomeIds = em
            .createQuery("select o.id from Outcome o where o.product.id = :pid", Long.class)
            .setParameter("pid", productId)
            .getResultList();
        deleteOutcomesInternal(outcomeIds);
        // Interviews reference the product via a NOT NULL FK; remove their
        // opportunity-interview join rows first, then the interviews themselves.
        em
            .createNativeQuery(
                "delete from rel_opportunity__interview where interview_id in (select id from interview where product_id = :pid)"
            )
            .setParameter("pid", productId)
            .executeUpdate();
        em.createQuery("delete from Interview i where i.product.id = :pid").setParameter("pid", productId).executeUpdate();
        em.createQuery("delete from NodeLink l where l.product.id = :pid").setParameter("pid", productId).executeUpdate();
        deleteHistoryFor(TreeNodeType.PRODUCT, List.of(productId));
        em.createQuery("delete from Product p where p.id = :id").setParameter("id", productId).executeUpdate();
        em.flush();
    }

    public void deleteOutcome(Long outcomeId) {
        LOG.debug("Cascade delete Outcome {}", outcomeId);
        teamAccessService.requireEditNode(TreeNodeType.OUTCOME, outcomeId);
        deleteOutcomesInternal(List.of(outcomeId));
        em.flush();
    }

    public void deleteOpportunity(Long opportunityId) {
        LOG.debug("Cascade delete Opportunity {}", opportunityId);
        teamAccessService.requireEditNode(TreeNodeType.OPPORTUNITY, opportunityId);
        deleteOpportunitiesInternal(List.of(opportunityId));
        em.flush();
    }

    public void deleteSolution(Long solutionId) {
        LOG.debug("Cascade delete Solution {}", solutionId);
        teamAccessService.requireEditNode(TreeNodeType.SOLUTION, solutionId);
        deleteSolutionsInternal(List.of(solutionId));
        em.flush();
    }

    public void deleteAssumption(Long assumptionId) {
        LOG.debug("Cascade delete Assumption {}", assumptionId);
        teamAccessService.requireEditNode(TreeNodeType.ASSUMPTION, assumptionId);
        deleteAssumptionsInternal(List.of(assumptionId));
        em.flush();
    }

    public void deleteEvidence(Long evidenceId) {
        LOG.debug("Cascade delete Evidence {}", evidenceId);
        teamAccessService.requireEditNode(TreeNodeType.EVIDENCE, evidenceId);
        deleteEvidenceInternal(List.of(evidenceId));
        em.flush();
    }

    /**
     * Delete any node with its whole subtree. The caller must be OWNER or EDITOR
     * of the node's team; otherwise (or when the node does not exist)
     * {@link TeamAccessDeniedException} is thrown and nothing is deleted.
     */
    public void deleteNode(TreeNodeType type, Long id) {
        if (type == null) {
            throw new TeamAccessDeniedException();
        }
        switch (type) {
            case PRODUCT -> deleteProduct(id);
            case OUTCOME -> deleteOutcome(id);
            case OPPORTUNITY -> deleteOpportunity(id);
            case SOLUTION -> deleteSolution(id);
            case ASSUMPTION -> deleteAssumption(id);
            case EVIDENCE -> deleteEvidence(id);
        }
    }

    // ---------------------------------------------------------------------
    // Internal cascade steps — assume authorisation already checked.
    // ---------------------------------------------------------------------

    private void deleteOutcomesInternal(List<Long> outcomeIds) {
        if (outcomeIds.isEmpty()) {
            return;
        }
        List<Long> topOpps = em
            .createQuery("select o.id from Opportunity o where o.outcome.id in :ids", Long.class)
            .setParameter("ids", outcomeIds)
            .getResultList();
        deleteOpportunitiesInternal(topOpps);
        // Comments hanging off the outcomes themselves.
        deleteCommentsFor("outcome", outcomeIds);
        deleteHistoryFor(TreeNodeType.OUTCOME, outcomeIds);
        em.createQuery("delete from NodeLink l where l.outcome.id in :ids").setParameter("ids", outcomeIds).executeUpdate();
        em.createQuery("delete from Outcome o where o.id in :ids").setParameter("ids", outcomeIds).executeUpdate();
    }

    private void deleteOpportunitiesInternal(List<Long> rootIds) {
        if (rootIds.isEmpty()) {
            return;
        }
        // Walk the parent → child edges level-by-level to collect every
        // descendant opportunity id, including the roots.
        Set<Long> allIds = new HashSet<>(rootIds);
        List<Long> frontier = new ArrayList<>(rootIds);
        while (!frontier.isEmpty()) {
            List<Long> children = em
                .createQuery("select o.id from Opportunity o where o.parent.id in :ids", Long.class)
                .setParameter("ids", frontier)
                .getResultList();
            children.removeAll(allIds); // guard against any cycle
            allIds.addAll(children);
            frontier = children;
        }
        List<Long> all = new ArrayList<>(allIds);

        // Delete every solution under the collected opportunities.
        List<Long> solutionIds = em
            .createQuery("select s.id from Solution s where s.opportunity.id in :ids", Long.class)
            .setParameter("ids", all)
            .getResultList();
        deleteSolutionsInternal(solutionIds);

        // Evidence hanging directly off the collected opportunities.
        List<Long> evidenceIds = em
            .createQuery("select e.id from Evidence e where e.opportunity.id in :ids", Long.class)
            .setParameter("ids", all)
            .getResultList();
        deleteEvidenceInternal(evidenceIds);

        // Clear dependents that reference opportunities.
        em
            .createNativeQuery("delete from rel_opportunity__interview where opportunity_id in (:ids)")
            .setParameter("ids", all)
            .executeUpdate();
        em.createNativeQuery("delete from rel_opportunity__tag where opportunity_id in (:ids)").setParameter("ids", all).executeUpdate();
        em.createQuery("delete from NodeLink l where l.opportunity.id in :ids").setParameter("ids", all).executeUpdate();
        em.createQuery("delete from OpenQuestion q where q.opportunity.id in :ids").setParameter("ids", all).executeUpdate();
        deleteCommentsFor("opportunity", all);
        deleteHistoryFor(TreeNodeType.OPPORTUNITY, all);

        // Break the self-referential parent link so we can bulk-delete without
        // caring about the deletion order.
        em.createNativeQuery("update opportunity set parent_id = null where parent_id in (:ids)").setParameter("ids", all).executeUpdate();
        em.createQuery("delete from Opportunity o where o.id in :ids").setParameter("ids", all).executeUpdate();
    }

    private void deleteSolutionsInternal(List<Long> solutionIds) {
        if (solutionIds.isEmpty()) {
            return;
        }
        em.createNativeQuery("delete from rel_solution__tag where solution_id in (:ids)").setParameter("ids", solutionIds).executeUpdate();
        em.createQuery("delete from NodeLink l where l.solution.id in :ids").setParameter("ids", solutionIds).executeUpdate();
        List<Long> assumptionIds = em
            .createQuery("select a.id from Assumption a where a.solution.id in :ids", Long.class)
            .setParameter("ids", solutionIds)
            .getResultList();
        deleteAssumptionsInternal(assumptionIds);
        deleteCommentsFor("solution", solutionIds);
        deleteHistoryFor(TreeNodeType.SOLUTION, solutionIds);
        em.createQuery("delete from Solution s where s.id in :ids").setParameter("ids", solutionIds).executeUpdate();
    }

    private void deleteAssumptionsInternal(List<Long> assumptionIds) {
        if (assumptionIds.isEmpty()) {
            return;
        }
        List<Long> evidenceIds = em
            .createQuery("select e.id from Evidence e where e.assumption.id in :ids", Long.class)
            .setParameter("ids", assumptionIds)
            .getResultList();
        deleteEvidenceInternal(evidenceIds);
        em.createQuery("delete from NodeLink l where l.assumption.id in :ids").setParameter("ids", assumptionIds).executeUpdate();
        deleteCommentsFor("assumption", assumptionIds);
        deleteHistoryFor(TreeNodeType.ASSUMPTION, assumptionIds);
        em.createQuery("delete from Assumption a where a.id in :ids").setParameter("ids", assumptionIds).executeUpdate();
    }

    private void deleteEvidenceInternal(List<Long> evidenceIds) {
        if (evidenceIds.isEmpty()) {
            return;
        }
        em.createQuery("delete from NodeLink l where l.evidence.id in :ids").setParameter("ids", evidenceIds).executeUpdate();
        deleteCommentsFor("evidence", evidenceIds);
        deleteHistoryFor(TreeNodeType.EVIDENCE, evidenceIds);
        em.createQuery("delete from Evidence e where e.id in :ids").setParameter("ids", evidenceIds).executeUpdate();
    }

    /** Delete the NodeHistory rows of the given nodes (history has no FKs, so it is matched by type + id). */
    private void deleteHistoryFor(TreeNodeType type, List<Long> nodeIds) {
        if (nodeIds.isEmpty()) {
            return;
        }
        em
            .createQuery("delete from NodeHistory h where h.nodeType = :type and h.nodeId in :ids")
            .setParameter("type", type)
            .setParameter("ids", nodeIds)
            .executeUpdate();
    }

    /**
     * Delete every comment that references any of the given node ids, honouring
     * the self-referential {@code parent_id} chain by nulling parents before
     * bulk-deleting.
     */
    private void deleteCommentsFor(String column, List<Long> nodeIds) {
        if (nodeIds.isEmpty()) {
            return;
        }
        String columnName = switch (column) {
            case "outcome" -> "outcome_id";
            case "opportunity" -> "opportunity_id";
            case "solution" -> "solution_id";
            case "assumption" -> "assumption_id";
            case "evidence" -> "evidence_id";
            default -> throw new IllegalArgumentException("Unsupported comment column: " + column);
        };
        // Collect target comment ids first, then null out any comment.parent_id
        // that points at one of them (either direct or via other comments), so
        // we can bulk-delete without violating the self-referential FK.
        @SuppressWarnings("unchecked")
        List<Number> ids = em
            .createNativeQuery("select id from comment where " + columnName + " in (:ids)")
            .setParameter("ids", nodeIds)
            .getResultList();
        if (ids.isEmpty()) {
            return;
        }
        List<Long> commentIds = ids.stream().map(Number::longValue).toList();
        em
            .createNativeQuery("update comment set parent_id = null where parent_id in (:ids)")
            .setParameter("ids", commentIds)
            .executeUpdate();
        em.createNativeQuery("delete from comment where id in (:ids)").setParameter("ids", commentIds).executeUpdate();
    }
}
