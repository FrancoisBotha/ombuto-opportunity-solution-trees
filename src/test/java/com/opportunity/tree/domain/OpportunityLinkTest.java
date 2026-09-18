package com.opportunity.tree.domain;

import static com.opportunity.tree.domain.OpportunityLinkTestSamples.*;
import static com.opportunity.tree.domain.OpportunityTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.opportunity.tree.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class OpportunityLinkTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(OpportunityLink.class);
        OpportunityLink opportunityLink1 = getOpportunityLinkSample1();
        OpportunityLink opportunityLink2 = new OpportunityLink();
        assertThat(opportunityLink1).isNotEqualTo(opportunityLink2);

        opportunityLink2.setId(opportunityLink1.getId());
        assertThat(opportunityLink1).isEqualTo(opportunityLink2);

        opportunityLink2 = getOpportunityLinkSample2();
        assertThat(opportunityLink1).isNotEqualTo(opportunityLink2);
    }

    @Test
    void opportunityTest() {
        OpportunityLink opportunityLink = getOpportunityLinkRandomSampleGenerator();
        Opportunity opportunityBack = getOpportunityRandomSampleGenerator();

        opportunityLink.setOpportunity(opportunityBack);
        assertThat(opportunityLink.getOpportunity()).isEqualTo(opportunityBack);

        opportunityLink.opportunity(null);
        assertThat(opportunityLink.getOpportunity()).isNull();
    }
}
