package com.opportunity.tree.domain;

import static com.opportunity.tree.domain.OpportunityTestSamples.*;
import static com.opportunity.tree.domain.SolutionTestSamples.*;
import static com.opportunity.tree.domain.TagTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.opportunity.tree.web.rest.TestUtil;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SolutionTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Solution.class);
        Solution solution1 = getSolutionSample1();
        Solution solution2 = new Solution();
        assertThat(solution1).isNotEqualTo(solution2);

        solution2.setId(solution1.getId());
        assertThat(solution1).isEqualTo(solution2);

        solution2 = getSolutionSample2();
        assertThat(solution1).isNotEqualTo(solution2);
    }

    @Test
    void opportunityTest() {
        Solution solution = getSolutionRandomSampleGenerator();
        Opportunity opportunityBack = getOpportunityRandomSampleGenerator();

        solution.setOpportunity(opportunityBack);
        assertThat(solution.getOpportunity()).isEqualTo(opportunityBack);

        solution.opportunity(null);
        assertThat(solution.getOpportunity()).isNull();
    }

    @Test
    void tagTest() {
        Solution solution = getSolutionRandomSampleGenerator();
        Tag tagBack = getTagRandomSampleGenerator();

        solution.addTag(tagBack);
        assertThat(solution.getTags()).containsOnly(tagBack);

        solution.removeTag(tagBack);
        assertThat(solution.getTags()).doesNotContain(tagBack);

        solution.tags(new HashSet<>(Set.of(tagBack)));
        assertThat(solution.getTags()).containsOnly(tagBack);

        solution.setTags(new HashSet<>());
        assertThat(solution.getTags()).doesNotContain(tagBack);
    }
}
