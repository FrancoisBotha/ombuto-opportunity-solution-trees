package com.opportunity.tree.web.rest;

import com.opportunity.tree.repository.SolutionRepository;
import com.opportunity.tree.service.SolutionQueryService;
import com.opportunity.tree.service.SolutionService;
import com.opportunity.tree.service.criteria.SolutionCriteria;
import com.opportunity.tree.service.dto.SolutionDTO;
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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.PaginationUtil;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link com.opportunity.tree.domain.Solution}.
 */
@RestController
@RequestMapping("/api/solutions")
public class SolutionResource {

    private static final Logger LOG = LoggerFactory.getLogger(SolutionResource.class);

    private static final String ENTITY_NAME = "solution";

    @Value("${jhipster.clientApp.name:opportunitySolutionTree}")
    private String applicationName;

    private final SolutionService solutionService;

    private final SolutionRepository solutionRepository;

    private final SolutionQueryService solutionQueryService;

    public SolutionResource(
        SolutionService solutionService,
        SolutionRepository solutionRepository,
        SolutionQueryService solutionQueryService
    ) {
        this.solutionService = solutionService;
        this.solutionRepository = solutionRepository;
        this.solutionQueryService = solutionQueryService;
    }

    /**
     * {@code POST  /solutions} : Create a new solution.
     *
     * @param solutionDTO the solutionDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new solutionDTO, or with status {@code 400 (Bad Request)} if the solution has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<SolutionDTO> createSolution(@Valid @RequestBody SolutionDTO solutionDTO) throws URISyntaxException {
        LOG.debug("REST request to save Solution : {}", solutionDTO);
        if (solutionDTO.getId() != null) {
            throw new BadRequestAlertException("A new solution cannot already have an ID", ENTITY_NAME, "idexists");
        }
        solutionDTO = solutionService.save(solutionDTO);
        return ResponseEntity.created(new URI("/api/solutions/" + solutionDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, false, ENTITY_NAME, solutionDTO.getId().toString()))
            .body(solutionDTO);
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
    public ResponseEntity<SolutionDTO> updateSolution(
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

        if (!solutionRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        solutionDTO = solutionService.update(solutionDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, false, ENTITY_NAME, solutionDTO.getId().toString()))
            .body(solutionDTO);
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
    public ResponseEntity<SolutionDTO> partialUpdateSolution(
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

        if (!solutionRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<SolutionDTO> result = solutionService.partialUpdate(solutionDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, false, ENTITY_NAME, solutionDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /solutions} : get all the Solutions.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Solutions in body.
     */
    @GetMapping("")
    public ResponseEntity<List<SolutionDTO>> getAllSolutions(
        SolutionCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get Solutions by criteria: {}", criteria);

        Page<SolutionDTO> page = solutionQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /solutions/count} : count all the solutions.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countSolutions(SolutionCriteria criteria) {
        LOG.debug("REST request to count Solutions by criteria: {}", criteria);
        return ResponseEntity.ok().body(solutionQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /solutions/:id} : get the "id" solution.
     *
     * @param id the id of the solutionDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the solutionDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<SolutionDTO> getSolution(@PathVariable("id") Long id) {
        LOG.debug("REST request to get Solution : {}", id);
        Optional<SolutionDTO> solutionDTO = solutionService.findOne(id);
        return ResponseUtil.wrapOrNotFound(solutionDTO);
    }

    /**
     * {@code DELETE  /solutions/:id} : delete the "id" solution.
     *
     * @param id the id of the solutionDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSolution(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete Solution : {}", id);
        solutionService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, false, ENTITY_NAME, id.toString()))
            .build();
    }
}
