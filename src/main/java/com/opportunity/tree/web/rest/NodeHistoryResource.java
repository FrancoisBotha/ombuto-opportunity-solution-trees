package com.opportunity.tree.web.rest;

import com.opportunity.tree.repository.NodeHistoryRepository;
import com.opportunity.tree.security.AuthoritiesConstants;
import com.opportunity.tree.service.NodeHistoryService;
import com.opportunity.tree.service.dto.NodeHistoryDTO;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.PaginationUtil;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link com.opportunity.tree.domain.NodeHistory}.
 */
@RestController
@RequestMapping("/api/node-histories")
@PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
public class NodeHistoryResource {

    private static final Logger LOG = LoggerFactory.getLogger(NodeHistoryResource.class);

    private static final String ENTITY_NAME = "nodeHistory";

    @Value("${jhipster.clientApp.name:opportunitySolutionTree}")
    private String applicationName;

    private final NodeHistoryService nodeHistoryService;

    private final NodeHistoryRepository nodeHistoryRepository;

    public NodeHistoryResource(NodeHistoryService nodeHistoryService, NodeHistoryRepository nodeHistoryRepository) {
        this.nodeHistoryService = nodeHistoryService;
        this.nodeHistoryRepository = nodeHistoryRepository;
    }

    /**
     * {@code POST  /node-histories} : Create a new nodeHistory.
     *
     * @param nodeHistoryDTO the nodeHistoryDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new nodeHistoryDTO, or with status {@code 400 (Bad Request)} if the nodeHistory has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<NodeHistoryDTO> createNodeHistory(@Valid @RequestBody NodeHistoryDTO nodeHistoryDTO) throws URISyntaxException {
        LOG.debug("REST request to save NodeHistory : {}", nodeHistoryDTO);
        if (nodeHistoryDTO.getId() != null) {
            throw new BadRequestAlertException("A new nodeHistory cannot already have an ID", ENTITY_NAME, "idexists");
        }
        nodeHistoryDTO = nodeHistoryService.save(nodeHistoryDTO);
        return ResponseEntity.created(new URI("/api/node-histories/" + nodeHistoryDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, false, ENTITY_NAME, nodeHistoryDTO.getId().toString()))
            .body(nodeHistoryDTO);
    }

    /**
     * {@code PUT  /node-histories/:id} : Updates an existing nodeHistory.
     *
     * @param id the id of the nodeHistoryDTO to save.
     * @param nodeHistoryDTO the nodeHistoryDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated nodeHistoryDTO,
     * or with status {@code 400 (Bad Request)} if the nodeHistoryDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the nodeHistoryDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<NodeHistoryDTO> updateNodeHistory(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody NodeHistoryDTO nodeHistoryDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update NodeHistory : {}, {}", id, nodeHistoryDTO);
        if (nodeHistoryDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, nodeHistoryDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!nodeHistoryRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        nodeHistoryDTO = nodeHistoryService.update(nodeHistoryDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, false, ENTITY_NAME, nodeHistoryDTO.getId().toString()))
            .body(nodeHistoryDTO);
    }

    /**
     * {@code PATCH  /node-histories/:id} : Partial updates given fields of an existing nodeHistory, field will ignore if it is null
     *
     * @param id the id of the nodeHistoryDTO to save.
     * @param nodeHistoryDTO the nodeHistoryDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated nodeHistoryDTO,
     * or with status {@code 400 (Bad Request)} if the nodeHistoryDTO is not valid,
     * or with status {@code 404 (Not Found)} if the nodeHistoryDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the nodeHistoryDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<NodeHistoryDTO> partialUpdateNodeHistory(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody NodeHistoryDTO nodeHistoryDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update NodeHistory partially : {}, {}", id, nodeHistoryDTO);
        if (nodeHistoryDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, nodeHistoryDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!nodeHistoryRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<NodeHistoryDTO> result = nodeHistoryService.partialUpdate(nodeHistoryDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, false, ENTITY_NAME, nodeHistoryDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /node-histories} : get all the Node Histories.
     *
     * @param pageable the pagination information.
     * @param eagerload flag to eager load entities from relationships (This is applicable for many-to-many).
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Node Histories in body.
     */
    @GetMapping("")
    public ResponseEntity<List<NodeHistoryDTO>> getAllNodeHistories(
        @org.springdoc.core.annotations.ParameterObject Pageable pageable,
        @RequestParam(name = "eagerload", required = false, defaultValue = "true") boolean eagerload
    ) {
        LOG.debug("REST request to get a page of NodeHistories");
        Page<NodeHistoryDTO> page;
        if (eagerload) {
            page = nodeHistoryService.findAllWithEagerRelationships(pageable);
        } else {
            page = nodeHistoryService.findAll(pageable);
        }
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /node-histories/:id} : get the "id" nodeHistory.
     *
     * @param id the id of the nodeHistoryDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the nodeHistoryDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<NodeHistoryDTO> getNodeHistory(@PathVariable("id") Long id) {
        LOG.debug("REST request to get NodeHistory : {}", id);
        Optional<NodeHistoryDTO> nodeHistoryDTO = nodeHistoryService.findOne(id);
        return ResponseUtil.wrapOrNotFound(nodeHistoryDTO);
    }

    /**
     * {@code DELETE  /node-histories/:id} : delete the "id" nodeHistory.
     *
     * @param id the id of the nodeHistoryDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNodeHistory(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete NodeHistory : {}", id);
        nodeHistoryService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, false, ENTITY_NAME, id.toString()))
            .build();
    }
}
