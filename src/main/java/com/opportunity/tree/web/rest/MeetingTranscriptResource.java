package com.opportunity.tree.web.rest;

import com.opportunity.tree.repository.MeetingTranscriptRepository;
import com.opportunity.tree.security.AuthoritiesConstants;
import com.opportunity.tree.service.MeetingTranscriptQueryService;
import com.opportunity.tree.service.MeetingTranscriptService;
import com.opportunity.tree.service.criteria.MeetingTranscriptCriteria;
import com.opportunity.tree.service.dto.MeetingTranscriptDTO;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.PaginationUtil;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link com.opportunity.tree.domain.MeetingTranscript}.
 */
@RestController
@RequestMapping("/api/meeting-transcripts")
@PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
public class MeetingTranscriptResource {

    private static final Logger LOG = LoggerFactory.getLogger(MeetingTranscriptResource.class);

    private static final String ENTITY_NAME = "meetingTranscript";

    @Value("${jhipster.clientApp.name:opportunitySolutionTree}")
    private String applicationName;

    private final MeetingTranscriptService meetingTranscriptService;

    private final MeetingTranscriptRepository meetingTranscriptRepository;

    private final MeetingTranscriptQueryService meetingTranscriptQueryService;

    public MeetingTranscriptResource(
        MeetingTranscriptService meetingTranscriptService,
        MeetingTranscriptRepository meetingTranscriptRepository,
        MeetingTranscriptQueryService meetingTranscriptQueryService
    ) {
        this.meetingTranscriptService = meetingTranscriptService;
        this.meetingTranscriptRepository = meetingTranscriptRepository;
        this.meetingTranscriptQueryService = meetingTranscriptQueryService;
    }

    /**
     * {@code POST  /meeting-transcripts} : Create a new meetingTranscript.
     *
     * @param meetingTranscriptDTO the meetingTranscriptDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new meetingTranscriptDTO, or with status {@code 400 (Bad Request)} if the meetingTranscript has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<MeetingTranscriptDTO> createMeetingTranscript(@Valid @RequestBody MeetingTranscriptDTO meetingTranscriptDTO)
        throws URISyntaxException {
        LOG.debug("REST request to save MeetingTranscript : {}", meetingTranscriptDTO);
        if (meetingTranscriptDTO.getId() != null) {
            throw new BadRequestAlertException("A new meetingTranscript cannot already have an ID", ENTITY_NAME, "idexists");
        }
        meetingTranscriptDTO = meetingTranscriptService.save(meetingTranscriptDTO);
        return ResponseEntity.created(new URI("/api/meeting-transcripts/" + meetingTranscriptDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, false, ENTITY_NAME, meetingTranscriptDTO.getId().toString()))
            .body(meetingTranscriptDTO);
    }

    /**
     * {@code PUT  /meeting-transcripts/:id} : Updates an existing meetingTranscript.
     *
     * @param id the id of the meetingTranscriptDTO to save.
     * @param meetingTranscriptDTO the meetingTranscriptDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated meetingTranscriptDTO,
     * or with status {@code 400 (Bad Request)} if the meetingTranscriptDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the meetingTranscriptDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<MeetingTranscriptDTO> updateMeetingTranscript(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody MeetingTranscriptDTO meetingTranscriptDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update MeetingTranscript : {}, {}", id, meetingTranscriptDTO);
        if (meetingTranscriptDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, meetingTranscriptDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!meetingTranscriptRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        meetingTranscriptDTO = meetingTranscriptService.update(meetingTranscriptDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, false, ENTITY_NAME, meetingTranscriptDTO.getId().toString()))
            .body(meetingTranscriptDTO);
    }

    /**
     * {@code PATCH  /meeting-transcripts/:id} : Partial updates given fields of an existing meetingTranscript, field will ignore if it is null
     *
     * @param id the id of the meetingTranscriptDTO to save.
     * @param meetingTranscriptDTO the meetingTranscriptDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated meetingTranscriptDTO,
     * or with status {@code 400 (Bad Request)} if the meetingTranscriptDTO is not valid,
     * or with status {@code 404 (Not Found)} if the meetingTranscriptDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the meetingTranscriptDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<MeetingTranscriptDTO> partialUpdateMeetingTranscript(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody MeetingTranscriptDTO meetingTranscriptDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update MeetingTranscript partially : {}, {}", id, meetingTranscriptDTO);
        if (meetingTranscriptDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, meetingTranscriptDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!meetingTranscriptRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<MeetingTranscriptDTO> result = meetingTranscriptService.partialUpdate(meetingTranscriptDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, false, ENTITY_NAME, meetingTranscriptDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /meeting-transcripts} : get all the Meeting Transcripts.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Meeting Transcripts in body.
     */
    @GetMapping("")
    public ResponseEntity<List<MeetingTranscriptDTO>> getAllMeetingTranscripts(
        MeetingTranscriptCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get MeetingTranscripts by criteria: {}", criteria);

        Page<MeetingTranscriptDTO> page = meetingTranscriptQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /meeting-transcripts/count} : count all the meetingTranscripts.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countMeetingTranscripts(MeetingTranscriptCriteria criteria) {
        LOG.debug("REST request to count MeetingTranscripts by criteria: {}", criteria);
        return ResponseEntity.ok().body(meetingTranscriptQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /meeting-transcripts/:id} : get the "id" meetingTranscript.
     *
     * @param id the id of the meetingTranscriptDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the meetingTranscriptDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<MeetingTranscriptDTO> getMeetingTranscript(@PathVariable("id") Long id) {
        LOG.debug("REST request to get MeetingTranscript : {}", id);
        Optional<MeetingTranscriptDTO> meetingTranscriptDTO = meetingTranscriptService.findOne(id);
        return ResponseUtil.wrapOrNotFound(meetingTranscriptDTO);
    }

    /**
     * {@code DELETE  /meeting-transcripts/:id} : delete the "id" meetingTranscript.
     *
     * @param id the id of the meetingTranscriptDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMeetingTranscript(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete MeetingTranscript : {}", id);
        meetingTranscriptService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, false, ENTITY_NAME, id.toString()))
            .build();
    }
}
