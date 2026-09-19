package com.opportunity.tree.domain;

import static com.opportunity.tree.domain.AssumptionTestSamples.*;
import static com.opportunity.tree.domain.EvidenceTestSamples.*;
import static com.opportunity.tree.domain.NodeLinkTestSamples.*;
import static com.opportunity.tree.domain.OpportunityTestSamples.*;
import static com.opportunity.tree.domain.OutcomeTestSamples.*;
import static com.opportunity.tree.domain.ProductTestSamples.*;
import static com.opportunity.tree.domain.SolutionTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.opportunity.tree.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class NodeLinkTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(NodeLink.class);
        NodeLink nodeLink1 = getNodeLinkSample1();
        NodeLink nodeLink2 = new NodeLink();
        assertThat(nodeLink1).isNotEqualTo(nodeLink2);

        nodeLink2.setId(nodeLink1.getId());
        assertThat(nodeLink1).isEqualTo(nodeLink2);

        nodeLink2 = getNodeLinkSample2();
        assertThat(nodeLink1).isNotEqualTo(nodeLink2);
    }

    @Test
    void productTest() {
        NodeLink nodeLink = getNodeLinkRandomSampleGenerator();
        Product productBack = getProductRandomSampleGenerator();

        nodeLink.setProduct(productBack);
        assertThat(nodeLink.getProduct()).isEqualTo(productBack);

        nodeLink.product(null);
        assertThat(nodeLink.getProduct()).isNull();
    }

    @Test
    void outcomeTest() {
        NodeLink nodeLink = getNodeLinkRandomSampleGenerator();
        Outcome outcomeBack = getOutcomeRandomSampleGenerator();

        nodeLink.setOutcome(outcomeBack);
        assertThat(nodeLink.getOutcome()).isEqualTo(outcomeBack);

        nodeLink.outcome(null);
        assertThat(nodeLink.getOutcome()).isNull();
    }

    @Test
    void opportunityTest() {
        NodeLink nodeLink = getNodeLinkRandomSampleGenerator();
        Opportunity opportunityBack = getOpportunityRandomSampleGenerator();

        nodeLink.setOpportunity(opportunityBack);
        assertThat(nodeLink.getOpportunity()).isEqualTo(opportunityBack);

        nodeLink.opportunity(null);
        assertThat(nodeLink.getOpportunity()).isNull();
    }

    @Test
    void solutionTest() {
        NodeLink nodeLink = getNodeLinkRandomSampleGenerator();
        Solution solutionBack = getSolutionRandomSampleGenerator();

        nodeLink.setSolution(solutionBack);
        assertThat(nodeLink.getSolution()).isEqualTo(solutionBack);

        nodeLink.solution(null);
        assertThat(nodeLink.getSolution()).isNull();
    }

    @Test
    void assumptionTest() {
        NodeLink nodeLink = getNodeLinkRandomSampleGenerator();
        Assumption assumptionBack = getAssumptionRandomSampleGenerator();

        nodeLink.setAssumption(assumptionBack);
        assertThat(nodeLink.getAssumption()).isEqualTo(assumptionBack);

        nodeLink.assumption(null);
        assertThat(nodeLink.getAssumption()).isNull();
    }

    @Test
    void evidenceTest() {
        NodeLink nodeLink = getNodeLinkRandomSampleGenerator();
        Evidence evidenceBack = getEvidenceRandomSampleGenerator();

        nodeLink.setEvidence(evidenceBack);
        assertThat(nodeLink.getEvidence()).isEqualTo(evidenceBack);

        nodeLink.evidence(null);
        assertThat(nodeLink.getEvidence()).isNull();
    }
}
