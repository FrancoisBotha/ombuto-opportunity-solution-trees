package com.opportunity.tree.service.impl;

import com.opportunity.tree.repository.ExperimentRepository;
import com.opportunity.tree.service.ExperimentService;
import com.opportunity.tree.service.dto.ExperimentDTO;
import com.opportunity.tree.service.mapper.ExperimentMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service Implementation for managing {@link com.opportunity.tree.domain.Experiment}.
 */
@Service
@Transactional
public class ExperimentServiceImpl implements ExperimentService {

    private static final Logger LOG = LoggerFactory.getLogger(ExperimentServiceImpl.class);

    private final ExperimentRepository experimentRepository;

    private final ExperimentMapper experimentMapper;

    public ExperimentServiceImpl(ExperimentRepository experimentRepository, ExperimentMapper experimentMapper) {
        this.experimentRepository = experimentRepository;
        this.experimentMapper = experimentMapper;
    }

    @Override
    public Mono<ExperimentDTO> save(ExperimentDTO experimentDTO) {
        LOG.debug("Request to save Experiment : {}", experimentDTO);
        return experimentRepository.save(experimentMapper.toEntity(experimentDTO)).map(experimentMapper::toDto);
    }

    @Override
    public Mono<ExperimentDTO> update(ExperimentDTO experimentDTO) {
        LOG.debug("Request to update Experiment : {}", experimentDTO);
        return experimentRepository.save(experimentMapper.toEntity(experimentDTO)).map(experimentMapper::toDto);
    }

    @Override
    public Mono<ExperimentDTO> partialUpdate(ExperimentDTO experimentDTO) {
        LOG.debug("Request to partially update Experiment : {}", experimentDTO);

        return experimentRepository
            .findById(experimentDTO.getId())
            .map(existingExperiment -> {
                experimentMapper.partialUpdate(existingExperiment, experimentDTO);

                return existingExperiment;
            })
            .flatMap(experimentRepository::save)
            .map(experimentMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Flux<ExperimentDTO> findAll(Pageable pageable) {
        LOG.debug("Request to get all Experiments");
        return experimentRepository.findAllBy(pageable).map(experimentMapper::toDto);
    }

    public Flux<ExperimentDTO> findAllWithEagerRelationships(Pageable pageable) {
        return experimentRepository.findAllWithEagerRelationships(pageable).map(experimentMapper::toDto);
    }

    public Mono<Long> countAll() {
        return experimentRepository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public Mono<ExperimentDTO> findOne(Long id) {
        LOG.debug("Request to get Experiment : {}", id);
        return experimentRepository.findOneWithEagerRelationships(id).map(experimentMapper::toDto);
    }

    @Override
    public Mono<Void> delete(Long id) {
        LOG.debug("Request to delete Experiment : {}", id);
        return experimentRepository.deleteById(id);
    }
}
