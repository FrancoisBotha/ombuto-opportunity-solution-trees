package com.opportunity.tree.service;

import com.opportunity.tree.service.dto.OutcomeDTO;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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
    OutcomeDTO save(OutcomeDTO outcomeDTO);

    /**
     * Updates a outcome.
     *
     * @param outcomeDTO the entity to update.
     * @return the persisted entity.
     */
    OutcomeDTO update(OutcomeDTO outcomeDTO);

    /**
     * Partially updates a outcome.
     *
     * @param outcomeDTO the entity to update partially.
     * @return the persisted entity.
     */
    Optional<OutcomeDTO> partialUpdate(OutcomeDTO outcomeDTO);

    /**
     * Get all the outcomes with eager load of many-to-many relationships.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    Page<OutcomeDTO> findAllWithEagerRelationships(Pageable pageable);

    /**
     * Get the "id" outcome.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<OutcomeDTO> findOne(Long id);

    /**
     * Delete the "id" outcome.
     *
     * @param id the id of the entity.
     */
    void delete(Long id);
}
