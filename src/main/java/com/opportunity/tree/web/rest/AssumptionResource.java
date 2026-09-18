package com.opportunity.tree.web.rest;

import com.opportunity.tree.repository.AssumptionRepository;
import com.opportunity.tree.security.AuthoritiesConstants;
import com.opportunity.tree.service.AssumptionService;
import com.opportunity.tree.service.dto.AssumptionDTO;
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
 * REST controller for managing {@link com.opportunity.tree.domain.Assumption}.
 */
@RestController
@RequestMapping("/api/assumptions")
@PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
public class AssumptionResource {

    private static final Logger LOG = LoggerFactory.getLogger(AssumptionResource.class);

    private static final String ENTITY_NAME = "assumption";

    @Value("${jhipster.clientApp.name:opportunitySolutionTree}")
    private String applicationName;

    private final AssumptionService assumptionService;

    private final AssumptionRepository assumptionRepository;

    public AssumptionResource(AssumptionService assumptionService, AssumptionRepository assumptionRepository) {
        this.assumptionService = assumptionService;
        this.assumptionRepository = assumptionRepository;
    }

    /**
     * {@code POST  /assumptions} : Create a new assumption.
     *
     * @param assumptionDTO the assumptionDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new assumptionDTO, or with status {@code 400 (Bad Request)} if the assumption has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<AssumptionDTO> createAssumption(@Valid @RequestBody AssumptionDTO assumptionDTO) throws URISyntaxException {
        LOG.debug("REST request to save Assumption : {}", assumptionDTO);
        if (assumptionDTO.getId() != null) {
            throw new BadRequestAlertException("A new assumption cannot already have an ID", ENTITY_NAME, "idexists");
        }
        assumptionDTO = assumptionService.save(assumptionDTO);
        return ResponseEntity.created(new URI("/api/assumptions/" + assumptionDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, false, ENTITY_NAME, assumptionDTO.getId().toString()))
            .body(assumptionDTO);
    }

    /**
     * {@code PUT  /assumptions/:id} : Updates an existing assumption.
     *
     * @param id the id of the assumptionDTO to save.
     * @param assumptionDTO the assumptionDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated assumptionDTO,
     * or with status {@code 400 (Bad Request)} if the assumptionDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the assumptionDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<AssumptionDTO> updateAssumption(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody AssumptionDTO assumptionDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update Assumption : {}, {}", id, assumptionDTO);
        if (assumptionDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, assumptionDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!assumptionRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        assumptionDTO = assumptionService.update(assumptionDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, false, ENTITY_NAME, assumptionDTO.getId().toString()))
            .body(assumptionDTO);
    }

    /**
     * {@code PATCH  /assumptions/:id} : Partial updates given fields of an existing assumption, field will ignore if it is null
     *
     * @param id the id of the assumptionDTO to save.
     * @param assumptionDTO the assumptionDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated assumptionDTO,
     * or with status {@code 400 (Bad Request)} if the assumptionDTO is not valid,
     * or with status {@code 404 (Not Found)} if the assumptionDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the assumptionDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<AssumptionDTO> partialUpdateAssumption(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody AssumptionDTO assumptionDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update Assumption partially : {}, {}", id, assumptionDTO);
        if (assumptionDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, assumptionDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!assumptionRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<AssumptionDTO> result = assumptionService.partialUpdate(assumptionDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, false, ENTITY_NAME, assumptionDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /assumptions} : get all the Assumptions.
     *
     * @param eagerload flag to eager load entities from relationships (This is applicable for many-to-many).
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Assumptions in body.
     */
    @GetMapping("")
    public List<AssumptionDTO> getAllAssumptions(
        @RequestParam(name = "eagerload", required = false, defaultValue = "true") boolean eagerload
    ) {
        LOG.debug("REST request to get all Assumptions");
        return assumptionService.findAll();
    }

    /**
     * {@code GET  /assumptions/:id} : get the "id" assumption.
     *
     * @param id the id of the assumptionDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the assumptionDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<AssumptionDTO> getAssumption(@PathVariable("id") Long id) {
        LOG.debug("REST request to get Assumption : {}", id);
        Optional<AssumptionDTO> assumptionDTO = assumptionService.findOne(id);
        return ResponseUtil.wrapOrNotFound(assumptionDTO);
    }

    /**
     * {@code DELETE  /assumptions/:id} : delete the "id" assumption.
     *
     * @param id the id of the assumptionDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAssumption(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete Assumption : {}", id);
        assumptionService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, false, ENTITY_NAME, id.toString()))
            .build();
    }
}
