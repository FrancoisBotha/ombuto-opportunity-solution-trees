package com.opportunity.tree.web.rest;

import com.opportunity.tree.repository.TeamMemberRepository;
import com.opportunity.tree.service.TeamMemberService;
import com.opportunity.tree.service.dto.TeamMemberDTO;
import com.opportunity.tree.web.rest.errors.BadRequestAlertException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link com.opportunity.tree.domain.TeamMember}.
 */
@RestController
@RequestMapping("/api/team-members")
public class TeamMemberResource {

    private static final Logger LOG = LoggerFactory.getLogger(TeamMemberResource.class);

    private static final String ENTITY_NAME = "teamMember";

    @Value("${jhipster.clientApp.name:opportunitySolutionTree}")
    private String applicationName;

    private final TeamMemberService teamMemberService;

    private final TeamMemberRepository teamMemberRepository;

    public TeamMemberResource(TeamMemberService teamMemberService, TeamMemberRepository teamMemberRepository) {
        this.teamMemberService = teamMemberService;
        this.teamMemberRepository = teamMemberRepository;
    }

    /**
     * {@code POST  /team-members} : Create a new teamMember.
     *
     * @param teamMemberDTO the teamMemberDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new teamMemberDTO, or with status {@code 400 (Bad Request)} if the teamMember has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<TeamMemberDTO> createTeamMember(@Valid @RequestBody TeamMemberDTO teamMemberDTO) throws URISyntaxException {
        LOG.debug("REST request to save TeamMember : {}", teamMemberDTO);
        if (teamMemberDTO.getId() != null) {
            throw new BadRequestAlertException("A new teamMember cannot already have an ID", ENTITY_NAME, "idexists");
        }
        teamMemberDTO = teamMemberService.save(teamMemberDTO);
        return ResponseEntity.created(new URI("/api/team-members/" + teamMemberDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, false, ENTITY_NAME, teamMemberDTO.getId().toString()))
            .body(teamMemberDTO);
    }

    /**
     * {@code PUT  /team-members/:id} : Updates an existing teamMember.
     *
     * @param id the id of the teamMemberDTO to save.
     * @param teamMemberDTO the teamMemberDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated teamMemberDTO,
     * or with status {@code 400 (Bad Request)} if the teamMemberDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the teamMemberDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<TeamMemberDTO> updateTeamMember(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody TeamMemberDTO teamMemberDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update TeamMember : {}, {}", id, teamMemberDTO);
        if (teamMemberDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, teamMemberDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!teamMemberRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        teamMemberDTO = teamMemberService.update(teamMemberDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, false, ENTITY_NAME, teamMemberDTO.getId().toString()))
            .body(teamMemberDTO);
    }

    /**
     * {@code PATCH  /team-members/:id} : Partial updates given fields of an existing teamMember, field will ignore if it is null
     *
     * @param id the id of the teamMemberDTO to save.
     * @param teamMemberDTO the teamMemberDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated teamMemberDTO,
     * or with status {@code 400 (Bad Request)} if the teamMemberDTO is not valid,
     * or with status {@code 404 (Not Found)} if the teamMemberDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the teamMemberDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<TeamMemberDTO> partialUpdateTeamMember(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody TeamMemberDTO teamMemberDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update TeamMember partially : {}, {}", id, teamMemberDTO);
        if (teamMemberDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, teamMemberDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!teamMemberRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<TeamMemberDTO> result = teamMemberService.partialUpdate(teamMemberDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, false, ENTITY_NAME, teamMemberDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /team-members} : get all the Team Members.
     *
     * @param eagerload flag to eager load entities from relationships (This is applicable for many-to-many).
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Team Members in body.
     */
    @GetMapping("")
    public List<TeamMemberDTO> getAllTeamMembers(
        @RequestParam(name = "eagerload", required = false, defaultValue = "true") boolean eagerload
    ) {
        LOG.debug("REST request to get all TeamMembers");
        return teamMemberService.findAll();
    }

    /**
     * {@code GET  /team-members/:id} : get the "id" teamMember.
     *
     * @param id the id of the teamMemberDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the teamMemberDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<TeamMemberDTO> getTeamMember(@PathVariable("id") Long id) {
        LOG.debug("REST request to get TeamMember : {}", id);
        Optional<TeamMemberDTO> teamMemberDTO = teamMemberService.findOne(id);
        return ResponseUtil.wrapOrNotFound(teamMemberDTO);
    }

    /**
     * {@code DELETE  /team-members/:id} : delete the "id" teamMember.
     *
     * @param id the id of the teamMemberDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTeamMember(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete TeamMember : {}", id);
        teamMemberService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, false, ENTITY_NAME, id.toString()))
            .build();
    }
}
