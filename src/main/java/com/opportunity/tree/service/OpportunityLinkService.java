package com.opportunity.tree.service;

import com.opportunity.tree.service.dto.OpportunityLinkDTO;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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
    OpportunityLinkDTO save(OpportunityLinkDTO opportunityLinkDTO);

    /**
     * Updates a opportunityLink.
     *
     * @param opportunityLinkDTO the entity to update.
     * @return the persisted entity.
     */
    OpportunityLinkDTO update(OpportunityLinkDTO opportunityLinkDTO);

    /**
     * Partially updates a opportunityLink.
     *
     * @param opportunityLinkDTO the entity to update partially.
     * @return the persisted entity.
     */
    Optional<OpportunityLinkDTO> partialUpdate(OpportunityLinkDTO opportunityLinkDTO);

    /**
     * Get all the opportunityLinks.
     *
     * @return the list of entities.
     */
    List<OpportunityLinkDTO> findAll();

    /**
     * Get all the opportunityLinks with eager load of many-to-many relationships.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    Page<OpportunityLinkDTO> findAllWithEagerRelationships(Pageable pageable);

    /**
     * Get the "id" opportunityLink.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<OpportunityLinkDTO> findOne(Long id);

    /**
     * Delete the "id" opportunityLink.
     *
     * @param id the id of the entity.
     */
    void delete(Long id);
}
