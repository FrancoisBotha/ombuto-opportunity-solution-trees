package com.opportunity.tree.service;

import com.opportunity.tree.service.dto.TeamMemberDTO;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service Interface for managing {@link com.opportunity.tree.domain.TeamMember}.
 */
public interface TeamMemberService {
    /**
     * Save a teamMember.
     *
     * @param teamMemberDTO the entity to save.
     * @return the persisted entity.
     */
    TeamMemberDTO save(TeamMemberDTO teamMemberDTO);

    /**
     * Updates a teamMember.
     *
     * @param teamMemberDTO the entity to update.
     * @return the persisted entity.
     */
    TeamMemberDTO update(TeamMemberDTO teamMemberDTO);

    /**
     * Partially updates a teamMember.
     *
     * @param teamMemberDTO the entity to update partially.
     * @return the persisted entity.
     */
    Optional<TeamMemberDTO> partialUpdate(TeamMemberDTO teamMemberDTO);

    /**
     * Get all the teamMembers.
     *
     * @return the list of entities.
     */
    List<TeamMemberDTO> findAll();

    /**
     * Get all the teamMembers with eager load of many-to-many relationships.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    Page<TeamMemberDTO> findAllWithEagerRelationships(Pageable pageable);

    /**
     * Get the "id" teamMember.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<TeamMemberDTO> findOne(Long id);

    /**
     * Delete the "id" teamMember.
     *
     * @param id the id of the entity.
     */
    void delete(Long id);
}
