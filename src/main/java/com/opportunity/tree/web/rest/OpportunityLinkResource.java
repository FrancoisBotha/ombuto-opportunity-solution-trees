package com.opportunity.tree.web.rest;

import com.opportunity.tree.repository.OpportunityLinkRepository;
import com.opportunity.tree.service.OpportunityLinkService;
import com.opportunity.tree.service.dto.OpportunityLinkDTO;
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
 * REST controller for managing {@link com.opportunity.tree.domain.OpportunityLink}.
 */
@RestController
@RequestMapping("/api/opportunity-links")
public class OpportunityLinkResource {

    private static final Logger LOG = LoggerFactory.getLogger(OpportunityLinkResource.class);

    private static final String ENTITY_NAME = "opportunityLink";

    @Value("${jhipster.clientApp.name:opportunitysolutiontree}")
    private String applicationName;

    private final OpportunityLinkService opportunityLinkService;

    private final OpportunityLinkRepository opportunityLinkRepository;

    public OpportunityLinkResource(OpportunityLinkService opportunityLinkService, OpportunityLinkRepository opportunityLinkRepository) {
        this.opportunityLinkService = opportunityLinkService;
        this.opportunityLinkRepository = opportunityLinkRepository;
    }

    /**
     * {@code POST  /opportunity-links} : Create a new opportunityLink.
     *
     * @param opportunityLinkDTO the opportunityLinkDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new opportunityLinkDTO, or with status {@code 400 (Bad Request)} if the opportunityLink has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public Mono<ResponseEntity<OpportunityLinkDTO>> createOpportunityLink(@Valid @RequestBody OpportunityLinkDTO opportunityLinkDTO)
        throws URISyntaxException {
        LOG.debug("REST request to save OpportunityLink : {}", opportunityLinkDTO);
        if (opportunityLinkDTO.getId() != null) {
            throw new BadRequestAlertException("A new opportunityLink cannot already have an ID", ENTITY_NAME, "idexists");
        }
        return opportunityLinkService
            .save(opportunityLinkDTO)
            .map(result -> {
                try {
                    return ResponseEntity.created(new URI("/api/opportunity-links/" + result.getId()))
                        .headers(HeaderUtil.createEntityCreationAlert(applicationName, false, ENTITY_NAME, result.getId().toString()))
                        .body(result);
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
            });
    }

    /**
     * {@code PUT  /opportunity-links/:id} : Updates an existing opportunityLink.
     *
     * @param id the id of the opportunityLinkDTO to save.
     * @param opportunityLinkDTO the opportunityLinkDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated opportunityLinkDTO,
     * or with status {@code 400 (Bad Request)} if the opportunityLinkDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the opportunityLinkDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public Mono<ResponseEntity<OpportunityLinkDTO>> updateOpportunityLink(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody OpportunityLinkDTO opportunityLinkDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update OpportunityLink : {}, {}", id, opportunityLinkDTO);
        if (opportunityLinkDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, opportunityLinkDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        return opportunityLinkRepository
            .existsById(id)
            .flatMap(exists -> {
                if (!exists) {
                    return Mono.error(new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound"));
                }

                return opportunityLinkService
                    .update(opportunityLinkDTO)
                    .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND)))
                    .map(result ->
                        ResponseEntity.ok()
                            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, false, ENTITY_NAME, result.getId().toString()))
                            .body(result)
                    );
            });
    }

    /**
     * {@code PATCH  /opportunity-links/:id} : Partial updates given fields of an existing opportunityLink, field will ignore if it is null
     *
     * @param id the id of the opportunityLinkDTO to save.
     * @param opportunityLinkDTO the opportunityLinkDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated opportunityLinkDTO,
     * or with status {@code 400 (Bad Request)} if the opportunityLinkDTO is not valid,
     * or with status {@code 404 (Not Found)} if the opportunityLinkDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the opportunityLinkDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public Mono<ResponseEntity<OpportunityLinkDTO>> partialUpdateOpportunityLink(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody OpportunityLinkDTO opportunityLinkDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update OpportunityLink partially : {}, {}", id, opportunityLinkDTO);
        if (opportunityLinkDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, opportunityLinkDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        return opportunityLinkRepository
            .existsById(id)
            .flatMap(exists -> {
                if (!exists) {
                    return Mono.error(new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound"));
                }

                Mono<OpportunityLinkDTO> result = opportunityLinkService.partialUpdate(opportunityLinkDTO);

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
     * {@code GET  /opportunity-links} : get all the Opportunity Links.
     *
     * @param eagerload flag to eager load entities from relationships (This is applicable for many-to-many).
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Opportunity Links in body.
     */
    @GetMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<List<OpportunityLinkDTO>> getAllOpportunityLinks(
        @RequestParam(name = "eagerload", required = false, defaultValue = "true") boolean eagerload
    ) {
        LOG.debug("REST request to get all OpportunityLinks");
        return opportunityLinkService.findAll().collectList();
    }

    /**
     * {@code GET  /opportunity-links} : get all the Opportunity Links as a stream.
     * @return the {@link Flux} of Opportunity Links.
     */
    @GetMapping(value = "", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public Flux<OpportunityLinkDTO> getAllOpportunityLinksAsStream() {
        LOG.debug("REST request to get all OpportunityLinks as a stream");
        return opportunityLinkService.findAll();
    }

    /**
     * {@code GET  /opportunity-links/:id} : get the "id" opportunityLink.
     *
     * @param id the id of the opportunityLinkDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the opportunityLinkDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public Mono<ResponseEntity<OpportunityLinkDTO>> getOpportunityLink(@PathVariable("id") Long id) {
        LOG.debug("REST request to get OpportunityLink : {}", id);
        Mono<OpportunityLinkDTO> opportunityLinkDTO = opportunityLinkService.findOne(id);
        return ResponseUtil.wrapOrNotFound(opportunityLinkDTO);
    }

    /**
     * {@code DELETE  /opportunity-links/:id} : delete the "id" opportunityLink.
     *
     * @param id the id of the opportunityLinkDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> deleteOpportunityLink(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete OpportunityLink : {}", id);
        return opportunityLinkService
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
