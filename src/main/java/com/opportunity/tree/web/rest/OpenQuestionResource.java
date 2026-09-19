package com.opportunity.tree.web.rest;

import com.opportunity.tree.repository.OpenQuestionRepository;
import com.opportunity.tree.security.AuthoritiesConstants;
import com.opportunity.tree.service.OpenQuestionService;
import com.opportunity.tree.service.dto.OpenQuestionDTO;
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
 * REST controller for managing {@link com.opportunity.tree.domain.OpenQuestion}.
 */
@RestController
@RequestMapping("/api/open-questions")
@PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
public class OpenQuestionResource {

    private static final Logger LOG = LoggerFactory.getLogger(OpenQuestionResource.class);

    private static final String ENTITY_NAME = "openQuestion";

    @Value("${jhipster.clientApp.name:opportunitySolutionTree}")
    private String applicationName;

    private final OpenQuestionService openQuestionService;

    private final OpenQuestionRepository openQuestionRepository;

    public OpenQuestionResource(OpenQuestionService openQuestionService, OpenQuestionRepository openQuestionRepository) {
        this.openQuestionService = openQuestionService;
        this.openQuestionRepository = openQuestionRepository;
    }

    /**
     * {@code POST  /open-questions} : Create a new openQuestion.
     *
     * @param openQuestionDTO the openQuestionDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new openQuestionDTO, or with status {@code 400 (Bad Request)} if the openQuestion has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<OpenQuestionDTO> createOpenQuestion(@Valid @RequestBody OpenQuestionDTO openQuestionDTO)
        throws URISyntaxException {
        LOG.debug("REST request to save OpenQuestion : {}", openQuestionDTO);
        if (openQuestionDTO.getId() != null) {
            throw new BadRequestAlertException("A new openQuestion cannot already have an ID", ENTITY_NAME, "idexists");
        }
        openQuestionDTO = openQuestionService.save(openQuestionDTO);
        return ResponseEntity.created(new URI("/api/open-questions/" + openQuestionDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, false, ENTITY_NAME, openQuestionDTO.getId().toString()))
            .body(openQuestionDTO);
    }

    /**
     * {@code PUT  /open-questions/:id} : Updates an existing openQuestion.
     *
     * @param id the id of the openQuestionDTO to save.
     * @param openQuestionDTO the openQuestionDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated openQuestionDTO,
     * or with status {@code 400 (Bad Request)} if the openQuestionDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the openQuestionDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<OpenQuestionDTO> updateOpenQuestion(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody OpenQuestionDTO openQuestionDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update OpenQuestion : {}, {}", id, openQuestionDTO);
        if (openQuestionDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, openQuestionDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!openQuestionRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        openQuestionDTO = openQuestionService.update(openQuestionDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, false, ENTITY_NAME, openQuestionDTO.getId().toString()))
            .body(openQuestionDTO);
    }

    /**
     * {@code PATCH  /open-questions/:id} : Partial updates given fields of an existing openQuestion, field will ignore if it is null
     *
     * @param id the id of the openQuestionDTO to save.
     * @param openQuestionDTO the openQuestionDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated openQuestionDTO,
     * or with status {@code 400 (Bad Request)} if the openQuestionDTO is not valid,
     * or with status {@code 404 (Not Found)} if the openQuestionDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the openQuestionDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<OpenQuestionDTO> partialUpdateOpenQuestion(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody OpenQuestionDTO openQuestionDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update OpenQuestion partially : {}, {}", id, openQuestionDTO);
        if (openQuestionDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, openQuestionDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!openQuestionRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<OpenQuestionDTO> result = openQuestionService.partialUpdate(openQuestionDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, false, ENTITY_NAME, openQuestionDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /open-questions} : get all the Open Questions.
     *
     * @param eagerload flag to eager load entities from relationships (This is applicable for many-to-many).
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Open Questions in body.
     */
    @GetMapping("")
    public List<OpenQuestionDTO> getAllOpenQuestions(
        @RequestParam(name = "eagerload", required = false, defaultValue = "true") boolean eagerload
    ) {
        LOG.debug("REST request to get all OpenQuestions");
        return openQuestionService.findAll();
    }

    /**
     * {@code GET  /open-questions/:id} : get the "id" openQuestion.
     *
     * @param id the id of the openQuestionDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the openQuestionDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<OpenQuestionDTO> getOpenQuestion(@PathVariable("id") Long id) {
        LOG.debug("REST request to get OpenQuestion : {}", id);
        Optional<OpenQuestionDTO> openQuestionDTO = openQuestionService.findOne(id);
        return ResponseUtil.wrapOrNotFound(openQuestionDTO);
    }

    /**
     * {@code DELETE  /open-questions/:id} : delete the "id" openQuestion.
     *
     * @param id the id of the openQuestionDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOpenQuestion(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete OpenQuestion : {}", id);
        openQuestionService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, false, ENTITY_NAME, id.toString()))
            .build();
    }
}
