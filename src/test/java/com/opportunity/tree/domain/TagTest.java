package com.opportunity.tree.domain;

import static com.opportunity.tree.domain.OpportunityTestSamples.*;
import static com.opportunity.tree.domain.SolutionTestSamples.*;
import static com.opportunity.tree.domain.TagTestSamples.*;
import static com.opportunity.tree.domain.TeamTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.opportunity.tree.web.rest.TestUtil;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TagTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Tag.class);
        Tag tag1 = getTagSample1();
        Tag tag2 = new Tag();
        assertThat(tag1).isNotEqualTo(tag2);

        tag2.setId(tag1.getId());
        assertThat(tag1).isEqualTo(tag2);

        tag2 = getTagSample2();
        assertThat(tag1).isNotEqualTo(tag2);
    }

    @Test
    void teamTest() {
        Tag tag = getTagRandomSampleGenerator();
        Team teamBack = getTeamRandomSampleGenerator();

        tag.setTeam(teamBack);
        assertThat(tag.getTeam()).isEqualTo(teamBack);

        tag.team(null);
        assertThat(tag.getTeam()).isNull();
    }

    @Test
    void opportunityTest() {
        Tag tag = getTagRandomSampleGenerator();
        Opportunity opportunityBack = getOpportunityRandomSampleGenerator();

        tag.addOpportunity(opportunityBack);
        assertThat(tag.getOpportunities()).containsOnly(opportunityBack);
        assertThat(opportunityBack.getTags()).containsOnly(tag);

        tag.removeOpportunity(opportunityBack);
        assertThat(tag.getOpportunities()).doesNotContain(opportunityBack);
        assertThat(opportunityBack.getTags()).doesNotContain(tag);

        tag.opportunities(new HashSet<>(Set.of(opportunityBack)));
        assertThat(tag.getOpportunities()).containsOnly(opportunityBack);
        assertThat(opportunityBack.getTags()).containsOnly(tag);

        tag.setOpportunities(new HashSet<>());
        assertThat(tag.getOpportunities()).doesNotContain(opportunityBack);
        assertThat(opportunityBack.getTags()).doesNotContain(tag);
    }

    @Test
    void solutionTest() {
        Tag tag = getTagRandomSampleGenerator();
        Solution solutionBack = getSolutionRandomSampleGenerator();

        tag.addSolution(solutionBack);
        assertThat(tag.getSolutions()).containsOnly(solutionBack);
        assertThat(solutionBack.getTags()).containsOnly(tag);

        tag.removeSolution(solutionBack);
        assertThat(tag.getSolutions()).doesNotContain(solutionBack);
        assertThat(solutionBack.getTags()).doesNotContain(tag);

        tag.solutions(new HashSet<>(Set.of(solutionBack)));
        assertThat(tag.getSolutions()).containsOnly(solutionBack);
        assertThat(solutionBack.getTags()).containsOnly(tag);

        tag.setSolutions(new HashSet<>());
        assertThat(tag.getSolutions()).doesNotContain(solutionBack);
        assertThat(solutionBack.getTags()).doesNotContain(tag);
    }
}
