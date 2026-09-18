package com.opportunity.tree.service.impl;

import com.opportunity.tree.repository.SolutionLinkRepository;
import com.opportunity.tree.service.SolutionLinkService;
import com.opportunity.tree.service.dto.SolutionLinkDTO;
import com.opportunity.tree.service.mapper.SolutionLinkMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service Implementation for managing {@link com.opportunity.tree.domain.SolutionLink}.
 */
@Service
@Transactional
public class SolutionLinkServiceImpl implements SolutionLinkService {

    private static final Logger LOG = LoggerFactory.getLogger(SolutionLinkServiceImpl.class);

    private final SolutionLinkRepository solutionLinkRepository;

    private final SolutionLinkMapper solutionLinkMapper;

    public SolutionLinkServiceImpl(SolutionLinkRepository solutionLinkRepository, SolutionLinkMapper solutionLinkMapper) {
        this.solutionLinkRepository = solutionLinkRepository;
        this.solutionLinkMapper = solutionLinkMapper;
    }

    @Override
    public Mono<SolutionLinkDTO> save(SolutionLinkDTO solutionLinkDTO) {
        LOG.debug("Request to save SolutionLink : {}", solutionLinkDTO);
        return solutionLinkRepository.save(solutionLinkMapper.toEntity(solutionLinkDTO)).map(solutionLinkMapper::toDto);
    }

    @Override
    public Mono<SolutionLinkDTO> update(SolutionLinkDTO solutionLinkDTO) {
        LOG.debug("Request to update SolutionLink : {}", solutionLinkDTO);
        return solutionLinkRepository.save(solutionLinkMapper.toEntity(solutionLinkDTO)).map(solutionLinkMapper::toDto);
    }

    @Override
    public Mono<SolutionLinkDTO> partialUpdate(SolutionLinkDTO solutionLinkDTO) {
        LOG.debug("Request to partially update SolutionLink : {}", solutionLinkDTO);

        return solutionLinkRepository
            .findById(solutionLinkDTO.getId())
            .map(existingSolutionLink -> {
                solutionLinkMapper.partialUpdate(existingSolutionLink, solutionLinkDTO);

                return existingSolutionLink;
            })
            .flatMap(solutionLinkRepository::save)
            .map(solutionLinkMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Flux<SolutionLinkDTO> findAll() {
        LOG.debug("Request to get all SolutionLinks");
        return solutionLinkRepository.findAll().map(solutionLinkMapper::toDto);
    }

    public Flux<SolutionLinkDTO> findAllWithEagerRelationships(Pageable pageable) {
        return solutionLinkRepository.findAllWithEagerRelationships(pageable).map(solutionLinkMapper::toDto);
    }

    public Mono<Long> countAll() {
        return solutionLinkRepository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public Mono<SolutionLinkDTO> findOne(Long id) {
        LOG.debug("Request to get SolutionLink : {}", id);
        return solutionLinkRepository.findOneWithEagerRelationships(id).map(solutionLinkMapper::toDto);
    }

    @Override
    public Mono<Void> delete(Long id) {
        LOG.debug("Request to delete SolutionLink : {}", id);
        return solutionLinkRepository.deleteById(id);
    }
}
