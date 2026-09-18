package com.opportunity.tree.service;

import com.opportunity.tree.service.dto.OpportunityLinkDTO;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service Interface for managing {@link com.opportunity.tree.domain.OpportunityLink}.
 */
public interface OpportunityLinkService {
    /**
     * Save a opportunityLink.
     *
     * @param opportunityLinkDTO the entity to save.
     * @return the persisted entity.
     */
    Mono<OpportunityLinkDTO> save(OpportunityLinkDTO opportunityLinkDTO);

    /**
     * Updates a opportunityLink.
     *
     * @param opportunityLinkDTO the entity to update.
     * @return the persisted entity.
     */
    Mono<OpportunityLinkDTO> update(OpportunityLinkDTO opportunityLinkDTO);

    /**
     * Partially updates a opportunityLink.
     *
     * @param opportunityLinkDTO the entity to update partially.
     * @return the persisted entity.
     */
    Mono<OpportunityLinkDTO> partialUpdate(OpportunityLinkDTO opportunityLinkDTO);

    /**
     * Get all the opportunityLinks.
     *
     * @return the list of entities.
     */
    Flux<OpportunityLinkDTO> findAll();

    /**
     * Get all the opportunityLinks with eager load of many-to-many relationships.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    Flux<OpportunityLinkDTO> findAllWithEagerRelationships(Pageable pageable);

    /**
     * Returns the number of opportunityLinks available.
     * @return the number of entities in the database.
     *
     */
    Mono<Long> countAll();

    /**
     * Get the "id" opportunityLink.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Mono<OpportunityLinkDTO> findOne(Long id);

    /**
     * Delete the "id" opportunityLink.
     *
     * @param id the id of the entity.
     * @return a Mono to signal the deletion
     */
    Mono<Void> delete(Long id);
}
