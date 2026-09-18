package com.opportunity.tree.web.rest;

import com.opportunity.tree.repository.InterviewRepository;
import com.opportunity.tree.service.InterviewService;
import com.opportunity.tree.service.dto.InterviewDTO;
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
 * REST controller for managing {@link com.opportunity.tree.domain.Interview}.
 */
@RestController
@RequestMapping("/api/interviews")
public class InterviewResource {

    private static final Logger LOG = LoggerFactory.getLogger(InterviewResource.class);

    private static final String ENTITY_NAME = "interview";

    @Value("${jhipster.clientApp.name:opportunitysolutiontree}")
    private String applicationName;

    private final InterviewService interviewService;

    private final InterviewRepository interviewRepository;

    public InterviewResource(InterviewService interviewService, InterviewRepository interviewRepository) {
        this.interviewService = interviewService;
        this.interviewRepository = interviewRepository;
    }

    /**
     * {@code POST  /interviews} : Create a new interview.
     *
     * @param interviewDTO the interviewDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new interviewDTO, or with status {@code 400 (Bad Request)} if the interview has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public Mono<ResponseEntity<InterviewDTO>> createInterview(@Valid @RequestBody InterviewDTO interviewDTO) throws URISyntaxException {
        LOG.debug("REST request to save Interview : {}", interviewDTO);
        if (interviewDTO.getId() != null) {
            throw new BadRequestAlertException("A new interview cannot already have an ID", ENTITY_NAME, "idexists");
        }
        return interviewService
            .save(interviewDTO)
            .map(result -> {
                try {
                    return ResponseEntity.created(new URI("/api/interviews/" + result.getId()))
                        .headers(HeaderUtil.createEntityCreationAlert(applicationName, false, ENTITY_NAME, result.getId().toString()))
                        .body(result);
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
            });
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
    public Mono<ResponseEntity<InterviewDTO>> updateInterview(
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

        return interviewRepository
            .existsById(id)
            .flatMap(exists -> {
                if (!exists) {
                    return Mono.error(new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound"));
                }

                return interviewService
                    .update(interviewDTO)
                    .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND)))
                    .map(result ->
                        ResponseEntity.ok()
                            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, false, ENTITY_NAME, result.getId().toString()))
                            .body(result)
                    );
            });
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
    public Mono<ResponseEntity<InterviewDTO>> partialUpdateInterview(
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

        return interviewRepository
            .existsById(id)
            .flatMap(exists -> {
                if (!exists) {
                    return Mono.error(new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound"));
                }

                Mono<InterviewDTO> result = interviewService.partialUpdate(interviewDTO);

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
     * {@code GET  /interviews} : get all the Interviews.
     *
     * @param pageable the pagination information.
     * @param request a {@link ServerHttpRequest} request.
     * @param eagerload flag to eager load entities from relationships (This is applicable for many-to-many).
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Interviews in body.
     */
    @GetMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<List<InterviewDTO>>> getAllInterviews(
        @org.springdoc.core.annotations.ParameterObject Pageable pageable,
        ServerHttpRequest request,
        @RequestParam(name = "eagerload", required = false, defaultValue = "true") boolean eagerload
    ) {
        LOG.debug("REST request to get a page of Interviews");
        return interviewService
            .countAll()
            .zipWith(interviewService.findAll(pageable).collectList())
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
     * {@code GET  /interviews/:id} : get the "id" interview.
     *
     * @param id the id of the interviewDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the interviewDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public Mono<ResponseEntity<InterviewDTO>> getInterview(@PathVariable("id") Long id) {
        LOG.debug("REST request to get Interview : {}", id);
        Mono<InterviewDTO> interviewDTO = interviewService.findOne(id);
        return ResponseUtil.wrapOrNotFound(interviewDTO);
    }

    /**
     * {@code DELETE  /interviews/:id} : delete the "id" interview.
     *
     * @param id the id of the interviewDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> deleteInterview(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete Interview : {}", id);
        return interviewService
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
