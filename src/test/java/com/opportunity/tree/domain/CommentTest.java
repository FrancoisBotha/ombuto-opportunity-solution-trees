package com.opportunity.tree.domain;

import static com.opportunity.tree.domain.AssumptionTestSamples.*;
import static com.opportunity.tree.domain.CommentTestSamples.*;
import static com.opportunity.tree.domain.CommentTestSamples.*;
import static com.opportunity.tree.domain.EvidenceTestSamples.*;
import static com.opportunity.tree.domain.OpportunityTestSamples.*;
import static com.opportunity.tree.domain.OutcomeTestSamples.*;
import static com.opportunity.tree.domain.SolutionTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.opportunity.tree.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class CommentTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Comment.class);
        Comment comment1 = getCommentSample1();
        Comment comment2 = new Comment();
        assertThat(comment1).isNotEqualTo(comment2);

        comment2.setId(comment1.getId());
        assertThat(comment1).isEqualTo(comment2);

        comment2 = getCommentSample2();
        assertThat(comment1).isNotEqualTo(comment2);
    }

    @Test
    void parentTest() {
        Comment comment = getCommentRandomSampleGenerator();
        Comment commentBack = getCommentRandomSampleGenerator();

        comment.setParent(commentBack);
        assertThat(comment.getParent()).isEqualTo(commentBack);

        comment.parent(null);
        assertThat(comment.getParent()).isNull();
    }

    @Test
    void outcomeTest() {
        Comment comment = getCommentRandomSampleGenerator();
        Outcome outcomeBack = getOutcomeRandomSampleGenerator();

        comment.setOutcome(outcomeBack);
        assertThat(comment.getOutcome()).isEqualTo(outcomeBack);

        comment.outcome(null);
        assertThat(comment.getOutcome()).isNull();
    }

    @Test
    void opportunityTest() {
        Comment comment = getCommentRandomSampleGenerator();
        Opportunity opportunityBack = getOpportunityRandomSampleGenerator();

        comment.setOpportunity(opportunityBack);
        assertThat(comment.getOpportunity()).isEqualTo(opportunityBack);

        comment.opportunity(null);
        assertThat(comment.getOpportunity()).isNull();
    }

    @Test
    void solutionTest() {
        Comment comment = getCommentRandomSampleGenerator();
        Solution solutionBack = getSolutionRandomSampleGenerator();

        comment.setSolution(solutionBack);
        assertThat(comment.getSolution()).isEqualTo(solutionBack);

        comment.solution(null);
        assertThat(comment.getSolution()).isNull();
    }

    @Test
    void assumptionTest() {
        Comment comment = getCommentRandomSampleGenerator();
        Assumption assumptionBack = getAssumptionRandomSampleGenerator();

        comment.setAssumption(assumptionBack);
        assertThat(comment.getAssumption()).isEqualTo(assumptionBack);

        comment.assumption(null);
        assertThat(comment.getAssumption()).isNull();
    }

    @Test
    void evidenceTest() {
        Comment comment = getCommentRandomSampleGenerator();
        Evidence evidenceBack = getEvidenceRandomSampleGenerator();

        comment.setEvidence(evidenceBack);
        assertThat(comment.getEvidence()).isEqualTo(evidenceBack);

        comment.evidence(null);
        assertThat(comment.getEvidence()).isNull();
    }
}
