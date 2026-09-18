package com.opportunity.tree.service.impl;

import com.opportunity.tree.domain.Solution;
import com.opportunity.tree.repository.SolutionRepository;
import com.opportunity.tree.service.SolutionService;
import com.opportunity.tree.service.dto.SolutionDTO;
import com.opportunity.tree.service.mapper.SolutionMapper;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public SolutionDTO save(SolutionDTO solutionDTO) {
        LOG.debug("Request to save Solution : {}", solutionDTO);
        Solution solution = solutionMapper.toEntity(solutionDTO);
        solution = solutionRepository.save(solution);
        return solutionMapper.toDto(solution);
    }

    @Override
    public SolutionDTO update(SolutionDTO solutionDTO) {
        LOG.debug("Request to update Solution : {}", solutionDTO);
        Solution solution = solutionMapper.toEntity(solutionDTO);
        solution = solutionRepository.save(solution);
        return solutionMapper.toDto(solution);
    }

    @Override
    public Optional<SolutionDTO> partialUpdate(SolutionDTO solutionDTO) {
        LOG.debug("Request to partially update Solution : {}", solutionDTO);

        return solutionRepository
            .findById(solutionDTO.getId())
            .map(existingSolution -> {
                solutionMapper.partialUpdate(existingSolution, solutionDTO);

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
        return solutionRepository.findOneWithEagerRelationships(id).map(solutionMapper::toDto);
    }

    @Override
    public void delete(Long id) {
        LOG.debug("Request to delete Solution : {}", id);
        solutionRepository.deleteById(id);
    }
}
