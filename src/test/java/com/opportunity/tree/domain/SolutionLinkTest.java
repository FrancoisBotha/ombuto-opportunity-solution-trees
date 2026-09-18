package com.opportunity.tree.domain;

import static com.opportunity.tree.domain.SolutionLinkTestSamples.*;
import static com.opportunity.tree.domain.SolutionTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.opportunity.tree.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class SolutionLinkTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(SolutionLink.class);
        SolutionLink solutionLink1 = getSolutionLinkSample1();
        SolutionLink solutionLink2 = new SolutionLink();
        assertThat(solutionLink1).isNotEqualTo(solutionLink2);

        solutionLink2.setId(solutionLink1.getId());
        assertThat(solutionLink1).isEqualTo(solutionLink2);

        solutionLink2 = getSolutionLinkSample2();
        assertThat(solutionLink1).isNotEqualTo(solutionLink2);
    }

    @Test
    void solutionTest() {
        SolutionLink solutionLink = getSolutionLinkRandomSampleGenerator();
        Solution solutionBack = getSolutionRandomSampleGenerator();

        solutionLink.setSolution(solutionBack);
        assertThat(solutionLink.getSolution()).isEqualTo(solutionBack);

        solutionLink.solution(null);
        assertThat(solutionLink.getSolution()).isNull();
    }
}
