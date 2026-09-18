package com.opportunity.tree.service.impl;

import com.opportunity.tree.repository.OutcomeRepository;
import com.opportunity.tree.service.OutcomeService;
import com.opportunity.tree.service.dto.OutcomeDTO;
import com.opportunity.tree.service.mapper.OutcomeMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service Implementation for managing {@link com.opportunity.tree.domain.Outcome}.
 */
@Service
@Transactional
public class OutcomeServiceImpl implements OutcomeService {

    private static final Logger LOG = LoggerFactory.getLogger(OutcomeServiceImpl.class);

    private final OutcomeRepository outcomeRepository;

    private final OutcomeMapper outcomeMapper;

    public OutcomeServiceImpl(OutcomeRepository outcomeRepository, OutcomeMapper outcomeMapper) {
        this.outcomeRepository = outcomeRepository;
        this.outcomeMapper = outcomeMapper;
    }

    @Override
    public Mono<OutcomeDTO> save(OutcomeDTO outcomeDTO) {
        LOG.debug("Request to save Outcome : {}", outcomeDTO);
        return outcomeRepository.save(outcomeMapper.toEntity(outcomeDTO)).map(outcomeMapper::toDto);
    }

    @Override
    public Mono<OutcomeDTO> update(OutcomeDTO outcomeDTO) {
        LOG.debug("Request to update Outcome : {}", outcomeDTO);
        return outcomeRepository.save(outcomeMapper.toEntity(outcomeDTO)).map(outcomeMapper::toDto);
    }

    @Override
    public Mono<OutcomeDTO> partialUpdate(OutcomeDTO outcomeDTO) {
        LOG.debug("Request to partially update Outcome : {}", outcomeDTO);

        return outcomeRepository
            .findById(outcomeDTO.getId())
            .map(existingOutcome -> {
                outcomeMapper.partialUpdate(existingOutcome, outcomeDTO);

                return existingOutcome;
            })
            .flatMap(outcomeRepository::save)
            .map(outcomeMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Flux<OutcomeDTO> findAll() {
        LOG.debug("Request to get all Outcomes");
        return outcomeRepository.findAll().map(outcomeMapper::toDto);
    }

    public Flux<OutcomeDTO> findAllWithEagerRelationships(Pageable pageable) {
        return outcomeRepository.findAllWithEagerRelationships(pageable).map(outcomeMapper::toDto);
    }

    public Mono<Long> countAll() {
        return outcomeRepository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public Mono<OutcomeDTO> findOne(Long id) {
        LOG.debug("Request to get Outcome : {}", id);
        return outcomeRepository.findOneWithEagerRelationships(id).map(outcomeMapper::toDto);
    }

    @Override
    public Mono<Void> delete(Long id) {
        LOG.debug("Request to delete Outcome : {}", id);
        return outcomeRepository.deleteById(id);
    }
}
