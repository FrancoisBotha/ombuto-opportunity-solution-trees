package com.opportunity.tree.service.impl;

import com.opportunity.tree.domain.NodeLink;
import com.opportunity.tree.repository.NodeLinkRepository;
import com.opportunity.tree.service.NodeLinkService;
import com.opportunity.tree.service.dto.NodeLinkDTO;
import com.opportunity.tree.service.mapper.NodeLinkMapper;
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
 * Service Implementation for managing {@link com.opportunity.tree.domain.NodeLink}.
 */
@Service
@Transactional
public class NodeLinkServiceImpl implements NodeLinkService {

    private static final Logger LOG = LoggerFactory.getLogger(NodeLinkServiceImpl.class);

    private final NodeLinkRepository nodeLinkRepository;

    private final NodeLinkMapper nodeLinkMapper;

    public NodeLinkServiceImpl(NodeLinkRepository nodeLinkRepository, NodeLinkMapper nodeLinkMapper) {
        this.nodeLinkRepository = nodeLinkRepository;
        this.nodeLinkMapper = nodeLinkMapper;
    }

    @Override
    public NodeLinkDTO save(NodeLinkDTO nodeLinkDTO) {
        LOG.debug("Request to save NodeLink : {}", nodeLinkDTO);
        NodeLink nodeLink = nodeLinkMapper.toEntity(nodeLinkDTO);
        nodeLink = nodeLinkRepository.save(nodeLink);
        return nodeLinkMapper.toDto(nodeLink);
    }

    @Override
    public NodeLinkDTO update(NodeLinkDTO nodeLinkDTO) {
        LOG.debug("Request to update NodeLink : {}", nodeLinkDTO);
        NodeLink nodeLink = nodeLinkMapper.toEntity(nodeLinkDTO);
        nodeLink = nodeLinkRepository.save(nodeLink);
        return nodeLinkMapper.toDto(nodeLink);
    }

    @Override
    public Optional<NodeLinkDTO> partialUpdate(NodeLinkDTO nodeLinkDTO) {
        LOG.debug("Request to partially update NodeLink : {}", nodeLinkDTO);

        return nodeLinkRepository
            .findById(nodeLinkDTO.getId())
            .map(existingNodeLink -> {
                nodeLinkMapper.partialUpdate(existingNodeLink, nodeLinkDTO);

                return existingNodeLink;
            })
            .map(nodeLinkRepository::save)
            .map(nodeLinkMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NodeLinkDTO> findAll() {
        LOG.debug("Request to get all NodeLinks");
        return nodeLinkRepository.findAll().stream().map(nodeLinkMapper::toDto).collect(Collectors.toCollection(LinkedList::new));
    }

    public Page<NodeLinkDTO> findAllWithEagerRelationships(Pageable pageable) {
        return nodeLinkRepository.findAllWithEagerRelationships(pageable).map(nodeLinkMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<NodeLinkDTO> findOne(Long id) {
        LOG.debug("Request to get NodeLink : {}", id);
        return nodeLinkRepository.findOneWithEagerRelationships(id).map(nodeLinkMapper::toDto);
    }

    @Override
    public void delete(Long id) {
        LOG.debug("Request to delete NodeLink : {}", id);
        nodeLinkRepository.deleteById(id);
    }
}
