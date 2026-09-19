package com.opportunity.tree.web.rest;

import com.opportunity.tree.repository.EvidenceRepository;
import com.opportunity.tree.security.AuthoritiesConstants;
import com.opportunity.tree.service.EvidenceService;
import com.opportunity.tree.service.dto.EvidenceDTO;
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
 * REST controller for managing {@link com.opportunity.tree.domain.Evidence}.
 */
@RestController
@RequestMapping("/api/evidences")
@PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
public class EvidenceResource {

    private static final Logger LOG = LoggerFactory.getLogger(EvidenceResource.class);

    private static final String ENTITY_NAME = "evidence";

    @Value("${jhipster.clientApp.name:opportunitySolutionTree}")
    private String applicationName;

    private final EvidenceService evidenceService;

    private final EvidenceRepository evidenceRepository;

    public EvidenceResource(EvidenceService evidenceService, EvidenceRepository evidenceRepository) {
        this.evidenceService = evidenceService;
        this.evidenceRepository = evidenceRepository;
    }

    /**
     * {@code POST  /evidences} : Create a new evidence.
     *
     * @param evidenceDTO the evidenceDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new evidenceDTO, or with status {@code 400 (Bad Request)} if the evidence has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<EvidenceDTO> createEvidence(@Valid @RequestBody EvidenceDTO evidenceDTO) throws URISyntaxException {
        LOG.debug("REST request to save Evidence : {}", evidenceDTO);
        if (evidenceDTO.getId() != null) {
            throw new BadRequestAlertException("A new evidence cannot already have an ID", ENTITY_NAME, "idexists");
        }
        evidenceDTO = evidenceService.save(evidenceDTO);
        return ResponseEntity.created(new URI("/api/evidences/" + evidenceDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, false, ENTITY_NAME, evidenceDTO.getId().toString()))
            .body(evidenceDTO);
    }

    /**
     * {@code PUT  /evidences/:id} : Updates an existing evidence.
     *
     * @param id the id of the evidenceDTO to save.
     * @param evidenceDTO the evidenceDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated evidenceDTO,
     * or with status {@code 400 (Bad Request)} if the evidenceDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the evidenceDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<EvidenceDTO> updateEvidence(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody EvidenceDTO evidenceDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update Evidence : {}, {}", id, evidenceDTO);
        if (evidenceDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, evidenceDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!evidenceRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        evidenceDTO = evidenceService.update(evidenceDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, false, ENTITY_NAME, evidenceDTO.getId().toString()))
            .body(evidenceDTO);
    }

    /**
     * {@code PATCH  /evidences/:id} : Partial updates given fields of an existing evidence, field will ignore if it is null
     *
     * @param id the id of the evidenceDTO to save.
     * @param evidenceDTO the evidenceDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated evidenceDTO,
     * or with status {@code 400 (Bad Request)} if the evidenceDTO is not valid,
     * or with status {@code 404 (Not Found)} if the evidenceDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the evidenceDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<EvidenceDTO> partialUpdateEvidence(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody EvidenceDTO evidenceDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update Evidence partially : {}, {}", id, evidenceDTO);
        if (evidenceDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, evidenceDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!evidenceRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<EvidenceDTO> result = evidenceService.partialUpdate(evidenceDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, false, ENTITY_NAME, evidenceDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /evidences} : get all the Evidences.
     *
     * @param eagerload flag to eager load entities from relationships (This is applicable for many-to-many).
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Evidences in body.
     */
    @GetMapping("")
    public List<EvidenceDTO> getAllEvidences(@RequestParam(name = "eagerload", required = false, defaultValue = "true") boolean eagerload) {
        LOG.debug("REST request to get all Evidences");
        return evidenceService.findAll();
    }

    /**
     * {@code GET  /evidences/:id} : get the "id" evidence.
     *
     * @param id the id of the evidenceDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the evidenceDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<EvidenceDTO> getEvidence(@PathVariable("id") Long id) {
        LOG.debug("REST request to get Evidence : {}", id);
        Optional<EvidenceDTO> evidenceDTO = evidenceService.findOne(id);
        return ResponseUtil.wrapOrNotFound(evidenceDTO);
    }

    /**
     * {@code DELETE  /evidences/:id} : delete the "id" evidence.
     *
     * @param id the id of the evidenceDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvidence(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete Evidence : {}", id);
        evidenceService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, false, ENTITY_NAME, id.toString()))
            .build();
    }
}
