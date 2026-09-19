package com.opportunity.tree.service;

import com.opportunity.tree.domain.Opportunity;
import com.opportunity.tree.domain.Outcome;
import com.opportunity.tree.domain.Product;
import com.opportunity.tree.domain.Solution;
import com.opportunity.tree.domain.Team;
import com.opportunity.tree.domain.enumeration.TeamRole;
import com.opportunity.tree.repository.TeamRepository;
import com.opportunity.tree.service.dto.tree.OpportunityTreeNodeDTO;
import com.opportunity.tree.service.dto.tree.OutcomeTreeNodeDTO;
import com.opportunity.tree.service.dto.tree.ProductTreeNodeDTO;
import com.opportunity.tree.service.dto.tree.SolutionTreeNodeDTO;
import com.opportunity.tree.service.dto.tree.TeamTreeDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Assembles the whole-team tree returned by {@code GET /api/teams/{teamId}/tree}
 * (TREE-001).
 *
 * <p>All reads are authorised through {@link TeamAccessService}: any member
 * (owner, editor or viewer) may load a team's tree, while non-members are
 * rejected uniformly through {@link TeamAccessDeniedException} (HTTP 403).
 *
 * <p>The tree is assembled with a bounded number of queries regardless of node
 * count — one query per node type (products, outcomes, opportunities,
 * solutions) plus the team lookup and role lookup. There is no per-node
 * navigation of lazy associations, so the query count is independent of the
 * size of the tree.
 */
@Service
@Transactional(readOnly = true)
public class TeamTreeService {

    private static final Logger LOG = LoggerFactory.getLogger(TeamTreeService.class);

    private final TeamRepository teamRepository;
    private final TeamAccessService teamAccessService;

    @PersistenceContext
    private EntityManager em;

    public TeamTreeService(TeamRepository teamRepository, TeamAccessService teamAccessService) {
        this.teamRepository = teamRepository;
        this.teamAccessService = teamAccessService;
    }

    /**
     * Return the full tree for the given team. The current user must be a
     * member of the team (any role); otherwise {@link TeamAccessDeniedException}
     * is thrown, which Spring Security maps to HTTP 403.
     */
    public TeamTreeDTO getTreeForTeam(Long teamId) {
        LOG.debug("Assembling team tree for team {}", teamId);
        teamAccessService.requireReadTeam(teamId);

        Team team = teamRepository.findById(teamId).orElseThrow(TeamAccessDeniedException::new);

        TeamTreeDTO dto = new TeamTreeDTO();
        dto.setId(team.getId());
        dto.setName(team.getName());
        dto.setDescription(team.getDescription());
        dto.setCreatedDate(team.getCreatedDate());

        Optional<TeamRole> role = teamAccessService.getCurrentUserRole(teamId);
        dto.setCurrentUserRole(role.orElse(null));
        dto.setCanEdit(role.map(r -> r == TeamRole.OWNER || r == TeamRole.EDITOR).orElse(false));

        List<Product> products = em
            .createQuery("select p from Product p where p.team.id = :teamId order by p.id asc", Product.class)
            .setParameter("teamId", teamId)
            .getResultList();

        Map<Long, ProductTreeNodeDTO> productById = new LinkedHashMap<>();
        for (Product p : products) {
            ProductTreeNodeDTO pn = new ProductTreeNodeDTO();
            pn.setId(p.getId());
            pn.setName(p.getName());
            pn.setDescription(p.getDescription());
            pn.setVision(p.getVision());
            pn.setArchived(p.getArchived());
            pn.setCreatedDate(p.getCreatedDate());
            productById.put(p.getId(), pn);
            dto.getProducts().add(pn);
        }

        if (products.isEmpty()) {
            return dto;
        }

        List<Outcome> outcomes = em
            .createQuery("select o from Outcome o where o.product.team.id = :teamId order by o.sortOrder asc, o.id asc", Outcome.class)
            .setParameter("teamId", teamId)
            .getResultList();

        Map<Long, OutcomeTreeNodeDTO> outcomeById = new HashMap<>();
        for (Outcome o : outcomes) {
            OutcomeTreeNodeDTO on = new OutcomeTreeNodeDTO();
            on.setId(o.getId());
            on.setTitle(o.getTitle());
            on.setDescription(o.getDescription());
            on.setSortOrder(o.getSortOrder());
            outcomeById.put(o.getId(), on);
            // outcome.getProduct().getId() returns the FK id from the proxy — no lazy load.
            Long productId = o.getProduct().getId();
            ProductTreeNodeDTO pn = productById.get(productId);
            if (pn != null) {
                pn.getOutcomes().add(on);
            }
        }

        if (outcomes.isEmpty()) {
            return dto;
        }

        List<Opportunity> opportunities = em
            .createQuery(
                "select o from Opportunity o where o.outcome.product.team.id = :teamId order by o.sortOrder asc, o.id asc",
                Opportunity.class
            )
            .setParameter("teamId", teamId)
            .getResultList();

        Map<Long, OpportunityTreeNodeDTO> opportunityById = new HashMap<>();
        // First pass: build DTOs.
        for (Opportunity op : opportunities) {
            OpportunityTreeNodeDTO opn = new OpportunityTreeNodeDTO();
            opn.setId(op.getId());
            opn.setTitle(op.getTitle());
            opn.setDescription(op.getDescription());
            opn.setStatus(op.getStatus());
            opn.setValuerating(op.getValuerating());
            opn.setSortOrder(op.getSortOrder());
            // getParent().getId() on a Hibernate proxy is a no-op that reads the FK — no lazy load.
            Opportunity parent = op.getParent();
            opn.setParentId(parent == null ? null : parent.getId());
            opportunityById.put(op.getId(), opn);
        }
        // Second pass: attach to parent (either another opportunity or an outcome).
        for (Opportunity op : opportunities) {
            OpportunityTreeNodeDTO opn = opportunityById.get(op.getId());
            Long parentId = opn.getParentId();
            if (parentId != null) {
                OpportunityTreeNodeDTO parentNode = opportunityById.get(parentId);
                if (parentNode != null) {
                    parentNode.getChildren().add(opn);
                    continue;
                }
                // Parent id points outside this team — skip defensively rather than orphan.
            }
            Long outcomeId = op.getOutcome().getId();
            OutcomeTreeNodeDTO on = outcomeById.get(outcomeId);
            if (on != null) {
                on.getOpportunities().add(opn);
            }
        }

        if (opportunities.isEmpty()) {
            return dto;
        }

        List<Solution> solutions = em
            .createQuery(
                "select s from Solution s where s.opportunity.outcome.product.team.id = :teamId order by s.sortOrder asc, s.id asc",
                Solution.class
            )
            .setParameter("teamId", teamId)
            .getResultList();

        for (Solution s : solutions) {
            SolutionTreeNodeDTO sn = new SolutionTreeNodeDTO();
            sn.setId(s.getId());
            sn.setTitle(s.getTitle());
            sn.setDescription(s.getDescription());
            sn.setStatus(s.getStatus());
            sn.setSortOrder(s.getSortOrder());
            Long opportunityId = s.getOpportunity().getId();
            OpportunityTreeNodeDTO opn = opportunityById.get(opportunityId);
            if (opn != null) {
                opn.getSolutions().add(sn);
            }
        }

        // Second-pass attach may have appended child opportunities in bulk order
        // rather than per-parent sort order; re-sort each opportunity's children
        // and solutions to guarantee sortOrder-ascending siblings everywhere.
        sortRecursively(dto);

        return dto;
    }

