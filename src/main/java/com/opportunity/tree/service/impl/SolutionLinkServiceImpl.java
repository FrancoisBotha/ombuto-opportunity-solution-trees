package com.opportunity.tree.service.impl;

import com.opportunity.tree.domain.SolutionLink;
import com.opportunity.tree.repository.SolutionLinkRepository;
import com.opportunity.tree.service.SolutionLinkService;
import com.opportunity.tree.service.dto.SolutionLinkDTO;
import com.opportunity.tree.service.mapper.SolutionLinkMapper;
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
    public SolutionLinkDTO save(SolutionLinkDTO solutionLinkDTO) {
        LOG.debug("Request to save SolutionLink : {}", solutionLinkDTO);
        SolutionLink solutionLink = solutionLinkMapper.toEntity(solutionLinkDTO);
        solutionLink = solutionLinkRepository.save(solutionLink);
        return solutionLinkMapper.toDto(solutionLink);
    }

    @Override
    public SolutionLinkDTO update(SolutionLinkDTO solutionLinkDTO) {
        LOG.debug("Request to update SolutionLink : {}", solutionLinkDTO);
        SolutionLink solutionLink = solutionLinkMapper.toEntity(solutionLinkDTO);
        solutionLink = solutionLinkRepository.save(solutionLink);
        return solutionLinkMapper.toDto(solutionLink);
    }

    @Override
    public Optional<SolutionLinkDTO> partialUpdate(SolutionLinkDTO solutionLinkDTO) {
        LOG.debug("Request to partially update SolutionLink : {}", solutionLinkDTO);

        return solutionLinkRepository
            .findById(solutionLinkDTO.getId())
            .map(existingSolutionLink -> {
                solutionLinkMapper.partialUpdate(existingSolutionLink, solutionLinkDTO);

                return existingSolutionLink;
            })
            .map(solutionLinkRepository::save)
            .map(solutionLinkMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SolutionLinkDTO> findAll() {
        LOG.debug("Request to get all SolutionLinks");
        return solutionLinkRepository.findAll().stream().map(solutionLinkMapper::toDto).collect(Collectors.toCollection(LinkedList::new));
    }

    public Page<SolutionLinkDTO> findAllWithEagerRelationships(Pageable pageable) {
        return solutionLinkRepository.findAllWithEagerRelationships(pageable).map(solutionLinkMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SolutionLinkDTO> findOne(Long id) {
        LOG.debug("Request to get SolutionLink : {}", id);
        return solutionLinkRepository.findOneWithEagerRelationships(id).map(solutionLinkMapper::toDto);
    }

    @Override
    public void delete(Long id) {
        LOG.debug("Request to delete SolutionLink : {}", id);
        solutionLinkRepository.deleteById(id);
    }
}
