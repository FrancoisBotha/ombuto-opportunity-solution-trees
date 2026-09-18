package com.opportunity.tree.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class OpportunityCriteriaTest {

    @Test
    void newOpportunityCriteriaHasAllFiltersNullTest() {
        var opportunityCriteria = new OpportunityCriteria();
        assertThat(opportunityCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void opportunityCriteriaFluentMethodsCreatesFiltersTest() {
        var opportunityCriteria = new OpportunityCriteria();

        setAllFilters(opportunityCriteria);

        assertThat(opportunityCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void opportunityCriteriaCopyCreatesNullFilterTest() {
        var opportunityCriteria = new OpportunityCriteria();
        var copy = opportunityCriteria.copy();

        assertThat(opportunityCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(opportunityCriteria)
        );
    }

    @Test
    void opportunityCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var opportunityCriteria = new OpportunityCriteria();
        setAllFilters(opportunityCriteria);

        var copy = opportunityCriteria.copy();

        assertThat(opportunityCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(opportunityCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var opportunityCriteria = new OpportunityCriteria();

        assertThat(opportunityCriteria).hasToString("OpportunityCriteria{}");
    }

    private static void setAllFilters(OpportunityCriteria opportunityCriteria) {
        opportunityCriteria.id();
        opportunityCriteria.title();
        opportunityCriteria.status();
        opportunityCriteria.value();
        opportunityCriteria.complexity();
        opportunityCriteria.sortOrder();
        opportunityCriteria.createdDate();
        opportunityCriteria.lastModifiedDate();
        opportunityCriteria.outcomeId();
        opportunityCriteria.parentId();
        opportunityCriteria.ownerId();
        opportunityCriteria.interviewId();
        opportunityCriteria.tagId();
        opportunityCriteria.distinct();
    }

    private static Condition<OpportunityCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getTitle()) &&
                condition.apply(criteria.getStatus()) &&
                condition.apply(criteria.getValue()) &&
                condition.apply(criteria.getComplexity()) &&
                condition.apply(criteria.getSortOrder()) &&
                condition.apply(criteria.getCreatedDate()) &&
                condition.apply(criteria.getLastModifiedDate()) &&
                condition.apply(criteria.getOutcomeId()) &&
                condition.apply(criteria.getParentId()) &&
                condition.apply(criteria.getOwnerId()) &&
                condition.apply(criteria.getInterviewId()) &&
                condition.apply(criteria.getTagId()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<OpportunityCriteria> copyFiltersAre(OpportunityCriteria copy, BiFunction<Object, Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getTitle(), copy.getTitle()) &&
                condition.apply(criteria.getStatus(), copy.getStatus()) &&
                condition.apply(criteria.getValue(), copy.getValue()) &&
                condition.apply(criteria.getComplexity(), copy.getComplexity()) &&
                condition.apply(criteria.getSortOrder(), copy.getSortOrder()) &&
                condition.apply(criteria.getCreatedDate(), copy.getCreatedDate()) &&
                condition.apply(criteria.getLastModifiedDate(), copy.getLastModifiedDate()) &&
                condition.apply(criteria.getOutcomeId(), copy.getOutcomeId()) &&
                condition.apply(criteria.getParentId(), copy.getParentId()) &&
                condition.apply(criteria.getOwnerId(), copy.getOwnerId()) &&
                condition.apply(criteria.getInterviewId(), copy.getInterviewId()) &&
                condition.apply(criteria.getTagId(), copy.getTagId()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
