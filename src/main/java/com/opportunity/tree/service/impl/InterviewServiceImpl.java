package com.opportunity.tree.service.impl;

import com.opportunity.tree.domain.Interview;
import com.opportunity.tree.repository.InterviewRepository;
import com.opportunity.tree.service.InterviewService;
import com.opportunity.tree.service.dto.InterviewDTO;
import com.opportunity.tree.service.mapper.InterviewMapper;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.opportunity.tree.domain.Interview}.
 */
@Service
@Transactional
public class InterviewServiceImpl implements InterviewService {

    private static final Logger LOG = LoggerFactory.getLogger(InterviewServiceImpl.class);

    private final InterviewRepository interviewRepository;

    private final InterviewMapper interviewMapper;

    public InterviewServiceImpl(InterviewRepository interviewRepository, InterviewMapper interviewMapper) {
        this.interviewRepository = interviewRepository;
        this.interviewMapper = interviewMapper;
    }

    @Override
    public InterviewDTO save(InterviewDTO interviewDTO) {
        LOG.debug("Request to save Interview : {}", interviewDTO);
        Interview interview = interviewMapper.toEntity(interviewDTO);
        interview = interviewRepository.save(interview);
        return interviewMapper.toDto(interview);
    }

    @Override
    public InterviewDTO update(InterviewDTO interviewDTO) {
        LOG.debug("Request to update Interview : {}", interviewDTO);
        Interview interview = interviewMapper.toEntity(interviewDTO);
        interview = interviewRepository.save(interview);
        return interviewMapper.toDto(interview);
    }

    @Override
    public Optional<InterviewDTO> partialUpdate(InterviewDTO interviewDTO) {
        LOG.debug("Request to partially update Interview : {}", interviewDTO);

        return interviewRepository
            .findById(interviewDTO.getId())
            .map(existingInterview -> {
                interviewMapper.partialUpdate(existingInterview, interviewDTO);

                return existingInterview;
            })
            .map(interviewRepository::save)
            .map(interviewMapper::toDto);
    }

    public Page<InterviewDTO> findAllWithEagerRelationships(Pageable pageable) {
        return interviewRepository.findAllWithEagerRelationships(pageable).map(interviewMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<InterviewDTO> findOne(Long id) {
        LOG.debug("Request to get Interview : {}", id);
        return interviewRepository.findOneWithEagerRelationships(id).map(interviewMapper::toDto);
    }

    @Override
    public void delete(Long id) {
        LOG.debug("Request to delete Interview : {}", id);
        interviewRepository.deleteById(id);
    }
}
