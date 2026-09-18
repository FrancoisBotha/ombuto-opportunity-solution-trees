package com.opportunity.tree.service;

import com.opportunity.tree.service.dto.ExperimentDTO;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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
    Mono<ExperimentDTO> save(ExperimentDTO experimentDTO);

    /**
     * Updates a experiment.
     *
     * @param experimentDTO the entity to update.
     * @return the persisted entity.
     */
    Mono<ExperimentDTO> update(ExperimentDTO experimentDTO);

    /**
     * Partially updates a experiment.
     *
     * @param experimentDTO the entity to update partially.
     * @return the persisted entity.
     */
    Mono<ExperimentDTO> partialUpdate(ExperimentDTO experimentDTO);

    /**
     * Get all the experiments.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    Flux<ExperimentDTO> findAll(Pageable pageable);

    /**
     * Get all the experiments with eager load of many-to-many relationships.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    Flux<ExperimentDTO> findAllWithEagerRelationships(Pageable pageable);

    /**
     * Returns the number of experiments available.
     * @return the number of entities in the database.
     *
     */
    Mono<Long> countAll();

    /**
     * Get the "id" experiment.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Mono<ExperimentDTO> findOne(Long id);

    /**
     * Delete the "id" experiment.
     *
     * @param id the id of the entity.
     * @return a Mono to signal the deletion
     */
    Mono<Void> delete(Long id);
}
