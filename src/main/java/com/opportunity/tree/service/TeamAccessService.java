package com.opportunity.tree.service;

import com.opportunity.tree.domain.Assumption;
import com.opportunity.tree.domain.Opportunity;
import com.opportunity.tree.domain.Outcome;
import com.opportunity.tree.domain.Product;
import com.opportunity.tree.domain.Solution;
import com.opportunity.tree.domain.Team;
import com.opportunity.tree.domain.TeamMember;
import com.opportunity.tree.domain.enumeration.TeamRole;
import com.opportunity.tree.repository.AssumptionRepository;
import com.opportunity.tree.repository.OpportunityRepository;
import com.opportunity.tree.repository.OutcomeRepository;
import com.opportunity.tree.repository.ProductRepository;
import com.opportunity.tree.repository.SolutionRepository;
import com.opportunity.tree.repository.TeamMemberRepository;
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
    private final ProductRepository productRepository;
    private final OutcomeRepository outcomeRepository;
    private final OpportunityRepository opportunityRepository;
    private final SolutionRepository solutionRepository;
    private final AssumptionRepository assumptionRepository;

    public TeamAccessService(
        TeamMemberRepository teamMemberRepository,
        ProductRepository productRepository,
        OutcomeRepository outcomeRepository,
        OpportunityRepository opportunityRepository,
        SolutionRepository solutionRepository,
        AssumptionRepository assumptionRepository
    ) {
        this.teamMemberRepository = teamMemberRepository;
        this.productRepository = productRepository;
        this.outcomeRepository = outcomeRepository;
        this.opportunityRepository = opportunityRepository;
        this.solutionRepository = solutionRepository;
        this.assumptionRepository = assumptionRepository;
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
    // Product-level checks
    // ---------------------------------------------------------------------

    public boolean canReadProduct(Long productId) {
        return teamIdForProduct(productId).map(this::canReadTeam).orElse(false);
    }

    public boolean canEditProduct(Long productId) {
        return teamIdForProduct(productId).map(this::canEditTeam).orElse(false);
    }

    public boolean isProductOwner(Long productId) {
        return teamIdForProduct(productId).map(this::isTeamOwner).orElse(false);
    }

    public void requireReadProduct(Long productId) {
        if (!canReadProduct(productId)) {
            throw new TeamAccessDeniedException();
        }
    }

    public void requireEditProduct(Long productId) {
        if (!canEditProduct(productId)) {
            throw new TeamAccessDeniedException();
        }
    }

    public void requireOwnerProduct(Long productId) {
        if (!isProductOwner(productId)) {
            throw new TeamAccessDeniedException();
        }
    }

    // ---------------------------------------------------------------------
    // Node-level checks (outcome, opportunity, solution, assumption)
    // ---------------------------------------------------------------------

    public boolean canReadOutcome(Long outcomeId) {
        return teamIdForOutcome(outcomeId).map(this::canReadTeam).orElse(false);
    }

    public boolean canEditOutcome(Long outcomeId) {
        return teamIdForOutcome(outcomeId).map(this::canEditTeam).orElse(false);
    }

    public boolean isOutcomeOwner(Long outcomeId) {
        return teamIdForOutcome(outcomeId).map(this::isTeamOwner).orElse(false);
    }

    public void requireReadOutcome(Long outcomeId) {
        if (!canReadOutcome(outcomeId)) {
            throw new TeamAccessDeniedException();
        }
    }

    public void requireEditOutcome(Long outcomeId) {
        if (!canEditOutcome(outcomeId)) {
            throw new TeamAccessDeniedException();
        }
    }

    public boolean canReadOpportunity(Long opportunityId) {
        return teamIdForOpportunity(opportunityId).map(this::canReadTeam).orElse(false);
    }

    public boolean canEditOpportunity(Long opportunityId) {
        return teamIdForOpportunity(opportunityId).map(this::canEditTeam).orElse(false);
    }

    public boolean isOpportunityOwner(Long opportunityId) {
        return teamIdForOpportunity(opportunityId).map(this::isTeamOwner).orElse(false);
    }

    public void requireReadOpportunity(Long opportunityId) {
        if (!canReadOpportunity(opportunityId)) {
            throw new TeamAccessDeniedException();
        }
    }

    public void requireEditOpportunity(Long opportunityId) {
        if (!canEditOpportunity(opportunityId)) {
            throw new TeamAccessDeniedException();
        }
    }

    public boolean canReadSolution(Long solutionId) {
        return teamIdForSolution(solutionId).map(this::canReadTeam).orElse(false);
    }

    public boolean canEditSolution(Long solutionId) {
        return teamIdForSolution(solutionId).map(this::canEditTeam).orElse(false);
    }

    public boolean isSolutionOwner(Long solutionId) {
        return teamIdForSolution(solutionId).map(this::isTeamOwner).orElse(false);
    }

    public void requireReadSolution(Long solutionId) {
        if (!canReadSolution(solutionId)) {
            throw new TeamAccessDeniedException();
        }
    }

    public void requireEditSolution(Long solutionId) {
        if (!canEditSolution(solutionId)) {
            throw new TeamAccessDeniedException();
        }
    }

    public boolean canReadAssumption(Long assumptionId) {
        return teamIdForAssumption(assumptionId).map(this::canReadTeam).orElse(false);
    }

    public boolean canEditAssumption(Long assumptionId) {
        return teamIdForAssumption(assumptionId).map(this::canEditTeam).orElse(false);
    }

    public boolean isAssumptionOwner(Long assumptionId) {
        return teamIdForAssumption(assumptionId).map(this::isTeamOwner).orElse(false);
    }

    public void requireReadAssumption(Long assumptionId) {
        if (!canReadAssumption(assumptionId)) {
            throw new TeamAccessDeniedException();
        }
    }

    public void requireEditAssumption(Long assumptionId) {
        if (!canEditAssumption(assumptionId)) {
            throw new TeamAccessDeniedException();
        }
    }

    // ---------------------------------------------------------------------
    // Internal helpers
    // ---------------------------------------------------------------------

    private List<TeamMember> currentUserMemberships() {
        return SecurityUtils.getCurrentUserLogin().map(teamMemberRepository::findAllByUserLogin).orElse(Collections.emptyList());
    }

    private Optional<Long> teamIdForProduct(Long productId) {
        if (productId == null) {
            return Optional.empty();
        }
        return productRepository.findById(productId).map(Product::getTeam).map(Team::getId);
    }

    private Optional<Long> teamIdForOutcome(Long outcomeId) {
        if (outcomeId == null) {
            return Optional.empty();
        }
        return outcomeRepository
            .findById(outcomeId)
            .map(Outcome::getProduct)
            .flatMap(p -> Optional.ofNullable(p.getTeam()))
            .map(Team::getId);
    }

    private Optional<Long> teamIdForOpportunity(Long opportunityId) {
        if (opportunityId == null) {
            return Optional.empty();
        }
        return opportunityRepository
            .findById(opportunityId)
            .map(Opportunity::getOutcome)
            .map(Outcome::getProduct)
            .map(Product::getTeam)
            .map(Team::getId);
    }

    private Optional<Long> teamIdForSolution(Long solutionId) {
        if (solutionId == null) {
            return Optional.empty();
        }
        return solutionRepository
            .findById(solutionId)
            .map(Solution::getOpportunity)
            .map(Opportunity::getOutcome)
            .map(Outcome::getProduct)
            .map(Product::getTeam)
            .map(Team::getId);
    }

    private Optional<Long> teamIdForAssumption(Long assumptionId) {
        if (assumptionId == null) {
            return Optional.empty();
        }
        return assumptionRepository
            .findById(assumptionId)
            .map(Assumption::getSolution)
            .map(Solution::getOpportunity)
            .map(Opportunity::getOutcome)
            .map(Outcome::getProduct)
            .map(Product::getTeam)
            .map(Team::getId);
    }
}
