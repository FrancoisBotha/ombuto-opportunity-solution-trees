package com.opportunity.tree.service.impl;

import com.opportunity.tree.repository.AssumptionRepository;
import com.opportunity.tree.service.AssumptionService;
import com.opportunity.tree.service.dto.AssumptionDTO;
import com.opportunity.tree.service.mapper.AssumptionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service Implementation for managing {@link com.opportunity.tree.domain.Assumption}.
 */
@Service
@Transactional
public class AssumptionServiceImpl implements AssumptionService {

    private static final Logger LOG = LoggerFactory.getLogger(AssumptionServiceImpl.class);

    private final AssumptionRepository assumptionRepository;

    private final AssumptionMapper assumptionMapper;

    public AssumptionServiceImpl(AssumptionRepository assumptionRepository, AssumptionMapper assumptionMapper) {
        this.assumptionRepository = assumptionRepository;
        this.assumptionMapper = assumptionMapper;
    }

    @Override
    public Mono<AssumptionDTO> save(AssumptionDTO assumptionDTO) {
        LOG.debug("Request to save Assumption : {}", assumptionDTO);
        return assumptionRepository.save(assumptionMapper.toEntity(assumptionDTO)).map(assumptionMapper::toDto);
    }

    @Override
    public Mono<AssumptionDTO> update(AssumptionDTO assumptionDTO) {
        LOG.debug("Request to update Assumption : {}", assumptionDTO);
        return assumptionRepository.save(assumptionMapper.toEntity(assumptionDTO)).map(assumptionMapper::toDto);
    }

    @Override
    public Mono<AssumptionDTO> partialUpdate(AssumptionDTO assumptionDTO) {
        LOG.debug("Request to partially update Assumption : {}", assumptionDTO);

        return assumptionRepository
            .findById(assumptionDTO.getId())
            .map(existingAssumption -> {
                assumptionMapper.partialUpdate(existingAssumption, assumptionDTO);

                return existingAssumption;
            })
            .flatMap(assumptionRepository::save)
            .map(assumptionMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Flux<AssumptionDTO> findAll() {
        LOG.debug("Request to get all Assumptions");
        return assumptionRepository.findAll().map(assumptionMapper::toDto);
    }

    public Flux<AssumptionDTO> findAllWithEagerRelationships(Pageable pageable) {
        return assumptionRepository.findAllWithEagerRelationships(pageable).map(assumptionMapper::toDto);
    }

    public Mono<Long> countAll() {
        return assumptionRepository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public Mono<AssumptionDTO> findOne(Long id) {
        LOG.debug("Request to get Assumption : {}", id);
        return assumptionRepository.findOneWithEagerRelationships(id).map(assumptionMapper::toDto);
    }

    @Override
    public Mono<Void> delete(Long id) {
        LOG.debug("Request to delete Assumption : {}", id);
        return assumptionRepository.deleteById(id);
    }
}
