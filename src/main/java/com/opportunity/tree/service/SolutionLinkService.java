package com.opportunity.tree.service;

import com.opportunity.tree.service.dto.SolutionLinkDTO;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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
    Mono<SolutionLinkDTO> save(SolutionLinkDTO solutionLinkDTO);

    /**
     * Updates a solutionLink.
     *
     * @param solutionLinkDTO the entity to update.
     * @return the persisted entity.
     */
    Mono<SolutionLinkDTO> update(SolutionLinkDTO solutionLinkDTO);

    /**
     * Partially updates a solutionLink.
     *
     * @param solutionLinkDTO the entity to update partially.
     * @return the persisted entity.
     */
    Mono<SolutionLinkDTO> partialUpdate(SolutionLinkDTO solutionLinkDTO);

    /**
     * Get all the solutionLinks.
     *
     * @return the list of entities.
     */
    Flux<SolutionLinkDTO> findAll();

    /**
     * Get all the solutionLinks with eager load of many-to-many relationships.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    Flux<SolutionLinkDTO> findAllWithEagerRelationships(Pageable pageable);

    /**
     * Returns the number of solutionLinks available.
     * @return the number of entities in the database.
     *
     */
    Mono<Long> countAll();

    /**
     * Get the "id" solutionLink.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Mono<SolutionLinkDTO> findOne(Long id);

    /**
     * Delete the "id" solutionLink.
     *
     * @param id the id of the entity.
     * @return a Mono to signal the deletion
     */
    Mono<Void> delete(Long id);
}
