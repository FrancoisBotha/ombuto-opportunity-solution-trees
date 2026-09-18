package com.opportunity.tree.service.impl;

import com.opportunity.tree.domain.OpportunityLink;
import com.opportunity.tree.repository.OpportunityLinkRepository;
import com.opportunity.tree.service.OpportunityLinkService;
import com.opportunity.tree.service.dto.OpportunityLinkDTO;
import com.opportunity.tree.service.mapper.OpportunityLinkMapper;
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
 * Service Implementation for managing {@link com.opportunity.tree.domain.OpportunityLink}.
 */
@Service
@Transactional
public class OpportunityLinkServiceImpl implements OpportunityLinkService {

    private static final Logger LOG = LoggerFactory.getLogger(OpportunityLinkServiceImpl.class);

    private final OpportunityLinkRepository opportunityLinkRepository;

    private final OpportunityLinkMapper opportunityLinkMapper;

    public OpportunityLinkServiceImpl(OpportunityLinkRepository opportunityLinkRepository, OpportunityLinkMapper opportunityLinkMapper) {
        this.opportunityLinkRepository = opportunityLinkRepository;
        this.opportunityLinkMapper = opportunityLinkMapper;
    }

    @Override
    public OpportunityLinkDTO save(OpportunityLinkDTO opportunityLinkDTO) {
        LOG.debug("Request to save OpportunityLink : {}", opportunityLinkDTO);
        OpportunityLink opportunityLink = opportunityLinkMapper.toEntity(opportunityLinkDTO);
        opportunityLink = opportunityLinkRepository.save(opportunityLink);
        return opportunityLinkMapper.toDto(opportunityLink);
    }

    @Override
    public OpportunityLinkDTO update(OpportunityLinkDTO opportunityLinkDTO) {
        LOG.debug("Request to update OpportunityLink : {}", opportunityLinkDTO);
        OpportunityLink opportunityLink = opportunityLinkMapper.toEntity(opportunityLinkDTO);
        opportunityLink = opportunityLinkRepository.save(opportunityLink);
        return opportunityLinkMapper.toDto(opportunityLink);
    }

    @Override
    public Optional<OpportunityLinkDTO> partialUpdate(OpportunityLinkDTO opportunityLinkDTO) {
        LOG.debug("Request to partially update OpportunityLink : {}", opportunityLinkDTO);

        return opportunityLinkRepository
            .findById(opportunityLinkDTO.getId())
            .map(existingOpportunityLink -> {
                opportunityLinkMapper.partialUpdate(existingOpportunityLink, opportunityLinkDTO);

                return existingOpportunityLink;
            })
            .map(opportunityLinkRepository::save)
            .map(opportunityLinkMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OpportunityLinkDTO> findAll() {
        LOG.debug("Request to get all OpportunityLinks");
        return opportunityLinkRepository
            .findAll()
            .stream()
            .map(opportunityLinkMapper::toDto)
            .collect(Collectors.toCollection(LinkedList::new));
    }

    public Page<OpportunityLinkDTO> findAllWithEagerRelationships(Pageable pageable) {
        return opportunityLinkRepository.findAllWithEagerRelationships(pageable).map(opportunityLinkMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OpportunityLinkDTO> findOne(Long id) {
        LOG.debug("Request to get OpportunityLink : {}", id);
        return opportunityLinkRepository.findOneWithEagerRelationships(id).map(opportunityLinkMapper::toDto);
    }

    @Override
    public void delete(Long id) {
        LOG.debug("Request to delete OpportunityLink : {}", id);
        opportunityLinkRepository.deleteById(id);
    }
}
