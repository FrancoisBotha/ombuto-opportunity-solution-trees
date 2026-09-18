package com.opportunity.tree.service.impl;

import com.opportunity.tree.repository.OpportunityLinkRepository;
import com.opportunity.tree.service.OpportunityLinkService;
import com.opportunity.tree.service.dto.OpportunityLinkDTO;
import com.opportunity.tree.service.mapper.OpportunityLinkMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service Implementation for managing {@link com.opportunity.tree.domain.OpportunityLink}.
 */
@Service
@Transactional
public class OpportunityLinkServiceImpl implements OpportunityLinkService {

    private static final Logger LOG = LoggerFactory.getLogger(OpportunityLinkServiceImpl.class);

    private final OpportunityLinkRepository opportunityLinkRepository;

    private final OpportunityLinkMapper opportunityLinkMapper;

    public OpportunityLinkServiceImpl(OpportunityLinkRepository opportunityLinkRepository, OpportunityLinkMapper opportunityLinkMapper) {
        this.opportunityLinkRepository = opportunityLinkRepository;
        this.opportunityLinkMapper = opportunityLinkMapper;
    }

    @Override
    public Mono<OpportunityLinkDTO> save(OpportunityLinkDTO opportunityLinkDTO) {
        LOG.debug("Request to save OpportunityLink : {}", opportunityLinkDTO);
        return opportunityLinkRepository.save(opportunityLinkMapper.toEntity(opportunityLinkDTO)).map(opportunityLinkMapper::toDto);
    }

    @Override
    public Mono<OpportunityLinkDTO> update(OpportunityLinkDTO opportunityLinkDTO) {
        LOG.debug("Request to update OpportunityLink : {}", opportunityLinkDTO);
        return opportunityLinkRepository.save(opportunityLinkMapper.toEntity(opportunityLinkDTO)).map(opportunityLinkMapper::toDto);
    }

    @Override
    public Mono<OpportunityLinkDTO> partialUpdate(OpportunityLinkDTO opportunityLinkDTO) {
        LOG.debug("Request to partially update OpportunityLink : {}", opportunityLinkDTO);

        return opportunityLinkRepository
            .findById(opportunityLinkDTO.getId())
            .map(existingOpportunityLink -> {
                opportunityLinkMapper.partialUpdate(existingOpportunityLink, opportunityLinkDTO);

                return existingOpportunityLink;
            })
            .flatMap(opportunityLinkRepository::save)
            .map(opportunityLinkMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Flux<OpportunityLinkDTO> findAll() {
        LOG.debug("Request to get all OpportunityLinks");
        return opportunityLinkRepository.findAll().map(opportunityLinkMapper::toDto);
    }

    public Flux<OpportunityLinkDTO> findAllWithEagerRelationships(Pageable pageable) {
        return opportunityLinkRepository.findAllWithEagerRelationships(pageable).map(opportunityLinkMapper::toDto);
    }

    public Mono<Long> countAll() {
        return opportunityLinkRepository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public Mono<OpportunityLinkDTO> findOne(Long id) {
        LOG.debug("Request to get OpportunityLink : {}", id);
        return opportunityLinkRepository.findOneWithEagerRelationships(id).map(opportunityLinkMapper::toDto);
    }

    @Override
    public Mono<Void> delete(Long id) {
        LOG.debug("Request to delete OpportunityLink : {}", id);
        return opportunityLinkRepository.deleteById(id);
    }
}
