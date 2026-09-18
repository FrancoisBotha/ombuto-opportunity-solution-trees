package com.opportunity.tree.service;

import com.opportunity.tree.service.dto.OpportunityDTO;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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
    OpportunityDTO save(OpportunityDTO opportunityDTO);

    /**
     * Updates a opportunity.
     *
     * @param opportunityDTO the entity to update.
     * @return the persisted entity.
     */
    OpportunityDTO update(OpportunityDTO opportunityDTO);

    /**
     * Partially updates a opportunity.
     *
     * @param opportunityDTO the entity to update partially.
     * @return the persisted entity.
     */
    Optional<OpportunityDTO> partialUpdate(OpportunityDTO opportunityDTO);

    /**
     * Get all the opportunities with eager load of many-to-many relationships.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    Page<OpportunityDTO> findAllWithEagerRelationships(Pageable pageable);

    /**
     * Get the "id" opportunity.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<OpportunityDTO> findOne(Long id);

    /**
     * Delete the "id" opportunity.
     *
     * @param id the id of the entity.
     */
    void delete(Long id);
}
