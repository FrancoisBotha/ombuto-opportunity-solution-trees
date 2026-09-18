package com.opportunity.tree.web.rest;

import com.opportunity.tree.repository.SolutionLinkRepository;
import com.opportunity.tree.service.SolutionLinkService;
import com.opportunity.tree.service.dto.SolutionLinkDTO;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.reactive.ResponseUtil;

/**
 * REST controller for managing {@link com.opportunity.tree.domain.SolutionLink}.
 */
@RestController
@RequestMapping("/api/solution-links")
public class SolutionLinkResource {

    private static final Logger LOG = LoggerFactory.getLogger(SolutionLinkResource.class);

    private static final String ENTITY_NAME = "solutionLink";

    @Value("${jhipster.clientApp.name:opportunitysolutiontree}")
    private String applicationName;

    private final SolutionLinkService solutionLinkService;

    private final SolutionLinkRepository solutionLinkRepository;

    public SolutionLinkResource(SolutionLinkService solutionLinkService, SolutionLinkRepository solutionLinkRepository) {
        this.solutionLinkService = solutionLinkService;
        this.solutionLinkRepository = solutionLinkRepository;
    }

    /**
     * {@code POST  /solution-links} : Create a new solutionLink.
     *
     * @param solutionLinkDTO the solutionLinkDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new solutionLinkDTO, or with status {@code 400 (Bad Request)} if the solutionLink has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public Mono<ResponseEntity<SolutionLinkDTO>> createSolutionLink(@Valid @RequestBody SolutionLinkDTO solutionLinkDTO)
        throws URISyntaxException {
        LOG.debug("REST request to save SolutionLink : {}", solutionLinkDTO);
        if (solutionLinkDTO.getId() != null) {
            throw new BadRequestAlertException("A new solutionLink cannot already have an ID", ENTITY_NAME, "idexists");
        }
        return solutionLinkService
            .save(solutionLinkDTO)
            .map(result -> {
                try {
                    return ResponseEntity.created(new URI("/api/solution-links/" + result.getId()))
                        .headers(HeaderUtil.createEntityCreationAlert(applicationName, false, ENTITY_NAME, result.getId().toString()))
                        .body(result);
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
            });
    }

    /**
     * {@code PUT  /solution-links/:id} : Updates an existing solutionLink.
     *
     * @param id the id of the solutionLinkDTO to save.
     * @param solutionLinkDTO the solutionLinkDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated solutionLinkDTO,
     * or with status {@code 400 (Bad Request)} if the solutionLinkDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the solutionLinkDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public Mono<ResponseEntity<SolutionLinkDTO>> updateSolutionLink(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody SolutionLinkDTO solutionLinkDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update SolutionLink : {}, {}", id, solutionLinkDTO);
        if (solutionLinkDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, solutionLinkDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        return solutionLinkRepository
            .existsById(id)
            .flatMap(exists -> {
                if (!exists) {
                    return Mono.error(new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound"));
                }

                return solutionLinkService
                    .update(solutionLinkDTO)
                    .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND)))
                    .map(result ->
                        ResponseEntity.ok()
                            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, false, ENTITY_NAME, result.getId().toString()))
                            .body(result)
                    );
            });
    }

    /**
     * {@code PATCH  /solution-links/:id} : Partial updates given fields of an existing solutionLink, field will ignore if it is null
     *
     * @param id the id of the solutionLinkDTO to save.
     * @param solutionLinkDTO the solutionLinkDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated solutionLinkDTO,
     * or with status {@code 400 (Bad Request)} if the solutionLinkDTO is not valid,
     * or with status {@code 404 (Not Found)} if the solutionLinkDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the solutionLinkDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public Mono<ResponseEntity<SolutionLinkDTO>> partialUpdateSolutionLink(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody SolutionLinkDTO solutionLinkDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update SolutionLink partially : {}, {}", id, solutionLinkDTO);
        if (solutionLinkDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, solutionLinkDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        return solutionLinkRepository
            .existsById(id)
            .flatMap(exists -> {
                if (!exists) {
                    return Mono.error(new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound"));
                }

                Mono<SolutionLinkDTO> result = solutionLinkService.partialUpdate(solutionLinkDTO);

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
     * {@code GET  /solution-links} : get all the Solution Links.
     *
     * @param eagerload flag to eager load entities from relationships (This is applicable for many-to-many).
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Solution Links in body.
     */
    @GetMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<List<SolutionLinkDTO>> getAllSolutionLinks(
        @RequestParam(name = "eagerload", required = false, defaultValue = "true") boolean eagerload
    ) {
        LOG.debug("REST request to get all SolutionLinks");
        return solutionLinkService.findAll().collectList();
    }

    /**
     * {@code GET  /solution-links} : get all the Solution Links as a stream.
     * @return the {@link Flux} of Solution Links.
     */
    @GetMapping(value = "", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public Flux<SolutionLinkDTO> getAllSolutionLinksAsStream() {
        LOG.debug("REST request to get all SolutionLinks as a stream");
        return solutionLinkService.findAll();
    }

    /**
     * {@code GET  /solution-links/:id} : get the "id" solutionLink.
     *
     * @param id the id of the solutionLinkDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the solutionLinkDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public Mono<ResponseEntity<SolutionLinkDTO>> getSolutionLink(@PathVariable("id") Long id) {
        LOG.debug("REST request to get SolutionLink : {}", id);
        Mono<SolutionLinkDTO> solutionLinkDTO = solutionLinkService.findOne(id);
        return ResponseUtil.wrapOrNotFound(solutionLinkDTO);
    }

    /**
     * {@code DELETE  /solution-links/:id} : delete the "id" solutionLink.
     *
     * @param id the id of the solutionLinkDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> deleteSolutionLink(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete SolutionLink : {}", id);
        return solutionLinkService
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
