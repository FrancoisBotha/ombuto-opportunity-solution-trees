package com.opportunity.tree.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class OutcomeCriteriaTest {

    @Test
    void newOutcomeCriteriaHasAllFiltersNullTest() {
        var outcomeCriteria = new OutcomeCriteria();
        assertThat(outcomeCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void outcomeCriteriaFluentMethodsCreatesFiltersTest() {
        var outcomeCriteria = new OutcomeCriteria();

        setAllFilters(outcomeCriteria);

        assertThat(outcomeCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void outcomeCriteriaCopyCreatesNullFilterTest() {
        var outcomeCriteria = new OutcomeCriteria();
        var copy = outcomeCriteria.copy();

        assertThat(outcomeCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(outcomeCriteria)
        );
    }

    @Test
    void outcomeCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var outcomeCriteria = new OutcomeCriteria();
        setAllFilters(outcomeCriteria);

        var copy = outcomeCriteria.copy();

        assertThat(outcomeCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(
                    copyFiltersAre(copy, (a, b) -> (a == null || a instanceof Boolean) ? a == b : (a != b && a.equals(b)))
                ),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(outcomeCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var outcomeCriteria = new OutcomeCriteria();

        assertThat(outcomeCriteria).hasToString("OutcomeCriteria{}");
    }

    private static void setAllFilters(OutcomeCriteria outcomeCriteria) {
        outcomeCriteria.id();
        outcomeCriteria.title();
        outcomeCriteria.metric();
        outcomeCriteria.targetValue();
        outcomeCriteria.currentValue();
        outcomeCriteria.status();
        outcomeCriteria.startDate();
        outcomeCriteria.targetDate();
        outcomeCriteria.sortOrder();
        outcomeCriteria.createdDate();
        outcomeCriteria.lastModifiedDate();
        outcomeCriteria.productId();
        outcomeCriteria.ownerId();
        outcomeCriteria.distinct();
    }

    private static Condition<OutcomeCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getTitle()) &&
                condition.apply(criteria.getMetric()) &&
                condition.apply(criteria.getTargetValue()) &&
                condition.apply(criteria.getCurrentValue()) &&
                condition.apply(criteria.getStatus()) &&
                condition.apply(criteria.getStartDate()) &&
                condition.apply(criteria.getTargetDate()) &&
                condition.apply(criteria.getSortOrder()) &&
                condition.apply(criteria.getCreatedDate()) &&
                condition.apply(criteria.getLastModifiedDate()) &&
                condition.apply(criteria.getProductId()) &&
                condition.apply(criteria.getOwnerId()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<OutcomeCriteria> copyFiltersAre(OutcomeCriteria copy, BiFunction<Object, Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getTitle(), copy.getTitle()) &&
                condition.apply(criteria.getMetric(), copy.getMetric()) &&
                condition.apply(criteria.getTargetValue(), copy.getTargetValue()) &&
                condition.apply(criteria.getCurrentValue(), copy.getCurrentValue()) &&
                condition.apply(criteria.getStatus(), copy.getStatus()) &&
                condition.apply(criteria.getStartDate(), copy.getStartDate()) &&
                condition.apply(criteria.getTargetDate(), copy.getTargetDate()) &&
                condition.apply(criteria.getSortOrder(), copy.getSortOrder()) &&
                condition.apply(criteria.getCreatedDate(), copy.getCreatedDate()) &&
                condition.apply(criteria.getLastModifiedDate(), copy.getLastModifiedDate()) &&
                condition.apply(criteria.getProductId(), copy.getProductId()) &&
                condition.apply(criteria.getOwnerId(), copy.getOwnerId()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
