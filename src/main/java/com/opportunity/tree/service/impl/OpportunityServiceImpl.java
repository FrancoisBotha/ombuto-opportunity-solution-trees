package com.opportunity.tree.service.impl;

import com.opportunity.tree.domain.Opportunity;
import com.opportunity.tree.domain.Outcome;
import com.opportunity.tree.domain.Product;
import com.opportunity.tree.domain.Team;
import com.opportunity.tree.repository.OpportunityRepository;
import com.opportunity.tree.repository.OutcomeRepository;
import com.opportunity.tree.service.NodeWriteRuleException;
import com.opportunity.tree.service.OpportunityService;
import com.opportunity.tree.service.TeamAccessDeniedException;
import com.opportunity.tree.service.TeamAccessService;
import com.opportunity.tree.service.dto.OpportunityDTO;
import com.opportunity.tree.service.dto.OutcomeDTO;
import com.opportunity.tree.service.mapper.OpportunityMapper;
import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.opportunity.tree.domain.Opportunity}.
 *
 * <p>Enforces TREE-002 node write rules: authorisation via {@link TeamAccessService},
 * server-set {@code createdDate}/{@code lastModifiedDate}/{@code sortOrder}, defaults
 * for {@code valuerating}/{@code complexity} (both 3), parent validation (an
 * Opportunity has exactly one parent — either an Outcome or another Opportunity),
 * a same-team-parent check and a cycle guard so an Opportunity cannot become its
 * own ancestor.
 */
@Service
@Transactional
public class OpportunityServiceImpl implements OpportunityService {

    private static final Logger LOG = LoggerFactory.getLogger(OpportunityServiceImpl.class);

    private static final String ENTITY_NAME = "opportunity";

    private final OpportunityRepository opportunityRepository;
    private final OutcomeRepository outcomeRepository;
    private final OpportunityMapper opportunityMapper;
    private final TeamAccessService teamAccessService;

    public OpportunityServiceImpl(
        OpportunityRepository opportunityRepository,
        OutcomeRepository outcomeRepository,
        OpportunityMapper opportunityMapper,
        TeamAccessService teamAccessService
    ) {
        this.opportunityRepository = opportunityRepository;
        this.outcomeRepository = outcomeRepository;
        this.opportunityMapper = opportunityMapper;
        this.teamAccessService = teamAccessService;
    }

    @Override
    public OpportunityDTO save(OpportunityDTO opportunityDTO) {
        LOG.debug("Request to save Opportunity : {}", opportunityDTO);
        Long parentOpportunityId = parentOpportunityIdOf(opportunityDTO);
        Long outcomeId = outcomeIdOf(opportunityDTO);

        if (parentOpportunityId == null && outcomeId == null) {
            throw new NodeWriteRuleException("An opportunity requires an Outcome or parent Opportunity", ENTITY_NAME, "parentmissing");
        }

        Outcome outcome;
        Opportunity parent = null;
        if (parentOpportunityId != null) {
            parent = opportunityRepository
                .findById(parentOpportunityId)
                .orElseThrow(() -> new NodeWriteRuleException("Parent opportunity does not exist", ENTITY_NAME, "parentmissing"));
            outcome = parent.getOutcome();
            if (outcomeId != null && !Objects.equals(outcomeId, outcome != null ? outcome.getId() : null)) {
                throw new NodeWriteRuleException("Opportunity outcome must match parent's outcome", ENTITY_NAME, "parentwrongteam");
            }
        } else {
            outcome = outcomeRepository
                .findById(outcomeId)
                .orElseThrow(() -> new NodeWriteRuleException("Parent outcome does not exist", ENTITY_NAME, "parentmissing"));
        }
        if (outcome == null) {
            throw new NodeWriteRuleException("Parent outcome does not exist", ENTITY_NAME, "parentmissing");
        }
        teamAccessService.requireEditOutcome(outcome.getId());

        Opportunity opportunity = opportunityMapper.toEntity(opportunityDTO);
        opportunity.setId(null);
        opportunity.setOutcome(outcome);
        opportunity.setParent(parent);
        if (opportunity.getValuerating() == null) {
            opportunity.setValuerating(3);
        }
        if (opportunity.getComplexity() == null) {
            opportunity.setComplexity(3);
        }
        Instant now = Instant.now();
        opportunity.setCreatedDate(now);
        opportunity.setLastModifiedDate(now);
        opportunity.setSortOrder(nextSortOrder(outcome.getId(), parent == null ? null : parent.getId()));
        opportunity = opportunityRepository.save(opportunity);
        return opportunityMapper.toDto(opportunity);
    }

