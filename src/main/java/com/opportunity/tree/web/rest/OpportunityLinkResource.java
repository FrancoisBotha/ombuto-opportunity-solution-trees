package com.opportunity.tree.web.rest;

import com.opportunity.tree.repository.OpportunityLinkRepository;
import com.opportunity.tree.security.AuthoritiesConstants;
import com.opportunity.tree.service.OpportunityLinkService;
import com.opportunity.tree.service.dto.OpportunityLinkDTO;
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
 * REST controller for managing {@link com.opportunity.tree.domain.OpportunityLink}.
 */
@RestController
@RequestMapping("/api/opportunity-links")
@PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
public class OpportunityLinkResource {

    private static final Logger LOG = LoggerFactory.getLogger(OpportunityLinkResource.class);

    private static final String ENTITY_NAME = "opportunityLink";

    @Value("${jhipster.clientApp.name:opportunitySolutionTree}")
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
    public ResponseEntity<OpportunityLinkDTO> createOpportunityLink(@Valid @RequestBody OpportunityLinkDTO opportunityLinkDTO)
        throws URISyntaxException {
        LOG.debug("REST request to save OpportunityLink : {}", opportunityLinkDTO);
        if (opportunityLinkDTO.getId() != null) {
            throw new BadRequestAlertException("A new opportunityLink cannot already have an ID", ENTITY_NAME, "idexists");
        }
        opportunityLinkDTO = opportunityLinkService.save(opportunityLinkDTO);
        return ResponseEntity.created(new URI("/api/opportunity-links/" + opportunityLinkDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, false, ENTITY_NAME, opportunityLinkDTO.getId().toString()))
            .body(opportunityLinkDTO);
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
    public ResponseEntity<OpportunityLinkDTO> updateOpportunityLink(
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

        if (!opportunityLinkRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        opportunityLinkDTO = opportunityLinkService.update(opportunityLinkDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, false, ENTITY_NAME, opportunityLinkDTO.getId().toString()))
            .body(opportunityLinkDTO);
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
    public ResponseEntity<OpportunityLinkDTO> partialUpdateOpportunityLink(
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

        if (!opportunityLinkRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<OpportunityLinkDTO> result = opportunityLinkService.partialUpdate(opportunityLinkDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, false, ENTITY_NAME, opportunityLinkDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /opportunity-links} : get all the Opportunity Links.
     *
     * @param eagerload flag to eager load entities from relationships (This is applicable for many-to-many).
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Opportunity Links in body.
     */
    @GetMapping("")
    public List<OpportunityLinkDTO> getAllOpportunityLinks(
        @RequestParam(name = "eagerload", required = false, defaultValue = "true") boolean eagerload
    ) {
        LOG.debug("REST request to get all OpportunityLinks");
        return opportunityLinkService.findAll();
    }

    /**
     * {@code GET  /opportunity-links/:id} : get the "id" opportunityLink.
     *
     * @param id the id of the opportunityLinkDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the opportunityLinkDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<OpportunityLinkDTO> getOpportunityLink(@PathVariable("id") Long id) {
        LOG.debug("REST request to get OpportunityLink : {}", id);
        Optional<OpportunityLinkDTO> opportunityLinkDTO = opportunityLinkService.findOne(id);
        return ResponseUtil.wrapOrNotFound(opportunityLinkDTO);
    }

    /**
     * {@code DELETE  /opportunity-links/:id} : delete the "id" opportunityLink.
     *
     * @param id the id of the opportunityLinkDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOpportunityLink(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete OpportunityLink : {}", id);
        opportunityLinkService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, false, ENTITY_NAME, id.toString()))
            .build();
    }
}
