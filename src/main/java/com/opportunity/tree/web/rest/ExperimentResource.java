package com.opportunity.tree.web.rest;

import com.opportunity.tree.repository.ExperimentRepository;
import com.opportunity.tree.service.ExperimentService;
import com.opportunity.tree.service.dto.ExperimentDTO;
import com.opportunity.tree.web.rest.errors.BadRequestAlertException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.ForwardedHeaderUtils;
import reactor.core.publisher.Mono;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.PaginationUtil;
import tech.jhipster.web.util.reactive.ResponseUtil;

/**
 * REST controller for managing {@link com.opportunity.tree.domain.Experiment}.
 */
@RestController
@RequestMapping("/api/experiments")
public class ExperimentResource {

    private static final Logger LOG = LoggerFactory.getLogger(ExperimentResource.class);

    private static final String ENTITY_NAME = "experiment";

    @Value("${jhipster.clientApp.name:opportunitysolutiontree}")
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
    public Mono<ResponseEntity<ExperimentDTO>> createExperiment(@Valid @RequestBody ExperimentDTO experimentDTO) throws URISyntaxException {
        LOG.debug("REST request to save Experiment : {}", experimentDTO);
        if (experimentDTO.getId() != null) {
            throw new BadRequestAlertException("A new experiment cannot already have an ID", ENTITY_NAME, "idexists");
        }
        return experimentService
            .save(experimentDTO)
            .map(result -> {
                try {
                    return ResponseEntity.created(new URI("/api/experiments/" + result.getId()))
                        .headers(HeaderUtil.createEntityCreationAlert(applicationName, false, ENTITY_NAME, result.getId().toString()))
                        .body(result);
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
            });
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
    public Mono<ResponseEntity<ExperimentDTO>> updateExperiment(
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

        return experimentRepository
            .existsById(id)
            .flatMap(exists -> {
                if (!exists) {
                    return Mono.error(new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound"));
                }

                return experimentService
                    .update(experimentDTO)
                    .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND)))
                    .map(result ->
                        ResponseEntity.ok()
                            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, false, ENTITY_NAME, result.getId().toString()))
                            .body(result)
                    );
            });
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
    public Mono<ResponseEntity<ExperimentDTO>> partialUpdateExperiment(
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

        return experimentRepository
            .existsById(id)
            .flatMap(exists -> {
                if (!exists) {
                    return Mono.error(new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound"));
                }

                Mono<ExperimentDTO> result = experimentService.partialUpdate(experimentDTO);

                return result
                    .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND)))
                    .map(res ->
                        ResponseEntity.ok()
                            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, false, ENTITY_NAME, res.getId().toString()))
                            .body(res)
                    );
            });
    }

    /**
     * {@code GET  /experiments} : get all the Experiments.
     *
     * @param pageable the pagination information.
     * @param request a {@link ServerHttpRequest} request.
     * @param eagerload flag to eager load entities from relationships (This is applicable for many-to-many).
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Experiments in body.
     */
    @GetMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<List<ExperimentDTO>>> getAllExperiments(
        @org.springdoc.core.annotations.ParameterObject Pageable pageable,
        ServerHttpRequest request,
        @RequestParam(name = "eagerload", required = false, defaultValue = "true") boolean eagerload
    ) {
        LOG.debug("REST request to get a page of Experiments");
        return experimentService
            .countAll()
            .zipWith(experimentService.findAll(pageable).collectList())
            .map(countWithEntities ->
                ResponseEntity.ok()
                    .headers(
                        PaginationUtil.generatePaginationHttpHeaders(
                            ForwardedHeaderUtils.adaptFromForwardedHeaders(request.getURI(), request.getHeaders()),
                            new PageImpl<>(countWithEntities.getT2(), pageable, countWithEntities.getT1())
                        )
                    )
                    .body(countWithEntities.getT2())
            );
    }

    /**
     * {@code GET  /experiments/:id} : get the "id" experiment.
     *
     * @param id the id of the experimentDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the experimentDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public Mono<ResponseEntity<ExperimentDTO>> getExperiment(@PathVariable("id") Long id) {
        LOG.debug("REST request to get Experiment : {}", id);
        Mono<ExperimentDTO> experimentDTO = experimentService.findOne(id);
        return ResponseUtil.wrapOrNotFound(experimentDTO);
    }

    /**
     * {@code DELETE  /experiments/:id} : delete the "id" experiment.
     *
     * @param id the id of the experimentDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> deleteExperiment(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete Experiment : {}", id);
        return experimentService
            .delete(id)
            .then(
                Mono.just(
                    ResponseEntity.noContent()
                        .headers(HeaderUtil.createEntityDeletionAlert(applicationName, false, ENTITY_NAME, id.toString()))
                        .build()
                )
            );
    }
}
