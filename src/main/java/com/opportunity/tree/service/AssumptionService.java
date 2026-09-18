package com.opportunity.tree.service;

import com.opportunity.tree.service.dto.AssumptionDTO;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service Interface for managing {@link com.opportunity.tree.domain.Assumption}.
 */
public interface AssumptionService {
    /**
     * Save a assumption.
     *
     * @param assumptionDTO the entity to save.
     * @return the persisted entity.
     */
    Mono<AssumptionDTO> save(AssumptionDTO assumptionDTO);

    /**
     * Updates a assumption.
     *
     * @param assumptionDTO the entity to update.
     * @return the persisted entity.
     */
    Mono<AssumptionDTO> update(AssumptionDTO assumptionDTO);

    /**
     * Partially updates a assumption.
     *
     * @param assumptionDTO the entity to update partially.
     * @return the persisted entity.
     */
    Mono<AssumptionDTO> partialUpdate(AssumptionDTO assumptionDTO);

    /**
     * Get all the assumptions.
     *
     * @return the list of entities.
     */
    Flux<AssumptionDTO> findAll();

    /**
     * Get all the assumptions with eager load of many-to-many relationships.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    Flux<AssumptionDTO> findAllWithEagerRelationships(Pageable pageable);

    /**
     * Returns the number of assumptions available.
     * @return the number of entities in the database.
     *
     */
    Mono<Long> countAll();

    /**
     * Get the "id" assumption.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Mono<AssumptionDTO> findOne(Long id);

    /**
     * Delete the "id" assumption.
     *
     * @param id the id of the entity.
     * @return a Mono to signal the deletion
     */
    Mono<Void> delete(Long id);
}
