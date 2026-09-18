package com.opportunity.tree.service;

import com.opportunity.tree.service.dto.OutcomeDTO;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service Interface for managing {@link com.opportunity.tree.domain.Outcome}.
 */
public interface OutcomeService {
    /**
     * Save a outcome.
     *
     * @param outcomeDTO the entity to save.
     * @return the persisted entity.
     */
    Mono<OutcomeDTO> save(OutcomeDTO outcomeDTO);

    /**
     * Updates a outcome.
     *
     * @param outcomeDTO the entity to update.
     * @return the persisted entity.
     */
    Mono<OutcomeDTO> update(OutcomeDTO outcomeDTO);

    /**
     * Partially updates a outcome.
     *
     * @param outcomeDTO the entity to update partially.
     * @return the persisted entity.
     */
    Mono<OutcomeDTO> partialUpdate(OutcomeDTO outcomeDTO);

    /**
     * Get all the outcomes.
     *
     * @return the list of entities.
     */
    Flux<OutcomeDTO> findAll();

    /**
     * Get all the outcomes with eager load of many-to-many relationships.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    Flux<OutcomeDTO> findAllWithEagerRelationships(Pageable pageable);

    /**
     * Returns the number of outcomes available.
     * @return the number of entities in the database.
     *
     */
    Mono<Long> countAll();

    /**
     * Get the "id" outcome.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Mono<OutcomeDTO> findOne(Long id);

    /**
     * Delete the "id" outcome.
     *
     * @param id the id of the entity.
     * @return a Mono to signal the deletion
     */
    Mono<Void> delete(Long id);
}
