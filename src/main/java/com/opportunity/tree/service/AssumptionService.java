package com.opportunity.tree.service;

import com.opportunity.tree.service.dto.AssumptionDTO;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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
    AssumptionDTO save(AssumptionDTO assumptionDTO);

    /**
     * Updates a assumption.
     *
     * @param assumptionDTO the entity to update.
     * @return the persisted entity.
     */
    AssumptionDTO update(AssumptionDTO assumptionDTO);

    /**
     * Partially updates a assumption.
     *
     * @param assumptionDTO the entity to update partially.
     * @return the persisted entity.
     */
    Optional<AssumptionDTO> partialUpdate(AssumptionDTO assumptionDTO);

    /**
     * Get all the assumptions.
     *
     * @return the list of entities.
     */
    List<AssumptionDTO> findAll();

    /**
     * Get all the assumptions with eager load of many-to-many relationships.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    Page<AssumptionDTO> findAllWithEagerRelationships(Pageable pageable);

    /**
     * Get the "id" assumption.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<AssumptionDTO> findOne(Long id);

    /**
     * Delete the "id" assumption.
     *
     * @param id the id of the entity.
     */
    void delete(Long id);
}
