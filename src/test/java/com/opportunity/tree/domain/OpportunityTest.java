package com.opportunity.tree.domain;

import static com.opportunity.tree.domain.InterviewTestSamples.*;
import static com.opportunity.tree.domain.OpportunityTestSamples.*;
import static com.opportunity.tree.domain.OpportunityTestSamples.*;
import static com.opportunity.tree.domain.OutcomeTestSamples.*;
import static com.opportunity.tree.domain.TagTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.opportunity.tree.web.rest.TestUtil;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class OpportunityTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Opportunity.class);
        Opportunity opportunity1 = getOpportunitySample1();
        Opportunity opportunity2 = new Opportunity();
        assertThat(opportunity1).isNotEqualTo(opportunity2);

        opportunity2.setId(opportunity1.getId());
        assertThat(opportunity1).isEqualTo(opportunity2);

        opportunity2 = getOpportunitySample2();
        assertThat(opportunity1).isNotEqualTo(opportunity2);
    }

    @Test
    void outcomeTest() {
        Opportunity opportunity = getOpportunityRandomSampleGenerator();
        Outcome outcomeBack = getOutcomeRandomSampleGenerator();

        opportunity.setOutcome(outcomeBack);
        assertThat(opportunity.getOutcome()).isEqualTo(outcomeBack);

        opportunity.outcome(null);
        assertThat(opportunity.getOutcome()).isNull();
    }

    @Test
    void parentTest() {
        Opportunity opportunity = getOpportunityRandomSampleGenerator();
        Opportunity opportunityBack = getOpportunityRandomSampleGenerator();

        opportunity.setParent(opportunityBack);
        assertThat(opportunity.getParent()).isEqualTo(opportunityBack);

        opportunity.parent(null);
        assertThat(opportunity.getParent()).isNull();
    }

    @Test
    void interviewTest() {
        Opportunity opportunity = getOpportunityRandomSampleGenerator();
        Interview interviewBack = getInterviewRandomSampleGenerator();

        opportunity.addInterview(interviewBack);
        assertThat(opportunity.getInterviews()).containsOnly(interviewBack);

        opportunity.removeInterview(interviewBack);
        assertThat(opportunity.getInterviews()).doesNotContain(interviewBack);

        opportunity.interviews(new HashSet<>(Set.of(interviewBack)));
        assertThat(opportunity.getInterviews()).containsOnly(interviewBack);

        opportunity.setInterviews(new HashSet<>());
        assertThat(opportunity.getInterviews()).doesNotContain(interviewBack);
    }

    @Test
    void tagTest() {
        Opportunity opportunity = getOpportunityRandomSampleGenerator();
        Tag tagBack = getTagRandomSampleGenerator();

        opportunity.addTag(tagBack);
        assertThat(opportunity.getTags()).containsOnly(tagBack);

        opportunity.removeTag(tagBack);
        assertThat(opportunity.getTags()).doesNotContain(tagBack);

        opportunity.tags(new HashSet<>(Set.of(tagBack)));
        assertThat(opportunity.getTags()).containsOnly(tagBack);

        opportunity.setTags(new HashSet<>());
        assertThat(opportunity.getTags()).doesNotContain(tagBack);
    }
}
