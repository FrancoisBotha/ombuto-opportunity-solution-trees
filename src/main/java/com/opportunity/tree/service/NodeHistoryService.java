package com.opportunity.tree.service;

import com.opportunity.tree.service.dto.NodeHistoryDTO;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service Interface for managing {@link com.opportunity.tree.domain.NodeHistory}.
 */
public interface NodeHistoryService {
    /**
     * Save a nodeHistory.
     *
     * @param nodeHistoryDTO the entity to save.
     * @return the persisted entity.
     */
    NodeHistoryDTO save(NodeHistoryDTO nodeHistoryDTO);

    /**
     * Updates a nodeHistory.
     *
     * @param nodeHistoryDTO the entity to update.
     * @return the persisted entity.
     */
    NodeHistoryDTO update(NodeHistoryDTO nodeHistoryDTO);

    /**
     * Partially updates a nodeHistory.
     *
     * @param nodeHistoryDTO the entity to update partially.
     * @return the persisted entity.
     */
    Optional<NodeHistoryDTO> partialUpdate(NodeHistoryDTO nodeHistoryDTO);

    /**
     * Get all the nodeHistories.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    Page<NodeHistoryDTO> findAll(Pageable pageable);

    /**
     * Get all the nodeHistories with eager load of many-to-many relationships.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    Page<NodeHistoryDTO> findAllWithEagerRelationships(Pageable pageable);

    /**
     * Get the "id" nodeHistory.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<NodeHistoryDTO> findOne(Long id);

    /**
     * Delete the "id" nodeHistory.
     *
     * @param id the id of the entity.
     */
    void delete(Long id);
}
