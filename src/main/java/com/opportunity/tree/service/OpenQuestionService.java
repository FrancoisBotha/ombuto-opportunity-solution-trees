package com.opportunity.tree.service;

import com.opportunity.tree.service.dto.OpenQuestionDTO;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service Interface for managing {@link com.opportunity.tree.domain.OpenQuestion}.
 */
public interface OpenQuestionService {
    /**
     * Save a openQuestion.
     *
     * @param openQuestionDTO the entity to save.
     * @return the persisted entity.
     */
    OpenQuestionDTO save(OpenQuestionDTO openQuestionDTO);

    /**
     * Updates a openQuestion.
     *
     * @param openQuestionDTO the entity to update.
     * @return the persisted entity.
     */
    OpenQuestionDTO update(OpenQuestionDTO openQuestionDTO);

    /**
     * Partially updates a openQuestion.
     *
     * @param openQuestionDTO the entity to update partially.
     * @return the persisted entity.
     */
    Optional<OpenQuestionDTO> partialUpdate(OpenQuestionDTO openQuestionDTO);

    /**
     * Get all the openQuestions.
     *
     * @return the list of entities.
     */
    List<OpenQuestionDTO> findAll();

    /**
     * Get all the openQuestions with eager load of many-to-many relationships.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    Page<OpenQuestionDTO> findAllWithEagerRelationships(Pageable pageable);

    /**
     * Get the "id" openQuestion.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<OpenQuestionDTO> findOne(Long id);

    /**
     * Delete the "id" openQuestion.
     *
     * @param id the id of the entity.
     */
    void delete(Long id);
}
