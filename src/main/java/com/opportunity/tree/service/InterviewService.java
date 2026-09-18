package com.opportunity.tree.service;

import com.opportunity.tree.service.dto.InterviewDTO;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service Interface for managing {@link com.opportunity.tree.domain.Interview}.
 */
public interface InterviewService {
    /**
     * Save a interview.
     *
     * @param interviewDTO the entity to save.
     * @return the persisted entity.
     */
    Mono<InterviewDTO> save(InterviewDTO interviewDTO);

    /**
     * Updates a interview.
     *
     * @param interviewDTO the entity to update.
     * @return the persisted entity.
     */
    Mono<InterviewDTO> update(InterviewDTO interviewDTO);

    /**
     * Partially updates a interview.
     *
     * @param interviewDTO the entity to update partially.
     * @return the persisted entity.
     */
    Mono<InterviewDTO> partialUpdate(InterviewDTO interviewDTO);

    /**
     * Get all the interviews.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    Flux<InterviewDTO> findAll(Pageable pageable);

    /**
     * Get all the interviews with eager load of many-to-many relationships.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    Flux<InterviewDTO> findAllWithEagerRelationships(Pageable pageable);

    /**
     * Returns the number of interviews available.
     * @return the number of entities in the database.
     *
     */
    Mono<Long> countAll();

    /**
     * Get the "id" interview.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Mono<InterviewDTO> findOne(Long id);

    /**
     * Delete the "id" interview.
     *
     * @param id the id of the entity.
     * @return a Mono to signal the deletion
     */
    Mono<Void> delete(Long id);
}
