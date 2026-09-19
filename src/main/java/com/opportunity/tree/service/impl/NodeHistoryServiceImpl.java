package com.opportunity.tree.service.impl;

import com.opportunity.tree.domain.NodeHistory;
import com.opportunity.tree.repository.NodeHistoryRepository;
import com.opportunity.tree.service.NodeHistoryService;
import com.opportunity.tree.service.dto.NodeHistoryDTO;
import com.opportunity.tree.service.mapper.NodeHistoryMapper;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.opportunity.tree.domain.NodeHistory}.
 */
@Service
@Transactional
public class NodeHistoryServiceImpl implements NodeHistoryService {

    private static final Logger LOG = LoggerFactory.getLogger(NodeHistoryServiceImpl.class);

    private final NodeHistoryRepository nodeHistoryRepository;

    private final NodeHistoryMapper nodeHistoryMapper;

    public NodeHistoryServiceImpl(NodeHistoryRepository nodeHistoryRepository, NodeHistoryMapper nodeHistoryMapper) {
        this.nodeHistoryRepository = nodeHistoryRepository;
        this.nodeHistoryMapper = nodeHistoryMapper;
    }

    @Override
    public NodeHistoryDTO save(NodeHistoryDTO nodeHistoryDTO) {
        LOG.debug("Request to save NodeHistory : {}", nodeHistoryDTO);
        NodeHistory nodeHistory = nodeHistoryMapper.toEntity(nodeHistoryDTO);
        nodeHistory = nodeHistoryRepository.save(nodeHistory);
        return nodeHistoryMapper.toDto(nodeHistory);
    }

    @Override
    public NodeHistoryDTO update(NodeHistoryDTO nodeHistoryDTO) {
        LOG.debug("Request to update NodeHistory : {}", nodeHistoryDTO);
        NodeHistory nodeHistory = nodeHistoryMapper.toEntity(nodeHistoryDTO);
        nodeHistory = nodeHistoryRepository.save(nodeHistory);
        return nodeHistoryMapper.toDto(nodeHistory);
    }

    @Override
    public Optional<NodeHistoryDTO> partialUpdate(NodeHistoryDTO nodeHistoryDTO) {
        LOG.debug("Request to partially update NodeHistory : {}", nodeHistoryDTO);

        return nodeHistoryRepository
            .findById(nodeHistoryDTO.getId())
            .map(existingNodeHistory -> {
                nodeHistoryMapper.partialUpdate(existingNodeHistory, nodeHistoryDTO);

                return existingNodeHistory;
            })
            .map(nodeHistoryRepository::save)
            .map(nodeHistoryMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NodeHistoryDTO> findAll(Pageable pageable) {
        LOG.debug("Request to get all NodeHistories");
        return nodeHistoryRepository.findAll(pageable).map(nodeHistoryMapper::toDto);
    }

    public Page<NodeHistoryDTO> findAllWithEagerRelationships(Pageable pageable) {
        return nodeHistoryRepository.findAllWithEagerRelationships(pageable).map(nodeHistoryMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<NodeHistoryDTO> findOne(Long id) {
        LOG.debug("Request to get NodeHistory : {}", id);
        return nodeHistoryRepository.findOneWithEagerRelationships(id).map(nodeHistoryMapper::toDto);
    }

    @Override
    public void delete(Long id) {
        LOG.debug("Request to delete NodeHistory : {}", id);
        nodeHistoryRepository.deleteById(id);
    }
}
