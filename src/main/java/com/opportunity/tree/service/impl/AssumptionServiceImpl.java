package com.opportunity.tree.service.impl;

import com.opportunity.tree.domain.Assumption;
import com.opportunity.tree.repository.AssumptionRepository;
import com.opportunity.tree.service.AssumptionService;
import com.opportunity.tree.service.dto.AssumptionDTO;
import com.opportunity.tree.service.mapper.AssumptionMapper;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public AssumptionDTO save(AssumptionDTO assumptionDTO) {
        LOG.debug("Request to save Assumption : {}", assumptionDTO);
        Assumption assumption = assumptionMapper.toEntity(assumptionDTO);
        assumption = assumptionRepository.save(assumption);
        return assumptionMapper.toDto(assumption);
    }

    @Override
    public AssumptionDTO update(AssumptionDTO assumptionDTO) {
        LOG.debug("Request to update Assumption : {}", assumptionDTO);
        Assumption assumption = assumptionMapper.toEntity(assumptionDTO);
        assumption = assumptionRepository.save(assumption);
        return assumptionMapper.toDto(assumption);
    }

    @Override
    public Optional<AssumptionDTO> partialUpdate(AssumptionDTO assumptionDTO) {
        LOG.debug("Request to partially update Assumption : {}", assumptionDTO);

        return assumptionRepository
            .findById(assumptionDTO.getId())
            .map(existingAssumption -> {
                assumptionMapper.partialUpdate(existingAssumption, assumptionDTO);

                return existingAssumption;
            })
            .map(assumptionRepository::save)
            .map(assumptionMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssumptionDTO> findAll() {
        LOG.debug("Request to get all Assumptions");
        return assumptionRepository.findAll().stream().map(assumptionMapper::toDto).collect(Collectors.toCollection(LinkedList::new));
    }

    public Page<AssumptionDTO> findAllWithEagerRelationships(Pageable pageable) {
        return assumptionRepository.findAllWithEagerRelationships(pageable).map(assumptionMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AssumptionDTO> findOne(Long id) {
        LOG.debug("Request to get Assumption : {}", id);
        return assumptionRepository.findOneWithEagerRelationships(id).map(assumptionMapper::toDto);
    }

    @Override
    public void delete(Long id) {
        LOG.debug("Request to delete Assumption : {}", id);
        assumptionRepository.deleteById(id);
    }
}
