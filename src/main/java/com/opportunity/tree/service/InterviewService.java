package com.opportunity.tree.service;

import com.opportunity.tree.service.dto.InterviewDTO;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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
    InterviewDTO save(InterviewDTO interviewDTO);

    /**
     * Updates a interview.
     *
     * @param interviewDTO the entity to update.
     * @return the persisted entity.
     */
    InterviewDTO update(InterviewDTO interviewDTO);

    /**
     * Partially updates a interview.
     *
     * @param interviewDTO the entity to update partially.
     * @return the persisted entity.
     */
    Optional<InterviewDTO> partialUpdate(InterviewDTO interviewDTO);

    /**
     * Get all the interviews with eager load of many-to-many relationships.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    Page<InterviewDTO> findAllWithEagerRelationships(Pageable pageable);

    /**
     * Get the "id" interview.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<InterviewDTO> findOne(Long id);

    /**
     * Delete the "id" interview.
     *
     * @param id the id of the entity.
     */
    void delete(Long id);
}