    @Override
    public OpportunityDTO update(OpportunityDTO opportunityDTO) {
        LOG.debug("Request to update Opportunity : {}", opportunityDTO);
        Opportunity existing = opportunityRepository.findById(opportunityDTO.getId()).orElseThrow(TeamAccessDeniedException::new);
        teamAccessService.requireEditOpportunity(existing.getId());

        Long existingTeamId = teamIdOfOpportunity(existing);
        Long targetParentOppId = parentOpportunityIdOf(opportunityDTO);
        Long targetOutcomeId = outcomeIdOf(opportunityDTO);

        Outcome newOutcome = existing.getOutcome();
        Opportunity newParent = existing.getParent();

        if (targetParentOppId != null) {
            if (Objects.equals(targetParentOppId, existing.getId())) {
                throw new NodeWriteRuleException("Opportunity cannot be its own parent", ENTITY_NAME, "cycle");
            }
            Opportunity candidate = opportunityRepository
                .findById(targetParentOppId)
                .orElseThrow(() -> new NodeWriteRuleException("Parent opportunity does not exist", ENTITY_NAME, "parentmissing"));
            assertNotDescendant(existing.getId(), candidate);
            Long candidateTeam = teamIdOfOpportunity(candidate);
            if (candidateTeam == null || !Objects.equals(candidateTeam, existingTeamId)) {
                throw new NodeWriteRuleException("Parent must belong to the same team", ENTITY_NAME, "parentwrongteam");
            }
            newParent = candidate;
            newOutcome = candidate.getOutcome();
        } else if (
            targetOutcomeId != null &&
            !Objects.equals(targetOutcomeId, existing.getOutcome() == null ? null : existing.getOutcome().getId())
        ) {
            Outcome candidate = outcomeRepository
                .findById(targetOutcomeId)
                .orElseThrow(() -> new NodeWriteRuleException("Parent outcome does not exist", ENTITY_NAME, "parentmissing"));
            Long candidateTeam = teamIdOfOutcome(candidate);
            if (candidateTeam == null || !Objects.equals(candidateTeam, existingTeamId)) {
                throw new NodeWriteRuleException("Parent must belong to the same team", ENTITY_NAME, "parentwrongteam");
            }
            newOutcome = candidate;
            newParent = null;
        }

        Opportunity opportunity = opportunityMapper.toEntity(opportunityDTO);
        opportunity.setOutcome(newOutcome);
        opportunity.setParent(newParent);
        opportunity.setCreatedDate(existing.getCreatedDate());
        opportunity.setSortOrder(existing.getSortOrder());
        if (opportunity.getValuerating() == null) {
            opportunity.setValuerating(existing.getValuerating() == null ? 3 : existing.getValuerating());
        }
        if (opportunity.getComplexity() == null) {
            opportunity.setComplexity(existing.getComplexity() == null ? 3 : existing.getComplexity());
        }
        opportunity.setLastModifiedDate(Instant.now());
        opportunity = opportunityRepository.save(opportunity);
        return opportunityMapper.toDto(opportunity);
    }

