package com.opportunity.tree.service;

import com.opportunity.tree.service.dto.SolutionDTO;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service Interface for managing {@link com.opportunity.tree.domain.Solution}.
 */
public interface SolutionService {
    /**
     * Save a solution.
     *
     * @param solutionDTO the entity to save.
     * @return the persisted entity.
     */
    SolutionDTO save(SolutionDTO solutionDTO);

    /**
     * Updates a solution.
     *
     * @param solutionDTO the entity to update.
     * @return the persisted entity.
     */
    SolutionDTO update(SolutionDTO solutionDTO);

    /**
     * Partially updates a solution.
     *
     * @param solutionDTO the entity to update partially.
     * @return the persisted entity.
     */
    Optional<SolutionDTO> partialUpdate(SolutionDTO solutionDTO);

    /**
     * Get all the solutions with eager load of many-to-many relationships.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    Page<SolutionDTO> findAllWithEagerRelationships(Pageable pageable);

    /**
     * Get the "id" solution.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<SolutionDTO> findOne(Long id);

    /**
     * Delete the "id" solution.
     *
     * @param id the id of the entity.
     */
    void delete(Long id);
}
