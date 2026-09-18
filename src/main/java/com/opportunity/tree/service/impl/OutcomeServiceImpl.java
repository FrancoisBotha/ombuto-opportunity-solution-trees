package com.opportunity.tree.service.impl;

import com.opportunity.tree.domain.Outcome;
import com.opportunity.tree.repository.OutcomeRepository;
import com.opportunity.tree.service.OutcomeService;
import com.opportunity.tree.service.dto.OutcomeDTO;
import com.opportunity.tree.service.mapper.OutcomeMapper;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public OutcomeDTO save(OutcomeDTO outcomeDTO) {
        LOG.debug("Request to save Outcome : {}", outcomeDTO);
        Outcome outcome = outcomeMapper.toEntity(outcomeDTO);
        outcome = outcomeRepository.save(outcome);
        return outcomeMapper.toDto(outcome);
    }

    @Override
    public OutcomeDTO update(OutcomeDTO outcomeDTO) {
        LOG.debug("Request to update Outcome : {}", outcomeDTO);
        Outcome outcome = outcomeMapper.toEntity(outcomeDTO);
        outcome = outcomeRepository.save(outcome);
        return outcomeMapper.toDto(outcome);
    }

    @Override
    public Optional<OutcomeDTO> partialUpdate(OutcomeDTO outcomeDTO) {
        LOG.debug("Request to partially update Outcome : {}", outcomeDTO);

        return outcomeRepository
            .findById(outcomeDTO.getId())
            .map(existingOutcome -> {
                outcomeMapper.partialUpdate(existingOutcome, outcomeDTO);

                return existingOutcome;
            })
            .map(outcomeRepository::save)
            .map(outcomeMapper::toDto);
    }

    public Page<OutcomeDTO> findAllWithEagerRelationships(Pageable pageable) {
        return outcomeRepository.findAllWithEagerRelationships(pageable).map(outcomeMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OutcomeDTO> findOne(Long id) {
        LOG.debug("Request to get Outcome : {}", id);
        return outcomeRepository.findOneWithEagerRelationships(id).map(outcomeMapper::toDto);
    }

    @Override
    public void delete(Long id) {
        LOG.debug("Request to delete Outcome : {}", id);
        outcomeRepository.deleteById(id);
    }
}
