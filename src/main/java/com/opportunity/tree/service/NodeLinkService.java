package com.opportunity.tree.service;

import com.opportunity.tree.service.dto.NodeLinkDTO;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service Interface for managing {@link com.opportunity.tree.domain.NodeLink}.
 */
public interface NodeLinkService {
    /**
     * Save a nodeLink.
     *
     * @param nodeLinkDTO the entity to save.
     * @return the persisted entity.
     */
    NodeLinkDTO save(NodeLinkDTO nodeLinkDTO);

    /**
     * Updates a nodeLink.
     *
     * @param nodeLinkDTO the entity to update.
     * @return the persisted entity.
     */
    NodeLinkDTO update(NodeLinkDTO nodeLinkDTO);

    /**
     * Partially updates a nodeLink.
     *
     * @param nodeLinkDTO the entity to update partially.
     * @return the persisted entity.
     */
    Optional<NodeLinkDTO> partialUpdate(NodeLinkDTO nodeLinkDTO);

    /**
     * Get all the nodeLinks.
     *
     * @return the list of entities.
     */
    List<NodeLinkDTO> findAll();

    /**
     * Get all the nodeLinks with eager load of many-to-many relationships.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    Page<NodeLinkDTO> findAllWithEagerRelationships(Pageable pageable);

    /**
     * Get the "id" nodeLink.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<NodeLinkDTO> findOne(Long id);

    /**
     * Delete the "id" nodeLink.
     *
     * @param id the id of the entity.
     */
    void delete(Long id);
}
