package com.opportunity.tree.service;

import com.opportunity.tree.service.dto.OpportunityDTO;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service Interface for managing {@link com.opportunity.tree.domain.Opportunity}.
 */
public interface OpportunityService {
    /**
     * Save a opportunity.
     *
     * @param opportunityDTO the entity to save.
     * @return the persisted entity.
     */
    Mono<OpportunityDTO> save(OpportunityDTO opportunityDTO);

    /**
     * Updates a opportunity.
     *
     * @param opportunityDTO the entity to update.
     * @return the persisted entity.
     */
    Mono<OpportunityDTO> update(OpportunityDTO opportunityDTO);

    /**
     * Partially updates a opportunity.
     *
     * @param opportunityDTO the entity to update partially.
     * @return the persisted entity.
     */
    Mono<OpportunityDTO> partialUpdate(OpportunityDTO opportunityDTO);

    /**
     * Get all the opportunities.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    Flux<OpportunityDTO> findAll(Pageable pageable);

    /**
     * Get all the opportunities with eager load of many-to-many relationships.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    Flux<OpportunityDTO> findAllWithEagerRelationships(Pageable pageable);

    /**
     * Returns the number of opportunities available.
     * @return the number of entities in the database.
     *
     */
    Mono<Long> countAll();

    /**
     * Get the "id" opportunity.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Mono<OpportunityDTO> findOne(Long id);

    /**
     * Delete the "id" opportunity.
     *
     * @param id the id of the entity.
     * @return a Mono to signal the deletion
     */
    Mono<Void> delete(Long id);
}
