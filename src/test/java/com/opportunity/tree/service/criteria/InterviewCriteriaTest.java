package com.opportunity.tree.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class InterviewCriteriaTest {

    @Test
    void newInterviewCriteriaHasAllFiltersNullTest() {
        var interviewCriteria = new InterviewCriteria();
        assertThat(interviewCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void interviewCriteriaFluentMethodsCreatesFiltersTest() {
        var interviewCriteria = new InterviewCriteria();

        setAllFilters(interviewCriteria);

        assertThat(interviewCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void interviewCriteriaCopyCreatesNullFilterTest() {
        var interviewCriteria = new InterviewCriteria();
        var copy = interviewCriteria.copy();

        assertThat(interviewCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(interviewCriteria)
        );
    }

    @Test
    void interviewCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var interviewCriteria = new InterviewCriteria();
        setAllFilters(interviewCriteria);

        var copy = interviewCriteria.copy();

        assertThat(interviewCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(interviewCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var interviewCriteria = new InterviewCriteria();

        assertThat(interviewCriteria).hasToString("InterviewCriteria{}");
    }

    private static void setAllFilters(InterviewCriteria interviewCriteria) {
        interviewCriteria.id();
        interviewCriteria.title();
        interviewCriteria.participant();
        interviewCriteria.interviewDate();
        interviewCriteria.recordingUrl();
        interviewCriteria.createdDate();
        interviewCriteria.productId();
        interviewCriteria.interviewerId();
        interviewCriteria.opportunityId();
        interviewCriteria.distinct();
    }

    private static Condition<InterviewCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getTitle()) &&
                condition.apply(criteria.getParticipant()) &&
                condition.apply(criteria.getInterviewDate()) &&
                condition.apply(criteria.getRecordingUrl()) &&
                condition.apply(criteria.getCreatedDate()) &&
                condition.apply(criteria.getProductId()) &&
                condition.apply(criteria.getInterviewerId()) &&
                condition.apply(criteria.getOpportunityId()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<InterviewCriteria> copyFiltersAre(InterviewCriteria copy, BiFunction<Object, Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getTitle(), copy.getTitle()) &&
                condition.apply(criteria.getParticipant(), copy.getParticipant()) &&
                condition.apply(criteria.getInterviewDate(), copy.getInterviewDate()) &&
                condition.apply(criteria.getRecordingUrl(), copy.getRecordingUrl()) &&
                condition.apply(criteria.getCreatedDate(), copy.getCreatedDate()) &&
                condition.apply(criteria.getProductId(), copy.getProductId()) &&
                condition.apply(criteria.getInterviewerId(), copy.getInterviewerId()) &&
                condition.apply(criteria.getOpportunityId(), copy.getOpportunityId()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
