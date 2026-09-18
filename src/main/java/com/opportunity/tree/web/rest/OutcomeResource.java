package com.opportunity.tree.web.rest;

import com.opportunity.tree.repository.OutcomeRepository;
import com.opportunity.tree.service.OutcomeService;
import com.opportunity.tree.service.dto.OutcomeDTO;
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
 * REST controller for managing {@link com.opportunity.tree.domain.Outcome}.
 */
@RestController
@RequestMapping("/api/outcomes")
public class OutcomeResource {

    private static final Logger LOG = LoggerFactory.getLogger(OutcomeResource.class);

    private static final String ENTITY_NAME = "outcome";

    @Value("${jhipster.clientApp.name:opportunitysolutiontree}")
    private String applicationName;

    private final OutcomeService outcomeService;

    private final OutcomeRepository outcomeRepository;

    public OutcomeResource(OutcomeService outcomeService, OutcomeRepository outcomeRepository) {
        this.outcomeService = outcomeService;
        this.outcomeRepository = outcomeRepository;
    }

    /**
     * {@code POST  /outcomes} : Create a new outcome.
     *
     * @param outcomeDTO the outcomeDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new outcomeDTO, or with status {@code 400 (Bad Request)} if the outcome has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public Mono<ResponseEntity<OutcomeDTO>> createOutcome(@Valid @RequestBody OutcomeDTO outcomeDTO) throws URISyntaxException {
        LOG.debug("REST request to save Outcome : {}", outcomeDTO);
        if (outcomeDTO.getId() != null) {
            throw new BadRequestAlertException("A new outcome cannot already have an ID", ENTITY_NAME, "idexists");
        }
        return outcomeService
            .save(outcomeDTO)
            .map(result -> {
                try {
                    return ResponseEntity.created(new URI("/api/outcomes/" + result.getId()))
                        .headers(HeaderUtil.createEntityCreationAlert(applicationName, false, ENTITY_NAME, result.getId().toString()))
                        .body(result);
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
            });
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
    public Mono<ResponseEntity<OutcomeDTO>> updateOutcome(
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

        return outcomeRepository
            .existsById(id)
            .flatMap(exists -> {
                if (!exists) {
                    return Mono.error(new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound"));
                }

                return outcomeService
                    .update(outcomeDTO)
                    .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND)))
                    .map(result ->
                        ResponseEntity.ok()
                            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, false, ENTITY_NAME, result.getId().toString()))
                            .body(result)
                    );
            });
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
    public Mono<ResponseEntity<OutcomeDTO>> partialUpdateOutcome(
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

        return outcomeRepository
            .existsById(id)
            .flatMap(exists -> {
                if (!exists) {
                    return Mono.error(new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound"));
                }

                Mono<OutcomeDTO> result = outcomeService.partialUpdate(outcomeDTO);

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
     * {@code GET  /outcomes} : get all the Outcomes.
     *
     * @param eagerload flag to eager load entities from relationships (This is applicable for many-to-many).
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Outcomes in body.
     */
    @GetMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<List<OutcomeDTO>> getAllOutcomes(
        @RequestParam(name = "eagerload", required = false, defaultValue = "true") boolean eagerload
    ) {
        LOG.debug("REST request to get all Outcomes");
        return outcomeService.findAll().collectList();
    }

    /**
     * {@code GET  /outcomes} : get all the Outcomes as a stream.
     * @return the {@link Flux} of Outcomes.
     */
    @GetMapping(value = "", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public Flux<OutcomeDTO> getAllOutcomesAsStream() {
        LOG.debug("REST request to get all Outcomes as a stream");
        return outcomeService.findAll();
    }

    /**
     * {@code GET  /outcomes/:id} : get the "id" outcome.
     *
     * @param id the id of the outcomeDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the outcomeDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public Mono<ResponseEntity<OutcomeDTO>> getOutcome(@PathVariable("id") Long id) {
        LOG.debug("REST request to get Outcome : {}", id);
        Mono<OutcomeDTO> outcomeDTO = outcomeService.findOne(id);
        return ResponseUtil.wrapOrNotFound(outcomeDTO);
    }

    /**
     * {@code DELETE  /outcomes/:id} : delete the "id" outcome.
     *
     * @param id the id of the outcomeDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> deleteOutcome(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete Outcome : {}", id);
        return outcomeService
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
