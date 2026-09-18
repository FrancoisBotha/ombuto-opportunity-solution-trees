package com.opportunity.tree.service;

import com.opportunity.tree.domain.*; // for static metamodels
import com.opportunity.tree.domain.Solution;
import com.opportunity.tree.repository.SolutionRepository;
import com.opportunity.tree.service.criteria.SolutionCriteria;
import com.opportunity.tree.service.dto.SolutionDTO;
import com.opportunity.tree.service.mapper.SolutionMapper;
import jakarta.persistence.criteria.JoinType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.jhipster.service.QueryService;

/**
 * Service for executing complex queries for {@link Solution} entities in the database.
 * The main input is a {@link SolutionCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link SolutionDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class SolutionQueryService extends QueryService<Solution> {

    private static final Logger LOG = LoggerFactory.getLogger(SolutionQueryService.class);

    private final SolutionRepository solutionRepository;

    private final SolutionMapper solutionMapper;

    public SolutionQueryService(SolutionRepository solutionRepository, SolutionMapper solutionMapper) {
        this.solutionRepository = solutionRepository;
        this.solutionMapper = solutionMapper;
    }

    /**
     * Return a {@link Page} of {@link SolutionDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<SolutionDTO> findByCriteria(SolutionCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<Solution> specification = createSpecification(criteria);
        return solutionRepository.fetchBagRelationships(solutionRepository.findAll(specification, page)).map(solutionMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(SolutionCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<Solution> specification = createSpecification(criteria);
        return solutionRepository.count(specification);
    }

    /**
     * Function to convert {@link SolutionCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<Solution> createSpecification(SolutionCriteria criteria) {
        Specification<Solution> specification = Specification.unrestricted();
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = Specification.allOf(
                Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : Specification.unrestricted(),
                buildRangeSpecification(criteria.getId(), Solution_.id),
                buildStringSpecification(criteria.getTitle(), Solution_.title),
                buildSpecification(criteria.getStatus(), Solution_.status),
                buildRangeSpecification(criteria.getEffort(), Solution_.effort),
                buildRangeSpecification(criteria.getSortOrder(), Solution_.sortOrder),
                buildRangeSpecification(criteria.getCreatedDate(), Solution_.createdDate),
                buildRangeSpecification(criteria.getLastModifiedDate(), Solution_.lastModifiedDate),
                buildSpecification(criteria.getOpportunityId(), root ->
                    root.join(Solution_.opportunity, JoinType.LEFT).get(Opportunity_.id)
                ),
                buildSpecification(criteria.getOwnerId(), root -> root.join(Solution_.owner, JoinType.LEFT).get(User_.id)),
                buildSpecification(criteria.getTagId(), root -> root.join(Solution_.tags, JoinType.LEFT).get(Tag_.id))
            );
        }
        return specification;
    }
}
