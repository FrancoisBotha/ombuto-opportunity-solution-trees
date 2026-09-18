package com.opportunity.tree.web.rest;

import com.opportunity.tree.repository.InterviewRepository;
import com.opportunity.tree.security.AuthoritiesConstants;
import com.opportunity.tree.service.InterviewQueryService;
import com.opportunity.tree.service.InterviewService;
import com.opportunity.tree.service.criteria.InterviewCriteria;
import com.opportunity.tree.service.dto.InterviewDTO;
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
 * REST controller for managing {@link com.opportunity.tree.domain.Interview}.
 */
@RestController
@RequestMapping("/api/interviews")
@PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
public class InterviewResource {

    private static final Logger LOG = LoggerFactory.getLogger(InterviewResource.class);

    private static final String ENTITY_NAME = "interview";

    @Value("${jhipster.clientApp.name:opportunitySolutionTree}")
    private String applicationName;

    private final InterviewService interviewService;

    private final InterviewRepository interviewRepository;

    private final InterviewQueryService interviewQueryService;

    public InterviewResource(
        InterviewService interviewService,
        InterviewRepository interviewRepository,
        InterviewQueryService interviewQueryService
    ) {
        this.interviewService = interviewService;
        this.interviewRepository = interviewRepository;
        this.interviewQueryService = interviewQueryService;
    }

    /**
     * {@code POST  /interviews} : Create a new interview.
     *
     * @param interviewDTO the interviewDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new interviewDTO, or with status {@code 400 (Bad Request)} if the interview has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<InterviewDTO> createInterview(@Valid @RequestBody InterviewDTO interviewDTO) throws URISyntaxException {
        LOG.debug("REST request to save Interview : {}", interviewDTO);
        if (interviewDTO.getId() != null) {
            throw new BadRequestAlertException("A new interview cannot already have an ID", ENTITY_NAME, "idexists");
        }
        interviewDTO = interviewService.save(interviewDTO);
        return ResponseEntity.created(new URI("/api/interviews/" + interviewDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, false, ENTITY_NAME, interviewDTO.getId().toString()))
            .body(interviewDTO);
    }

    /**
     * {@code PUT  /interviews/:id} : Updates an existing interview.
     *
     * @param id the id of the interviewDTO to save.
     * @param interviewDTO the interviewDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated interviewDTO,
     * or with status {@code 400 (Bad Request)} if the interviewDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the interviewDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<InterviewDTO> updateInterview(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody InterviewDTO interviewDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update Interview : {}, {}", id, interviewDTO);
        if (interviewDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, interviewDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!interviewRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        interviewDTO = interviewService.update(interviewDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, false, ENTITY_NAME, interviewDTO.getId().toString()))
            .body(interviewDTO);
    }

    /**
     * {@code PATCH  /interviews/:id} : Partial updates given fields of an existing interview, field will ignore if it is null
     *
     * @param id the id of the interviewDTO to save.
     * @param interviewDTO the interviewDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated interviewDTO,
     * or with status {@code 400 (Bad Request)} if the interviewDTO is not valid,
     * or with status {@code 404 (Not Found)} if the interviewDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the interviewDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<InterviewDTO> partialUpdateInterview(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody InterviewDTO interviewDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update Interview partially : {}, {}", id, interviewDTO);
        if (interviewDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, interviewDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!interviewRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<InterviewDTO> result = interviewService.partialUpdate(interviewDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, false, ENTITY_NAME, interviewDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /interviews} : get all the Interviews.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Interviews in body.
     */
    @GetMapping("")
    public ResponseEntity<List<InterviewDTO>> getAllInterviews(
        InterviewCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get Interviews by criteria: {}", criteria);

        Page<InterviewDTO> page = interviewQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /interviews/count} : count all the interviews.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countInterviews(InterviewCriteria criteria) {
        LOG.debug("REST request to count Interviews by criteria: {}", criteria);
        return ResponseEntity.ok().body(interviewQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /interviews/:id} : get the "id" interview.
     *
     * @param id the id of the interviewDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the interviewDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<InterviewDTO> getInterview(@PathVariable("id") Long id) {
        LOG.debug("REST request to get Interview : {}", id);
        Optional<InterviewDTO> interviewDTO = interviewService.findOne(id);
        return ResponseUtil.wrapOrNotFound(interviewDTO);
    }

    /**
     * {@code DELETE  /interviews/:id} : delete the "id" interview.
     *
     * @param id the id of the interviewDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInterview(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete Interview : {}", id);
        interviewService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, false, ENTITY_NAME, id.toString()))
            .build();
    }
}
