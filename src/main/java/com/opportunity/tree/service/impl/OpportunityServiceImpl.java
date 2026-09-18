package com.opportunity.tree.service.impl;

import com.opportunity.tree.domain.Opportunity;
import com.opportunity.tree.repository.OpportunityRepository;
import com.opportunity.tree.service.OpportunityService;
import com.opportunity.tree.service.dto.OpportunityDTO;
import com.opportunity.tree.service.mapper.OpportunityMapper;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.opportunity.tree.domain.Opportunity}.
 */
@Service
@Transactional
public class OpportunityServiceImpl implements OpportunityService {

    private static final Logger LOG = LoggerFactory.getLogger(OpportunityServiceImpl.class);

    private final OpportunityRepository opportunityRepository;

    private final OpportunityMapper opportunityMapper;

    public OpportunityServiceImpl(OpportunityRepository opportunityRepository, OpportunityMapper opportunityMapper) {
        this.opportunityRepository = opportunityRepository;
        this.opportunityMapper = opportunityMapper;
    }

    @Override
    public OpportunityDTO save(OpportunityDTO opportunityDTO) {
        LOG.debug("Request to save Opportunity : {}", opportunityDTO);
        Opportunity opportunity = opportunityMapper.toEntity(opportunityDTO);
        opportunity = opportunityRepository.save(opportunity);
        return opportunityMapper.toDto(opportunity);
    }

    @Override
    public OpportunityDTO update(OpportunityDTO opportunityDTO) {
        LOG.debug("Request to update Opportunity : {}", opportunityDTO);
        Opportunity opportunity = opportunityMapper.toEntity(opportunityDTO);
        opportunity = opportunityRepository.save(opportunity);
        return opportunityMapper.toDto(opportunity);
    }

    @Override
    public Optional<OpportunityDTO> partialUpdate(OpportunityDTO opportunityDTO) {
        LOG.debug("Request to partially update Opportunity : {}", opportunityDTO);

        return opportunityRepository
            .findById(opportunityDTO.getId())
            .map(existingOpportunity -> {
                opportunityMapper.partialUpdate(existingOpportunity, opportunityDTO);

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
        return opportunityRepository.findOneWithEagerRelationships(id).map(opportunityMapper::toDto);
    }

    @Override
    public void delete(Long id) {
        LOG.debug("Request to delete Opportunity : {}", id);
        opportunityRepository.deleteById(id);
    }
}
