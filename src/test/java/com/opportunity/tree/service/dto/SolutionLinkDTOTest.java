package com.opportunity.tree.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.opportunity.tree.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class SolutionLinkDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(SolutionLinkDTO.class);
        SolutionLinkDTO solutionLinkDTO1 = new SolutionLinkDTO();
        solutionLinkDTO1.setId(1L);
        SolutionLinkDTO solutionLinkDTO2 = new SolutionLinkDTO();
        assertThat(solutionLinkDTO1).isNotEqualTo(solutionLinkDTO2);
        solutionLinkDTO2.setId(solutionLinkDTO1.getId());
        assertThat(solutionLinkDTO1).isEqualTo(solutionLinkDTO2);
        solutionLinkDTO2.setId(2L);
        assertThat(solutionLinkDTO1).isNotEqualTo(solutionLinkDTO2);
        solutionLinkDTO1.setId(null);
        assertThat(solutionLinkDTO1).isNotEqualTo(solutionLinkDTO2);
    }
}
