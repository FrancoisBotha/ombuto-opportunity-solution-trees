package com.opportunity.tree.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class MeetingTranscriptCriteriaTest {

    @Test
    void newMeetingTranscriptCriteriaHasAllFiltersNullTest() {
        var meetingTranscriptCriteria = new MeetingTranscriptCriteria();
        assertThat(meetingTranscriptCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void meetingTranscriptCriteriaFluentMethodsCreatesFiltersTest() {
        var meetingTranscriptCriteria = new MeetingTranscriptCriteria();

        setAllFilters(meetingTranscriptCriteria);

        assertThat(meetingTranscriptCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void meetingTranscriptCriteriaCopyCreatesNullFilterTest() {
        var meetingTranscriptCriteria = new MeetingTranscriptCriteria();
        var copy = meetingTranscriptCriteria.copy();

        assertThat(meetingTranscriptCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(meetingTranscriptCriteria)
        );
    }

    @Test
    void meetingTranscriptCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var meetingTranscriptCriteria = new MeetingTranscriptCriteria();
        setAllFilters(meetingTranscriptCriteria);

        var copy = meetingTranscriptCriteria.copy();

        assertThat(meetingTranscriptCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(meetingTranscriptCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var meetingTranscriptCriteria = new MeetingTranscriptCriteria();

        assertThat(meetingTranscriptCriteria).hasToString("MeetingTranscriptCriteria{}");
    }

    private static void setAllFilters(MeetingTranscriptCriteria meetingTranscriptCriteria) {
        meetingTranscriptCriteria.id();
        meetingTranscriptCriteria.title();
        meetingTranscriptCriteria.meetingDate();
        meetingTranscriptCriteria.attendees();
        meetingTranscriptCriteria.source();
        meetingTranscriptCriteria.createdDate();
        meetingTranscriptCriteria.editedDate();
        meetingTranscriptCriteria.authorId();
        meetingTranscriptCriteria.productId();
        meetingTranscriptCriteria.outcomeId();
        meetingTranscriptCriteria.opportunityId();
        meetingTranscriptCriteria.solutionId();
        meetingTranscriptCriteria.assumptionId();
        meetingTranscriptCriteria.evidenceId();
        meetingTranscriptCriteria.distinct();
    }

    private static Condition<MeetingTranscriptCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getTitle()) &&
                condition.apply(criteria.getMeetingDate()) &&
                condition.apply(criteria.getAttendees()) &&
                condition.apply(criteria.getSource()) &&
                condition.apply(criteria.getCreatedDate()) &&
                condition.apply(criteria.getEditedDate()) &&
                condition.apply(criteria.getAuthorId()) &&
                condition.apply(criteria.getProductId()) &&
                condition.apply(criteria.getOutcomeId()) &&
                condition.apply(criteria.getOpportunityId()) &&
                condition.apply(criteria.getSolutionId()) &&
                condition.apply(criteria.getAssumptionId()) &&
                condition.apply(criteria.getEvidenceId()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<MeetingTranscriptCriteria> copyFiltersAre(
        MeetingTranscriptCriteria copy,
        BiFunction<Object, Object, Boolean> condition
    ) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getTitle(), copy.getTitle()) &&
                condition.apply(criteria.getMeetingDate(), copy.getMeetingDate()) &&
                condition.apply(criteria.getAttendees(), copy.getAttendees()) &&
                condition.apply(criteria.getSource(), copy.getSource()) &&
                condition.apply(criteria.getCreatedDate(), copy.getCreatedDate()) &&
                condition.apply(criteria.getEditedDate(), copy.getEditedDate()) &&
                condition.apply(criteria.getAuthorId(), copy.getAuthorId()) &&
                condition.apply(criteria.getProductId(), copy.getProductId()) &&
                condition.apply(criteria.getOutcomeId(), copy.getOutcomeId()) &&
                condition.apply(criteria.getOpportunityId(), copy.getOpportunityId()) &&
                condition.apply(criteria.getSolutionId(), copy.getSolutionId()) &&
                condition.apply(criteria.getAssumptionId(), copy.getAssumptionId()) &&
                condition.apply(criteria.getEvidenceId(), copy.getEvidenceId()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
