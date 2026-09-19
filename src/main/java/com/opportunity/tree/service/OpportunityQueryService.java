package com.opportunity.tree.service;

import com.opportunity.tree.domain.*; // for static metamodels
import com.opportunity.tree.domain.Opportunity;
import com.opportunity.tree.repository.OpportunityRepository;
import com.opportunity.tree.service.criteria.OpportunityCriteria;
import com.opportunity.tree.service.dto.OpportunityDTO;
import com.opportunity.tree.service.mapper.OpportunityMapper;
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
 * Service for executing complex queries for {@link Opportunity} entities in the database.
 * The main input is a {@link OpportunityCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link OpportunityDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class OpportunityQueryService extends QueryService<Opportunity> {

    private static final Logger LOG = LoggerFactory.getLogger(OpportunityQueryService.class);

    private final OpportunityRepository opportunityRepository;

    private final OpportunityMapper opportunityMapper;

    public OpportunityQueryService(OpportunityRepository opportunityRepository, OpportunityMapper opportunityMapper) {
        this.opportunityRepository = opportunityRepository;
        this.opportunityMapper = opportunityMapper;
    }

    /**
     * Return a {@link Page} of {@link OpportunityDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<OpportunityDTO> findByCriteria(OpportunityCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<Opportunity> specification = createSpecification(criteria);
        return opportunityRepository
            .fetchBagRelationships(opportunityRepository.findAll(specification, page))
            .map(opportunityMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(OpportunityCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<Opportunity> specification = createSpecification(criteria);
        return opportunityRepository.count(specification);
    }

    /**
     * Function to convert {@link OpportunityCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<Opportunity> createSpecification(OpportunityCriteria criteria) {
        Specification<Opportunity> specification = Specification.unrestricted();
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = Specification.allOf(
                Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : Specification.unrestricted(),
                buildRangeSpecification(criteria.getId(), Opportunity_.id),
                buildStringSpecification(criteria.getTitle(), Opportunity_.title),
                buildSpecification(criteria.getStatus(), Opportunity_.status),
                buildRangeSpecification(criteria.getValuerating(), Opportunity_.valuerating),
                buildRangeSpecification(criteria.getPriority(), Opportunity_.priority),
                buildRangeSpecification(criteria.getSortOrder(), Opportunity_.sortOrder),
                buildRangeSpecification(criteria.getCreatedDate(), Opportunity_.createdDate),
                buildRangeSpecification(criteria.getLastModifiedDate(), Opportunity_.lastModifiedDate),
                buildSpecification(criteria.getOutcomeId(), root -> root.join(Opportunity_.outcome, JoinType.LEFT).get(Outcome_.id)),
                buildSpecification(criteria.getParentId(), root -> root.join(Opportunity_.parent, JoinType.LEFT).get(Opportunity_.id)),
                buildSpecification(criteria.getOwnerId(), root -> root.join(Opportunity_.owner, JoinType.LEFT).get(User_.id)),
                buildSpecification(criteria.getInterviewId(), root -> root.join(Opportunity_.interviews, JoinType.LEFT).get(Interview_.id)),
                buildSpecification(criteria.getTagId(), root -> root.join(Opportunity_.tags, JoinType.LEFT).get(Tag_.id))
            );
        }
        return specification;
    }
}
