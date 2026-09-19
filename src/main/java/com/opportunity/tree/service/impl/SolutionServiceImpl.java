package com.opportunity.tree.service.impl;

import com.opportunity.tree.domain.Opportunity;
import com.opportunity.tree.domain.Solution;
import com.opportunity.tree.repository.OpportunityRepository;
import com.opportunity.tree.repository.SolutionRepository;
import com.opportunity.tree.service.NodeWriteRuleException;
import com.opportunity.tree.service.SolutionService;
import com.opportunity.tree.service.TeamAccessDeniedException;
import com.opportunity.tree.service.TeamAccessService;
import com.opportunity.tree.service.dto.OpportunityDTO;
import com.opportunity.tree.service.dto.SolutionDTO;
import com.opportunity.tree.service.mapper.SolutionMapper;
import java.time.Instant;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.opportunity.tree.domain.Solution}.
 *
 * <p>Enforces TREE-002 node write rules: authorisation via {@link TeamAccessService},
 * server-set {@code createdDate}/{@code lastModifiedDate}/{@code sortOrder}, and
 * parent validation (a Solution requires an Opportunity parent).
 */
@Service
@Transactional
public class SolutionServiceImpl implements SolutionService {

    private static final Logger LOG = LoggerFactory.getLogger(SolutionServiceImpl.class);

    private static final String ENTITY_NAME = "solution";

    private final SolutionRepository solutionRepository;
    private final OpportunityRepository opportunityRepository;
    private final SolutionMapper solutionMapper;
    private final TeamAccessService teamAccessService;

    public SolutionServiceImpl(
        SolutionRepository solutionRepository,
        OpportunityRepository opportunityRepository,
        SolutionMapper solutionMapper,
        TeamAccessService teamAccessService
    ) {
        this.solutionRepository = solutionRepository;
        this.opportunityRepository = opportunityRepository;
        this.solutionMapper = solutionMapper;
        this.teamAccessService = teamAccessService;
    }

    @Override
    public SolutionDTO save(SolutionDTO solutionDTO) {
        LOG.debug("Request to save Solution : {}", solutionDTO);
        Long opportunityId = opportunityIdOf(solutionDTO);
        if (opportunityId == null) {
            throw new NodeWriteRuleException("A solution must have an opportunity parent", ENTITY_NAME, "parentmissing");
        }
        Opportunity opportunity = opportunityRepository
            .findById(opportunityId)
            .orElseThrow(() -> new NodeWriteRuleException("Parent opportunity does not exist", ENTITY_NAME, "parentmissing"));
        teamAccessService.requireEditOpportunity(opportunity.getId());

        Solution solution = solutionMapper.toEntity(solutionDTO);
        solution.setId(null);
        solution.setOpportunity(opportunity);
        Instant now = Instant.now();
        solution.setCreatedDate(now);
        solution.setLastModifiedDate(now);
        solution.setSortOrder(nextSortOrder(opportunity.getId()));
        solution = solutionRepository.save(solution);
        return solutionMapper.toDto(solution);
    }

    @Override
    public SolutionDTO update(SolutionDTO solutionDTO) {
        LOG.debug("Request to update Solution : {}", solutionDTO);
        Solution existing = solutionRepository.findById(solutionDTO.getId()).orElseThrow(TeamAccessDeniedException::new);
        teamAccessService.requireEditSolution(existing.getId());

        Long targetOppId = opportunityIdOf(solutionDTO);
        Long existingOppId = existing.getOpportunity() != null ? existing.getOpportunity().getId() : null;
        Opportunity opportunity = existing.getOpportunity();
        if (targetOppId != null && !java.util.Objects.equals(existingOppId, targetOppId)) {
            opportunity = opportunityRepository
                .findById(targetOppId)
                .orElseThrow(() -> new NodeWriteRuleException("Parent opportunity does not exist", ENTITY_NAME, "parentmissing"));
            teamAccessService.requireEditOpportunity(opportunity.getId());
            Long targetTeamId = teamIdOfOpportunity(opportunity);
            Long existingTeamId = teamIdOfOpportunity(existing.getOpportunity());
            if (targetTeamId == null || !java.util.Objects.equals(targetTeamId, existingTeamId)) {
                throw new NodeWriteRuleException("Parent must belong to the same team", ENTITY_NAME, "parentwrongteam");
            }
        }

        Solution solution = solutionMapper.toEntity(solutionDTO);
        solution.setOpportunity(opportunity);
        solution.setCreatedDate(existing.getCreatedDate());
        solution.setSortOrder(existing.getSortOrder());
        solution.setLastModifiedDate(Instant.now());
        solution = solutionRepository.save(solution);
        return solutionMapper.toDto(solution);
    }

    @Override
    public Optional<SolutionDTO> partialUpdate(SolutionDTO solutionDTO) {
        LOG.debug("Request to partially update Solution : {}", solutionDTO);
        Solution existing = solutionRepository.findById(solutionDTO.getId()).orElseThrow(TeamAccessDeniedException::new);
        teamAccessService.requireEditSolution(existing.getId());

        // Captured before mapping: findById returns the same managed instance as
        // "existing", so the mapper would otherwise overwrite these values.
        Instant preservedCreatedDate = existing.getCreatedDate();
        Integer preservedSortOrder = existing.getSortOrder();
        Opportunity preservedOpportunity = existing.getOpportunity();

        return solutionRepository
            .findById(solutionDTO.getId())
            .map(existingSolution -> {
                solutionMapper.partialUpdate(existingSolution, solutionDTO);
                existingSolution.setCreatedDate(preservedCreatedDate);
                existingSolution.setSortOrder(preservedSortOrder);
                existingSolution.setOpportunity(preservedOpportunity);
                existingSolution.setLastModifiedDate(Instant.now());
                return existingSolution;
            })
            .map(solutionRepository::save)
            .map(solutionMapper::toDto);
    }

    public Page<SolutionDTO> findAllWithEagerRelationships(Pageable pageable) {
        return solutionRepository.findAllWithEagerRelationships(pageable).map(solutionMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SolutionDTO> findOne(Long id) {
        LOG.debug("Request to get Solution : {}", id);
        if (id == null || !teamAccessService.canReadSolution(id)) {
            return Optional.empty();
        }
        return solutionRepository.findOneWithEagerRelationships(id).map(solutionMapper::toDto);
    }

    @Override
    public void delete(Long id) {
        LOG.debug("Request to delete Solution : {}", id);
        Solution existing = solutionRepository.findById(id).orElseThrow(TeamAccessDeniedException::new);
        teamAccessService.requireEditSolution(existing.getId());
        solutionRepository.deleteById(id);
    }

    private int nextSortOrder(Long opportunityId) {
        Integer max = solutionRepository.findMaxSortOrderByOpportunityId(opportunityId);
        return (max == null ? -1 : max) + 1;
    }

    private static Long opportunityIdOf(SolutionDTO dto) {
        OpportunityDTO opportunity = dto == null ? null : dto.getOpportunity();
        return opportunity == null ? null : opportunity.getId();
    }

    private static Long teamIdOfOpportunity(Opportunity opp) {
        if (opp == null || opp.getOutcome() == null) {
            return null;
        }
        var product = opp.getOutcome().getProduct();
        if (product == null || product.getTeam() == null) {
            return null;
        }
        return product.getTeam().getId();
    }
}
