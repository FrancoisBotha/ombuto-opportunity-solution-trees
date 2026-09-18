package com.opportunity.tree.domain;

import static com.opportunity.tree.domain.InterviewTestSamples.*;
import static com.opportunity.tree.domain.OpportunityTestSamples.*;
import static com.opportunity.tree.domain.ProductTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.opportunity.tree.web.rest.TestUtil;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class InterviewTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Interview.class);
        Interview interview1 = getInterviewSample1();
        Interview interview2 = new Interview();
        assertThat(interview1).isNotEqualTo(interview2);

        interview2.setId(interview1.getId());
        assertThat(interview1).isEqualTo(interview2);

        interview2 = getInterviewSample2();
        assertThat(interview1).isNotEqualTo(interview2);
    }

    @Test
    void productTest() {
        Interview interview = getInterviewRandomSampleGenerator();
        Product productBack = getProductRandomSampleGenerator();

        interview.setProduct(productBack);
        assertThat(interview.getProduct()).isEqualTo(productBack);

        interview.product(null);
        assertThat(interview.getProduct()).isNull();
    }

    @Test
    void opportunityTest() {
        Interview interview = getInterviewRandomSampleGenerator();
        Opportunity opportunityBack = getOpportunityRandomSampleGenerator();

        interview.addOpportunity(opportunityBack);
        assertThat(interview.getOpportunities()).containsOnly(opportunityBack);
        assertThat(opportunityBack.getInterviews()).containsOnly(interview);

        interview.removeOpportunity(opportunityBack);
        assertThat(interview.getOpportunities()).doesNotContain(opportunityBack);
        assertThat(opportunityBack.getInterviews()).doesNotContain(interview);

        interview.opportunities(new HashSet<>(Set.of(opportunityBack)));
        assertThat(interview.getOpportunities()).containsOnly(opportunityBack);
        assertThat(opportunityBack.getInterviews()).containsOnly(interview);

        interview.setOpportunities(new HashSet<>());
        assertThat(interview.getOpportunities()).doesNotContain(opportunityBack);
        assertThat(opportunityBack.getInterviews()).doesNotContain(interview);
    }
}
