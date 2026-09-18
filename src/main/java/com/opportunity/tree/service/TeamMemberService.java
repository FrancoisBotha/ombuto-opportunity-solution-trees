package com.opportunity.tree.service;

import com.opportunity.tree.service.dto.TeamMemberDTO;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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
    Mono<TeamMemberDTO> save(TeamMemberDTO teamMemberDTO);

    /**
     * Updates a teamMember.
     *
     * @param teamMemberDTO the entity to update.
     * @return the persisted entity.
     */
    Mono<TeamMemberDTO> update(TeamMemberDTO teamMemberDTO);

    /**
     * Partially updates a teamMember.
     *
     * @param teamMemberDTO the entity to update partially.
     * @return the persisted entity.
     */
    Mono<TeamMemberDTO> partialUpdate(TeamMemberDTO teamMemberDTO);

    /**
     * Get all the teamMembers.
     *
     * @return the list of entities.
     */
    Flux<TeamMemberDTO> findAll();

    /**
     * Get all the teamMembers with eager load of many-to-many relationships.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    Flux<TeamMemberDTO> findAllWithEagerRelationships(Pageable pageable);

    /**
     * Returns the number of teamMembers available.
     * @return the number of entities in the database.
     *
     */
    Mono<Long> countAll();

    /**
     * Get the "id" teamMember.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Mono<TeamMemberDTO> findOne(Long id);

    /**
     * Delete the "id" teamMember.
     *
     * @param id the id of the entity.
     * @return a Mono to signal the deletion
     */
    Mono<Void> delete(Long id);
}
