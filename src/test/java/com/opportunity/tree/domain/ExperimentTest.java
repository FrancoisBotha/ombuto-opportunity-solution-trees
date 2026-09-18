package com.opportunity.tree.domain;

import static com.opportunity.tree.domain.AssumptionTestSamples.*;
import static com.opportunity.tree.domain.ExperimentTestSamples.*;
import static com.opportunity.tree.domain.SolutionTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.opportunity.tree.web.rest.TestUtil;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ExperimentTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Experiment.class);
        Experiment experiment1 = getExperimentSample1();
        Experiment experiment2 = new Experiment();
        assertThat(experiment1).isNotEqualTo(experiment2);

        experiment2.setId(experiment1.getId());
        assertThat(experiment1).isEqualTo(experiment2);

        experiment2 = getExperimentSample2();
        assertThat(experiment1).isNotEqualTo(experiment2);
    }

    @Test
    void solutionTest() {
        Experiment experiment = getExperimentRandomSampleGenerator();
        Solution solutionBack = getSolutionRandomSampleGenerator();

        experiment.setSolution(solutionBack);
        assertThat(experiment.getSolution()).isEqualTo(solutionBack);

        experiment.solution(null);
        assertThat(experiment.getSolution()).isNull();
    }

    @Test
    void assumptionTest() {
        Experiment experiment = getExperimentRandomSampleGenerator();
        Assumption assumptionBack = getAssumptionRandomSampleGenerator();

        experiment.addAssumption(assumptionBack);
        assertThat(experiment.getAssumptions()).containsOnly(assumptionBack);

        experiment.removeAssumption(assumptionBack);
        assertThat(experiment.getAssumptions()).doesNotContain(assumptionBack);

        experiment.assumptions(new HashSet<>(Set.of(assumptionBack)));
        assertThat(experiment.getAssumptions()).containsOnly(assumptionBack);

        experiment.setAssumptions(new HashSet<>());
        assertThat(experiment.getAssumptions()).doesNotContain(assumptionBack);
    }
}
