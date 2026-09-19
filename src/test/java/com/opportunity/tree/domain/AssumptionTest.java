package com.opportunity.tree.domain;

import static com.opportunity.tree.domain.AssumptionTestSamples.*;
import static com.opportunity.tree.domain.SolutionTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.opportunity.tree.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class AssumptionTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Assumption.class);
        Assumption assumption1 = getAssumptionSample1();
        Assumption assumption2 = new Assumption();
        assertThat(assumption1).isNotEqualTo(assumption2);

        assumption2.setId(assumption1.getId());
        assertThat(assumption1).isEqualTo(assumption2);

        assumption2 = getAssumptionSample2();
        assertThat(assumption1).isNotEqualTo(assumption2);
    }

    @Test
    void solutionTest() {
        Assumption assumption = getAssumptionRandomSampleGenerator();
        Solution solutionBack = getSolutionRandomSampleGenerator();

        assumption.setSolution(solutionBack);
        assertThat(assumption.getSolution()).isEqualTo(solutionBack);

        assumption.solution(null);
        assertThat(assumption.getSolution()).isNull();
    }
}
