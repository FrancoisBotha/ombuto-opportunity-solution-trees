package com.opportunity.tree.service;

import com.opportunity.tree.service.dto.MeetingTranscriptDTO;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service Interface for managing {@link com.opportunity.tree.domain.MeetingTranscript}.
 */
public interface MeetingTranscriptService {
    /**
     * Save a meetingTranscript.
     *
     * @param meetingTranscriptDTO the entity to save.
     * @return the persisted entity.
     */
    MeetingTranscriptDTO save(MeetingTranscriptDTO meetingTranscriptDTO);

    /**
     * Updates a meetingTranscript.
     *
     * @param meetingTranscriptDTO the entity to update.
     * @return the persisted entity.
     */
    MeetingTranscriptDTO update(MeetingTranscriptDTO meetingTranscriptDTO);

    /**
     * Partially updates a meetingTranscript.
     *
     * @param meetingTranscriptDTO the entity to update partially.
     * @return the persisted entity.
     */
    Optional<MeetingTranscriptDTO> partialUpdate(MeetingTranscriptDTO meetingTranscriptDTO);

    /**
     * Get all the meetingTranscripts with eager load of many-to-many relationships.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    Page<MeetingTranscriptDTO> findAllWithEagerRelationships(Pageable pageable);

    /**
     * Get the "id" meetingTranscript.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<MeetingTranscriptDTO> findOne(Long id);

    /**
     * Delete the "id" meetingTranscript.
     *
     * @param id the id of the entity.
     */
    void delete(Long id);
}
