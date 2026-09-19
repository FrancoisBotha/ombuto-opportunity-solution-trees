package com.opportunity.tree.domain;

import static com.opportunity.tree.domain.AssumptionTestSamples.*;
import static com.opportunity.tree.domain.EvidenceTestSamples.*;
import static com.opportunity.tree.domain.OpportunityTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.opportunity.tree.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class EvidenceTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Evidence.class);
        Evidence evidence1 = getEvidenceSample1();
        Evidence evidence2 = new Evidence();
        assertThat(evidence1).isNotEqualTo(evidence2);

        evidence2.setId(evidence1.getId());
        assertThat(evidence1).isEqualTo(evidence2);

        evidence2 = getEvidenceSample2();
        assertThat(evidence1).isNotEqualTo(evidence2);
    }

    @Test
    void opportunityTest() {
        Evidence evidence = getEvidenceRandomSampleGenerator();
        Opportunity opportunityBack = getOpportunityRandomSampleGenerator();

        evidence.setOpportunity(opportunityBack);
        assertThat(evidence.getOpportunity()).isEqualTo(opportunityBack);

        evidence.opportunity(null);
        assertThat(evidence.getOpportunity()).isNull();
    }

    @Test
    void assumptionTest() {
        Evidence evidence = getEvidenceRandomSampleGenerator();
        Assumption assumptionBack = getAssumptionRandomSampleGenerator();

        evidence.setAssumption(assumptionBack);
        assertThat(evidence.getAssumption()).isEqualTo(assumptionBack);

        evidence.assumption(null);
        assertThat(evidence.getAssumption()).isNull();
    }
}
