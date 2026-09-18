package com.opportunity.tree.domain;

import static com.opportunity.tree.domain.OutcomeTestSamples.*;
import static com.opportunity.tree.domain.ProductTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.opportunity.tree.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class OutcomeTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Outcome.class);
        Outcome outcome1 = getOutcomeSample1();
        Outcome outcome2 = new Outcome();
        assertThat(outcome1).isNotEqualTo(outcome2);

        outcome2.setId(outcome1.getId());
        assertThat(outcome1).isEqualTo(outcome2);

        outcome2 = getOutcomeSample2();
        assertThat(outcome1).isNotEqualTo(outcome2);
    }

    @Test
    void productTest() {
        Outcome outcome = getOutcomeRandomSampleGenerator();
        Product productBack = getProductRandomSampleGenerator();

        outcome.setProduct(productBack);
        assertThat(outcome.getProduct()).isEqualTo(productBack);

        outcome.product(null);
        assertThat(outcome.getProduct()).isNull();
    }
}