    @Override
    public Optional<OpportunityDTO> partialUpdate(OpportunityDTO opportunityDTO) {
        LOG.debug("Request to partially update Opportunity : {}", opportunityDTO);
        Opportunity existing = opportunityRepository.findById(opportunityDTO.getId()).orElseThrow(TeamAccessDeniedException::new);
        teamAccessService.requireEditOpportunity(existing.getId());

        // Captured before mapping: findById returns the same managed instance as
        // "existing", so the mapper would otherwise overwrite these values.
        Instant preservedCreatedDate = existing.getCreatedDate();
        Integer preservedSortOrder = existing.getSortOrder();
        Outcome preservedOutcome = existing.getOutcome();
        Opportunity preservedParent = existing.getParent();

        return opportunityRepository
            .findById(opportunityDTO.getId())
            .map(existingOpportunity -> {
                opportunityMapper.partialUpdate(existingOpportunity, opportunityDTO);
                existingOpportunity.setCreatedDate(preservedCreatedDate);
                existingOpportunity.setSortOrder(preservedSortOrder);
                existingOpportunity.setOutcome(preservedOutcome);
                existingOpportunity.setParent(preservedParent);
                if (existingOpportunity.getValuerating() == null) {
                    existingOpportunity.setValuerating(3);
                }
                if (existingOpportunity.getComplexity() == null) {
                    existingOpportunity.setComplexity(3);
                }
                existingOpportunity.setLastModifiedDate(Instant.now());
                return existingOpportunity;
            })
            .map(opportunityRepository::save)
            .map(opportunityMapper::toDto);
    }

    public Page<OpportunityDTO> findAllWithEagerRelationships(Pageable pageable) {
        return opportunityRepository.findAllWithEagerRelationships(pageable).map(opportunityMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OpportunityDTO> findOne(Long id) {
        LOG.debug("Request to get Opportunity : {}", id);
        if (id == null || !teamAccessService.canReadOpportunity(id)) {
            return Optional.empty();
        }
        return opportunityRepository.findOneWithEagerRelationships(id).map(opportunityMapper::toDto);
    }

    @Override
    public void delete(Long id) {
        LOG.debug("Request to delete Opportunity : {}", id);
        Opportunity existing = opportunityRepository.findById(id).orElseThrow(TeamAccessDeniedException::new);
        teamAccessService.requireEditOpportunity(existing.getId());
        opportunityRepository.deleteById(id);
    }

    private int nextSortOrder(Long outcomeId, Long parentOpportunityId) {
        Integer max;
        if (parentOpportunityId != null) {
            max = opportunityRepository.findMaxSortOrderByParentId(parentOpportunityId);
        } else {
            max = opportunityRepository.findMaxSortOrderByOutcomeIdRoot(outcomeId);
        }
        return (max == null ? -1 : max) + 1;
    }

    private void assertNotDescendant(Long selfId, Opportunity candidate) {
        Set<Long> seen = new HashSet<>();
        Opportunity node = candidate;
        while (node != null) {
            if (!seen.add(node.getId())) {
                // Cycle already present upstream — refuse defensively.
                throw new NodeWriteRuleException("Opportunity parent chain has a cycle", ENTITY_NAME, "cycle");
            }
            if (Objects.equals(node.getId(), selfId)) {
                throw new NodeWriteRuleException("Opportunity cannot be its own ancestor", ENTITY_NAME, "cycle");
            }
            node = node.getParent();
        }
    }

    private static Long parentOpportunityIdOf(OpportunityDTO dto) {
        OpportunityDTO parent = dto == null ? null : dto.getParent();
        return parent == null ? null : parent.getId();
    }

    private static Long outcomeIdOf(OpportunityDTO dto) {
        OutcomeDTO outcome = dto == null ? null : dto.getOutcome();
        return outcome == null ? null : outcome.getId();
    }

    private static Long teamIdOfOpportunity(Opportunity opp) {
        if (opp == null) {
            return null;
        }
        return teamIdOfOutcome(opp.getOutcome());
    }

    private static Long teamIdOfOutcome(Outcome outcome) {
        if (outcome == null) {
            return null;
        }
        Product product = outcome.getProduct();
        if (product == null) {
            return null;
        }
        Team team = product.getTeam();
        return team == null ? null : team.getId();
    }
}
