package com.opportunity.tree.service;

import com.opportunity.tree.domain.*; // for static metamodels
import com.opportunity.tree.domain.MeetingTranscript;
import com.opportunity.tree.repository.MeetingTranscriptRepository;
import com.opportunity.tree.service.criteria.MeetingTranscriptCriteria;
import com.opportunity.tree.service.dto.MeetingTranscriptDTO;
import com.opportunity.tree.service.mapper.MeetingTranscriptMapper;
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
 * Service for executing complex queries for {@link MeetingTranscript} entities in the database.
 * The main input is a {@link MeetingTranscriptCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link MeetingTranscriptDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class MeetingTranscriptQueryService extends QueryService<MeetingTranscript> {

    private static final Logger LOG = LoggerFactory.getLogger(MeetingTranscriptQueryService.class);

    private final MeetingTranscriptRepository meetingTranscriptRepository;

    private final MeetingTranscriptMapper meetingTranscriptMapper;

    public MeetingTranscriptQueryService(
        MeetingTranscriptRepository meetingTranscriptRepository,
        MeetingTranscriptMapper meetingTranscriptMapper
    ) {
        this.meetingTranscriptRepository = meetingTranscriptRepository;
        this.meetingTranscriptMapper = meetingTranscriptMapper;
    }

    /**
     * Return a {@link Page} of {@link MeetingTranscriptDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<MeetingTranscriptDTO> findByCriteria(MeetingTranscriptCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<MeetingTranscript> specification = createSpecification(criteria);
        return meetingTranscriptRepository.findAll(specification, page).map(meetingTranscriptMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(MeetingTranscriptCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<MeetingTranscript> specification = createSpecification(criteria);
        return meetingTranscriptRepository.count(specification);
    }

    /**
     * Function to convert {@link MeetingTranscriptCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<MeetingTranscript> createSpecification(MeetingTranscriptCriteria criteria) {
        Specification<MeetingTranscript> specification = Specification.unrestricted();
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = Specification.allOf(
                Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : Specification.unrestricted(),
                buildRangeSpecification(criteria.getId(), MeetingTranscript_.id),
                buildStringSpecification(criteria.getTitle(), MeetingTranscript_.title),
                buildRangeSpecification(criteria.getMeetingDate(), MeetingTranscript_.meetingDate),
                buildStringSpecification(criteria.getAttendees(), MeetingTranscript_.attendees),
                buildSpecification(criteria.getSource(), MeetingTranscript_.source),
                buildRangeSpecification(criteria.getCreatedDate(), MeetingTranscript_.createdDate),
                buildRangeSpecification(criteria.getEditedDate(), MeetingTranscript_.editedDate),
                buildSpecification(criteria.getAuthorId(), root -> root.join(MeetingTranscript_.author, JoinType.LEFT).get(User_.id)),
                buildSpecification(criteria.getProductId(), root -> root.join(MeetingTranscript_.product, JoinType.LEFT).get(Product_.id)),
                buildSpecification(criteria.getOutcomeId(), root -> root.join(MeetingTranscript_.outcome, JoinType.LEFT).get(Outcome_.id)),
                buildSpecification(criteria.getOpportunityId(), root ->
                    root.join(MeetingTranscript_.opportunity, JoinType.LEFT).get(Opportunity_.id)
                ),
                buildSpecification(criteria.getSolutionId(), root ->
                    root.join(MeetingTranscript_.solution, JoinType.LEFT).get(Solution_.id)
                ),
                buildSpecification(criteria.getAssumptionId(), root ->
                    root.join(MeetingTranscript_.assumption, JoinType.LEFT).get(Assumption_.id)
                ),
                buildSpecification(criteria.getEvidenceId(), root ->
                    root.join(MeetingTranscript_.evidence, JoinType.LEFT).get(Evidence_.id)
                )
            );
        }
        return specification;
    }
}
