package com.opportunity.tree.service;

import com.opportunity.tree.service.dto.ExperimentDTO;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service Interface for managing {@link com.opportunity.tree.domain.Experiment}.
 */
public interface ExperimentService {
    /**
     * Save a experiment.
     *
     * @param experimentDTO the entity to save.
     * @return the persisted entity.
     */
    ExperimentDTO save(ExperimentDTO experimentDTO);

    /**
     * Updates a experiment.
     *
     * @param experimentDTO the entity to update.
     * @return the persisted entity.
     */
    ExperimentDTO update(ExperimentDTO experimentDTO);

    /**
     * Partially updates a experiment.
     *
     * @param experimentDTO the entity to update partially.
     * @return the persisted entity.
     */
    Optional<ExperimentDTO> partialUpdate(ExperimentDTO experimentDTO);

    /**
     * Get all the experiments.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    Page<ExperimentDTO> findAll(Pageable pageable);

    /**
     * Get all the experiments with eager load of many-to-many relationships.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    Page<ExperimentDTO> findAllWithEagerRelationships(Pageable pageable);

    /**
     * Get the "id" experiment.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<ExperimentDTO> findOne(Long id);

    /**
     * Delete the "id" experiment.
     *
     * @param id the id of the entity.
     */
    void delete(Long id);
}
