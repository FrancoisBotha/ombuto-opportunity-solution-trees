package com.opportunity.tree.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class SolutionCriteriaTest {

    @Test
    void newSolutionCriteriaHasAllFiltersNullTest() {
        var solutionCriteria = new SolutionCriteria();
        assertThat(solutionCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void solutionCriteriaFluentMethodsCreatesFiltersTest() {
        var solutionCriteria = new SolutionCriteria();

        setAllFilters(solutionCriteria);

        assertThat(solutionCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void solutionCriteriaCopyCreatesNullFilterTest() {
        var solutionCriteria = new SolutionCriteria();
        var copy = solutionCriteria.copy();

        assertThat(solutionCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(solutionCriteria)
        );
    }

    @Test
    void solutionCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var solutionCriteria = new SolutionCriteria();
        setAllFilters(solutionCriteria);

        var copy = solutionCriteria.copy();

        assertThat(solutionCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(solutionCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var solutionCriteria = new SolutionCriteria();

        assertThat(solutionCriteria).hasToString("SolutionCriteria{}");
    }

    private static void setAllFilters(SolutionCriteria solutionCriteria) {
        solutionCriteria.id();
        solutionCriteria.title();
        solutionCriteria.status();
        solutionCriteria.effort();
        solutionCriteria.sortOrder();
        solutionCriteria.createdDate();
        solutionCriteria.lastModifiedDate();
        solutionCriteria.opportunityId();
        solutionCriteria.ownerId();
        solutionCriteria.tagId();
        solutionCriteria.distinct();
    }

    private static Condition<SolutionCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getTitle()) &&
                condition.apply(criteria.getStatus()) &&
                condition.apply(criteria.getEffort()) &&
                condition.apply(criteria.getSortOrder()) &&
                condition.apply(criteria.getCreatedDate()) &&
                condition.apply(criteria.getLastModifiedDate()) &&
                condition.apply(criteria.getOpportunityId()) &&
                condition.apply(criteria.getOwnerId()) &&
                condition.apply(criteria.getTagId()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<SolutionCriteria> copyFiltersAre(SolutionCriteria copy, BiFunction<Object, Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getTitle(), copy.getTitle()) &&
                condition.apply(criteria.getStatus(), copy.getStatus()) &&
                condition.apply(criteria.getEffort(), copy.getEffort()) &&
                condition.apply(criteria.getSortOrder(), copy.getSortOrder()) &&
                condition.apply(criteria.getCreatedDate(), copy.getCreatedDate()) &&
                condition.apply(criteria.getLastModifiedDate(), copy.getLastModifiedDate()) &&
                condition.apply(criteria.getOpportunityId(), copy.getOpportunityId()) &&
                condition.apply(criteria.getOwnerId(), copy.getOwnerId()) &&
                condition.apply(criteria.getTagId(), copy.getTagId()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
