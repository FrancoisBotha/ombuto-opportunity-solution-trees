package com.opportunity.tree.service.impl;

import com.opportunity.tree.repository.SolutionRepository;
import com.opportunity.tree.service.SolutionService;
import com.opportunity.tree.service.dto.SolutionDTO;
import com.opportunity.tree.service.mapper.SolutionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service Implementation for managing {@link com.opportunity.tree.domain.Solution}.
 */
@Service
@Transactional
public class SolutionServiceImpl implements SolutionService {

    private static final Logger LOG = LoggerFactory.getLogger(SolutionServiceImpl.class);

    private final SolutionRepository solutionRepository;

    private final SolutionMapper solutionMapper;

    public SolutionServiceImpl(SolutionRepository solutionRepository, SolutionMapper solutionMapper) {
        this.solutionRepository = solutionRepository;
        this.solutionMapper = solutionMapper;
    }

    @Override
    public Mono<SolutionDTO> save(SolutionDTO solutionDTO) {
        LOG.debug("Request to save Solution : {}", solutionDTO);
        return solutionRepository.save(solutionMapper.toEntity(solutionDTO)).map(solutionMapper::toDto);
    }

    @Override
    public Mono<SolutionDTO> update(SolutionDTO solutionDTO) {
        LOG.debug("Request to update Solution : {}", solutionDTO);
        return solutionRepository.save(solutionMapper.toEntity(solutionDTO)).map(solutionMapper::toDto);
    }

    @Override
    public Mono<SolutionDTO> partialUpdate(SolutionDTO solutionDTO) {
        LOG.debug("Request to partially update Solution : {}", solutionDTO);

        return solutionRepository
            .findById(solutionDTO.getId())
            .map(existingSolution -> {
                solutionMapper.partialUpdate(existingSolution, solutionDTO);

                return existingSolution;
            })
            .flatMap(solutionRepository::save)
            .map(solutionMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Flux<SolutionDTO> findAll(Pageable pageable) {
        LOG.debug("Request to get all Solutions");
        return solutionRepository.findAllBy(pageable).map(solutionMapper::toDto);
    }

    public Flux<SolutionDTO> findAllWithEagerRelationships(Pageable pageable) {
        return solutionRepository.findAllWithEagerRelationships(pageable).map(solutionMapper::toDto);
    }

    public Mono<Long> countAll() {
        return solutionRepository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public Mono<SolutionDTO> findOne(Long id) {
        LOG.debug("Request to get Solution : {}", id);
        return solutionRepository.findOneWithEagerRelationships(id).map(solutionMapper::toDto);
    }

    @Override
    public Mono<Void> delete(Long id) {
        LOG.debug("Request to delete Solution : {}", id);
        return solutionRepository.deleteById(id);
    }
}
