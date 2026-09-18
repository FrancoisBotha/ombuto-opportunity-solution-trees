package com.opportunity.tree.web.rest;

import com.opportunity.tree.repository.SolutionRepository;
import com.opportunity.tree.service.SolutionService;
import com.opportunity.tree.service.dto.SolutionDTO;
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
 * REST controller for managing {@link com.opportunity.tree.domain.Solution}.
 */
@RestController
@RequestMapping("/api/solutions")
public class SolutionResource {

    private static final Logger LOG = LoggerFactory.getLogger(SolutionResource.class);

    private static final String ENTITY_NAME = "solution";

    @Value("${jhipster.clientApp.name:opportunitysolutiontree}")
    private String applicationName;

    private final SolutionService solutionService;

    private final SolutionRepository solutionRepository;

    public SolutionResource(SolutionService solutionService, SolutionRepository solutionRepository) {
        this.solutionService = solutionService;
        this.solutionRepository = solutionRepository;
    }

    /**
     * {@code POST  /solutions} : Create a new solution.
     *
     * @param solutionDTO the solutionDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new solutionDTO, or with status {@code 400 (Bad Request)} if the solution has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public Mono<ResponseEntity<SolutionDTO>> createSolution(@Valid @RequestBody SolutionDTO solutionDTO) throws URISyntaxException {
        LOG.debug("REST request to save Solution : {}", solutionDTO);
        if (solutionDTO.getId() != null) {
            throw new BadRequestAlertException("A new solution cannot already have an ID", ENTITY_NAME, "idexists");
        }
        return solutionService
            .save(solutionDTO)
            .map(result -> {
                try {
                    return ResponseEntity.created(new URI("/api/solutions/" + result.getId()))
                        .headers(HeaderUtil.createEntityCreationAlert(applicationName, false, ENTITY_NAME, result.getId().toString()))
                        .body(result);
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
            });
    }

    /**
     * {@code PUT  /solutions/:id} : Updates an existing solution.
     *
     * @param id the id of the solutionDTO to save.
     * @param solutionDTO the solutionDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated solutionDTO,
     * or with status {@code 400 (Bad Request)} if the solutionDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the solutionDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public Mono<ResponseEntity<SolutionDTO>> updateSolution(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody SolutionDTO solutionDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update Solution : {}, {}", id, solutionDTO);
        if (solutionDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, solutionDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        return solutionRepository
            .existsById(id)
            .flatMap(exists -> {
                if (!exists) {
                    return Mono.error(new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound"));
                }

                return solutionService
                    .update(solutionDTO)
                    .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND)))
                    .map(result ->
                        ResponseEntity.ok()
                            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, false, ENTITY_NAME, result.getId().toString()))
                            .body(result)
                    );
            });
    }

    /**
     * {@code PATCH  /solutions/:id} : Partial updates given fields of an existing solution, field will ignore if it is null
     *
     * @param id the id of the solutionDTO to save.
     * @param solutionDTO the solutionDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated solutionDTO,
     * or with status {@code 400 (Bad Request)} if the solutionDTO is not valid,
     * or with status {@code 404 (Not Found)} if the solutionDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the solutionDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public Mono<ResponseEntity<SolutionDTO>> partialUpdateSolution(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody SolutionDTO solutionDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update Solution partially : {}, {}", id, solutionDTO);
        if (solutionDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, solutionDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        return solutionRepository
            .existsById(id)
            .flatMap(exists -> {
                if (!exists) {
                    return Mono.error(new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound"));
                }

                Mono<SolutionDTO> result = solutionService.partialUpdate(solutionDTO);

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
     * {@code GET  /solutions} : get all the Solutions.
     *
     * @param pageable the pagination information.
     * @param request a {@link ServerHttpRequest} request.
     * @param eagerload flag to eager load entities from relationships (This is applicable for many-to-many).
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Solutions in body.
     */
    @GetMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<List<SolutionDTO>>> getAllSolutions(
        @org.springdoc.core.annotations.ParameterObject Pageable pageable,
        ServerHttpRequest request,
        @RequestParam(name = "eagerload", required = false, defaultValue = "true") boolean eagerload
    ) {
        LOG.debug("REST request to get a page of Solutions");
        return solutionService
            .countAll()
            .zipWith(solutionService.findAll(pageable).collectList())
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
     * {@code GET  /solutions/:id} : get the "id" solution.
     *
     * @param id the id of the solutionDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the solutionDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public Mono<ResponseEntity<SolutionDTO>> getSolution(@PathVariable("id") Long id) {
        LOG.debug("REST request to get Solution : {}", id);
        Mono<SolutionDTO> solutionDTO = solutionService.findOne(id);
        return ResponseUtil.wrapOrNotFound(solutionDTO);
    }

    /**
     * {@code DELETE  /solutions/:id} : delete the "id" solution.
     *
     * @param id the id of the solutionDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> deleteSolution(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete Solution : {}", id);
        return solutionService
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
