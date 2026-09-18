package com.opportunity.tree.service.impl;

import com.opportunity.tree.repository.OpportunityRepository;
import com.opportunity.tree.service.OpportunityService;
import com.opportunity.tree.service.dto.OpportunityDTO;
import com.opportunity.tree.service.mapper.OpportunityMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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
    public Mono<OpportunityDTO> save(OpportunityDTO opportunityDTO) {
        LOG.debug("Request to save Opportunity : {}", opportunityDTO);
        return opportunityRepository.save(opportunityMapper.toEntity(opportunityDTO)).map(opportunityMapper::toDto);
    }

    @Override
    public Mono<OpportunityDTO> update(OpportunityDTO opportunityDTO) {
        LOG.debug("Request to update Opportunity : {}", opportunityDTO);
        return opportunityRepository.save(opportunityMapper.toEntity(opportunityDTO)).map(opportunityMapper::toDto);
    }

    @Override
    public Mono<OpportunityDTO> partialUpdate(OpportunityDTO opportunityDTO) {
        LOG.debug("Request to partially update Opportunity : {}", opportunityDTO);

        return opportunityRepository
            .findById(opportunityDTO.getId())
            .map(existingOpportunity -> {
                opportunityMapper.partialUpdate(existingOpportunity, opportunityDTO);

                return existingOpportunity;
            })
            .flatMap(opportunityRepository::save)
            .map(opportunityMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Flux<OpportunityDTO> findAll(Pageable pageable) {
        LOG.debug("Request to get all Opportunities");
        return opportunityRepository.findAllBy(pageable).map(opportunityMapper::toDto);
    }

    public Flux<OpportunityDTO> findAllWithEagerRelationships(Pageable pageable) {
        return opportunityRepository.findAllWithEagerRelationships(pageable).map(opportunityMapper::toDto);
    }

    public Mono<Long> countAll() {
        return opportunityRepository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public Mono<OpportunityDTO> findOne(Long id) {
        LOG.debug("Request to get Opportunity : {}", id);
        return opportunityRepository.findOneWithEagerRelationships(id).map(opportunityMapper::toDto);
    }

    @Override
    public Mono<Void> delete(Long id) {
        LOG.debug("Request to delete Opportunity : {}", id);
        return opportunityRepository.deleteById(id);
    }
}
