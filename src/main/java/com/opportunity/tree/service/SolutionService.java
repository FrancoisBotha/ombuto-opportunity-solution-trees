package com.opportunity.tree.service;

import com.opportunity.tree.service.dto.SolutionDTO;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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
    Mono<SolutionDTO> save(SolutionDTO solutionDTO);

    /**
     * Updates a solution.
     *
     * @param solutionDTO the entity to update.
     * @return the persisted entity.
     */
    Mono<SolutionDTO> update(SolutionDTO solutionDTO);

    /**
     * Partially updates a solution.
     *
     * @param solutionDTO the entity to update partially.
     * @return the persisted entity.
     */
    Mono<SolutionDTO> partialUpdate(SolutionDTO solutionDTO);

    /**
     * Get all the solutions.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    Flux<SolutionDTO> findAll(Pageable pageable);

    /**
     * Get all the solutions with eager load of many-to-many relationships.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    Flux<SolutionDTO> findAllWithEagerRelationships(Pageable pageable);

    /**
     * Returns the number of solutions available.
     * @return the number of entities in the database.
     *
     */
    Mono<Long> countAll();

    /**
     * Get the "id" solution.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Mono<SolutionDTO> findOne(Long id);

    /**
     * Delete the "id" solution.
     *
     * @param id the id of the entity.
     * @return a Mono to signal the deletion
     */
    Mono<Void> delete(Long id);
}
