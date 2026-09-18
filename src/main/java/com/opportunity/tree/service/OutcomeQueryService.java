package com.opportunity.tree.service;

import com.opportunity.tree.domain.*; // for static metamodels
import com.opportunity.tree.domain.Outcome;
import com.opportunity.tree.repository.OutcomeRepository;
import com.opportunity.tree.service.criteria.OutcomeCriteria;
import com.opportunity.tree.service.dto.OutcomeDTO;
import com.opportunity.tree.service.mapper.OutcomeMapper;
import jakarta.persistence.criteria.JoinType;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.jhipster.service.QueryService;

/**
 * Service for executing complex queries for {@link Outcome} entities in the database.
 * The main input is a {@link OutcomeCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link List} of {@link OutcomeDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class OutcomeQueryService extends QueryService<Outcome> {

    private static final Logger LOG = LoggerFactory.getLogger(OutcomeQueryService.class);

    private final OutcomeRepository outcomeRepository;

    private final OutcomeMapper outcomeMapper;

    public OutcomeQueryService(OutcomeRepository outcomeRepository, OutcomeMapper outcomeMapper) {
        this.outcomeRepository = outcomeRepository;
        this.outcomeMapper = outcomeMapper;
    }

    /**
     * Return a {@link List} of {@link OutcomeDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public List<OutcomeDTO> findByCriteria(OutcomeCriteria criteria) {
        LOG.debug("find by criteria : {}", criteria);
        final Specification<Outcome> specification = createSpecification(criteria);
        return outcomeMapper.toDto(outcomeRepository.findAll(specification));
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(OutcomeCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<Outcome> specification = createSpecification(criteria);
        return outcomeRepository.count(specification);
    }

    /**
     * Function to convert {@link OutcomeCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<Outcome> createSpecification(OutcomeCriteria criteria) {
        Specification<Outcome> specification = Specification.unrestricted();
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = Specification.allOf(
                Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : Specification.unrestricted(),
                buildRangeSpecification(criteria.getId(), Outcome_.id),
                buildStringSpecification(criteria.getTitle(), Outcome_.title),
                buildStringSpecification(criteria.getMetric(), Outcome_.metric),
                buildStringSpecification(criteria.getTargetValue(), Outcome_.targetValue),
                buildStringSpecification(criteria.getCurrentValue(), Outcome_.currentValue),
                buildSpecification(criteria.getStatus(), Outcome_.status),
                buildRangeSpecification(criteria.getStartDate(), Outcome_.startDate),
                buildRangeSpecification(criteria.getTargetDate(), Outcome_.targetDate),
                buildRangeSpecification(criteria.getSortOrder(), Outcome_.sortOrder),
                buildRangeSpecification(criteria.getCreatedDate(), Outcome_.createdDate),
                buildRangeSpecification(criteria.getLastModifiedDate(), Outcome_.lastModifiedDate),
                buildSpecification(criteria.getProductId(), root -> root.join(Outcome_.product, JoinType.LEFT).get(Product_.id)),
                buildSpecification(criteria.getOwnerId(), root -> root.join(Outcome_.owner, JoinType.LEFT).get(User_.id))
            );
        }
        return specification;
    }
}
