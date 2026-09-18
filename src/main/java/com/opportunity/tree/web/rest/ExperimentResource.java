package com.opportunity.tree.web.rest;

import com.opportunity.tree.repository.ExperimentRepository;
import com.opportunity.tree.security.AuthoritiesConstants;
import com.opportunity.tree.service.ExperimentService;
import com.opportunity.tree.service.dto.ExperimentDTO;
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
 * REST controller for managing {@link com.opportunity.tree.domain.Experiment}.
 */
@RestController
@RequestMapping("/api/experiments")
@PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
public class ExperimentResource {

    private static final Logger LOG = LoggerFactory.getLogger(ExperimentResource.class);

    private static final String ENTITY_NAME = "experiment";

    @Value("${jhipster.clientApp.name:opportunitySolutionTree}")
    private String applicationName;

    private final ExperimentService experimentService;

    private final ExperimentRepository experimentRepository;

    public ExperimentResource(ExperimentService experimentService, ExperimentRepository experimentRepository) {
        this.experimentService = experimentService;
        this.experimentRepository = experimentRepository;
    }

    /**
     * {@code POST  /experiments} : Create a new experiment.
     *
     * @param experimentDTO the experimentDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new experimentDTO, or with status {@code 400 (Bad Request)} if the experiment has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<ExperimentDTO> createExperiment(@Valid @RequestBody ExperimentDTO experimentDTO) throws URISyntaxException {
        LOG.debug("REST request to save Experiment : {}", experimentDTO);
        if (experimentDTO.getId() != null) {
            throw new BadRequestAlertException("A new experiment cannot already have an ID", ENTITY_NAME, "idexists");
        }
        experimentDTO = experimentService.save(experimentDTO);
        return ResponseEntity.created(new URI("/api/experiments/" + experimentDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, false, ENTITY_NAME, experimentDTO.getId().toString()))
            .body(experimentDTO);
    }

    /**
     * {@code PUT  /experiments/:id} : Updates an existing experiment.
     *
     * @param id the id of the experimentDTO to save.
     * @param experimentDTO the experimentDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated experimentDTO,
     * or with status {@code 400 (Bad Request)} if the experimentDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the experimentDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ExperimentDTO> updateExperiment(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody ExperimentDTO experimentDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update Experiment : {}, {}", id, experimentDTO);
        if (experimentDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, experimentDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!experimentRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        experimentDTO = experimentService.update(experimentDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, false, ENTITY_NAME, experimentDTO.getId().toString()))
            .body(experimentDTO);
    }

    /**
     * {@code PATCH  /experiments/:id} : Partial updates given fields of an existing experiment, field will ignore if it is null
     *
     * @param id the id of the experimentDTO to save.
     * @param experimentDTO the experimentDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated experimentDTO,
     * or with status {@code 400 (Bad Request)} if the experimentDTO is not valid,
     * or with status {@code 404 (Not Found)} if the experimentDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the experimentDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<ExperimentDTO> partialUpdateExperiment(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody ExperimentDTO experimentDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update Experiment partially : {}, {}", id, experimentDTO);
        if (experimentDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, experimentDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!experimentRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<ExperimentDTO> result = experimentService.partialUpdate(experimentDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, false, ENTITY_NAME, experimentDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /experiments} : get all the Experiments.
     *
     * @param pageable the pagination information.
     * @param eagerload flag to eager load entities from relationships (This is applicable for many-to-many).
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Experiments in body.
     */
    @GetMapping("")
    public ResponseEntity<List<ExperimentDTO>> getAllExperiments(
        @org.springdoc.core.annotations.ParameterObject Pageable pageable,
        @RequestParam(name = "eagerload", required = false, defaultValue = "true") boolean eagerload
    ) {
        LOG.debug("REST request to get a page of Experiments");
        Page<ExperimentDTO> page;
        if (eagerload) {
            page = experimentService.findAllWithEagerRelationships(pageable);
        } else {
            page = experimentService.findAll(pageable);
        }
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /experiments/:id} : get the "id" experiment.
     *
     * @param id the id of the experimentDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the experimentDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ExperimentDTO> getExperiment(@PathVariable("id") Long id) {
        LOG.debug("REST request to get Experiment : {}", id);
        Optional<ExperimentDTO> experimentDTO = experimentService.findOne(id);
        return ResponseUtil.wrapOrNotFound(experimentDTO);
    }

    /**
     * {@code DELETE  /experiments/:id} : delete the "id" experiment.
     *
     * @param id the id of the experimentDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExperiment(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete Experiment : {}", id);
        experimentService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, false, ENTITY_NAME, id.toString()))
            .build();
    }
}
