package com.opportunity.tree.web.rest;

import com.opportunity.tree.repository.OutcomeRepository;
import com.opportunity.tree.security.AuthoritiesConstants;
import com.opportunity.tree.service.OutcomeQueryService;
import com.opportunity.tree.service.OutcomeService;
import com.opportunity.tree.service.criteria.OutcomeCriteria;
import com.opportunity.tree.service.dto.OutcomeDTO;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link com.opportunity.tree.domain.Outcome}.
 */
@RestController
@RequestMapping("/api/outcomes")
@PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
public class OutcomeResource {

    private static final Logger LOG = LoggerFactory.getLogger(OutcomeResource.class);

    private static final String ENTITY_NAME = "outcome";

    @Value("${jhipster.clientApp.name:opportunitySolutionTree}")
    private String applicationName;

    private final OutcomeService outcomeService;

    private final OutcomeRepository outcomeRepository;

    private final OutcomeQueryService outcomeQueryService;

    public OutcomeResource(OutcomeService outcomeService, OutcomeRepository outcomeRepository, OutcomeQueryService outcomeQueryService) {
        this.outcomeService = outcomeService;
        this.outcomeRepository = outcomeRepository;
        this.outcomeQueryService = outcomeQueryService;
    }

    /**
     * {@code POST  /outcomes} : Create a new outcome.
     *
     * @param outcomeDTO the outcomeDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new outcomeDTO, or with status {@code 400 (Bad Request)} if the outcome has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<OutcomeDTO> createOutcome(@Valid @RequestBody OutcomeDTO outcomeDTO) throws URISyntaxException {
        LOG.debug("REST request to save Outcome : {}", outcomeDTO);
        if (outcomeDTO.getId() != null) {
            throw new BadRequestAlertException("A new outcome cannot already have an ID", ENTITY_NAME, "idexists");
        }
        outcomeDTO = outcomeService.save(outcomeDTO);
        return ResponseEntity.created(new URI("/api/outcomes/" + outcomeDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, false, ENTITY_NAME, outcomeDTO.getId().toString()))
            .body(outcomeDTO);
    }

    /**
     * {@code PUT  /outcomes/:id} : Updates an existing outcome.
     *
     * @param id the id of the outcomeDTO to save.
     * @param outcomeDTO the outcomeDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated outcomeDTO,
     * or with status {@code 400 (Bad Request)} if the outcomeDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the outcomeDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<OutcomeDTO> updateOutcome(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody OutcomeDTO outcomeDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update Outcome : {}, {}", id, outcomeDTO);
        if (outcomeDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, outcomeDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!outcomeRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        outcomeDTO = outcomeService.update(outcomeDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, false, ENTITY_NAME, outcomeDTO.getId().toString()))
            .body(outcomeDTO);
    }

    /**
     * {@code PATCH  /outcomes/:id} : Partial updates given fields of an existing outcome, field will ignore if it is null
     *
     * @param id the id of the outcomeDTO to save.
     * @param outcomeDTO the outcomeDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated outcomeDTO,
     * or with status {@code 400 (Bad Request)} if the outcomeDTO is not valid,
     * or with status {@code 404 (Not Found)} if the outcomeDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the outcomeDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<OutcomeDTO> partialUpdateOutcome(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody OutcomeDTO outcomeDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update Outcome partially : {}, {}", id, outcomeDTO);
        if (outcomeDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, outcomeDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!outcomeRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<OutcomeDTO> result = outcomeService.partialUpdate(outcomeDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, false, ENTITY_NAME, outcomeDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /outcomes} : get all the Outcomes.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Outcomes in body.
     */
    @GetMapping("")
    public ResponseEntity<List<OutcomeDTO>> getAllOutcomes(OutcomeCriteria criteria) {
        LOG.debug("REST request to get Outcomes by criteria: {}", criteria);

        List<OutcomeDTO> entityList = outcomeQueryService.findByCriteria(criteria);
        return ResponseEntity.ok().body(entityList);
    }

    /**
     * {@code GET  /outcomes/count} : count all the outcomes.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countOutcomes(OutcomeCriteria criteria) {
        LOG.debug("REST request to count Outcomes by criteria: {}", criteria);
        return ResponseEntity.ok().body(outcomeQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /outcomes/:id} : get the "id" outcome.
     *
     * @param id the id of the outcomeDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the outcomeDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<OutcomeDTO> getOutcome(@PathVariable("id") Long id) {
        LOG.debug("REST request to get Outcome : {}", id);
        Optional<OutcomeDTO> outcomeDTO = outcomeService.findOne(id);
        return ResponseUtil.wrapOrNotFound(outcomeDTO);
    }

    /**
     * {@code DELETE  /outcomes/:id} : delete the "id" outcome.
     *
     * @param id the id of the outcomeDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOutcome(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete Outcome : {}", id);
        outcomeService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, false, ENTITY_NAME, id.toString()))
            .build();
    }
}
