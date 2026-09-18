package com.opportunity.tree.service.impl;

import com.opportunity.tree.repository.InterviewRepository;
import com.opportunity.tree.service.InterviewService;
import com.opportunity.tree.service.dto.InterviewDTO;
import com.opportunity.tree.service.mapper.InterviewMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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
    public Mono<InterviewDTO> save(InterviewDTO interviewDTO) {
        LOG.debug("Request to save Interview : {}", interviewDTO);
        return interviewRepository.save(interviewMapper.toEntity(interviewDTO)).map(interviewMapper::toDto);
    }

    @Override
    public Mono<InterviewDTO> update(InterviewDTO interviewDTO) {
        LOG.debug("Request to update Interview : {}", interviewDTO);
        return interviewRepository.save(interviewMapper.toEntity(interviewDTO)).map(interviewMapper::toDto);
    }

    @Override
    public Mono<InterviewDTO> partialUpdate(InterviewDTO interviewDTO) {
        LOG.debug("Request to partially update Interview : {}", interviewDTO);

        return interviewRepository
            .findById(interviewDTO.getId())
            .map(existingInterview -> {
                interviewMapper.partialUpdate(existingInterview, interviewDTO);

                return existingInterview;
            })
            .flatMap(interviewRepository::save)
            .map(interviewMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Flux<InterviewDTO> findAll(Pageable pageable) {
        LOG.debug("Request to get all Interviews");
        return interviewRepository.findAllBy(pageable).map(interviewMapper::toDto);
    }

    public Flux<InterviewDTO> findAllWithEagerRelationships(Pageable pageable) {
        return interviewRepository.findAllWithEagerRelationships(pageable).map(interviewMapper::toDto);
    }

    public Mono<Long> countAll() {
        return interviewRepository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public Mono<InterviewDTO> findOne(Long id) {
        LOG.debug("Request to get Interview : {}", id);
        return interviewRepository.findOneWithEagerRelationships(id).map(interviewMapper::toDto);
    }

    @Override
    public Mono<Void> delete(Long id) {
        LOG.debug("Request to delete Interview : {}", id);
        return interviewRepository.deleteById(id);
    }
}
