package com.opportunity.tree.domain;

import static com.opportunity.tree.domain.AssumptionTestSamples.*;
import static com.opportunity.tree.domain.EvidenceTestSamples.*;
import static com.opportunity.tree.domain.MeetingTranscriptTestSamples.*;
import static com.opportunity.tree.domain.OpportunityTestSamples.*;
import static com.opportunity.tree.domain.OutcomeTestSamples.*;
import static com.opportunity.tree.domain.ProductTestSamples.*;
import static com.opportunity.tree.domain.SolutionTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.opportunity.tree.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class MeetingTranscriptTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(MeetingTranscript.class);
        MeetingTranscript meetingTranscript1 = getMeetingTranscriptSample1();
        MeetingTranscript meetingTranscript2 = new MeetingTranscript();
        assertThat(meetingTranscript1).isNotEqualTo(meetingTranscript2);

        meetingTranscript2.setId(meetingTranscript1.getId());
        assertThat(meetingTranscript1).isEqualTo(meetingTranscript2);

        meetingTranscript2 = getMeetingTranscriptSample2();
        assertThat(meetingTranscript1).isNotEqualTo(meetingTranscript2);
    }

    @Test
    void productTest() {
        MeetingTranscript meetingTranscript = getMeetingTranscriptRandomSampleGenerator();
        Product productBack = getProductRandomSampleGenerator();

        meetingTranscript.setProduct(productBack);
        assertThat(meetingTranscript.getProduct()).isEqualTo(productBack);

        meetingTranscript.product(null);
        assertThat(meetingTranscript.getProduct()).isNull();
    }

    @Test
    void outcomeTest() {
        MeetingTranscript meetingTranscript = getMeetingTranscriptRandomSampleGenerator();
        Outcome outcomeBack = getOutcomeRandomSampleGenerator();

        meetingTranscript.setOutcome(outcomeBack);
        assertThat(meetingTranscript.getOutcome()).isEqualTo(outcomeBack);

        meetingTranscript.outcome(null);
        assertThat(meetingTranscript.getOutcome()).isNull();
    }

    @Test
    void opportunityTest() {
        MeetingTranscript meetingTranscript = getMeetingTranscriptRandomSampleGenerator();
        Opportunity opportunityBack = getOpportunityRandomSampleGenerator();

        meetingTranscript.setOpportunity(opportunityBack);
        assertThat(meetingTranscript.getOpportunity()).isEqualTo(opportunityBack);

        meetingTranscript.opportunity(null);
        assertThat(meetingTranscript.getOpportunity()).isNull();
    }

    @Test
    void solutionTest() {
        MeetingTranscript meetingTranscript = getMeetingTranscriptRandomSampleGenerator();
        Solution solutionBack = getSolutionRandomSampleGenerator();

        meetingTranscript.setSolution(solutionBack);
        assertThat(meetingTranscript.getSolution()).isEqualTo(solutionBack);

        meetingTranscript.solution(null);
        assertThat(meetingTranscript.getSolution()).isNull();
    }

    @Test
    void assumptionTest() {
        MeetingTranscript meetingTranscript = getMeetingTranscriptRandomSampleGenerator();
        Assumption assumptionBack = getAssumptionRandomSampleGenerator();

        meetingTranscript.setAssumption(assumptionBack);
        assertThat(meetingTranscript.getAssumption()).isEqualTo(assumptionBack);

        meetingTranscript.assumption(null);
        assertThat(meetingTranscript.getAssumption()).isNull();
    }

    @Test
    void evidenceTest() {
        MeetingTranscript meetingTranscript = getMeetingTranscriptRandomSampleGenerator();
        Evidence evidenceBack = getEvidenceRandomSampleGenerator();

        meetingTranscript.setEvidence(evidenceBack);
        assertThat(meetingTranscript.getEvidence()).isEqualTo(evidenceBack);

        meetingTranscript.evidence(null);
        assertThat(meetingTranscript.getEvidence()).isNull();
    }
}