    private static void sortRecursively(TeamTreeDTO dto) {
        Comparator<OpportunityTreeNodeDTO> byOppSort = Comparator.comparing(
            OpportunityTreeNodeDTO::getSortOrder,
            Comparator.nullsLast(Comparator.naturalOrder())
        ).thenComparing(OpportunityTreeNodeDTO::getId, Comparator.nullsLast(Comparator.naturalOrder()));
        Comparator<SolutionTreeNodeDTO> bySolSort = Comparator.comparing(
            SolutionTreeNodeDTO::getSortOrder,
            Comparator.nullsLast(Comparator.naturalOrder())
        ).thenComparing(SolutionTreeNodeDTO::getId, Comparator.nullsLast(Comparator.naturalOrder()));
        Comparator<OutcomeTreeNodeDTO> byOutcomeSort = Comparator.comparing(
            OutcomeTreeNodeDTO::getSortOrder,
            Comparator.nullsLast(Comparator.naturalOrder())
        ).thenComparing(OutcomeTreeNodeDTO::getId, Comparator.nullsLast(Comparator.naturalOrder()));

        for (ProductTreeNodeDTO product : dto.getProducts()) {
            Collections.sort(product.getOutcomes(), byOutcomeSort);
            for (OutcomeTreeNodeDTO outcome : product.getOutcomes()) {
                Collections.sort(outcome.getOpportunities(), byOppSort);
                for (OpportunityTreeNodeDTO opp : outcome.getOpportunities()) {
                    sortOpportunityRecursive(opp, byOppSort, bySolSort);
                }
            }
        }
    }

    private static void sortOpportunityRecursive(
        OpportunityTreeNodeDTO opp,
        Comparator<OpportunityTreeNodeDTO> byOppSort,
        Comparator<SolutionTreeNodeDTO> bySolSort
    ) {
        List<OpportunityTreeNodeDTO> children = new ArrayList<>(opp.getChildren());
        children.sort(byOppSort);
        opp.setChildren(children);
        List<SolutionTreeNodeDTO> solutions = new ArrayList<>(opp.getSolutions());
        solutions.sort(bySolSort);
        opp.setSolutions(solutions);
        for (OpportunityTreeNodeDTO child : opp.getChildren()) {
            sortOpportunityRecursive(child, byOppSort, bySolSort);
        }
    }
}
