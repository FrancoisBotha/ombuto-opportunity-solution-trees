package com.opportunity.tree.service.impl;

import com.opportunity.tree.domain.Experiment;
import com.opportunity.tree.repository.ExperimentRepository;
import com.opportunity.tree.service.ExperimentService;
import com.opportunity.tree.service.dto.ExperimentDTO;
import com.opportunity.tree.service.mapper.ExperimentMapper;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public ExperimentDTO save(ExperimentDTO experimentDTO) {
        LOG.debug("Request to save Experiment : {}", experimentDTO);
        Experiment experiment = experimentMapper.toEntity(experimentDTO);
        experiment = experimentRepository.save(experiment);
        return experimentMapper.toDto(experiment);
    }

    @Override
    public ExperimentDTO update(ExperimentDTO experimentDTO) {
        LOG.debug("Request to update Experiment : {}", experimentDTO);
        Experiment experiment = experimentMapper.toEntity(experimentDTO);
        experiment = experimentRepository.save(experiment);
        return experimentMapper.toDto(experiment);
    }

    @Override
    public Optional<ExperimentDTO> partialUpdate(ExperimentDTO experimentDTO) {
        LOG.debug("Request to partially update Experiment : {}", experimentDTO);

        return experimentRepository
            .findById(experimentDTO.getId())
            .map(existingExperiment -> {
                experimentMapper.partialUpdate(existingExperiment, experimentDTO);

                return existingExperiment;
            })
            .map(experimentRepository::save)
            .map(experimentMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ExperimentDTO> findAll(Pageable pageable) {
        LOG.debug("Request to get all Experiments");
        return experimentRepository.findAll(pageable).map(experimentMapper::toDto);
    }

    public Page<ExperimentDTO> findAllWithEagerRelationships(Pageable pageable) {
        return experimentRepository.findAllWithEagerRelationships(pageable).map(experimentMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ExperimentDTO> findOne(Long id) {
        LOG.debug("Request to get Experiment : {}", id);
        return experimentRepository.findOneWithEagerRelationships(id).map(experimentMapper::toDto);
    }

    @Override
    public void delete(Long id) {
        LOG.debug("Request to delete Experiment : {}", id);
        experimentRepository.deleteById(id);
    }
}
