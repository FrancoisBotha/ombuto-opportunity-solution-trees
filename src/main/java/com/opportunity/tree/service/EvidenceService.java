package com.opportunity.tree.service;

import com.opportunity.tree.service.dto.EvidenceDTO;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service Interface for managing {@link com.opportunity.tree.domain.Evidence}.
 */
public interface EvidenceService {
    /**
     * Save a evidence.
     *
     * @param evidenceDTO the entity to save.
     * @return the persisted entity.
     */
    EvidenceDTO save(EvidenceDTO evidenceDTO);

    /**
     * Updates a evidence.
     *
     * @param evidenceDTO the entity to update.
     * @return the persisted entity.
     */
    EvidenceDTO update(EvidenceDTO evidenceDTO);

    /**
     * Partially updates a evidence.
     *
     * @param evidenceDTO the entity to update partially.
     * @return the persisted entity.
     */
    Optional<EvidenceDTO> partialUpdate(EvidenceDTO evidenceDTO);

    /**
     * Get all the evidences.
     *
     * @return the list of entities.
     */
    List<EvidenceDTO> findAll();

    /**
     * Get all the evidences with eager load of many-to-many relationships.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    Page<EvidenceDTO> findAllWithEagerRelationships(Pageable pageable);

    /**
     * Get the "id" evidence.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<EvidenceDTO> findOne(Long id);

    /**
     * Delete the "id" evidence.
     *
     * @param id the id of the entity.
     */
    void delete(Long id);
}
