package com.opportunity.tree.domain;

import static com.opportunity.tree.domain.OpenQuestionTestSamples.*;
import static com.opportunity.tree.domain.OpportunityTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.opportunity.tree.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class OpenQuestionTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(OpenQuestion.class);
        OpenQuestion openQuestion1 = getOpenQuestionSample1();
        OpenQuestion openQuestion2 = new OpenQuestion();
        assertThat(openQuestion1).isNotEqualTo(openQuestion2);

        openQuestion2.setId(openQuestion1.getId());
        assertThat(openQuestion1).isEqualTo(openQuestion2);

        openQuestion2 = getOpenQuestionSample2();
        assertThat(openQuestion1).isNotEqualTo(openQuestion2);
    }

    @Test
    void opportunityTest() {
        OpenQuestion openQuestion = getOpenQuestionRandomSampleGenerator();
        Opportunity opportunityBack = getOpportunityRandomSampleGenerator();

        openQuestion.setOpportunity(opportunityBack);
        assertThat(openQuestion.getOpportunity()).isEqualTo(opportunityBack);

        openQuestion.opportunity(null);
        assertThat(openQuestion.getOpportunity()).isNull();
    }
}
