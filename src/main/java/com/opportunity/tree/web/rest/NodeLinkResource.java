package com.opportunity.tree.web.rest;

import com.opportunity.tree.repository.NodeLinkRepository;
import com.opportunity.tree.security.AuthoritiesConstants;
import com.opportunity.tree.service.NodeLinkService;
import com.opportunity.tree.service.dto.NodeLinkDTO;
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
 * REST controller for managing {@link com.opportunity.tree.domain.NodeLink}.
 */
@RestController
@RequestMapping("/api/node-links")
@PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
public class NodeLinkResource {

    private static final Logger LOG = LoggerFactory.getLogger(NodeLinkResource.class);

    private static final String ENTITY_NAME = "nodeLink";

    @Value("${jhipster.clientApp.name:opportunitySolutionTree}")
    private String applicationName;

    private final NodeLinkService nodeLinkService;

    private final NodeLinkRepository nodeLinkRepository;

    public NodeLinkResource(NodeLinkService nodeLinkService, NodeLinkRepository nodeLinkRepository) {
        this.nodeLinkService = nodeLinkService;
        this.nodeLinkRepository = nodeLinkRepository;
    }

    /**
     * {@code POST  /node-links} : Create a new nodeLink.
     *
     * @param nodeLinkDTO the nodeLinkDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new nodeLinkDTO, or with status {@code 400 (Bad Request)} if the nodeLink has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<NodeLinkDTO> createNodeLink(@Valid @RequestBody NodeLinkDTO nodeLinkDTO) throws URISyntaxException {
        LOG.debug("REST request to save NodeLink : {}", nodeLinkDTO);
        if (nodeLinkDTO.getId() != null) {
            throw new BadRequestAlertException("A new nodeLink cannot already have an ID", ENTITY_NAME, "idexists");
        }
        nodeLinkDTO = nodeLinkService.save(nodeLinkDTO);
        return ResponseEntity.created(new URI("/api/node-links/" + nodeLinkDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, false, ENTITY_NAME, nodeLinkDTO.getId().toString()))
            .body(nodeLinkDTO);
    }

    /**
     * {@code PUT  /node-links/:id} : Updates an existing nodeLink.
     *
     * @param id the id of the nodeLinkDTO to save.
     * @param nodeLinkDTO the nodeLinkDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated nodeLinkDTO,
     * or with status {@code 400 (Bad Request)} if the nodeLinkDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the nodeLinkDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<NodeLinkDTO> updateNodeLink(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody NodeLinkDTO nodeLinkDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update NodeLink : {}, {}", id, nodeLinkDTO);
        if (nodeLinkDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, nodeLinkDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!nodeLinkRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        nodeLinkDTO = nodeLinkService.update(nodeLinkDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, false, ENTITY_NAME, nodeLinkDTO.getId().toString()))
            .body(nodeLinkDTO);
    }

    /**
     * {@code PATCH  /node-links/:id} : Partial updates given fields of an existing nodeLink, field will ignore if it is null
     *
     * @param id the id of the nodeLinkDTO to save.
     * @param nodeLinkDTO the nodeLinkDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated nodeLinkDTO,
     * or with status {@code 400 (Bad Request)} if the nodeLinkDTO is not valid,
     * or with status {@code 404 (Not Found)} if the nodeLinkDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the nodeLinkDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<NodeLinkDTO> partialUpdateNodeLink(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody NodeLinkDTO nodeLinkDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update NodeLink partially : {}, {}", id, nodeLinkDTO);
        if (nodeLinkDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, nodeLinkDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!nodeLinkRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<NodeLinkDTO> result = nodeLinkService.partialUpdate(nodeLinkDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, false, ENTITY_NAME, nodeLinkDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /node-links} : get all the Node Links.
     *
     * @param eagerload flag to eager load entities from relationships (This is applicable for many-to-many).
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Node Links in body.
     */
    @GetMapping("")
    public List<NodeLinkDTO> getAllNodeLinks(@RequestParam(name = "eagerload", required = false, defaultValue = "true") boolean eagerload) {
        LOG.debug("REST request to get all NodeLinks");
        return nodeLinkService.findAll();
    }

    /**
     * {@code GET  /node-links/:id} : get the "id" nodeLink.
     *
     * @param id the id of the nodeLinkDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the nodeLinkDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<NodeLinkDTO> getNodeLink(@PathVariable("id") Long id) {
        LOG.debug("REST request to get NodeLink : {}", id);
        Optional<NodeLinkDTO> nodeLinkDTO = nodeLinkService.findOne(id);
        return ResponseUtil.wrapOrNotFound(nodeLinkDTO);
    }

    /**
     * {@code DELETE  /node-links/:id} : delete the "id" nodeLink.
     *
     * @param id the id of the nodeLinkDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNodeLink(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete NodeLink : {}", id);
        nodeLinkService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, false, ENTITY_NAME, id.toString()))
            .build();
    }
}
