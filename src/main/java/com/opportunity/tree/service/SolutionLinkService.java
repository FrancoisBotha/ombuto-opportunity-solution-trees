package com.opportunity.tree.service;

import com.opportunity.tree.service.dto.SolutionLinkDTO;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service Interface for managing {@link com.opportunity.tree.domain.SolutionLink}.
 */
public interface SolutionLinkService {
    /**
     * Save a solutionLink.
     *
     * @param solutionLinkDTO the entity to save.
     * @return the persisted entity.
     */
    SolutionLinkDTO save(SolutionLinkDTO solutionLinkDTO);

    /**
     * Updates a solutionLink.
     *
     * @param solutionLinkDTO the entity to update.
     * @return the persisted entity.
     */
    SolutionLinkDTO update(SolutionLinkDTO solutionLinkDTO);

    /**
     * Partially updates a solutionLink.
     *
     * @param solutionLinkDTO the entity to update partially.
     * @return the persisted entity.
     */
    Optional<SolutionLinkDTO> partialUpdate(SolutionLinkDTO solutionLinkDTO);

    /**
     * Get all the solutionLinks.
     *
     * @return the list of entities.
     */
    List<SolutionLinkDTO> findAll();

    /**
     * Get all the solutionLinks with eager load of many-to-many relationships.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    Page<SolutionLinkDTO> findAllWithEagerRelationships(Pageable pageable);

    /**
     * Get the "id" solutionLink.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<SolutionLinkDTO> findOne(Long id);

    /**
     * Delete the "id" solutionLink.
     *
     * @param id the id of the entity.
     */
    void delete(Long id);
}
